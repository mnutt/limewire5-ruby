package org.limewire.ui.swing.library.manager;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import org.jdesktop.swingx.icon.EmptyIcon;
import org.limewire.ui.swing.util.IconManager;

class FolderRenderer extends DefaultTreeCellRenderer {
    
    private final Icon emptyIcon;
    private final IconManager iconManager;
    
    public FolderRenderer(IconManager iconManager) {
        this.iconManager = iconManager;
        this.emptyIcon = new EmptyIcon(16, 16);
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel,
            boolean expanded, boolean leaf, int row, boolean hasFocus) {
        if(value instanceof LibraryManagerItem) {
            LibraryManagerItem item = (LibraryManagerItem)value;
            super.getTreeCellRendererComponent(tree, item.displayName(), false, expanded, leaf, row, false);
            Icon icon = iconManager.getIconForFile(item.getFile());
            if(icon == null) {
                icon = emptyIcon;
            }
            setIcon(icon);
            return this;
        } else {
            return super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        }
    }
}
