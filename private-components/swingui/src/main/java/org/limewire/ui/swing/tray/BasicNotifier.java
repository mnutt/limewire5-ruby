package org.limewire.ui.swing.tray;

import java.awt.Dimension;
import java.util.EventObject;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.jdesktop.application.Resource;
import org.limewire.ui.swing.util.GuiUtils;

/**
 * This class handles user notifications for platform that do not support JDIC.
 * It currently displays notifications only.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class BasicNotifier implements TrayNotifier {   

	private NotificationWindow notificationWindow;
	
	@Resource
	private Icon limeIcon;
	
	public BasicNotifier() {
	    GuiUtils.assignResources(this);
		notificationWindow = new NotificationWindow(GuiUtils.getMainFrame());
		notificationWindow.setLocationOffset(new Dimension(1, 1));
		notificationWindow.setTitle("LimeWire");
		notificationWindow.setIcon(limeIcon);
	}
	
	public boolean supportsSystemTray() {
	    return false;
	}
	
    public boolean showTrayIcon() { return true; }

    public void hideTrayIcon() {}

    public void showMessage(Notification notification) {
        notificationWindow.addNotification(notification);
        notificationWindow.showWindow();
    }

	public void hideMessage(Notification notification) {
		notificationWindow.removeNotification(notification);
	}

    public void updateUI() {
        SwingUtilities.updateComponentTreeUI(notificationWindow);        
    }
    
    @Override
    public boolean isExitEvent(EventObject event) {
        return false;
    }
}
