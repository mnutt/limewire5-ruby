package com.limegroup.gnutella.gui.tabs;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.SWTBrowserSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.swingbrowser.SwingBrowser;

/**
 * This class constructs the search/download tab, including all UI elements.
 */
public final class SwingBrowserSearchTab extends AbstractTab {
    
    /** visible component. */
    private final SwingBrowser browser;
        
    final static Color LWS_BACKGROUND_COLOR = new Color(26, 58, 78, 255);

    public SwingBrowserSearchTab() {
        super(I18n.tr(GUIUtils.stripAmpersand(SWTBrowserSettings.getTitleSetting().getValue())),
                I18n.tr(SWTBrowserSettings.getTooltipSetting().getValue()),
                "browser_tab");
        
        browser = new SwingBrowser();
        browser.goHome();
        browser.setUpdateTitle(false);
        SettingListener listener = new SettingListener() {
            public void settingChanged(final SettingEvent evt) {
                if (evt.getEventType() == SettingEvent.EventType.VALUE_CHANGED) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            if (evt.getSetting() == SWTBrowserSettings.getTitleSetting())
                                changeTitle(I18n.tr(GUIUtils.stripAmpersand(SWTBrowserSettings.getTitleSetting().getValue())));
                            else if (evt.getSetting() == SWTBrowserSettings.getTooltipSetting())
                                changeTooltip(I18n.tr(SWTBrowserSettings.getTooltipSetting().getValue()));
                        }
                    });
                }
            }
        };
        SWTBrowserSettings.getTitleSetting().addSettingListener(listener);
        SWTBrowserSettings.getTooltipSetting().addSettingListener(listener);
    }
    
    @Override
    public final void storeState(boolean visible) {
        ApplicationSettings.SWT_BROWSER_VIEW_ENABLED.setValue(visible);
    }

    @Override
    public final JComponent getComponent() {
        return browser;
    }
    
}
