package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.Action;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * Creates a button that is undecorated and its text behaves like a hyperlink.
 * On mouse over the text changes colors and the cursor changes to a hand as
 * is expected for a hyperlink. Displays the number of files associated with
 * this hyperlink.
 */
public class NumberedHyperLinkButton extends HyperLinkButton {
    
    @Resource
    private Color numberedForeGroundColor;
    @Resource
    private Color mouseOverColor;
    @Resource
    private Color disabledColor;
    @Resource
    private int fontSize;
    
    private int displayNumber = 0;
    
    public NumberedHyperLinkButton(String text) {
        super(text);
        
        setDisabledColor(disabledColor);
    }
    
    public NumberedHyperLinkButton(String text, Action action) {
        super(text, action);
        
        GuiUtils.assignResources(this); 

		//need to set these values in super even though they've been
		//injected
        setForeground(numberedForeGroundColor);
        setMouseOverColor(mouseOverColor);
        setDisabledColor(disabledColor);
        FontUtils.setSize(this, fontSize);
        FontUtils.changeStyle(this, Font.BOLD);
    }
    
    public void setDisplayNumber(int value) {
        this.displayNumber = value;
        if(value > 0) {
            FontUtils.underline(this);
            setEnabled(true);
        } else {
            FontUtils.removeUnderline(this);
            setEnabled(false);
        }
        super.setText(text + "(" + displayNumber + ")");
    }
    
    public void setDisabledColor(Color color) {
        this.disabledColor = color;
    }
    
    @Override
    public void setText(String text) {
        this.text = text;
        super.setText(text + "(" + displayNumber + ")");
    }
    
    @Override
    public void mouseEntered(MouseEvent e) {
        if(displayNumber > 0 && isEnabled()) {
            super.setForeground(mouseOverColor);
            oldCursor = getCursor();
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }
}