package com.denis.claude.netbeans.settings;

import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Gestion des paramètres du plugin Claude.
 * Stocke la clé API et autres préférences utilisateur.
 */
public class ClaudeSettings {

    private static final String PREF_API_KEY = "apiKey";
    private static final String PREF_MODEL = "model";
    private static final String PREF_MAX_TOKENS = "maxTokens";
    private static final String PREF_TEMPERATURE = "temperature";

    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";
    private static final int DEFAULT_MAX_TOKENS = 4096;
    private static final double DEFAULT_TEMPERATURE = 0.7;

    private static ClaudeSettings instance;
    private final Preferences prefs;

    private ClaudeSettings() {
        prefs = NbPreferences.forModule(ClaudeSettings.class);
    }

    public static synchronized ClaudeSettings getInstance() {
        if (instance == null) {
            instance = new ClaudeSettings();
        }
        return instance;
    }

    public String getApiKey() {
        return prefs.get(PREF_API_KEY, "");
    }

    public void setApiKey(String apiKey) {
        prefs.put(PREF_API_KEY, apiKey);
    }

    public String getModel() {
        return prefs.get(PREF_MODEL, DEFAULT_MODEL);
    }

    public void setModel(String model) {
        prefs.put(PREF_MODEL, model);
    }

    public int getMaxTokens() {
        return prefs.getInt(PREF_MAX_TOKENS, DEFAULT_MAX_TOKENS);
    }

    public void setMaxTokens(int maxTokens) {
        prefs.putInt(PREF_MAX_TOKENS, maxTokens);
    }

    public double getTemperature() {
        return prefs.getDouble(PREF_TEMPERATURE, DEFAULT_TEMPERATURE);
    }

    public void setTemperature(double temperature) {
        prefs.putDouble(PREF_TEMPERATURE, temperature);
    }

    public boolean isConfigured() {
        String apiKey = getApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }
}
