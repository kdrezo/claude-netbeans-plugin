package com.denis.claude.netbeans.actions;

import com.denis.claude.netbeans.settings.ClaudeSettings;
import com.denis.claude.netbeans.ui.ClaudeChatTopComponent;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Action pour envoyer du code sélectionné à Claude.
 * Accessible via le menu contextuel de l'éditeur.
 */
@ActionID(
        category = "Edit",
        id = "com.denis.claude.netbeans.actions.SendToClaudeAction"
)
@ActionRegistration(
        displayName = "#CTL_SendToClaudeAction"
)
@ActionReferences({
        @ActionReference(path = "Editors/text/x-php5/Popup", position = 1500),
        @ActionReference(path = "Editors/text/html/Popup", position = 1500),
        @ActionReference(path = "Editors/text/javascript/Popup", position = 1500),
        @ActionReference(path = "Editors/text/css/Popup", position = 1500),
        @ActionReference(path = "Editors/text/x-java/Popup", position = 1500),
        @ActionReference(path = "Editors/text/plain/Popup", position = 1500),
        @ActionReference(path = "Menu/Edit", position = 2000)
})
@Messages("CTL_SendToClaudeAction=Envoyer à Claude")
public final class SendToClaudeAction implements ActionListener {

    private final DataObject context;

    public SendToClaudeAction(DataObject context) {
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

        EditorCookie ec = context.getLookup().lookup(EditorCookie.class);
        if (ec == null) {
            return;
        }

        JTextComponent editor = findActiveEditor();
        if (editor == null) {
            return;
        }

        String selectedText = editor.getSelectedText();
        if (selectedText == null || selectedText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Veuillez sélectionner du code à envoyer à Claude",
                    "Aucune sélection",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String fileName = context.getPrimaryFile().getNameExt();
        String language = detectLanguage(fileName);

        // Ouvrir le panneau de chat et y envoyer le code
        ClaudeChatTopComponent chatComponent = ClaudeChatTopComponent.openAndGetInstance();
        chatComponent.getChatPanel().appendCodeForAnalysis(selectedText, language, fileName);
    }

    private JTextComponent findActiveEditor() {
        TopComponent activated = TopComponent.getRegistry().getActivated();
        if (activated == null) {
            return null;
        }
        return activated.getLookup().lookup(JTextComponent.class);
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
        } else if (lowerName.endsWith(".json")) {
            return "JSON";
        } else if (lowerName.endsWith(".xml")) {
            return "XML";
        } else if (lowerName.endsWith(".sql")) {
            return "SQL";
        } else if (lowerName.endsWith(".py")) {
            return "Python";
        } else {
            return "code";
        }
    }
}
