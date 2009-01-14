/*
 * ShutdownPaneItem.java
 *
 * Created on November 3, 2001, 8:42 AM
 */

package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.i18n.I18nMarker;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.ResourceManager;

/**
 * This class defines the panel in the options
 * window that allows the user to select the
 * default shutdown behavior.
 */
public class ShutdownPaneItem extends AbstractPaneItem { 
    
    public final static String TITLE = I18n.tr("Shutdown Behavior");
    
    public final static String LABEL = I18n.tr("You can choose the default shutdown behavior.");

    /** RadioButton for selecting immediate shutdown
     */    
    private JRadioButton shutdownImmediately;
    
    /** RadioButton for selecting the shutdown after transfer
     * completion option.
     */    
    private JRadioButton shutdownAfterTransfers;
    
    /** RadioButton for selecting the minimize to tray option.  This
     * option is only displayed on systems that support the tray.
     */    
    private JRadioButton minimizeToTray;

    /** Creates new ShutdownOptionsPaneItem
     *
     * @param key the key for this <tt>AbstractPaneItem</tt> that 
     *      the superclass uses to generate locale-specific keys
     */
    public ShutdownPaneItem() {
        super(TITLE, LABEL);
        
        BoxPanel buttonPanel = new BoxPanel();
        
        String immediateLabel = I18nMarker.marktr("Shutdown Immediately");
        String whenReadyLabel = I18nMarker.marktr("Shutdown after Transfers");
        String minimizeLabel  = I18nMarker.marktr("Minimize to System Tray");
        shutdownImmediately = new JRadioButton(I18n.tr(immediateLabel));
        shutdownAfterTransfers = new JRadioButton(I18n.tr(whenReadyLabel));
        minimizeToTray = new JRadioButton(I18n.tr(minimizeLabel));
        
        ButtonGroup bg = new ButtonGroup();
        buttonPanel.add(shutdownImmediately);
        buttonPanel.add(shutdownAfterTransfers);
        bg.add(shutdownImmediately);
        bg.add(shutdownAfterTransfers);
        if (OSUtils.supportsTray() && ResourceManager.instance().isTrayIconAvailable()) {
            buttonPanel.add(minimizeToTray);
            bg.add(minimizeToTray);
        }
        
        BoxPanel mainPanel = new BoxPanel(BoxPanel.X_AXIS);
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createHorizontalGlue());
        
        add(mainPanel);
    }

    /**
     * Applies the options currently set in this <tt>PaneItem</tt>.
     *
     * @throws IOException if the options could not be fully applied
     */
    @Override
    public boolean applyOptions() throws IOException {
//        if (minimizeToTray.isSelected()) {
//            ApplicationSettings.MINIMIZE_TO_TRAY.setValue(true);
//            ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.setValue(false);
//        } else if (shutdownAfterTransfers.isSelected()) {
//            ApplicationSettings.MINIMIZE_TO_TRAY.setValue(false);
//            ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.setValue(true);
//        } else { // if(shutdownImmediately.isSelected())
//            ApplicationSettings.MINIMIZE_TO_TRAY.setValue(false);
//            ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.setValue(false);
//        }
        return false;
    }
    
    /**
     * Sets the options for the fields in this <tt>PaneItem</tt> when the
     * window is shown.
     */
    @Override
    public void initOptions() {
        if (ApplicationSettings.MINIMIZE_TO_TRAY.getValue()) {
            if (OSUtils.supportsTray() && !ResourceManager.instance().isTrayIconAvailable()) {
                shutdownAfterTransfers.setSelected(true);
            } else {
                minimizeToTray.setSelected(true);
            }
//        } else if (ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.getValue()) {
//             shutdownAfterTransfers.setSelected(true);
        } else {
            shutdownImmediately.setSelected(true);
        }
    }
    
    public boolean isDirty() {
        return false;
//        boolean minimized =
//            ApplicationSettings.MINIMIZE_TO_TRAY.getValue() &&
//            !ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.getValue();
//        boolean reallyMinimized = minimized && ResourceManager.instance().isTrayIconAvailable();
//        
//        boolean afterTransfers =
//            !ApplicationSettings.MINIMIZE_TO_TRAY.getValue() &&
//            ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.getValue();
//        boolean reallyAfterTransfers =
//            afterTransfers || (minimized && !ResourceManager.instance().isTrayLibraryLoaded());
//        
//        boolean immediate = 
//            !ApplicationSettings.MINIMIZE_TO_TRAY.getValue() &&
//            !ApplicationSettings.SHUTDOWN_AFTER_TRANSFERS.getValue();
//        
//        return minimizeToTray.isSelected() != reallyMinimized ||
//               shutdownAfterTransfers.isSelected() != reallyAfterTransfers ||
//               shutdownImmediately.isSelected() != immediate;
    }
}
