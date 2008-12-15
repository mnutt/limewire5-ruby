package com.limegroup.gnutella.gui.trees;

import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Custom renderer to fix Java's default spacing issues.
 */
public class LimeTreeCellRenderer extends DefaultTreeCellRenderer {

    /**
     * Creates a new renderer instance, with the proper LNF.
     */
    public LimeTreeCellRenderer() {
        super();
        setOpaque(false);
        setBackground(null);
        setBackgroundNonSelectionColor(null);
        setLeafIcon(null);
        setOpenIcon(null);
        setClosedIcon(null);
    }
    
    /**
     * Add left spacing to labels, matching already-provided right spacing.
     */
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value,
            boolean sel, boolean expanded, boolean leaf, int row, boolean focused) {  
        JLabel jl = (JLabel)super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        jl.setBorder(BorderFactory.createEmptyBorder(0,4,0,0));
        return jl;
    }
}
