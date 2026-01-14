package com.denis.claude.netbeans.ui;

import com.denis.claude.netbeans.api.ClaudeApiClient;
import com.denis.claude.netbeans.settings.ClaudeSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Panneau de configuration pour le plugin Claude.
 * Accessible via NetBeans > Préférences > Claude AI
 */
public final class SettingsPanel extends JPanel {

    private final JPasswordField apiKeyField;
    private final JButton showHideButton;
    private final JComboBox<String> modelCombo;
    private final JSpinner maxTokensSpinner;
    private final JSpinner temperatureSpinner;
    private final JButton testButton;
    private final JLabel statusLabel;
    private boolean apiKeyVisible = false;

    private static final String[] MODELS = {
            "claude-sonnet-4-20250514",
            "claude-opus-4-20250514",
            "claude-3-5-sonnet-20241022",
            "claude-3-5-haiku-20241022",
            "claude-3-opus-20240229"
    };

    public SettingsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel principal avec GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Clé API
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new JLabel("Clé API Anthropic:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        apiKeyField = new JPasswordField(35);

        // Panel pour le champ API key + bouton afficher/masquer
        JPanel apiKeyPanel = new JPanel(new BorderLayout(5, 0));
        apiKeyPanel.add(apiKeyField, BorderLayout.CENTER);

        showHideButton = new JButton("Afficher");
        showHideButton.setPreferredSize(new Dimension(85, showHideButton.getPreferredSize().height));
        showHideButton.addActionListener(e -> toggleApiKeyVisibility());
        apiKeyPanel.add(showHideButton, BorderLayout.EAST);

        formPanel.add(apiKeyPanel, gbc);

        // Modèle
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Modèle:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        modelCombo = new JComboBox<>(MODELS);
        formPanel.add(modelCombo, gbc);

        // Max tokens
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Tokens maximum:"), gbc);

        gbc.gridx = 1;
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(4096, 100, 200000, 100));
        formPanel.add(maxTokensSpinner, gbc);

        // Température
        gbc.gridx = 0;
        gbc.gridy = 3;
        formPanel.add(new JLabel("Température (0.0-1.0):"), gbc);

        gbc.gridx = 1;
        temperatureSpinner = new JSpinner(new SpinnerNumberModel(0.7, 0.0, 1.0, 0.1));
        formPanel.add(temperatureSpinner, gbc);

        // Bouton de test et statut
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        testButton = new JButton("Tester la connexion");
        testButton.addActionListener(e -> testConnection());
        buttonPanel.add(testButton);

        formPanel.add(buttonPanel, gbc);

        // Label de statut sur une ligne séparée
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.CENTER;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        formPanel.add(statusLabel, gbc);

        // Note d'information
        gbc.gridy = 6;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel noteLabel = new JLabel("<html><i>Obtenez votre clé API sur console.anthropic.com</i></html>");
        noteLabel.setForeground(Color.GRAY);
        formPanel.add(noteLabel, gbc);

        add(formPanel, BorderLayout.NORTH);

        // Charger les valeurs actuelles
        load();
    }

    private void toggleApiKeyVisibility() {
        apiKeyVisible = !apiKeyVisible;
        if (apiKeyVisible) {
            apiKeyField.setEchoChar((char) 0); // Afficher en clair
            showHideButton.setText("Masquer");
        } else {
            apiKeyField.setEchoChar('*'); // Masquer
            showHideButton.setText("Afficher");
        }
    }

    public void load() {
        ClaudeSettings settings = ClaudeSettings.getInstance();
        apiKeyField.setText(settings.getApiKey());
        modelCombo.setSelectedItem(settings.getModel());
        maxTokensSpinner.setValue(settings.getMaxTokens());
        temperatureSpinner.setValue(settings.getTemperature());
    }

    public void store() {
        ClaudeSettings settings = ClaudeSettings.getInstance();
        settings.setApiKey(new String(apiKeyField.getPassword()));
        settings.setModel((String) modelCombo.getSelectedItem());
        settings.setMaxTokens((Integer) maxTokensSpinner.getValue());
        settings.setTemperature((Double) temperatureSpinner.getValue());

        // Réinitialiser le client avec les nouveaux paramètres
        ClaudeApiClient.getInstance().reinitialize();
    }

    public boolean valid() {
        return true;
    }

    private void testConnection() {
        // Sauvegarder d'abord
        store();

        String apiKey = new String(apiKeyField.getPassword()).trim();

        if (apiKey.isEmpty()) {
            showTestResult(false, "Veuillez entrer une clé API", null);
            return;
        }

        if (!apiKey.startsWith("sk-ant-")) {
            showTestResult(false, "Format de clé API invalide",
                    "La clé API doit commencer par 'sk-ant-'");
            return;
        }

        statusLabel.setText("Test en cours...");
        statusLabel.setForeground(new Color(0, 100, 200));
        testButton.setEnabled(false);

        ClaudeApiClient.getInstance()
                .sendMessageWithoutHistory("Réponds uniquement par 'OK' si tu me reçois.", null)
                .thenAccept(response -> {
                    SwingUtilities.invokeLater(() -> {
                        showTestResult(true, "Connexion réussie!",
                                "La connexion à l'API Claude fonctionne correctement.\n\n" +
                                "Modèle utilisé: " + modelCombo.getSelectedItem());
                        testButton.setEnabled(true);
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = ex.getMessage();
                        if (errorMsg.contains("401")) {
                            showTestResult(false, "Clé API invalide",
                                    "La clé API n'est pas reconnue par Anthropic.\n" +
                                    "Vérifiez que vous avez copié la clé correctement.");
                        } else if (errorMsg.contains("403")) {
                            showTestResult(false, "Accès refusé",
                                    "Votre compte n'a pas accès à l'API.\n" +
                                    "Vérifiez votre abonnement sur console.anthropic.com");
                        } else if (errorMsg.contains("429")) {
                            showTestResult(false, "Limite de requêtes atteinte",
                                    "Trop de requêtes. Réessayez dans quelques instants.");
                        } else if (errorMsg.contains("UnknownHostException") || errorMsg.contains("connexion")) {
                            showTestResult(false, "Erreur de connexion",
                                    "Impossible de contacter l'API Anthropic.\n" +
                                    "Vérifiez votre connexion internet.");
                        } else {
                            showTestResult(false, "Erreur", errorMsg);
                        }
                        testButton.setEnabled(true);
                    });
                    return null;
                });
    }

    private void showTestResult(boolean success, String shortMessage, String detailMessage) {
        if (success) {
            statusLabel.setText("✓ " + shortMessage);
            statusLabel.setForeground(new Color(0, 150, 0));
            if (detailMessage != null) {
                JOptionPane.showMessageDialog(this,
                        detailMessage,
                        "Test de connexion réussi",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            statusLabel.setText("✗ " + shortMessage);
            statusLabel.setForeground(new Color(200, 0, 0));
            if (detailMessage != null) {
                JOptionPane.showMessageDialog(this,
                        detailMessage,
                        "Échec du test de connexion",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
