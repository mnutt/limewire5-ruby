package com.limegroup.gnutella.gui.swingbrowser;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.limewire.core.settings.SWTBrowserSettings;
import org.limewire.io.GUID;
import org.limewire.setting.BooleanSetting;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.impl.ChromeAdapter;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.LimeAction;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Extends Mozilla's Swing browser to add LimeWire-specific buttons and home.
 * Opens target="_blank" links in native browser.
 */
public class SwingBrowser extends MozillaPanel {

    /**
     * The text pane holding the current address.
     */
    private JTextField swingAddressField;


    /**
     * Action for going back.
     */
    private Action swingBackAction;

    /**
     * Action for going forward.
     */
    private Action swingForwardAction;

    /**
     * Action for stopping.
     */
    private Action swingStopAction;
    
   
    public SwingBrowser() {
        this(null, null);
    }

    public SwingBrowser(VisibilityMode toolbarVisMode, VisibilityMode statusbarVisMode) {
        super(toolbarVisMode, statusbarVisMode);
        createToolbar();
    }

    protected void createToolbar() {
        makeNavigationPanel();
    }
   
    
    private void makeNavigationPanel() {
        toolbar = new JToolBar();
        toolbar.setFloatable(false);
        add(toolbar, BorderLayout.NORTH);

        // Add the navigation buttons
        final JPanel buttonRow = makeBrowserButtons();
        toolbar.add(buttonRow);
        buttonRow.setFocusable(false);

        // The URL field
        swingAddressField = new JTextField(60);
        toolbar.add(swingAddressField);
        final Action goAction = new AbstractAction(I18n.tr("Go")) {
            public void actionPerformed(ActionEvent e) {
                String url = swingAddressField.getText();
                if (url == null)
                    return;
                load(makeNicerURL(url.trim()));
            }
        };
        swingAddressField.addActionListener(goAction);
        final JButton go = new JButton(goAction);
        toolbar.add(go);

        final BooleanSetting setting = SWTBrowserSettings.BROWSER_SHOW_ADDRESS;
        swingAddressField.setVisible(setting.getValue());
        go.setVisible(setting.getValue());
        setting.addSettingListener(new SettingListener() {
            public void settingChanged(final SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        swingAddressField.setVisible(setting.getValue());
                        go.setVisible(setting.getValue());
                    }
                });
            }
        });

    }

    private ButtonRow makeBrowserButtons() {
        Action[] as = new Action[] { new BrowserButtonAction(I18n.tr("Back"), "SWT_BROWSER_BACK") {
            public void actionPerformed(ActionEvent e) {
                goBack();
            }
        }, new BrowserButtonAction(I18n.tr("Stop"), "SWT_BROWSER_STOP") {
            public void actionPerformed(ActionEvent e) {
                stop();
            }
        }, new BrowserButtonAction(I18n.tr("Store"), "SWT_BROWSER_HOME") {
            public void actionPerformed(ActionEvent e) {
                goHome();
            }
        }, new BrowserButtonAction(I18n.tr("Forward"), "SWT_BROWSER_NEXT") {
            public void actionPerformed(ActionEvent e) {
                goForward();
            }
        }, };
        ButtonRow res = new ButtonRow(as, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);
        swingBackAction = as[0];
        swingBackAction.setEnabled(false);
        swingStopAction = as[1];
        swingStopAction.setEnabled(false);
        swingForwardAction = as[as.length - 1];
        swingForwardAction.setEnabled(false);
        return res;
    }

    /**Cleans up the location URL.
     * 
     * @param location the URL to be cleaned up
     * @return location with everything from "guid=" on removed
     */
    private String cleanUpLocation(String location) {
        String guid = GUID.toHexString(GuiCoreMediator.getApplicationServices().getMyGUID());
        if (location != null) {
            int idx = location.indexOf("guid=" + guid);
            if (idx != -1 && idx != 0) {
                location = location.substring(0, idx - 1);
            }
        }
        return location;
    }

    private static abstract class BrowserButtonAction extends AbstractAction {
        BrowserButtonAction(String shortDescriptionKey, String iconName) {

            // We need to preserve the size, if it's not set then the text
            // field grows taller than we want
            putValue(Action.NAME, "");

            putValue(Action.SHORT_DESCRIPTION, shortDescriptionKey);
            putValue(LimeAction.ICON_NAME, iconName);
        }
    }

    private static String makeNicerURL(String url) {       
        if (url.indexOf("://") == -1)
            url = "http://" + url;
        return url;
    }

    /**
     * Pressing the home button on the browser.
     */
    public void goHome() {
        setURL(getHomeURL());
    }

    private void setURL(final String u) {
        load(u);
    }

    private String getHomeURL() {
        String url = SWTBrowserSettings.BROWSER_HOME_URL.getValue();
        byte[] guid = GuiCoreMediator.getApplicationServices().getMyGUID();
        return LimeWireUtils.addLWInfoToUrl(url, guid);
    }

    @Override
    public void onSetStatus(String text) {
        statusField.setText(text);
    }

    @Override
    public void onEnableBackButton(boolean enabled) {
        swingBackAction.setEnabled(enabled);
    }

    @Override
    public void onEnableForwardButton(boolean enabled) {
        swingForwardAction.setEnabled(enabled);
    }

    @Override
    public void onEnableReloadButton(boolean enabled) {
        // we don't have a reload button
    }

    @Override
    public void onEnableStopButton(boolean enabled) {
        swingStopAction.setEnabled(enabled);
    }

    @Override
    public void onSetUrlbarText(String url) {
        swingAddressField.setText(cleanUpLocation(url));
    }
    
    private void initialize() {
        BrowserUtils.addDomListener(getChromeAdapter());
        addKeyListener(new MozillaKeyListener(getChromeAdapter()));
    }
  
    //overridden to remove LimeDomListener
    @Override
    public void onDetachBrowser() {
        BrowserUtils.removeDomListener(getChromeAdapter());
        super.onDetachBrowser();
    }
    
    //overridden for browser initialization that can not be done earlier
    @Override
    public void onAttachBrowser(ChromeAdapter chromeAdapter, ChromeAdapter parentChromeAdapter){
        super.onAttachBrowser(chromeAdapter, parentChromeAdapter);
        initialize();
    }
  
}
   

