package com.limegroup.gnutella.gui.menu;

import java.awt.event.ActionEvent;

import org.limewire.core.settings.UISettings;
import org.limewire.setting.BooleanSetting;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LanguageWindow;
import com.limegroup.gnutella.gui.ResourceManager;
import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.actions.ToggleSettingAction;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeSettings;


/**
 * This class manages the "view" menu that allows the user to dynamically select
 * which tabs should be viewable at runtime & themes to use.
 */
final class ViewMenu extends AbstractMenu {
    
    
    ViewMenu(final String key) {
        super(I18n.tr("&View"));
        MENU.add(new ShowHideMenu().getMenu() );
        addSeparator();

        MENU.add( new ThemeMenu().getMenu() );
        
        MENU.addSeparator();
        ToggleSettingAction toggleAction = new ToggleIconSettingAction(UISettings.SMALL_ICONS,
                I18n.tr("Use &Small Icons"),
                I18n.tr("Use Small Icons"));
        addToggleMenuItem(toggleAction);
        
        toggleAction = new ToggleIconSettingAction(UISettings.TEXT_WITH_ICONS,
                I18n.tr("Show Icon &Text"),
                I18n.tr("Show Text Below Icons"));
        addToggleMenuItem(toggleAction);
        
        addMenuItem(new ChangeFontSizeAction(2, 
                I18n.tr("&Increase Font Size"),
                I18n.tr("Increases the Font Size")));
        
        addMenuItem(new ChangeFontSizeAction(-2,
                I18n.tr("&Decrease Font Size"),
                I18n.tr("Decreases the Font Size")));
        
        MENU.addSeparator();
        
        addMenuItem(new ShowLanguageWindowAction());
    }
    
    private static class ShowLanguageWindowAction extends AbstractAction {
        
        public ShowLanguageWindowAction() {
            super(I18n.tr("C&hange Language"));
            putValue(LONG_DESCRIPTION, I18n.tr("Select your Language Prefereces"));
    }
    
    	public void actionPerformed(ActionEvent e) {
        	LanguageWindow lw = new LanguageWindow();
        	GUIUtils.centerOnScreen(lw);
            lw.setVisible(true);
    	}
    }
    
    private static class ToggleIconSettingAction extends ToggleSettingAction {
        
        public ToggleIconSettingAction(BooleanSetting setting, String name, String description) {
            super(setting, name, description);
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            super.actionPerformed(e);
            GUIMediator.instance().buttonViewChanged();
        }
   }
   
   private static class ChangeFontSizeAction extends AbstractAction {

       private final int increment;
       
       public ChangeFontSizeAction(int inc, String name, String description) {
           super(name);
           putValue(LONG_DESCRIPTION, description);
           increment = inc;
       }
       
       
       public void actionPerformed(ActionEvent e) {
           int inc = ThemeSettings.FONT_SIZE_INCREMENT.getValue();
           inc += increment;
           ThemeSettings.FONT_SIZE_INCREMENT.setValue(inc);
           ResourceManager.setFontSizes(increment);
           ThemeMediator.updateComponentHierarchy();
       }
       
   }
    
}







