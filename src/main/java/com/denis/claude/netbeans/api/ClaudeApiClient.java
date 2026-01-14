package com.denis.claude.netbeans.api;

import com.denis.claude.netbeans.settings.ClaudeSettings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client pour Claude Code CLI.
 * Utilise le processus claude en sous-processus pour exploiter l'abonnement Max.
 */
public class ClaudeApiClient {

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

            // Construire le prompt avec l'historique pour simuler une conversation
            StringBuilder fullPrompt = new StringBuilder();

            if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                fullPrompt.append("[Instructions système: ").append(systemPrompt).append("]\n\n");
            }

            // Ajouter l'historique de conversation
            for (Message msg : conversationHistory) {
                if ("user".equals(msg.role)) {
                    fullPrompt.append("Utilisateur: ").append(msg.content).append("\n\n");
                } else {
                    fullPrompt.append("Assistant: ").append(msg.content).append("\n\n");
                }
            }

            // Ajouter le nouveau message
            fullPrompt.append("Utilisateur: ").append(userMessage);

            try {
                String response = callClaude(fullPrompt.toString());

                // Ajouter à l'historique
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
        command.add(prompt);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);

        // Définir l'environnement pour éviter les problèmes de terminal
        pb.environment().put("TERM", "dumb");
        pb.environment().put("NO_COLOR", "1");

        Process process = pb.start();

        // Lire la sortie standard
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() > 0) {
                    output.append("\n");
                }
                output.append(line);
            }
        }

        // Lire les erreurs
        StringBuilder errorOutput = new StringBuilder();
        try (BufferedReader errorReader = new BufferedReader(
                new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = errorReader.readLine()) != null) {
                if (errorOutput.length() > 0) {
                    errorOutput.append("\n");
                }
                errorOutput.append(line);
            }
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            String error = errorOutput.length() > 0 ? errorOutput.toString() : output.toString();
            throw new RuntimeException("Claude Code a retourné une erreur (code " + exitCode + "): " + error);
        }

        String result = output.toString().trim();
        if (result.isEmpty() && errorOutput.length() > 0) {
            throw new RuntimeException("Claude Code erreur: " + errorOutput.toString());
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
