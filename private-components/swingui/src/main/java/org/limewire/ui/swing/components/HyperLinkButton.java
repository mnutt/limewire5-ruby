package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Action;

import org.jdesktop.swingx.JXButton;

import org.limewire.ui.swing.util.FontUtils;

/**
 * Creates a button that is undecorated and its text behaves like a hyperlink.
 * On mouse over the text changes colors and the cursor changes to a hand as
 * is expected for a hyperlink.
 */
public class HyperLinkButton extends JXButton implements MouseListener {
    protected Color mouseOverColor = getForeground();
    protected Color foregroundColor = getForeground();
    
    protected String text;
    
    protected Cursor oldCursor;
    
    public HyperLinkButton() {
        this(null);
    }

    public HyperLinkButton(String text) {
        initialize(text);
    }
    
    public HyperLinkButton(String text, Action action) {
        super(action);
        initialize(text);
    }
    
    private void initialize(String text) {
        if(text != null)
            setText(text);
        
        setBorderPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setFocusPainted(false);
        setRolloverEnabled(false);
        setOpaque(false);        
        setBorder(null);
        setContentAreaFilled(false);
        setFocusable(false);
        addMouseListener(this);
        FontUtils.underline(this);
    }
    
    public void setFont(Font font) {
        super.setFont(font);
        if(!FontUtils.isUnderlined(this)) {
            FontUtils.underline(this);
        }
    }
    
    public void setMouseOverColor(Color color) {
        this.mouseOverColor = color;
    }
    
    public void setForeground(Color color) {
        super.setForeground(color);
        this.foregroundColor = color;
    }        
    
    public void setText(String text) {
        this.text = text;
        super.setText(text);
    }
    
    @Override
    public void setAction(Action a) {
        super.setAction(a);
        setText(text); // FIXME: ?
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        if(isEnabled()) {
            super.setForeground(mouseOverColor);
            oldCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if(isEnabled()) {
            super.setForeground(foregroundColor);
            setCursor(oldCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
}