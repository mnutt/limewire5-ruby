package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.Random;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
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
class HelpMenu extends MnemonicMenu {

    @Inject
    public HelpMenu(Application application, final IconManager iconManager,
            final TrayNotifier trayNotifier, final Navigator navigator, 
            final StorePanel storePanel) {
        // TODO fberger
        //super(I18n.tr("&Help"));
        super(I18n.tr("Help"));

        // add(new AbstractAction(I18n.tr("&Using LimeWire")) {
        add(new AbstractAction(I18n.tr("Using LimeWire")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils.openURL("http://www.limewire.com/client_redirect/?page=support");
            }
        });

        // add(new AbstractAction(I18n.tr("&FAQ")) {
        add(new AbstractAction(I18n.tr("FAQ")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                NativeLaunchUtils
                        .openURL("http://www.limewire.com/client_redirect/?page=faq");
            }
        });
        
        if(!application.isProVersion()) {
            addSeparator();
            //add(new AbstractAction(I18n.tr("Get personalized &tech support")) {
            add(new AbstractAction(I18n.tr("Get personalized tech support")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NativeLaunchUtils
                    .openURL("http://www.limewire.com/client_redirect/?page=gopro");
                }
            });
        }
        
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            // add(new AbstractAction(I18n.tr("&About LimeWire...")) {
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
                        Notification notification = new Notification("This is a not tooo long message title",
                                "This is a super looooooooooooooooooooooooooooooooong message.",
                                this);
                        trayNotifier.showMessage(notification);
                    } else if (new Random().nextBoolean()) {
                        Notification notification = new Notification("Long super loooooooooooooong loooon loooong message title",
                                "This is a another very loooong  loooong loooong loooong loooong loooong loooong loooong loooong message.",
                                this);
                        trayNotifier.showMessage(notification);
                    } else {
                        Notification notification = new Notification("Short Title", "Short message.", this);
                        trayNotifier.showMessage(notification);
                    }
                }
            });
        }
    }
}
