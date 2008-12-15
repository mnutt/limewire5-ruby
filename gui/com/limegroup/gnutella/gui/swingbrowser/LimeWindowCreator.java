package com.limegroup.gnutella.gui.swingbrowser;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import org.limewire.util.SystemUtils;
import org.mozilla.browser.MozillaAutomation;
import org.mozilla.browser.MozillaPanel;
import org.mozilla.browser.impl.WindowCreator;
import org.mozilla.interfaces.nsISupports;
import org.mozilla.interfaces.nsIWebBrowserChrome;
import org.mozilla.xpcom.Mozilla;

import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.GUIMediator;

class LimeWindowCreator extends WindowCreator {

    private final WindowCreator delegateCreator;

    LimeWindowCreator(WindowCreator delegateCreator) {
        this.delegateCreator = delegateCreator;
    }

    private void setLimeIcon(MozillaPanel panel) {
        if (panel.getParent() instanceof JFrame) {
            JFrame frame = (JFrame) panel.getParent();
            ImageIcon limeIcon = GUIMediator.getThemeImage(GUIConstants.LIMEWIRE_ICON);
            frame.setIconImage(limeIcon.getImage());
            SystemUtils.setWindowIcon(frame, GUIConstants.LIMEWIRE_EXE_FILE);
        }
    }

    private void addGoToNativeButton(final MozillaPanel panel) {
        JToolBar toolbar = panel.getToolbar();
        toolbar.add(new AbstractAction("Out") {
            // TODO: Add a picture.
            public void actionPerformed(ActionEvent e) {
                GUIMediator.openURL(panel.getUrl());
            }
        });
    }

    private void addKeyListener(MozillaPanel panel, nsIWebBrowserChrome chrome) {
        panel.addKeyListener(new MozillaKeyListener(chrome));
    }

    private void addClosingListener(MozillaPanel panel, nsIWebBrowserChrome chrome) {
        if (panel.getParent() instanceof JFrame) {
            JFrame frame = (JFrame) panel.getParent();
            frame.addWindowListener(new MozillaClosingListener(chrome));
        }
    }

    @Override
    public nsIWebBrowserChrome createChromeWindow(nsIWebBrowserChrome parent, long chromeFlags) {
        final nsIWebBrowserChrome chrome = delegateCreator.createChromeWindow(parent, chromeFlags);
        final MozillaPanel window = MozillaAutomation.findWindow(chrome);
        BrowserUtils.addDomListener(chrome);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                // Note: setLimeIcon requires that the window has had addNotify
                // called and is displayable. This is a precondition of moz's
                // returning a window, so we're OK.
                setLimeIcon(window);
                addGoToNativeButton(window);
                addKeyListener(window, chrome);
                addClosingListener(window, chrome);
            }
        });
        return chrome;
    }

    @Override
    public void ensurePrecreatedWindows(int winNum) {
        delegateCreator.ensurePrecreatedWindows(winNum);
    }

    @Override
    public nsISupports queryInterface(String aiid) {
        return Mozilla.queryInterface(this, aiid);
    }

}
