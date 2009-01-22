package org.limewire.ui.swing.components;

import java.awt.Cursor;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;


/**
 * Factory (and decorator used when extending LimeComboBox) that creates the three
 *  types of of "combo boxes" we use in the limewire ui.
 *  
 *  Types:
 *  
 *    Full - The full combo box type with a selectable slot.  These work mostly 
 *            the same as regular JComboBoxes.  
 *            ie.  Search category dropdown.
 *            
 *   Mini - These boxes act more as toggles for drop down menu's of actions.
 *           ie.  The More button, the from widget, etc.
 *  
 *  Colour Scheme:
 *  
 *    Dark - Use the a "dark" colour scheme.  These are usually found
 *                        ontop of dark panels such at the header bars
 *                      
 *    Light - Use a ligher colour sheme.  These are usually found ontop of 
 *                        lightly coloured panels.  In this case mostly 
 *                        the top search bar
 *                        
 *                        
 *                        
 *  
 */
@Singleton
public class LimeComboBoxFactory {
    
    private final ButtonDecorator buttonDecorator;
    
    @Resource private Icon miniRegIcon;
    @Resource private Icon miniHoverIcon;
    @Resource private Icon miniDownIcon;
    @Resource private Icon lightFullIcon;
    @Resource private Icon darkFullIcon;
    
    @Inject
    LimeComboBoxFactory(ButtonDecorator buttonDecorator) {
        GuiUtils.assignResources(this);  
        
        this.buttonDecorator = buttonDecorator;        
    }
    
    public LimeComboBox createDarkFullComboBox() {
        return this.createDarkFullComboBox(null);
    }
    
    public LimeComboBox createDarkFullComboBox(List<Action> items) {
        LimeComboBox box = new LimeComboBox(items);
        buttonDecorator.decorateDarkFullButton(box);
        box.setIcon(this.darkFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        return box;
    }    
    
    public LimeComboBox createDarkFullComboBox(List<Action> items, AccentType accentType) {
        LimeComboBox box = new LimeComboBox(items);
        buttonDecorator.decorateDarkFullButton(box, accentType);
        box.setIcon(this.darkFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        return box;
    }
        
    
    public LimeComboBox createLightFullComboBox(List<Action> items) {
        LimeComboBox box = new LimeComboBox(items);
        buttonDecorator.decorateLightFullButton(box);
        box.setIcon(this.lightFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        return box;
    }
    
    public LimeComboBox createMiniComboBox() {
        return createMiniComboBox(null,null);
    }
    
    public LimeComboBox createMiniComboBox(String promptText, List<Action> items) {
        LimeComboBox box = new LimeComboBox(items);
        this.decorateMiniComboBox(box, promptText);
        return box;
    }
    
    public void decorateDarkMiniComboBox(LimeComboBox box, String promptText) {
        buttonDecorator.decorateDarkFullButton(box);
        box.setIcon(this.darkFullIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,10,2,20));
        box.setText(promptText);
    }
    
    public void decorateMiniComboBox(LimeComboBox box, String promptText) {
        buttonDecorator.decorateMiniButton(box);
        box.setIcons(this.miniRegIcon, this.miniHoverIcon, this.miniDownIcon);
        box.setBorder(BorderFactory.createEmptyBorder(2,6,3,15));
        box.setText(promptText);
        box.setMouseOverCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
}
