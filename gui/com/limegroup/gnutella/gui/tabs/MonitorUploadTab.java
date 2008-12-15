package com.limegroup.gnutella.gui.tabs;

import javax.swing.JComponent;
import javax.swing.JSplitPane;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.UISettings;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MonitorView;
import com.limegroup.gnutella.gui.tables.ComponentMediator;
import com.limegroup.gnutella.gui.util.DividerLocationSettingUpdater;

/**
 * This class contains all elements of the tab for the monitor and upload
 * displays.
 */
public final class MonitorUploadTab extends AbstractTab {

	/**
	 * Constant for the <tt>JSplitPane</tt> instance separating the 
	 * monitor from the uploads.
	 */
	private final JSplitPane SPLIT_PANE;

	/**
	 * Constructs the tab for monitors and uploads.
	 *
	 * @param MONITOR_VIEW the <tt>MonitorView</tt> instance containing
	 *  all component for the monitor display and handling
	 * @param UPLOAD_MEDIATOR the <tt>UploadMediator</tt> instance containing
	 *  all component for the monitor display and handling 
	 */
	public MonitorUploadTab(final MonitorView MONITOR_VIEW, 
							final ComponentMediator UPLOAD_MEDIATOR) {
		super(I18n.tr("Monitor"),
		        I18n.tr("View Searches and Uploads"), "monitor_tab");
		SPLIT_PANE = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
									MONITOR_VIEW, 
									UPLOAD_MEDIATOR.getComponent());
        SPLIT_PANE.setContinuousLayout(true);
		SPLIT_PANE.setOneTouchExpandable(true);
		DividerLocationSettingUpdater.install(SPLIT_PANE,
				UISettings.UI_MONITOR_UPLOAD_TAB_DIVIDER_LOCATION);
	}

	@Override
    public void storeState(boolean visible) {
        ApplicationSettings.MONITOR_VIEW_ENABLED.setValue(visible);
	}

	@Override
    public JComponent getComponent() {
		return SPLIT_PANE;
	}
}
