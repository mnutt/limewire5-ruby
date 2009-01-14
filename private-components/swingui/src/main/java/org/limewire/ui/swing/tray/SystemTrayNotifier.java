package org.limewire.ui.swing.tray;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.ActionMap;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Puts an icon and menu in the system tray. Delegates System Notifications to
 * an Internal BasicNotifier.
 */
class SystemTrayNotifier implements TrayNotifier {

    private final SystemTray tray;

    private final TrayIcon trayIcon;

    private final PopupMenu popupMenu;

    private final BasicNotifier basicNotifier;

    @Resource
    private Icon trayIconResource;

    public SystemTrayNotifier() {
        this.basicNotifier = new BasicNotifier();

        if (SystemTray.isSupported()) {
            GuiUtils.assignResources(this);
            tray = SystemTray.getSystemTray();
            popupMenu = buildPopupMenu();
            trayIcon = buildTrayIcon("LimeWire");
        } else {
            tray = null;
            trayIcon = null;
            popupMenu = null;
        }
    }

    private TrayIcon buildTrayIcon(String desc) {
        TrayIcon icon = new TrayIcon(((ImageIcon) trayIconResource).getImage(), desc, popupMenu);

        // left click restores. This happens on the awt thread.
        icon.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ActionMap map = Application.getInstance().getContext().getActionManager()
                        .getActionMap();
                map.get("restoreView").actionPerformed(e);
            }
        });

        icon.setImageAutoSize(true);
        return icon;
    }

    private PopupMenu buildPopupMenu() {
        PopupMenu menu = new PopupMenu();

        // restore
        MenuItem item = new MenuItem(I18n.tr("Show LimeWire"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ActionMap map = Application.getInstance().getContext().getActionManager()
                        .getActionMap();
                map.get("restoreView").actionPerformed(e);
            }
        });
        menu.add(item);

        menu.addSeparator();

        // exit after transfers
        item = new MenuItem(I18n.tr("Exit After Transfers"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ActionMap map = Application.getInstance().getContext().getActionManager()
                        .getActionMap();
                map.get("shutdownAfterTransfers").actionPerformed(e);
            }
        });
        menu.add(item);

        // exit
        item = new MenuItem(I18n.tr("Exit"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Application.getInstance().exit(e);
            }
        });
        menu.add(item);

        return menu;
    }

    @Override
    public boolean isExitEvent(EventObject event) {
        if (!SwingUiSettings.MINIMIZE_TO_TRAY.getValue()) {
            return true;
        }

        if ((event != null) && (event.getSource() instanceof MenuItem)) {
            // Return true on exit from system tray popup menu.
            MenuItem item = (MenuItem) event.getSource();
            return item.getParent() == popupMenu;

        } else if (event instanceof ActionEvent) {
            // Return true on action to shutdown application.
            return "Shutdown".equals(((ActionEvent) event).getActionCommand());

        } else {
            return false;
        }
    }

    public boolean showTrayIcon() {
        if (tray == null || trayIconResource == null) {
            return false;
        }

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            return false;
        } catch (IllegalArgumentException iae) {
            return false;
        }

        return true;
    }

    public boolean supportsSystemTray() {
        return trayIcon != null && tray != null;
    }

    public void hideTrayIcon() {
        if (tray != null) {
            tray.remove(trayIcon);
        }
    }

    public void showMessage(Notification notification) {
        basicNotifier.showMessage(notification);
    }

    public void hideMessage(Notification notification) {
        basicNotifier.hideMessage(notification);
    }

    public void updateUI() {
        basicNotifier.updateUI();
    }

}
