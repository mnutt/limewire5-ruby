package com.limegroup.gnutella.gui.menu;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.GUIMediator.Tabs;
import com.limegroup.gnutella.util.LogUtils;

/**
 * Contains all of the menu items for the navigation menu.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class NavMenu extends AbstractMenu {
    
    /**
	 * Creates a new <tt>NavMenu</tt>, using the <tt>key</tt> 
	 * argument for setting the locale-specific title and 
	 * accessibility text.
	 *
	 * @param key the key for locale-specific string resources unique
	 *            to the menu
	 */
	NavMenu() {
	    super(I18n.tr("&Navigation"));
	
		for (Tabs tab : Tabs.values()) {
		    if (tab == Tabs.CONSOLE && !LogUtils.isLog4JAvailable()) {
		        continue;
		    }
		    if( tab != Tabs.LWS || GUIMediator.isBrowserCapable() )
		        addMenuItem(tab.getNavigationAction());
        }
    }
}
