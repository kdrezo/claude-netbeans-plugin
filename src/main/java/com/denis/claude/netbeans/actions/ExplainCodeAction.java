package com.denis.claude.netbeans.actions;

import com.denis.claude.netbeans.api.ClaudeApiClient;
import com.denis.claude.netbeans.settings.ClaudeSettings;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.cookies.EditorCookie;
import org.openide.loaders.DataObject;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Action pour demander à Claude d'expliquer le code sélectionné.
 * Affiche l'explication dans une fenêtre popup.
 */
@ActionID(
        category = "Edit",
        id = "com.denis.claude.netbeans.actions.ExplainCodeAction"
)
@ActionRegistration(
        displayName = "#CTL_ExplainCodeAction"
)
@ActionReferences({
        @ActionReference(path = "Editors/text/x-php5/Popup", position = 1510),
        @ActionReference(path = "Editors/text/html/Popup", position = 1510),
        @ActionReference(path = "Editors/text/javascript/Popup", position = 1510),
        @ActionReference(path = "Editors/text/css/Popup", position = 1510),
        @ActionReference(path = "Editors/text/x-java/Popup", position = 1510),
        @ActionReference(path = "Editors/text/plain/Popup", position = 1510)
})
@Messages("CTL_ExplainCodeAction=Expliquer avec Claude")
public final class ExplainCodeAction implements ActionListener {

    private final DataObject context;

    public ExplainCodeAction(DataObject context) {
        this.context = context;
    }

    @Override
    public void actionPerformed(ActionEvent ev) {
        if (!ClaudeSettings.getInstance().isConfigured()) {
            JOptionPane.showMessageDialog(null,
                    "Veuillez configurer votre clé API dans Tools > Options > Claude AI",
                    "Configuration requise",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextComponent editor = findActiveEditor();
        if (editor == null) {
            return;
        }

        String selectedText = editor.getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Veuillez sélectionner du code à expliquer",
                    "Aucune sélection",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String fileName = context.getPrimaryFile().getNameExt();
        String language = detectLanguage(fileName);

        // Afficher une fenêtre de chargement
        JDialog loadingDialog = createLoadingDialog();
        loadingDialog.setVisible(true);

        // Envoyer à Claude pour explication
        ClaudeApiClient.getInstance()
                .analyzeCode(selectedText, language, "Explique ce code de manière claire et concise. " +
                        "Décris ce qu'il fait, comment il fonctionne, et mentionne tout problème potentiel.")
                .thenAccept(explanation -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        showExplanationDialog(explanation, language);
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        loadingDialog.dispose();
                        JOptionPane.showMessageDialog(null,
                                "Erreur: " + ex.getMessage(),
                                "Erreur",
                                JOptionPane.ERROR_MESSAGE);
                    });
                    return null;
                });
    }

    private JTextComponent findActiveEditor() {
        TopComponent activated = TopComponent.getRegistry().getActivated();
        if (activated == null) {
            return null;
        }
        return activated.getLookup().lookup(JTextComponent.class);
    }

    private JDialog createLoadingDialog() {
        JDialog dialog = new JDialog((Frame) null, "Claude", true);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new FlowLayout());
        dialog.add(new JLabel("Claude analyse le code..."));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        dialog.add(progressBar);
        dialog.pack();
        dialog.setLocationRelativeTo(null);

        // Rendre non-modal pour permettre la mise à jour
        SwingUtilities.invokeLater(() -> dialog.setModal(false));

        return dialog;
    }

    private void showExplanationDialog(String explanation, String language) {
        JDialog dialog = new JDialog((Frame) null, "Explication Claude - " + language, false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        JTextArea textArea = new JTextArea(explanation);
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        textArea.setMargin(new Insets(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));

        JButton closeButton = new JButton("Fermer");
        closeButton.addActionListener(e -> dialog.dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(closeButton);

        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private String detectLanguage(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".php")) {
            return "PHP";
        } else if (lowerName.endsWith(".html") || lowerName.endsWith(".htm")) {
            return "HTML";
        } else if (lowerName.endsWith(".js")) {
            return "JavaScript";
        } else if (lowerName.endsWith(".css")) {
            return "CSS";
        } else if (lowerName.endsWith(".java")) {
            return "Java";
        } else {
            return "code";
        }
    }
}
