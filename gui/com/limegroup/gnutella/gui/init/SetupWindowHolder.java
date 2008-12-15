package com.limegroup.gnutella.gui.init;

import java.awt.CardLayout;

import javax.swing.JPanel;

/**
 * This class serves two purposes.  First, it is a JPanel that
 * contains the body of a LimeWire setup window.  Second, it 
 * serves as a proxy for the underlying SetupWindow object that
 * that handles the actual drawing.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class SetupWindowHolder extends JPanel {

	/**
	 * The <tt>CardLayout</tt> instance for the setup windows.
	 */
	private final CardLayout CARD_LAYOUT = new CardLayout();

	/**
	 * Sets the <tt>CardLayout</tt> for the setup windows.
	 */
	SetupWindowHolder() {
		setLayout(CARD_LAYOUT);	   
	}

	/**
	 * Adds the speficied window to the CardLayout based on its title.
	 *
	 * @param window the <tt>SetupWindow</tt> to add
	 */
	void add(SetupWindow window) {
		add(window, window.getKey());
	}

	/**
	 * Shows the window speficied by its title.
	 * 
	 * @param key the unique key of the <tt>Component</tt> to show
	 */
	void show(String key) {
		CARD_LAYOUT.show(this, key);
	}
	
}
