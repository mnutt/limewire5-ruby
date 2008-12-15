package com.limegroup.gnutella.gui.menu;

import java.awt.event.ActionEvent;

import org.limewire.i18n.I18nMarker;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.AbstractAction;

/**
 * Contains all of the menu items for the tools menu.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class ToolsMenu extends AbstractMenu {
    
    @InspectablePrimitive("optionsShownFromMenu")
    private static int optionsShown = 0;
    
	/**
	 * Creates a new <tt>ToolsMenu</tt>, using the <tt>key</tt> 
	 * argument for setting the locale-specific title and 
	 * accessibility text.
	 *
	 * @param key the key for locale-specific string resources unique
	 *            to the menu
	 */
	ToolsMenu() {
	    super(I18n.tr("&Tools"));

        if (!OSUtils.isMacOSX()) {
            addMenuItem(new ShowOptionsAction());
        }

        MENU.add(new AdvancedMenu().getMenu());
    }

    private static class ShowOptionsAction extends AbstractAction {
	    
	    public ShowOptionsAction() {
	        super(I18n.tr("&Options"));
	        putValue(LONG_DESCRIPTION, I18nMarker.marktr("Display the Options Screen"));
        }
	    
	    public void actionPerformed(ActionEvent e) {
	        ++optionsShown;
	        GUIMediator.instance().setOptionsVisible(true);
	    }
	}

}
