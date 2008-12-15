package com.limegroup.gnutella.gui.tabs;

import javax.swing.JComponent;

import org.limewire.core.settings.ApplicationSettings;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.logging.LoggingMediator;

public class LoggingTab extends AbstractTab {
    
    /** visible component. */
    private final JComponent COMPONENT;

    /**
     * Constructs the tab for the console.
     * 
     * @param CONSOLE
     *            the <tt>Console</tt> instance containing all component for
     *            the console display and handling
     */
    public LoggingTab(final LoggingMediator logger) {
        super(I18n.tr("Logging"),
                I18n.tr("View Logging Messages"), "logging_tab");
        COMPONENT = logger.getComponent();
    }

    @Override
    public void storeState(boolean visible) {
        ApplicationSettings.LOGGING_VIEW_ENABLED.setValue(visible);
    }

    @Override
    public JComponent getComponent() {
        return COMPONENT;
    }
}
