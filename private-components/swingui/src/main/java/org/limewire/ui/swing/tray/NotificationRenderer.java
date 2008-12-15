package org.limewire.ui.swing.tray;

import java.awt.Component;

/**
 * Used by {@link NotificationWindow} to render notifications.
 */
interface NotificationRenderer {

    /**
     * Returns a component that displays <code>value</code>.
     * 
     * @param window the notification window
     * @param value the notification
     * @param index the index of value
     * @return the component
     */
    Component getNotificationRendererComponent(NotificationWindow window,
            Object value, int index);

}
