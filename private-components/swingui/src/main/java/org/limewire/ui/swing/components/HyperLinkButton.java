package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.TextAttribute;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a button that is undecorated and its text behaves like a hyperlink.
 * On mouse over the text changes colors and the cursor changes to a hand as
 * is expected for a hyperlink.
 */
public class HyperlinkButton extends JXButton implements MouseListener {
    
    private final HyperlinkButtonResources r = new HyperlinkButtonResources();
    
    private Cursor oldCursor;
    
    public HyperlinkButton() {
        initialize();
    }
    
    public HyperlinkButton(Action action) {
        super(action);
        initialize();
    }

    public HyperlinkButton(String text) {
        initialize();
        setText(text);
    }
    
    public HyperlinkButton(String text, Action action) {
        super(action);
        setHideActionText(true);
        setText(text);
        initialize();
    }
    
    private void initialize() {
        setBorder(BorderFactory.createEmptyBorder());
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
    
    public void removeUnderline() {
        // Note: cannot use FontUtils.removeUnderline because that
        // calls setFont, which will just re-add the underline!
        Font font = getFont();
        if (font != null) {
            Map<TextAttribute, ?> map = font.getAttributes();
            Map<TextAttribute, Object> newMap = new HashMap<TextAttribute, Object>(map);
            newMap.put(TextAttribute.UNDERLINE, Integer.valueOf(-1));    
            super.setFont(font.deriveFont(newMap));
        }
    }
    
    public void setRolloverForeground(Color color) {
        r.rolloverForeground = color;
    }
    
    public void setForeground(Color color) {
        super.setForeground(color);
        // r may be null because this is set by the constructor
        if(r != null) {
            r.foreground = color;
        }
    }
    
    public void setDisabledForeground(Color color) {
        r.disabledForeground = color;
    }
    
    @Override
    public void setEnabled(boolean value) {
        super.setEnabled(value);
        if(r != null) {
            if(value) {
                super.setForeground(r.foreground);
            } else {
                super.setForeground(r.disabledForeground);
            }
        }
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        if(isEnabled()) {
            super.setForeground(r.rolloverForeground);
            oldCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if(isEnabled()) {
            super.setForeground(r.foreground);
            setCursor(oldCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}
    @Override
    public void mouseReleased(MouseEvent e) {}
    @Override
    public void mouseClicked(MouseEvent e) {}
    
    private class HyperlinkButtonResources {
        @Resource Color rolloverForeground;
        @Resource Color foreground;
        @Resource Color disabledForeground;
        
        public HyperlinkButtonResources() {
            GuiUtils.assignResources(this);
            HyperlinkButton.super.setForeground(foreground);
        }
    }
}