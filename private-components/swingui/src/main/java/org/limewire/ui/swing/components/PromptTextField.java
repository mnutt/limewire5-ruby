package org.limewire.ui.swing.components;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;

import org.jdesktop.swingx.painter.Painter;

public class PromptTextField extends JTextField implements FocusListener, Paintable {
    
    private String promptText;
    
    private Painter<JTextField> backgroundPainter;
    private Painter<JTextField> promptPainter;
    private Insets paintedInsets;
    
    public PromptTextField() {
        this.init();
    }
    
  
    public PromptTextField(String promptText) {
        this.setPromptText(promptText);        
        this.init();
    }

    public PromptTextField(String promptText, int columns) {
        this.promptText = promptText;
        this.setColumns(columns);        
        init();
    }
    
    private void init() {

        TextFieldClipboardControl.install(this);
        this.addFocusListener(this);
                
        this.setMinimumSize(new Dimension(150,22));
        this.setPreferredSize(this.getMinimumSize());
    }
    
    
    public void setPromptText(String text){
        this.promptText = text;
    }
    
    public String getPromptText() {
        return this.promptText;
    }
    
    
    /**
     * Repaints this component when focus is gained
     * so default text can be removed.
     */
    @Override
    public void focusGained(FocusEvent e) {
        // Select all text if focus gained from another component.
        // But don't do it if it's temporary -- this is a hack to allow
        // focus to gain on the search field when clicking a search tab,
        // without selecting it all.
        if (e.getOppositeComponent() != null && !e.isTemporary()) {
            selectAll();
        }
        repaint();
    }
    
    /**
     * Repaints this component when focus is lost
     * so default text can be displayed if no text has been entered.
     */
    @Override
    public void focusLost(FocusEvent e) {
        repaint();
    }
    
    /**
     * Returns the effective insets rendered by a custom painter.  This can be
     * used to determine the actual dimensions as drawn on the screen.
     */
    @Override
    public Insets getPaintedInsets() {
        return (backgroundPainter == null) ? new Insets(0, 0, 0, 0) : paintedInsets;
    }
    
    /**
     * Sets the background painter for this component
     */
    public void setBackgroundPainter(Painter<JTextField> painter) {
        this.backgroundPainter = painter;
        
        // Set painted insets.  For now, we assume that a non-null painter 
        // visualizes the component with 1-pixel insets around the edges.
        if (painter != null) {
            paintedInsets = new Insets(1, 1, 1, 1);
        }
    }
    
    /**
     * Sets the painter on this component that manages painting of 
     *  the prompt text when the text field is empty
     */
    public void setPromptPainter(Painter<JTextField> painter) {
        this.promptPainter = painter;
    }
    
    /**
     * Paints this component, including an icon and
     * the default text when this component has focus and has no text value.
     */
    @Override
    protected void paintComponent(Graphics g) {

        if (backgroundPainter != null) {
            this.backgroundPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        }
            
        super.paintComponent(g);
    
        if (promptPainter != null && !hasFocus() && getText().isEmpty() && promptText != null) {
            promptPainter.paint((Graphics2D) g, this, getWidth(), getHeight());
        }

    }
    
}
