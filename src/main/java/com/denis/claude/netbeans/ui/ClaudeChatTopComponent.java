package com.denis.claude.netbeans.ui;

import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;

import java.awt.BorderLayout;

/**
 * TopComponent pour le panneau de chat Claude.
 * S'affiche comme un panneau latéral dans NetBeans.
 */
@ConvertAsProperties(
        dtd = "-//com.denis.claude.netbeans.ui//ClaudeChat//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "ClaudeChatTopComponent",
        iconBase = "com/denis/claude/netbeans/claude-icon.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(
        mode = "output",
        openAtStartup = false
)
@ActionID(
        category = "Window",
        id = "com.denis.claude.netbeans.ui.ClaudeChatTopComponent"
)
@ActionReference(
        path = "Menu/Window",
        position = 333
)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_ClaudeChatTopComponent",
        preferredID = "ClaudeChatTopComponent"
)
@Messages({
        "CTL_ClaudeChatTopComponent=Claude Chat",
        "HINT_ClaudeChatTopComponent=Panneau de chat avec Claude AI"
})
public final class ClaudeChatTopComponent extends TopComponent {

    private ChatPanel chatPanel;

    public ClaudeChatTopComponent() {
        initComponents();
        setName("Claude Chat");
        setToolTipText("Panneau de chat avec Claude AI");
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        chatPanel = new ChatPanel();
        add(chatPanel, BorderLayout.CENTER);
    }

    @Override
    public void componentOpened() {
        // Appelé quand le composant est ouvert
    }

    @Override
    public void componentClosed() {
        // Appelé quand le composant est fermé
    }

    void writeProperties(java.util.Properties p) {
        // Sauvegarde des propriétés
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        // Lecture des propriétés
        String version = p.getProperty("version");
    }

    /**
     * Retourne le panneau de chat pour interaction externe.
     */
    public ChatPanel getChatPanel() {
        return chatPanel;
    }

    /**
     * Trouve et retourne l'instance ouverte du TopComponent.
     */
    public static ClaudeChatTopComponent findInstance() {
        return (ClaudeChatTopComponent) TopComponent.getRegistry()
                .getOpened()
                .stream()
                .filter(tc -> tc instanceof ClaudeChatTopComponent)
                .findFirst()
                .orElse(null);
    }

    /**
     * Ouvre le TopComponent et retourne l'instance.
     */
    public static ClaudeChatTopComponent openAndGetInstance() {
        ClaudeChatTopComponent tc = findInstance();
        if (tc == null) {
            tc = new ClaudeChatTopComponent();
        }
        tc.open();
        tc.requestActive();
        return tc;
    }
}
