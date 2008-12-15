package org.limewire.ui.swing.tray;

import java.awt.AWTException;
import java.awt.Dimension;
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
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.application.Application;
import org.jdesktop.application.Resource;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.UISettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;


/**
 * Puts an icon and menu in the system tray.
 */
class SystemTrayNotifier implements TrayNotifier {
	
    private static final Log LOG = LogFactory.getLog(DefaultNotificationRenderer.class);
    
	private final SystemTray tray;
	private final TrayIcon trayIcon;
	private final NotificationWindow notificationWindow;
	private final PopupMenu popupMenu;
	
	@Resource
	private Icon trayIconResource;
	
	public SystemTrayNotifier() {
	    if(SystemTray.isSupported()) {
    	    GuiUtils.assignResources(this);
    		tray = SystemTray.getSystemTray();
    		popupMenu = buildPopupMenu();
    		trayIcon = buildTrayIcon("LimeWire");
    		notificationWindow = buildNotificationWindow();
	    } else {
	        tray = null;
	        trayIcon = null;
	        notificationWindow = null;
	        popupMenu = null;
	    }
	}

	private TrayIcon buildTrayIcon(String desc) {
        TrayIcon icon = new TrayIcon(((ImageIcon)trayIconResource).getImage(), desc, popupMenu);

    	// left click restores.  This happens on the awt thread.
        icon.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        	    ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
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
			    ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
                map.get("restoreView").actionPerformed(e);
			}
		});
		menu.add(item);
		
		menu.addSeparator();
		
		//exit after transfers
		item = new MenuItem(I18n.tr("Exit After Transfers"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    ActionMap map = Application.getInstance().getContext().getActionManager().getActionMap();
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
	    if (!ApplicationSettings.MINIMIZE_TO_TRAY.getValue()) {
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
	
	private NotificationWindow buildNotificationWindow() {
		NotificationWindow window = new NotificationWindow(GuiUtils.getMainFrame());
		window.setLocationOffset(new Dimension(2, 7));
		window.setTitle("LimeWire");
		window.setIcon(trayIconResource);
		return window;
	}
	
	public boolean showTrayIcon() {
	    if(trayIconResource == null) {
	        return false;
	    }
	    
	    try {
	        tray.add(trayIcon);
        } catch (AWTException e) {
            return false;
	    } catch(IllegalArgumentException iae) {
	        return false;
	    }

        notificationWindow.setParentSize(trayIcon.getSize());

        return true;
	}
	
	public boolean supportsSystemTray() {
	    return trayIconResource != null;
	}

	public void hideTrayIcon() {
		tray.remove(trayIcon);
		notificationWindow.setParentLocation(null);
		notificationWindow.setParentSize(null);
	}

	public void showMessage(Notification notification) {
	    try {
	        notificationWindow.addNotification(notification);
	        try {
	            // TODO: trayIcon.getLocationOnScreen not available in AWT TrayIcon
	            notificationWindow.setParentLocation(null);
	        } catch (NullPointerException ignore) {
	            // thrown if the native peer is not found (GUI-273)?
	        }
	        notificationWindow.showWindow();
        } catch (Exception e) {
            // see GUI-239
            LOG.error("Disabling notifications due to error", e);
            UISettings.SHOW_NOTIFICATIONS.setValue(false);
            notificationWindow.hideWindowImmediately();
        }
	}

	public void hideMessage(Notification notification) {
		notificationWindow.removeNotification(notification);
	}

    public void updateUI() {
        SwingUtilities.updateComponentTreeUI(notificationWindow);
    }
	
}
