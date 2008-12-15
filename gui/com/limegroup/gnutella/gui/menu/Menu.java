package com.limegroup.gnutella.gui.menu;

import javax.swing.JMenu;

/**
 * Defines the minimal necessary methods for menus in the main application
 * menu bar.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public interface Menu {

	/**
	 * Returns the <tt>JMenu</tt> instance for this <tt>Menu</tt>.
	 * 
	 * @return the <tt>JMenu</tt> instance for this <tt>Menu</tt>	
	 */
	JMenu getMenu();
}
