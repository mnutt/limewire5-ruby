package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.Random;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.event.AboutDisplayEvent;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;

class HelpMenu extends MnemonicMenu {

    @Inject
    public HelpMenu(Application application, final TrayNotifier trayNotifier, final Navigator navigator, final HomeMediator homeMediator) {
        super(I18n.tr("&Help"));

        add(new AbstractAction(I18n.tr("&Home Screen")) {
            @Override
           public void actionPerformed(ActionEvent e) {
                navigator.getNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME).select();
                homeMediator.getComponent().loadDefaultUrl();
           }
        });
        
        add(new UrlAction(I18n.tr("&Using LimeWire"), "http://www.limewire.com/client_redirect/?page=support"));
        
        add(new UrlAction(I18n.tr("&FAQ"), "http://www.limewire.com/client_redirect/?page=faq"));
        
        if(!application.isProVersion()) {
            addSeparator();
            add(new UrlAction(I18n.tr("Get Personalized &Tech Support"),"http://www.limewire.com/client_redirect/?page=gopro"));
        }
        
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(new AbstractAction(I18n.tr("&About LimeWire...")) {
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
