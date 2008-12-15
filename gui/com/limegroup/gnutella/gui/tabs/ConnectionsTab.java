package com.limegroup.gnutella.gui.tabs;

import javax.swing.JComponent;

import org.limewire.core.settings.ApplicationSettings;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tables.ComponentMediator;

/**
 * This class contains access to the connections tab properties.
 */
public final class ConnectionsTab extends AbstractTab {

	/**
	 * Constant for the <tt>Component</tt> instance containing the 
	 * elements of this tab.
	 */
	private final JComponent COMPONENT;

	/**
	 * Construcs the connections tab.
	 *
	 * @param CONNECTION_MEDIATOR the <tt>ConectionMediator</tt> instance
	 */
	public ConnectionsTab(final ComponentMediator CONNECTION_MEDIATOR) {
		super(I18n.tr("Connections"),
		        I18n.tr("Show Connections to Other Clients"), "connection_tab");
		COMPONENT = CONNECTION_MEDIATOR.getComponent();
	}

	@Override
    public void storeState(boolean visible) {
        ApplicationSettings.CONNECTION_VIEW_ENABLED.setValue(visible);
	}

	@Override
    public JComponent getComponent() {
		return COMPONENT;
	}
}
