package com.limegroup.gnutella.gui;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * This class contains the logo and the searching icon for the application.
 */
final class LogoPanel extends BoxPanel /* implements ThemeObserver */ {

	/**
	 * Icon for the when we're searching.
	 */
	private ImageIcon _searchingIcon;

	/**
	 * Icon for not searching.
	 */
	private ImageIcon _notSearchingIcon;

	/**
	 * Constant for the <tt>JLabel</tt> used for displaying the lime/spinning
	 * lime search status indicator.
	 */
	private final JLabel ICON_LABEL = new JLabel();

	private final JLabel LOGO_LABEL = new JLabel();

	private boolean _searching;

	/**
	 * Constructs a new panel containing the logo and the search icon.
	 */
	LogoPanel() {
		super(BoxPanel.X_AXIS);
		updateTheme();

		final LogoPanel LOGO_PANEL = this;

		this.addMouseListener(new MouseAdapter() {
			@Override
            public void mouseClicked(MouseEvent me) {
				GUIMediator.openURL("http://www.limewire.com");
			}

			@Override
            public void mouseEntered(MouseEvent me){
				LOGO_PANEL.setCursor(new Cursor(Cursor.HAND_CURSOR));
			}
		});
		//GUIMediator.addThemeObserver(this);
	}

	// inherit doc comment
	public void updateTheme() {
		_searchingIcon = GUIMediator.getThemeImage("radar_lime");
		_notSearchingIcon = GUIMediator.getThemeImage("still_lime");
		if(_searching) {
			ICON_LABEL.setIcon(_searchingIcon);
		} else {
			ICON_LABEL.setIcon(_notSearchingIcon);
		}
		ImageIcon logoIcon = GUIMediator.getThemeImage("lw_logo");
		LOGO_LABEL.setIcon(logoIcon);
		LOGO_LABEL.setSize(logoIcon.getIconWidth(),
						   logoIcon.getIconHeight());
		ICON_LABEL.setSize(_searchingIcon.getIconWidth(),
						   _searchingIcon.getIconHeight());

		Dimension dim = new Dimension(LOGO_LABEL.getSize().width+
									  ICON_LABEL.getSize().width+12,
									  ICON_LABEL.getSize().height);
		this.setPreferredSize(dim);
		this.setSize(dim.width, dim.height);
		buildPanel();
        GUIUtils.setOpaque(false, this);
	}
	
	private void buildPanel() {
	    removeAll();
        add(ICON_LABEL);
        add(LOGO_LABEL);
	}       

	/**
	 * Sets the searching or not searching status of the application.
	 *
	 * @param searching the searching status of the application
	 */
	void setSearching(boolean searching) {
		_searching = searching;
		if(searching) {
			ICON_LABEL.setIcon(_searchingIcon);
		} else {
			ICON_LABEL.setIcon(_notSearchingIcon);
		}
	}
}
