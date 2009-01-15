package org.limewire.ui.swing.friends.chat;

import java.util.EventObject;

import org.limewire.ui.swing.tray.Notification;
import org.limewire.ui.swing.tray.TrayNotifier;

public class MockTrayNotifier implements TrayNotifier {

    @Override
    public void hideMessage(Notification notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public void hideTrayIcon() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isExitEvent(EventObject event) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void showMessage(Notification notification) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean showTrayIcon() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean supportsSystemTray() {
        // TODO Auto-generated method stub
        return false;
    }

}
