package org.limewire.ui.swing.event;

import java.awt.event.ActionEvent;

/**
 * Event to indicate that the user has requested that the application exits.
 */
public class ExitApplicationEvent extends AbstractEDTEvent {

    private final ActionEvent actionEvent;

    public ExitApplicationEvent() {
        this.actionEvent = null;
    }

    public ExitApplicationEvent(ActionEvent actionEvent) {
        this.actionEvent = actionEvent;
    }

    /**
     * Returns an action event representing this shutdown request. If null is
     * returned the application will minimize instead.
     */
    public ActionEvent getActionEvent() {
        return actionEvent;
    }

}
