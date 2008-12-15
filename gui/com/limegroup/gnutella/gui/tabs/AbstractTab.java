package com.limegroup.gnutella.gui.tabs;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.Icon;
import javax.swing.JComponent;

import com.limegroup.gnutella.gui.GUIMediator;

/**
 * This class provided a rudimentary implementation of key functions for
 * any tab in the primary window.  
 */
abstract class AbstractTab implements Tab {
	
	/** Constant for the title of this tab. */
	private String title;
	
	/** Constant for the tool tip for this tab. */
	private String toolTip;

	/** <tt>Icon</tt> instance to use for this tab. */
	private Icon icon;

	/** Constant for the unique key for the specific tab instance. */
	private String iconFile;
	
	/** PropertyChangeSupport. */
	private final PropertyChangeSupport propertyChangeSupport;

	/** Constructs the elements of the tab. */
	AbstractTab(String title, String tooltip, String icon) {
		this.title     = title;
		this.toolTip  = tooltip;
		this.iconFile = icon;
		this.icon     = GUIMediator.getThemeImage(iconFile);
		this.propertyChangeSupport = new PropertyChangeSupport(this);
	}

	public abstract void storeState(boolean state);
	
	public abstract JComponent getComponent();

	public String getTitle() {
		return title;
	}

	public String getToolTip() {
		return toolTip;
	}

	public Icon getIcon() {
		return icon;
	}
	
	public String getIconName() {
	    return iconFile;
	}

	@Override
    public String toString() {
		return title + " tab";
	}
	
	void changeTitle(String newTitle) {
	    String oldTitle = title;
	    this.title = newTitle;
	    propertyChangeSupport.firePropertyChange("title", oldTitle, newTitle);
	}
	
	void changeTooltip(String newTooltip) {
	    String oldTooltip = toolTip;
	    this.toolTip = newTooltip;
	    propertyChangeSupport.firePropertyChange("tooltip", oldTooltip, newTooltip);
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
	    propertyChangeSupport.addPropertyChangeListener(listener);
	}
	
	public void mouseClicked() {}
	    
}
