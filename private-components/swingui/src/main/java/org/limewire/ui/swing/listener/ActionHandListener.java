package org.limewire.ui.swing.listener;

import java.awt.Cursor;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;

public class ActionHandListener extends MouseActionListener {

    public ActionHandListener(ActionListener actionListener) {
        super(actionListener);
    }
    
    public ActionHandListener() {
        this(null);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        JComponent comp = (JComponent) e.getComponent();
        if (!Boolean.TRUE.equals(comp.getClientProperty("limewire.actionHand.disabled"))) {
            comp.getTopLevelAncestor().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        JComponent comp = (JComponent) e.getComponent();
        comp.getTopLevelAncestor().setCursor(Cursor.getDefaultCursor());
    }
    
    /** Disables or re-enables the action hand from drawing. */
    public static void setActionHandDrawingDisabled(JComponent component, boolean disabled) {
        component.putClientProperty("limewire.actionHand.disabled", disabled);
    }   
}
