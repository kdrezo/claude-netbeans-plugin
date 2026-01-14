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
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Action pour générer du code avec Claude.
 * Demande une description à l'utilisateur et insère le code généré.
 */
@ActionID(
        category = "Edit",
        id = "com.denis.claude.netbeans.actions.GenerateCodeAction"
)
@ActionRegistration(
        displayName = "#CTL_GenerateCodeAction"
)
@ActionReferences({
        @ActionReference(path = "Editors/text/x-php5/Popup", position = 1520),
        @ActionReference(path = "Editors/text/html/Popup", position = 1520),
        @ActionReference(path = "Editors/text/javascript/Popup", position = 1520),
        @ActionReference(path = "Editors/text/css/Popup", position = 1520),
        @ActionReference(path = "Editors/text/x-java/Popup", position = 1520),
        @ActionReference(path = "Editors/text/plain/Popup", position = 1520),
        @ActionReference(path = "Menu/Edit", position = 2010)
})
@Messages("CTL_GenerateCodeAction=Générer du code avec Claude")
public final class GenerateCodeAction implements ActionListener {

    private final DataObject context;

    public GenerateCodeAction(DataObject context) {
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

        String fileName = context.getPrimaryFile().getNameExt();
        String language = detectLanguage(fileName);

        // Demander la description à l'utilisateur
        String description = showInputDialog(language);
        if (description == null || description.trim().isEmpty()) {
            return;
        }

        JTextComponent editor = findActiveEditor();
        if (editor == null) {
            JOptionPane.showMessageDialog(null,
                    "Aucun éditeur actif trouvé",
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Afficher une fenêtre de chargement
        JDialog loadingDialog = createLoadingDialog();

        // Exécuter en arrière-plan
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return ClaudeApiClient.getInstance()
                        .generateCode(description, language)
                        .get();
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    String generatedCode = get();
                    // Nettoyer le code (enlever les balises markdown si présentes)
                    generatedCode = cleanGeneratedCode(generatedCode, language);
                    showCodePreviewDialog(generatedCode, language, editor);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(null,
                            "Erreur: " + ex.getMessage(),
                            "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
        loadingDialog.setVisible(true);
    }

    private String showInputDialog(String language) {
        JTextArea textArea = new JTextArea(5, 40);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(textArea);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Décrivez le code " + language + " à générer:"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(null, panel,
                "Générer du code avec Claude",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            return textArea.getText();
        }
        return null;
    }

    private JTextComponent findActiveEditor() {
        TopComponent activated = TopComponent.getRegistry().getActivated();
        if (activated == null) {
            return null;
        }
        return activated.getLookup().lookup(JTextComponent.class);
    }

    private JDialog createLoadingDialog() {
        JDialog dialog = new JDialog((Frame) null, "Claude", false);
        dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialog.setLayout(new FlowLayout());
        dialog.add(new JLabel("Claude génère le code..."));
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        dialog.add(progressBar);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        return dialog;
    }

    private String cleanGeneratedCode(String code, String language) {
        // Enlever les balises de code markdown si présentes
        String langLower = language.toLowerCase();
        code = code.replaceAll("(?s)^```" + langLower + "\\s*\\n?", "");
        code = code.replaceAll("(?s)^```\\w*\\s*\\n?", "");
        code = code.replaceAll("(?s)\\n?```\\s*$", "");
        return code.trim();
    }

    private void showCodePreviewDialog(String code, String language, JTextComponent editor) {
        JDialog dialog = new JDialog((Frame) null, "Code généré - " + language, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout());

        JTextArea codeArea = new JTextArea(code);
        codeArea.setFont(new Font("Consolas", Font.PLAIN, 13));
        codeArea.setTabSize(4);
        JScrollPane scrollPane = new JScrollPane(codeArea);
        scrollPane.setPreferredSize(new Dimension(700, 400));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        JButton insertButton = new JButton("Insérer dans l'éditeur");
        insertButton.addActionListener(e -> {
            insertCodeAtCaret(editor, codeArea.getText());
            dialog.dispose();
        });

        JButton copyButton = new JButton("Copier");
        copyButton.addActionListener(e -> {
            codeArea.selectAll();
            codeArea.copy();
            JOptionPane.showMessageDialog(dialog, "Code copié dans le presse-papiers");
        });

        JButton cancelButton = new JButton("Annuler");
        cancelButton.addActionListener(e -> dialog.dispose());

        buttonPanel.add(copyButton);
        buttonPanel.add(insertButton);
        buttonPanel.add(cancelButton);

        dialog.add(new JLabel("  Code généré (vous pouvez le modifier avant insertion):"), BorderLayout.NORTH);
        dialog.add(scrollPane, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private void insertCodeAtCaret(JTextComponent editor, String code) {
        try {
            int caretPos = editor.getCaretPosition();
            editor.getDocument().insertString(caretPos, code, null);
        } catch (BadLocationException e) {
            JOptionPane.showMessageDialog(null,
                    "Erreur lors de l'insertion: " + e.getMessage(),
                    "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
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
