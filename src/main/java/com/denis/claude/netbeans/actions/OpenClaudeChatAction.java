package com.denis.claude.netbeans.actions;

import com.denis.claude.netbeans.ui.ClaudeChatTopComponent;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Action pour ouvrir le panneau de chat Claude.
 */
public final class OpenClaudeChatAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        ClaudeChatTopComponent.openAndGetInstance();
    }
}
