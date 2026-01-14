package com.denis.claude.netbeans.settings;

import java.io.File;
import java.util.prefs.Preferences;
import org.openide.util.NbPreferences;

/**
 * Gestion des paramètres du plugin Claude.
 * Stocke le chemin vers Claude Code CLI et autres préférences.
 */
public class ClaudeSettings {

    private static final String PREF_CLAUDE_PATH = "claudePath";
    private static final String PREF_MAX_TOKENS = "maxTokens";

    // Chemin par défaut sur macOS (installation via npm global ou homebrew)
    private static final String DEFAULT_CLAUDE_PATH = "/usr/local/bin/claude";
    private static final int DEFAULT_MAX_TOKENS = 4096;

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

    public String getClaudePath() {
        return prefs.get(PREF_CLAUDE_PATH, detectClaudePath());
    }

    public void setClaudePath(String claudePath) {
        prefs.put(PREF_CLAUDE_PATH, claudePath);
    }

    public int getMaxTokens() {
        return prefs.getInt(PREF_MAX_TOKENS, DEFAULT_MAX_TOKENS);
    }

    public void setMaxTokens(int maxTokens) {
        prefs.putInt(PREF_MAX_TOKENS, maxTokens);
    }

    /**
     * Détecte automatiquement le chemin de Claude Code CLI.
     */
    private String detectClaudePath() {
        // Chemins possibles sur macOS
        String[] possiblePaths = {
            "/usr/local/bin/claude",
            "/opt/homebrew/bin/claude",
            System.getProperty("user.home") + "/.npm-global/bin/claude",
            System.getProperty("user.home") + "/node_modules/.bin/claude",
            "/usr/bin/claude"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists() && file.canExecute()) {
                return path;
            }
        }

        // Retourner le chemin par défaut même s'il n'existe pas
        return DEFAULT_CLAUDE_PATH;
    }

    /**
     * Vérifie si Claude Code est configuré et accessible.
     */
    public boolean isConfigured() {
        String claudePath = getClaudePath();
        if (claudePath == null || claudePath.trim().isEmpty()) {
            return false;
        }
        File file = new File(claudePath);
        return file.exists() && file.canExecute();
    }

    /**
     * Vérifie si le chemin donné est valide.
     */
    public static boolean isValidClaudePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return false;
        }
        File file = new File(path);
        return file.exists() && file.canExecute();
    }

    // Méthodes de compatibilité (obsolètes, gardées pour la transition)
    @Deprecated
    public String getApiKey() {
        return "";
    }

    @Deprecated
    public void setApiKey(String apiKey) {
        // Ignoré
    }

    @Deprecated
    public String getModel() {
        return "claude-code";
    }

    @Deprecated
    public void setModel(String model) {
        // Ignoré - Claude Code utilise le modèle configuré dans l'abonnement
    }

    @Deprecated
    public double getTemperature() {
        return 0.7;
    }

    @Deprecated
    public void setTemperature(double temperature) {
        // Ignoré
    }
}
