package com.limegroup.gnutella.gui.init;

import javax.swing.Icon;
import org.limewire.i18n.I18nMarker;
import com.limegroup.gnutella.gui.GUIMediator;

/**
 * this class displays information welcoming the user to the
 * setup wizard.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class WelcomeWindow extends SetupWindow {
    
	/**
	 * Creates the window and its components
	 */
	WelcomeWindow(boolean partial) {
		super(I18nMarker.marktr("Welcome"), partial ?
		    I18nMarker
                    .marktr("Welcome to the LimeWire setup wizard. LimeWire has recently added new features that require your configuration. LimeWire will guide you through a series of steps to configure these new features.") : I18nMarker
                    .marktr("Welcome to the LimeWire setup wizard. LimeWire will guide you through a series of steps to configure LimeWire for optimum performance."));
	}
	
	@Override
    public Icon getIcon() {
		return GUIMediator.getThemeImage("lw_logo");
	}

    protected void createPageContent() { }
}
