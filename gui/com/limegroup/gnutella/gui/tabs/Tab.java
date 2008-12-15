package com.limegroup.gnutella.gui.tabs;

import java.beans.PropertyChangeListener;

import javax.swing.Icon;
import javax.swing.JComponent;

/**
 * This interface outlines the required functionality of any of the
 * primary tabs in the main application window.
 */
public interface Tab {
	
	/**
	 * Stores the visible/invisible state of the tab to disk.
	 *
	 * @param visible the visibility state to apply
	 */
	void storeState(boolean visible);
	
	/**
	 * Returns the <tt>JComponent</tt> instance containing all of the
	 * UI elements for the tab.
	 *
	 * @return the <tt>JComponent</tt> intance containing  all of the
	 *  UI elements for the tab
	 */
	JComponent getComponent();

	/**
	 * Returns the title of the tab as it's displayed to the user.
	 *
	 * @return the title of the tab as it's displayed to the user
	 */
	String getTitle();

	/**
	 * Returns the tooltip text for the tab.
	 *
	 * @return the tooltip text for the tab
	 */
	String getToolTip();

	/**
	 * Returns the <tt>Icon</tt> instance for the tab.
	 *
	 * @return the <tt>Icon</tt> instance for the tab
	 */
	Icon getIcon();
	
	/**
	 * Returns the name of the icon or null if not available.
	 */
	String getIconName();
	
	/**
	 * Notification that the specified tab has been clicked.
	 */
	void mouseClicked();
	
	/** Adds a listener to property changes on this tab. */
	void addPropertyChangeListener(PropertyChangeListener listener);
}
