package com.denis.claude.netbeans.ui;

import com.denis.claude.netbeans.api.ClaudeApiClient;
import com.denis.claude.netbeans.settings.ClaudeSettings;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;

/**
 * Panneau de configuration pour le plugin Claude.
 * Accessible via NetBeans > Préférences > Claude AI
 */
public final class SettingsPanel extends JPanel {

    private final JTextField claudePathField;
    private final JButton browseButton;
    private final JButton detectButton;
    private final JSpinner maxTokensSpinner;
    private final JButton testButton;
    private final JLabel statusLabel;
    private final JLabel pathStatusLabel;

    public SettingsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Panel principal avec GridBagLayout
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Titre explicatif
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        JLabel titleLabel = new JLabel("<html><b>Configuration de Claude Code</b><br>" +
                "<small>Ce plugin utilise Claude Code CLI avec votre abonnement Max.</small></html>");
        formPanel.add(titleLabel, gbc);

        // Espace
        gbc.gridy = 1;
        formPanel.add(Box.createVerticalStrut(10), gbc);

        // Chemin Claude Code
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Chemin Claude Code:"), gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        // Panel pour le champ chemin + boutons
        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        claudePathField = new JTextField(30);
        pathPanel.add(claudePathField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
        browseButton = new JButton("Parcourir...");
        browseButton.addActionListener(e -> browseForClaude());
        buttonPanel.add(browseButton);

        detectButton = new JButton("Détecter");
        detectButton.addActionListener(e -> detectClaude());
        buttonPanel.add(detectButton);

        pathPanel.add(buttonPanel, BorderLayout.EAST);
        formPanel.add(pathPanel, gbc);

        // Statut du chemin
        gbc.gridx = 1;
        gbc.gridy = 3;
        pathStatusLabel = new JLabel(" ");
        pathStatusLabel.setFont(pathStatusLabel.getFont().deriveFont(Font.ITALIC, 11f));
        formPanel.add(pathStatusLabel, gbc);

        // Max tokens
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(new JLabel("Tokens maximum:"), gbc);

        gbc.gridx = 1;
        maxTokensSpinner = new JSpinner(new SpinnerNumberModel(4096, 100, 200000, 100));
        formPanel.add(maxTokensSpinner, gbc);

        // Espace
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        formPanel.add(Box.createVerticalStrut(15), gbc);

        // Bouton de test et statut
        gbc.gridy = 6;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;

        JPanel testPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        testButton = new JButton("Tester la connexion");
        testButton.addActionListener(e -> testConnection());
        testPanel.add(testButton);

        formPanel.add(testPanel, gbc);

        // Label de statut sur une ligne séparée
        gbc.gridy = 7;
        gbc.anchor = GridBagConstraints.CENTER;
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD));
        formPanel.add(statusLabel, gbc);

        // Note d'information
        gbc.gridy = 8;
        gbc.anchor = GridBagConstraints.WEST;
        JLabel noteLabel = new JLabel("<html><i>Claude Code doit être installé et connecté à votre compte Anthropic.<br>" +
                "Installation: npm install -g @anthropic-ai/claude-code</i></html>");
        noteLabel.setForeground(Color.GRAY);
        formPanel.add(noteLabel, gbc);

        add(formPanel, BorderLayout.NORTH);

        // Charger les valeurs actuelles
        load();

        // Vérifier le chemin à l'initialisation
        updatePathStatus();

        // Listener pour mettre à jour le statut quand le chemin change
        claudePathField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updatePathStatus(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updatePathStatus(); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updatePathStatus(); }
        });
    }

    private void browseForClaude() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Sélectionner l'exécutable Claude Code");
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

        // Commencer dans /usr/local/bin ou le dossier actuel du chemin
        String currentPath = claudePathField.getText();
        if (!currentPath.isEmpty()) {
            File current = new File(currentPath);
            if (current.getParentFile() != null && current.getParentFile().exists()) {
                chooser.setCurrentDirectory(current.getParentFile());
            }
        } else {
            chooser.setCurrentDirectory(new File("/usr/local/bin"));
        }

        chooser.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().equals("claude") || f.getName().startsWith("claude");
            }

            @Override
            public String getDescription() {
                return "Claude Code CLI (claude)";
            }
        });

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            claudePathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void detectClaude() {
        // Chemins possibles
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
                claudePathField.setText(path);
                JOptionPane.showMessageDialog(this,
                        "Claude Code détecté:\n" + path,
                        "Détection réussie",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
        }

        // Essayer avec 'which claude'
        try {
            ProcessBuilder pb = new ProcessBuilder("which", "claude");
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            process.waitFor();
            if (line != null && !line.isEmpty()) {
                File file = new File(line.trim());
                if (file.exists() && file.canExecute()) {
                    claudePathField.setText(line.trim());
                    JOptionPane.showMessageDialog(this,
                            "Claude Code détecté:\n" + line.trim(),
                            "Détection réussie",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
            }
        } catch (Exception e) {
            // Ignorer
        }

        JOptionPane.showMessageDialog(this,
                "Claude Code n'a pas été trouvé sur ce système.\n\n" +
                "Installez-le avec:\n" +
                "npm install -g @anthropic-ai/claude-code\n\n" +
                "Puis lancez 'claude' dans un terminal pour vous connecter.",
                "Claude Code non trouvé",
                JOptionPane.WARNING_MESSAGE);
    }

    private void updatePathStatus() {
        String path = claudePathField.getText().trim();
        if (path.isEmpty()) {
            pathStatusLabel.setText("Aucun chemin spécifié");
            pathStatusLabel.setForeground(Color.ORANGE.darker());
        } else if (ClaudeSettings.isValidClaudePath(path)) {
            pathStatusLabel.setText("Chemin valide");
            pathStatusLabel.setForeground(new Color(0, 150, 0));
        } else {
            pathStatusLabel.setText("Fichier non trouvé ou non exécutable");
            pathStatusLabel.setForeground(new Color(200, 0, 0));
        }
    }

    public void load() {
        ClaudeSettings settings = ClaudeSettings.getInstance();
        claudePathField.setText(settings.getClaudePath());
        maxTokensSpinner.setValue(settings.getMaxTokens());
    }

    public void store() {
        ClaudeSettings settings = ClaudeSettings.getInstance();
        settings.setClaudePath(claudePathField.getText().trim());
        settings.setMaxTokens((Integer) maxTokensSpinner.getValue());

        // Réinitialiser le client
        ClaudeApiClient.getInstance().reinitialize();
    }

    public boolean valid() {
        return true;
    }

    private void testConnection() {
        // Sauvegarder d'abord
        store();

        String claudePath = claudePathField.getText().trim();

        if (claudePath.isEmpty()) {
            showTestResult(false, "Chemin non configuré",
                    "Veuillez spécifier le chemin vers Claude Code.");
            return;
        }

        if (!ClaudeSettings.isValidClaudePath(claudePath)) {
            showTestResult(false, "Chemin invalide",
                    "Le fichier spécifié n'existe pas ou n'est pas exécutable.\n\n" +
                    "Chemin: " + claudePath);
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
                                "Claude Code fonctionne correctement.\n\n" +
                                "Réponse: " + (response.length() > 100 ? response.substring(0, 100) + "..." : response));
                        testButton.setEnabled(true);
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        String errorMsg = ex.getMessage();
                        if (errorMsg.contains("code 1") || errorMsg.contains("not logged in") || errorMsg.contains("login")) {
                            showTestResult(false, "Non connecté",
                                    "Claude Code n'est pas connecté à votre compte.\n\n" +
                                    "Ouvrez un terminal et exécutez:\n" +
                                    "claude\n\n" +
                                    "Puis suivez les instructions pour vous connecter.");
                        } else if (errorMsg.contains("No such file") || errorMsg.contains("not found")) {
                            showTestResult(false, "Exécutable non trouvé",
                                    "Le fichier Claude Code n'existe pas à ce chemin.\n\n" +
                                    "Utilisez le bouton 'Détecter' ou 'Parcourir' pour trouver claude.");
                        } else if (errorMsg.contains("Permission denied")) {
                            showTestResult(false, "Permission refusée",
                                    "Le fichier n'est pas exécutable.\n\n" +
                                    "Exécutez: chmod +x " + claudePath);
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
            statusLabel.setText("OK " + shortMessage);
            statusLabel.setForeground(new Color(0, 150, 0));
            if (detailMessage != null) {
                JOptionPane.showMessageDialog(this,
                        detailMessage,
                        "Test de connexion réussi",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } else {
            statusLabel.setText("Erreur: " + shortMessage);
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
