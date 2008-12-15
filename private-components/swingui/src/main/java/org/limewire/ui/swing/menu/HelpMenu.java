package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Random;

import javax.swing.Icon;
import javax.swing.JMenu;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.event.AboutDisplayEvent;
import org.limewire.ui.swing.mainframe.StorePanel;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class HelpMenu extends JMenu {

    @Inject
    public HelpMenu(Application application, final IconManager iconManager,
            final TrayNotifier trayNotifier, final Navigator navigator, 
            final StorePanel storePanel) {
        super(I18n.tr("Help"));

        add(new AbstractAction(I18n.tr("Using LimeWire")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL("http://www.limewire.com/support/");
            }
        });

        add(new AbstractAction(I18n.tr("FAQ")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils
                        .openURL("http://wiki.limewire.org/index.php?title=Frequently_Asked_Questions");
            }
        });
        
        if(!application.isProVersion()) {
            addSeparator();
            add(new AbstractAction(I18n.tr("Get personalized tech support")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NativeLaunchUtils
                    .openURL("http://limewire.com/download/pro.php?ref=H");
                }
            });
        }
        
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(new AbstractAction(I18n.tr("About LimeWire...")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new AboutDisplayEvent().publish();
                }
            });
        }

        if (application.isTestingVersion()) {
            addSeparator();
            add(new AbstractAction("Error Test") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    throw new RuntimeException("Test Error");
                }
            });

            add(new AbstractAction("Tray Test") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (new Random().nextBoolean()) {
                        Icon icon = iconManager.getIconForFile(new File("limewire.exe"));
                        Notification notification = new Notification(
                                "This is a very looooooooooooooooooooooooooooooooong message.",
                                icon, this);
                        trayNotifier.showMessage(notification);
                    } else if (new Random().nextBoolean()) {
                        Icon icon = iconManager.getIconForFile(new File("limewire.html"));
                        Notification notification = new Notification(
                                "This is a another very loooong  loooong loooong loooong loooong loooong loooong loooong loooong message.",
                                icon, this);
                        trayNotifier.showMessage(notification);
                    } else {
                        Icon icon = iconManager.getIconForFile(new File("limewire.html"));
                        Notification notification = new Notification("Short message.", icon, this);
                        trayNotifier.showMessage(notification);
                    }
                }
            });
        }
    }
}
