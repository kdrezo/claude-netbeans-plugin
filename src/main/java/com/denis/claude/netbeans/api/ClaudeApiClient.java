package com.denis.claude.netbeans.api;

import com.denis.claude.netbeans.settings.ClaudeSettings;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Client pour l'API Claude d'Anthropic.
 * Utilise java.net.HttpURLConnection pour éviter les dépendances externes.
 */
public class ClaudeApiClient {

    private static final String API_URL = "https://api.anthropic.com/v1/messages";
    private static final String API_VERSION = "2023-06-01";

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
        // Rien à réinitialiser avec l'implémentation HTTP native
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
                throw new IllegalStateException("Client non configuré. Veuillez définir votre clé API.");
            }

            // Ajouter le message utilisateur à l'historique
            conversationHistory.add(new Message("user", userMessage));

            try {
                String response = callApi(conversationHistory, systemPrompt);

                // Ajouter la réponse à l'historique
                conversationHistory.add(new Message("assistant", response));

                return response;
            } catch (Exception e) {
                // Retirer le message de l'historique en cas d'erreur
                conversationHistory.remove(conversationHistory.size() - 1);
                throw new RuntimeException("Erreur API: " + e.getMessage(), e);
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
                throw new IllegalStateException("Client non configuré. Veuillez définir votre clé API.");
            }

            List<Message> messages = new ArrayList<>();
            messages.add(new Message("user", userMessage));

            try {
                return callApi(messages, systemPrompt);
            } catch (Exception e) {
                throw new RuntimeException("Erreur API: " + e.getMessage(), e);
            }
        }, executor);
    }

    private String callApi(List<Message> messages, String systemPrompt) throws Exception {
        ClaudeSettings settings = ClaudeSettings.getInstance();

        URL url = new URL(API_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", settings.getApiKey());
        conn.setRequestProperty("anthropic-version", API_VERSION);
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(120000);

        // Construire le JSON de la requête
        String requestBody = buildRequestJson(messages, systemPrompt, settings);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            throw new RuntimeException("Erreur HTTP " + responseCode + ": " + errorResponse.toString());
        }

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        return extractTextFromResponse(response.toString());
    }

    private String buildRequestJson(List<Message> messages, String systemPrompt, ClaudeSettings settings) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"model\":\"").append(escapeJson(settings.getModel())).append("\",");
        json.append("\"max_tokens\":").append(settings.getMaxTokens()).append(",");

        if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
            json.append("\"system\":\"").append(escapeJson(systemPrompt)).append("\",");
        }

        json.append("\"messages\":[");
        for (int i = 0; i < messages.size(); i++) {
            Message msg = messages.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{\"role\":\"").append(msg.role).append("\",");
            json.append("\"content\":\"").append(escapeJson(msg.content)).append("\"}");
        }
        json.append("]");
        json.append("}");

        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractTextFromResponse(String jsonResponse) {
        // Extraction simple du texte de la réponse JSON
        // Format attendu: {"content":[{"type":"text","text":"..."}],...}
        Pattern pattern = Pattern.compile("\"text\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher matcher = pattern.matcher(jsonResponse);

        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String text = matcher.group(1);
            // Déséchapper le JSON
            text = text.replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
            result.append(text);
        }

        return result.toString();
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
