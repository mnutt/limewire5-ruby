package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.Random;

import org.limewire.core.api.Application;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.home.HomeMediator;
import org.limewire.ui.swing.mainframe.AboutAction;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

class HelpMenu extends MnemonicMenu {

    private final Application application;
    
    private final Provider<TrayNotifier> trayNotifierProvider;
    private final Provider<Navigator> navigatorProvider;
    private final Provider<HomeMediator> homeMediatorProvider;
    private final Provider<AboutAction> aboutAction;
    
    @Inject
    public HelpMenu(Application application, 
            Provider<TrayNotifier> trayNotifierProvider,
            Provider<Navigator> navigatorProvider, 
            Provider<HomeMediator> homeMediatorProvider,
            Provider<AboutAction> aboutAction) {
        
        super(I18n.tr("&Help"));

        this.application = application;
        
        this.trayNotifierProvider = trayNotifierProvider;
        this.navigatorProvider = navigatorProvider;
        this.homeMediatorProvider = homeMediatorProvider;
        this.aboutAction = aboutAction;
    }

    @Override
    public void createMenuItems() {
        add(new AbstractAction(I18n.tr("&Home Screen")) {
            @Override
           public void actionPerformed(ActionEvent e) {
                navigatorProvider.get().getNavItem(NavCategory.LIMEWIRE, HomeMediator.NAME).select();
                homeMediatorProvider.get().getComponent().loadDefaultUrl();
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
            add(aboutAction.get());
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
                        trayNotifierProvider.get().showMessage(notification);
                    } else if (new Random().nextBoolean()) {
                        Notification notification = new Notification("Long super loooooooooooooong loooon loooong message title",
                                "This is a another very loooong  loooong loooong loooong loooong loooong loooong loooong loooong message.",
                                this);
                        trayNotifierProvider.get().showMessage(notification);
                    } else {
                        Notification notification = new Notification("Short Title", "Short message.", this);
                        trayNotifierProvider.get().showMessage(notification);
                    }
                }
            });
        }
    }
}
