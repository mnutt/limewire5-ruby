package org.limewire.ui.swing.components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.BorderFactory;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.text.BadLocationException;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;

public class LimePromptPasswordField extends JPasswordField implements FocusListener {
    
    private String promptText;
    
    @Resource private Color promptColour;
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color borderColour;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    private Painter<JTextField> backgroundPainter;
    private Painter<LimePromptPasswordField> promptPainter;
    
    /**
     * Controls the visibility of the faint white accent bubble
     *  that is painted under components in some areas. 
     */
    private AccentType accentType = AccentType.SHADOW;
    
    /**
     * Allows the default border paint to be overridden
     */
    private Paint workingBorder = null;  
    
    
    public LimePromptPasswordField() {
        this.init();
    }
    
    public LimePromptPasswordField(AccentType accentType) {
        this.accentType = accentType;
        this.init();
    }
    
    public LimePromptPasswordField(String promptText) {
        this.setPromptText(promptText);        
        this.init();
    }
    
    public LimePromptPasswordField(String promptText, AccentType accentType) {
        this.accentType = accentType;
        this.setPromptText(promptText);        
        this.init();
    }
    
    public LimePromptPasswordField(String promptText, AccentType accentType, Paint border) {
        this.promptText = promptText;
        this.accentType = accentType;
        this.workingBorder = border;        
        init();
    }
    
    
    public LimePromptPasswordField(String promptText, int columns) {
        this.promptText = promptText;
        this.setColumns(columns);
        init();
    }
    
    private void init() {
        GuiUtils.assignResources(this);
        
        if (workingBorder == null) {
            workingBorder = borderColour;
        }            
        
        TextFieldClipboardControl.install(this);
        this.addFocusListener(this);
        this.setOpaque(false);
        
        this.setMinimumSize(new Dimension(150,22));
        this.setPreferredSize(this.getMinimumSize());
        
        this.setBorder(BorderFactory.createEmptyBorder(2,10,2,12));
        this.backgroundPainter = createBackgroundPainter();
        this.promptPainter = createPromptPainter();        
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
     * Sets the background painter for this component
     */
    public void setBackgroundPainter(Painter<JTextField> painter) {
        this.backgroundPainter = painter;
    }
    
    /**
     * Sets the painter on this component that manages painting of 
     *  the prompt text when the text field is empty
     */
    public void setPromptPainter(Painter<LimePromptPasswordField> painter) {
        this.promptPainter = painter;
    }
    
    /**
     * Paints this component, including an icon and
     * the default text when this component has focus and has no text value.
     */
    @Override
    protected void paintComponent(Graphics g) {
        this.backgroundPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());        
        super.paintComponent(g);    
        if (!hasFocus() && new String(getPassword()).isEmpty() && this.promptText != null) {
            this.promptPainter.paint((Graphics2D) g, this, this.getWidth(), this.getHeight());
        }

    }
    
    private Painter<LimePromptPasswordField> createPromptPainter() {
        AbstractPainter<LimePromptPasswordField> painter = new AbstractPainter<LimePromptPasswordField>() {

            @Override
            protected void doPaint(Graphics2D g, LimePromptPasswordField object, int width, int height) {
                g.setColor(promptColour);
                g.setFont(object.getFont());
                
                int dot  = object.getCaret().getDot();
                Rectangle r = null;
                
                // Find the carat position
                try {
                    r = object.modelToView(dot);
                } catch (BadLocationException e) { 
                    // Carat location could not be found 
                    //  therefore do not attempt to print
                    //  the prompt text since 
                    //  it will not match properly with
                    //  the text position
                    return; 
                }
                
                int x = r.x;
                int y = r.y + r.height - 3;
                g.drawString(object.getPromptText(), x, y);
            }
        };
        
        painter.setAntialiasing(true);
        
        return painter;
    }
    
    private Painter<JTextField> createBackgroundPainter() {
        
        CompoundPainter<JTextField> compoundPainter = new CompoundPainter<JTextField>();
        
        RectanglePainter<JTextField> painter = new RectanglePainter<JTextField>();
        
        painter.setRounded(true);
        painter.setFillPaint(Color.WHITE);
        painter.setRoundWidth(this.arcWidth);
        painter.setRoundHeight(this.arcHeight);
        painter.setInsets(new Insets(2,2,2,2));
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        
        compoundPainter.setPainters(painter, new BorderPainter<JTextField>(this.arcWidth, this.arcHeight,
                workingBorder,  this.bevelLeft,  this.bevelTop1,  this.bevelTop2, 
                this.bevelRight,  this.bevelBottom, this.accentType));
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
    }
}
