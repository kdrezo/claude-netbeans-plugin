package com.denis.claude.netbeans.ui;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import org.netbeans.spi.options.OptionsCategory;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.ImageUtilities;

/**
 * Catégorie d'options pour Claude AI.
 */
public class ClaudeOptionsCategory extends OptionsCategory {

    @Override
    public Icon getIcon() {
        return ImageUtilities.loadImageIcon("com/denis/claude/netbeans/claude-icon.png", false);
    }

    @Override
    public String getCategoryName() {
        return "Claude AI";
    }

    @Override
    public String getTitle() {
        return "Claude AI";
    }

    @Override
    public OptionsPanelController create() {
        return new ClaudeOptionsPanelController();
    }

    public static OptionsCategory createCategory() {
        return new ClaudeOptionsCategory();
    }
}
