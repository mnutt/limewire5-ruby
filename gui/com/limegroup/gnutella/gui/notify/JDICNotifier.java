package com.limegroup.gnutella.gui.notify;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.jdic.tray.SystemTray;
import org.jdesktop.jdic.tray.TrayIcon;
import org.limewire.core.settings.UISettings;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;

/**
 * Puts an icon and menu in the system tray.
 * Works on Windows, Linux, and any other platforms JDIC supports.
 */
public class JDICNotifier implements NotifyUser {
	
    private static final Log LOG = LogFactory.getLog(DefaultNotificationRenderer.class);
    
	private final SystemTray _tray;
	private TrayIcon _icon;
	private NotificationWindow notificationWindow;
	
	public JDICNotifier() {
		_tray = SystemTray.getDefaultSystemTray();
		buildPopupMenu();
		buildTrayIcon(I18n.tr("LimeWire"), "limeicon");
		buildNotificationWindow();
	}

	private void buildTrayIcon(String desc, String imageFileName) {
        //String tip = "LimeWire: Running the Gnutella Network";
        _icon = new TrayIcon(GUIMediator.getThemeImage(imageFileName), desc, GUIMediator.getTrayMenu());
        
    	// left click restores.  This happens on the awt thread.
        _icon.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent e) {
        		GUIMediator.restoreView();
        	}
        });
        
        _icon.setIconAutoSize(true);
	}
	
	private JPopupMenu buildPopupMenu() {
		JPopupMenu menu = GUIMediator.getTrayMenu();
		
		// restore
		JMenuItem item = new JMenuItem(I18n.tr("Restore"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GUIMediator.restoreView();
			}
		});
		menu.add(item);
		
		menu.addSeparator();
		
		// about box
		item = new JMenuItem(I18n.tr("About"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GUIMediator.showAboutWindow();
			}
		});
		menu.add(item);
		
		menu.addSeparator();
		
		//exit after transfers
		item = new JMenuItem(I18n.tr("Exit after Transfers"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GUIMediator.shutdownAfterTransfers();
			}
		});
		menu.add(item);
		
		// exit
		item = new JMenuItem(I18n.tr("Exit"));
		item.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				GUIMediator.shutdown();
			}
		});
		menu.add(item);
		
		return menu;
	}
	
	private void buildNotificationWindow() {
		notificationWindow = new NotificationWindow(GUIMediator.getAppFrame());
		notificationWindow.setLocationOffset(new Dimension(2, 7));
		notificationWindow.setTitle("LimeWire");
		notificationWindow.setIcon(GUIMediator.getThemeImage("limeicon.gif"));
	}
	
	public boolean showTrayIcon() {
	    try {
	        _tray.addTrayIcon(_icon);
	    } catch(IllegalArgumentException iae) {
	        // Sometimes JDIC can't load the trayIcon :(
	        return false;
	    }

        // XXX use the actual icon size once the necessary call is available in JDIC 
        //notificationWindow.setParentSize(_icon.getSize());
        notificationWindow.setParentSize(new Dimension(22, 22));

        return true;
	}
	
	public boolean supportsSystemTray() {
	    return true;
	}

	public void hideTrayIcon() {
		_tray.removeTrayIcon(_icon);
		notificationWindow.setParentLocation(null);
		notificationWindow.setParentSize(null);
	}

	public void showMessage(Notification notification) {
	    try {
	        notificationWindow.addNotification(notification);
	        try {
	            notificationWindow.setParentLocation(_icon.getLocationOnScreen());
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
