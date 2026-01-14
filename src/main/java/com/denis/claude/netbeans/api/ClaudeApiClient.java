package com.denis.claude.netbeans.api;

import com.denis.claude.netbeans.settings.ClaudeSettings;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Client pour Claude Code CLI.
 * Utilise le processus claude en sous-processus pour exploiter l'abonnement Max.
 */
public class ClaudeApiClient {

    private static final int TIMEOUT_SECONDS = 120;

    private static ClaudeApiClient instance;
    private final ExecutorService executor;
    private final List<Message> conversationHistory;

    private ClaudeApiClient() {
        executor = Executors.newCachedThreadPool();
        conversationHistory = new ArrayList<>();
    }

    public static synchronized ClaudeApiClient getInstance() {
        if (instance == null) {
            instance = new ClaudeApiClient();
        }
        return instance;
    }

    public void reinitialize() {
        // Rien à réinitialiser
    }

    public boolean isReady() {
        return ClaudeSettings.getInstance().isConfigured();
    }

    public CompletableFuture<String> sendMessage(String userMessage) {
        return sendMessage(userMessage, null);
    }

    public CompletableFuture<String> sendMessage(String userMessage, String systemPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isReady()) {
                throw new IllegalStateException("Claude Code non configuré. Vérifiez le chemin dans les paramètres.");
            }

            try {
                // Envoyer directement le message (Claude Code gère sa propre session)
                String response = callClaude(userMessage);

                // Ajouter à l'historique local pour référence
                conversationHistory.add(new Message("user", userMessage));
                conversationHistory.add(new Message("assistant", response));

                return response;
            } catch (Exception e) {
                throw new RuntimeException("Erreur Claude Code: " + e.getMessage(), e);
            }
        }, executor);
    }

    public CompletableFuture<String> analyzeCode(String code, String language, String instruction) {
        String prompt = String.format(
                "Voici du code %s à analyser:\n\n```%s\n%s\n```\n\n%s",
                language, language, code, instruction
        );
        return sendMessageWithoutHistory(prompt, "Tu es un assistant expert en programmation. Réponds en français.");
    }

    public CompletableFuture<String> generateCode(String description, String language) {
        String prompt = String.format(
                "Génère du code %s pour: %s\n\nRetourne uniquement le code, sans explications supplémentaires.",
                language, description
        );
        return sendMessageWithoutHistory(prompt, "Tu es un assistant expert en programmation. Génère du code propre et bien commenté.");
    }

    public CompletableFuture<String> sendMessageWithoutHistory(String userMessage, String systemPrompt) {
        return CompletableFuture.supplyAsync(() -> {
            if (!isReady()) {
                throw new IllegalStateException("Claude Code non configuré. Vérifiez le chemin dans les paramètres.");
            }

            StringBuilder fullPrompt = new StringBuilder();
            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                fullPrompt.append("[Instructions: ").append(systemPrompt).append("]\n\n");
            }
            fullPrompt.append(userMessage);

            try {
                return callClaude(fullPrompt.toString());
            } catch (Exception e) {
                throw new RuntimeException("Erreur Claude Code: " + e.getMessage(), e);
            }
        }, executor);
    }

    private String callClaude(String prompt) throws Exception {
        ClaudeSettings settings = ClaudeSettings.getInstance();
        String claudePath = settings.getClaudePath();

        // Construire la commande
        List<String> command = new ArrayList<>();
        command.add(claudePath);
        command.add("-p");  // Mode prompt unique (non-interactif)
        command.add("--output-format");
        command.add("text");  // Format texte simple
        command.add(prompt);

        ProcessBuilder pb = new ProcessBuilder(command);

        // IMPORTANT: Rediriger stdin depuis /dev/null pour éviter que claude attende une entrée
        pb.redirectInput(ProcessBuilder.Redirect.from(new File("/dev/null")));

        // Fusionner stderr dans stdout pour simplifier la lecture
        pb.redirectErrorStream(true);

        // Définir l'environnement nécessaire pour Claude Code
        String home = System.getProperty("user.home");
        pb.environment().put("HOME", home);
        pb.environment().put("USER", System.getProperty("user.name"));
        pb.environment().put("XDG_CONFIG_HOME", home + "/.config");

        // Éviter les problèmes de terminal/couleurs
        pb.environment().put("TERM", "dumb");
        pb.environment().put("NO_COLOR", "1");
        pb.environment().put("FORCE_COLOR", "0");

        // Hériter du PATH pour trouver les dépendances (node, etc.)
        String path = System.getenv("PATH");
        if (path != null) {
            pb.environment().put("PATH", path);
        } else {
            // PATH minimal si non disponible
            pb.environment().put("PATH", "/usr/local/bin:/usr/bin:/bin:" + home + "/.local/bin");
        }

        Process process = pb.start();

        // Lire la sortie dans un thread séparé pour éviter les deadlocks
        StringBuilder output = new StringBuilder();
        Thread readerThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (output.length() > 0) {
                        output.append("\n");
                    }
                    output.append(line);
                }
            } catch (Exception e) {
                output.append("\n[Erreur de lecture: ").append(e.getMessage()).append("]");
            }
        });
        readerThread.start();

        // Attendre avec timeout
        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("Timeout: Claude Code n'a pas répondu en " + TIMEOUT_SECONDS + " secondes");
        }

        // Attendre que le thread de lecture se termine
        readerThread.join(5000);

        int exitCode = process.exitValue();
        String result = output.toString().trim();

        if (exitCode != 0) {
            if (result.isEmpty()) {
                throw new RuntimeException("Claude Code a échoué avec le code " + exitCode);
            }
            // Si on a une sortie, c'est peut-être un message d'erreur utile
            if (result.toLowerCase().contains("not logged in") ||
                result.toLowerCase().contains("authentication") ||
                result.toLowerCase().contains("login")) {
                throw new RuntimeException("Non connecté: Exécutez 'claude' dans un terminal pour vous authentifier");
            }
            throw new RuntimeException("Erreur (code " + exitCode + "): " + result);
        }

        if (result.isEmpty()) {
            throw new RuntimeException("Claude Code n'a retourné aucune réponse");
        }

        return result;
    }

    public void clearHistory() {
        conversationHistory.clear();
    }

    public void shutdown() {
        executor.shutdown();
    }

    private static class Message {
        final String role;
        final String content;

        Message(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}
