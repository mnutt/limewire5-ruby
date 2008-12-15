package com.limegroup.gnutella.gui.upload;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.limegroup.gnutella.gui.I18n;

/**
 * This class contains the popup menu that is visible on right-click events in
 * the upload window.
 */
final class UploadPopupMenu {
	
	/**
	 * Constant for the <tt>JPopupMenu</tt> that contains all of the menu items.
	 */
	private final JPopupMenu MENU = new JPopupMenu();

	/**
	 * The index of the kill download menu item.
	 */
	static final int KILL_INDEX = 0;

	/**
	 * The index of the chat menu item.
	 */
	static final int CHAT_INDEX = 1;
	
	/**
	 * The index of the browse menu item.
	 */
	static final int BROWSE_INDEX = 2;
	
	UploadPopupMenu(final UploadMediator um) {
        /**
    	 * Constant for the kill download menu item.
    	 */
    	final JMenuItem KILL_ITEM = new JMenuItem(
            I18n.tr("Kill Upload")
        );
    
    	/**
    	 * Constant for the chat menu item.
    	 */
    	final JMenuItem CHAT_ITEM = new JMenuItem(
            I18n.tr("Chat with Host")
        );
        
        /**
         * Constant for the browse menu item.
         */
        final JMenuItem BROWSE_ITEM = new JMenuItem(
            I18n.tr("Browse Host")
        );
    
   		KILL_ITEM.addActionListener( um.REMOVE_LISTENER );
        CHAT_ITEM.addActionListener( um.CHAT_LISTENER );
        BROWSE_ITEM.addActionListener( um.BROWSE_LISTENER );
        MENU.add(KILL_ITEM);
        MENU.add(CHAT_ITEM);
        MENU.add(BROWSE_ITEM);
    }
    
    JPopupMenu getComponent() { return MENU; }
    
}
