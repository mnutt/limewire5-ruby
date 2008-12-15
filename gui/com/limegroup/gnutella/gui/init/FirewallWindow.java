package com.limegroup.gnutella.gui.init;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;

/**
 * Informs the user that firewall warnings might appear, and makes them appear.
 */
final class FirewallWindow extends SetupWindow {

	/**
	 * Creates the window and its components.
	 */
	FirewallWindow() {
		super(I18nMarker.marktr("Firewall Setup"), I18nMarker
                .marktr("LimeWire performs best with full access to the Internet. If firewall or security warnings appear, please unblock LimeWire and choose to remember the setting. Your firewall software may look different than the below picture."));
    }
    
    protected void createPageContent() {

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JLabel(GUIMediator.getThemeImage("firewall_warnings")), BorderLayout.CENTER);
        setSetupComponent(mainPanel);
	}
	
	@Override
    public void beforePageShown() {
	    super.beforePageShown();
	    
        GuiCoreMediator.getLifecycleManager().loadBackgroundTasks();
    }
}
