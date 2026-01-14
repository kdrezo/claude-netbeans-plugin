package com.denis.claude.netbeans.ui;

import com.denis.claude.netbeans.api.ClaudeApiClient;
import com.denis.claude.netbeans.settings.ClaudeSettings;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Panneau de chat avec Claude AI.
 * Affiche l'historique des messages et permet d'envoyer de nouvelles requêtes.
 */
public class ChatPanel extends JPanel {

    private final JEditorPane chatDisplay;
    private final JTextArea inputArea;
    private final JButton sendButton;
    private final JButton clearButton;
    private final StringBuilder chatHistory;
    private final Parser markdownParser;
    private final HtmlRenderer htmlRenderer;

    public ChatPanel() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        chatHistory = new StringBuilder();
        markdownParser = Parser.builder().build();
        htmlRenderer = HtmlRenderer.builder().build();

        // Zone d'affichage du chat
        chatDisplay = new JEditorPane();
        chatDisplay.setEditable(false);
        chatDisplay.setContentType("text/html");
        setupHtmlStyles();

        JScrollPane chatScroll = new JScrollPane(chatDisplay);
        chatScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // Zone de saisie
        inputArea = new JTextArea(3, 40);
        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Raccourci Ctrl+Enter pour envoyer
        inputArea.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK), "send");
        inputArea.getActionMap().put("send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        JScrollPane inputScroll = new JScrollPane(inputArea);

        // Boutons
        sendButton = new JButton("Envoyer");
        sendButton.addActionListener(e -> sendMessage());

        clearButton = new JButton("Effacer");
        clearButton.addActionListener(e -> clearChat());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);

        // Panel du bas (input + boutons)
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        bottomPanel.add(inputScroll, BorderLayout.CENTER);
        bottomPanel.add(buttonPanel, BorderLayout.EAST);

        // Assemblage
        add(chatScroll, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Message initial
        if (!ClaudeSettings.getInstance().isConfigured()) {
            appendSystemMessage("Bienvenue! Veuillez configurer Claude Code dans NetBeans > Préférences > Claude AI");
        } else {
            appendSystemMessage("Bienvenue! Tapez votre message et appuyez sur Ctrl+Entrée pour envoyer.");
        }
    }

    private void setupHtmlStyles() {
        HTMLEditorKit kit = new HTMLEditorKit();
        StyleSheet styleSheet = kit.getStyleSheet();
        styleSheet.addRule("body { font-family: 'Segoe UI', Arial, sans-serif; font-size: 13px; margin: 10px; }");
        styleSheet.addRule(".user { background-color: #e3f2fd; padding: 10px; border-radius: 10px; margin: 5px 0; }");
        styleSheet.addRule(".assistant { background-color: #f5f5f5; padding: 10px; border-radius: 10px; margin: 5px 0; }");
        styleSheet.addRule(".system { background-color: #fff3e0; padding: 10px; border-radius: 10px; margin: 5px 0; font-style: italic; }");
        styleSheet.addRule(".error { background-color: #ffebee; padding: 10px; border-radius: 10px; margin: 5px 0; color: #c62828; }");
        styleSheet.addRule("pre { background-color: #263238; color: #aed581; padding: 10px; border-radius: 5px; overflow-x: auto; }");
        styleSheet.addRule("code { background-color: #eceff1; padding: 2px 5px; border-radius: 3px; font-family: 'Consolas', monospace; }");
        chatDisplay.setEditorKit(kit);
    }

    private void sendMessage() {
        String message = inputArea.getText().trim();
        if (message.isEmpty()) {
            return;
        }

        if (!ClaudeSettings.getInstance().isConfigured()) {
            appendErrorMessage("Veuillez configurer Claude Code dans NetBeans > Préférences > Claude AI");
            return;
        }

        // Afficher le message utilisateur
        appendUserMessage(message);
        inputArea.setText("");
        inputArea.setEnabled(false);
        sendButton.setEnabled(false);

        // Indicateur de chargement
        appendSystemMessage("Claude réfléchit...");

        // Envoyer à Claude
        ClaudeApiClient.getInstance().sendMessage(message)
                .thenAccept(response -> {
                    SwingUtilities.invokeLater(() -> {
                        removeLastMessage(); // Retirer "Claude réfléchit..."
                        appendAssistantMessage(response);
                        inputArea.setEnabled(true);
                        sendButton.setEnabled(true);
                        inputArea.requestFocus();
                    });
                })
                .exceptionally(ex -> {
                    SwingUtilities.invokeLater(() -> {
                        removeLastMessage();
                        appendErrorMessage("Erreur: " + ex.getMessage());
                        inputArea.setEnabled(true);
                        sendButton.setEnabled(true);
                        inputArea.requestFocus();
                    });
                    return null;
                });
    }

    private void appendUserMessage(String message) {
        chatHistory.append("<div class='user'><strong>Vous:</strong><br/>")
                .append(escapeHtml(message).replace("\n", "<br/>"))
                .append("</div>");
        updateDisplay();
    }

    private void appendAssistantMessage(String message) {
        // Convertir le Markdown en HTML
        Node document = markdownParser.parse(message);
        String htmlContent = htmlRenderer.render(document);

        chatHistory.append("<div class='assistant'><strong>Claude:</strong><br/>")
                .append(htmlContent)
                .append("</div>");
        updateDisplay();
    }

    private void appendSystemMessage(String message) {
        chatHistory.append("<div class='system'>")
                .append(escapeHtml(message))
                .append("</div>");
        updateDisplay();
    }

    private void appendErrorMessage(String message) {
        chatHistory.append("<div class='error'>")
                .append(escapeHtml(message))
                .append("</div>");
        updateDisplay();
    }

    private void removeLastMessage() {
        int lastDivEnd = chatHistory.lastIndexOf("</div>");
        if (lastDivEnd > 0) {
            int lastDivStart = chatHistory.lastIndexOf("<div", lastDivEnd - 1);
            if (lastDivStart >= 0) {
                chatHistory.delete(lastDivStart, lastDivEnd + 6);
            }
        }
        updateDisplay();
    }

    private void updateDisplay() {
        String html = "<html><body>" + chatHistory.toString() + "</body></html>";
        chatDisplay.setText(html);
        // Scroll vers le bas
        SwingUtilities.invokeLater(() -> {
            chatDisplay.setCaretPosition(chatDisplay.getDocument().getLength());
        });
    }

    private void clearChat() {
        chatHistory.setLength(0);
        ClaudeApiClient.getInstance().clearHistory();
        updateDisplay();
        appendSystemMessage("Conversation effacée. Nouvelle conversation commencée.");
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public void appendCodeForAnalysis(String code, String language, String fileName) {
        String message = String.format("Analyse ce code %s (fichier: %s):\n\n```%s\n%s\n```",
                language, fileName, language, code);
        inputArea.setText(message);
        inputArea.requestFocus();
    }
}
