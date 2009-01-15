package org.limewire.ui.swing.advanced.connection;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.Timer;

/**
 * A manager for a popup window.  PopupManager is associated with a 
 * PopupProvider that supplies the content for the popup window. 
 */
public class PopupManager {
    private static final int POPUP_DURATION = 4000;
    
    private final PopupProvider popupProvider;
    private Popup popup;
    private Timer exitTimer;

    /**
     * Constructs a PopupManager with the specified popup content provider.
     */
    public PopupManager(PopupProvider popupProvider) {
        this.popupProvider = popupProvider;
    }

    /**
     * Displays a popup window over the specified owner component.  The 
     * specified location is relative to the component origin.  The popup is 
     * automatically dismissed after four seconds, or when the popup is
     * clicked. 
     */
    public void showTimedPopup(Component owner, int x, int y) {
        // Get popup content.
        Component content = popupProvider.getPopupContent();
        
        if ((popup == null) && (content != null)) {
            // Add listener to dismiss popup on mouse click.
            content.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    hidePopup();
                }
            });
            
            // Get owner location.
            Point location = owner.getLocationOnScreen();

            // Use factory to create popup and display.
            PopupFactory factory = PopupFactory.getSharedInstance();
            popup = factory.getPopup(owner, content, location.x + x, location.y + y + 20);
            popup.show();
            
            // Start timer to hide popup.
            startExitTimer();
        }
    }
    
    /**
     * Hides the popup window.
     */
    public void hidePopup() {
        // Hide existing popup and reset.
        if (popup != null) {
            popup.hide();
            popup = null;
        }
        
        // Stop exit timer.
        stopExitTimer();
    }
    
    /**
     * Starts the timer to hide the popup window.
     */
    private void startExitTimer() {
        // Stop existing timer.
        stopExitTimer();

        // Create new timer to hide popup.
        exitTimer = new Timer(POPUP_DURATION, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                hidePopup();
            }
        });
        
        // Start timer.
        exitTimer.start();
    }
    
    /**
     * Stops the timer to hide the popup window.
     */
    private void stopExitTimer() {
        if (exitTimer != null) {
            exitTimer.stop();
            exitTimer = null;
        }
    }
    
    /**
     * Defines a component that provides content for a popup window. 
     */
    public static interface PopupProvider {
        /**
         * Returns the content for a popup window. 
         */
        public Component getPopupContent();
    }
}
