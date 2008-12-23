package org.limewire.ui.swing.tray;

import javax.swing.Action;
import javax.swing.Icon;

/**
 * Represents a notification. A notification must have a message and can
 * optionally have an icon and associated actions.
 */
public class Notification {

    private String title;

    private String message;

    private Action[] actions;

    private Icon icon;

    public Notification(String title, String message, Icon icon, Action... actions) {
        this.title = title;
        this.message = message;
        this.icon = icon;
        this.actions = actions;
    }

    public Notification(String message, Icon icon, Action... actions) {
        this(null, message, icon, actions);
    }

    public Notification(String title, String message) {
        this(title, message, null, new Action[0]);
    }

    public String getMessage() {
        return message;
    }

    public Action[] getActions() {
        return actions;
    }

    public Icon getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }
}
