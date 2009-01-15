package org.limewire.ui.swing.painter;

import java.awt.Graphics2D;
import java.awt.Paint;

import javax.swing.Action;
import javax.swing.Icon;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.painter.AbstractPainter;
import org.limewire.ui.swing.components.LimeComboBox;

/**
 * Painter to be used to extend general font and icon behaviour to all
 *  custom drawn buttons.  Should only be accessed through it's factory
 *  to avoid resource duplication.  
 *  
 *  NOTE: Will not respect icon alignment
 */
public class ButtonForegroundPainter extends AbstractPainter<JXButton> {

    private final Paint pressedForeground;
    private final Paint hoverForeground;
    private final Paint disabledForeground;
    
    /**
     * Creates a button foreground painter that does not
     *  change it's font colour based on mouse state.
     *  
     *  NOTE: This should not be used as a shortcut to creating
     *          buttons since it will ignore the default app style
     *          -- instead use the factory
     */
    ButtonForegroundPainter() {
        this(null, null, null);
    }
       
    /** 
     * Can be used to create a foreground painter with unique pressed and hover font colours
     *  and a right aligned icon.  
     *  
     *  NOTE: Will ignore default app style.  Use the factory if regular behaviour is desired.
     */
    public ButtonForegroundPainter(Paint hoverForeground, Paint pressedForeground, Paint disabledForeground) {
        this.pressedForeground = pressedForeground;
        this.hoverForeground = hoverForeground;
        this.disabledForeground = disabledForeground;
        
        setCacheable(false);
    }
    
    @Override
    protected void doPaint(Graphics2D g, JXButton object, int width, int height) {
        
        int textBaseline = (object.getHeight()-3)/2 
                + g.getFontMetrics().getAscent()/2;
        
        Icon icon = null;
        Paint foreground = null;
         
        if (!object.isEnabled()) {
            foreground = disabledForeground;
        }
        else if (object.getModel().isPressed()) {
            icon = object.getPressedIcon();
            foreground = pressedForeground; 
        }
        else if (object.getModel().isRollover() || object.hasFocus()) {
            icon = object.getRolloverIcon();
            foreground = hoverForeground;
        }
        else {
            icon = object.getIcon();
        }
            
        if (foreground == null) {
            foreground = object.getForeground();
        }
        
        g.setFont(object.getFont());
        g.setPaint(foreground);
        
        if (object.getText() != null) {
            g.drawString(object.getText(), object.getInsets().left, textBaseline);
            
            if (icon != null) {
                icon.paintIcon(object, g, object.getWidth() - object.getInsets().right + 3, 
                        object.getHeight()/2 - icon.getIconHeight()/2);
            }
        } 
        // TODO: should use a more OO solution in future
        else if (object instanceof LimeComboBox) {            
            LimeComboBox box = (LimeComboBox) object;            
            Action action = box.getSelectedAction();
            if (action != null) {
                Icon actionIcon = (Icon)action.getValue(Action.SMALL_ICON);
                int leftGap = object.getInsets().left;
                if(actionIcon != null) {
                    actionIcon.paintIcon(box, g, leftGap, (box.getHeight()- actionIcon.getIconHeight())/2);
                    leftGap += actionIcon.getIconWidth() + 4;
                }
                g.drawString((String)action.getValue(Action.NAME), leftGap, textBaseline);
            }
            
            if (icon != null) {
                icon.paintIcon(box, g, box.getWidth() - object.getInsets().right + icon.getIconWidth(), 
                        box.getHeight()/2 - icon.getIconHeight()/2);
            }
        }

        
    }

}
