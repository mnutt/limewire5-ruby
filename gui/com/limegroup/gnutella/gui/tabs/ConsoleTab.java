package com.limegroup.gnutella.gui.tabs;

import javax.swing.JComponent;

import org.limewire.core.settings.ApplicationSettings;

import com.limegroup.gnutella.gui.Console;
import com.limegroup.gnutella.gui.I18n;

/**
 * This class contains all elements of the tab for the console display.
 */
public final class ConsoleTab extends AbstractTab {

    /**
     * Constant for the <tt>JSplitPane</tt> instance separating the monitor
     * from the uploads.
     */
    private final JComponent COMPONENT;

    /**
     * Constructs the tab for the console.
     * 
     * @param CONSOLE
     *            the <tt>Console</tt> instance containing all component for
     *            the console display and handling
     */
    public ConsoleTab(final Console CONSOLE) {
        super(I18n.tr("Console"),
                I18n.tr("View Console Messages"), "console_tab");
        COMPONENT = CONSOLE;
    }

    @Override
    public void storeState(boolean visible) {
        ApplicationSettings.CONSOLE_VIEW_ENABLED.setValue(visible);
    }

    @Override
    public JComponent getComponent() {
        return COMPONENT;
    }
}
