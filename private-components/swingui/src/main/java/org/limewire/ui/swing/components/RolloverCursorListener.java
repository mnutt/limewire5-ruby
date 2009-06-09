package org.limewire.ui.swing.components;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A mouse listener that displays a rollover or hover cursor when the mouse
 * enters the component on which the listener has been installed.
 */
public class RolloverCursorListener extends MouseAdapter {

    private final Cursor rolloverCursor;
    
    /**
     * Constructs a RolloverCursorListener with the default rollover cursor.
     * The default rollover cursor is Cursor.HAND_CURSOR.
     */
    public RolloverCursorListener() {
        this(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    /**
     * Constructs a RolloverCursorListener with the specified rollover cursor.
     */
    public RolloverCursorListener(Cursor rolloverCursor) {
        this.rolloverCursor = rolloverCursor;
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        e.getComponent().setCursor(rolloverCursor);
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        e.getComponent().setCursor(Cursor.getDefaultCursor());
    }
}
