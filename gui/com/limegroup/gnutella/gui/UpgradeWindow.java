package com.limegroup.gnutella.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;

import org.limewire.core.settings.StartupAdSettings;

import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.startup.ImagePanel;
import com.limegroup.gnutella.gui.startup.StartupAd;
import com.limegroup.gnutella.gui.startup.StartupBanner;
import com.limegroup.gnutella.gui.util.BackgroundExecutorService;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Dialog for upgrading.
 */
// 2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
final class UpgradeWindow {

    /**
     * Constant handle to the <tt>JDialog</tt> that contains about
     * information.
     */
    private JDialog dialog;

    /**
     * Constant handle to the main <tt>BoxPanel</tt> instance.
     */
    private final BoxPanel MAIN_PANEL = new BoxPanel(BoxPanel.Y_AXIS);

    /**
     * Default background image to display behind the text
     */
    private final ImageIcon defaultBackgroundImage = GUIMediator.getThemeImage("proAdBackground");

    /**
     * Constant dimension for the dialog.
     */
    private final Dimension DIALOG_DIMENSION = new Dimension(380, 180);
    
    /**
     * Constant for the image in case remote image takes a while, this will resize
     * correctly even if the remote image is a different size as long as some
     * preferred size has been set
     */
    private final static Dimension defaultImageSize = new Dimension(361, 108);

    /**
     * Current Ad that is being displayed
     */
    private StartupAd ad;

    /**
     * List of Actions for the buttons to be displayed in the dialog
     */
    private Action[] actions;
    
    /**
     * Panel that displays the background image and the main message
     */
    private final ImagePanel imagePanel;
    

    /**
     * Constructs the elements of the upgrade window.
     */
    public UpgradeWindow() {
        ad = loadAd();

        int labelWidth = 240;

        dialog = new JDialog(GUIMediator.getAppFrame());
        dialog.setModal(true);
        dialog.setResizable(true);
        dialog.setTitle( (ad.getTitle() == null) ? I18n.tr("Upgrade to PRO") : ad.getTitle());

        // set the main panel's border
        Border border = BorderFactory.createEmptyBorder(11, 9, 6, 9);

        imagePanel = new ImagePanel((ad.getMessage() == null) ? "" : ad.getMessage(), labelWidth);

        ImageIcon image = getBackgroundImage();
        // we're using the local image
        if( image != null ) {
            imagePanel.setBackgroundImage(image.getImage());
            imagePanel.setPreferredSize(new Dimension(image.getIconWidth(), image.getIconHeight()));
        } else {
            // just set the size, will update once the remote image is loaded
            imagePanel.setPreferredSize( defaultImageSize);
        }


        JLabel label2 = new JLabel(ad.getButtonMessage());
        label2.setFont(new Font("Dialog", Font.PLAIN, 12));


        BoxPanel questionPanel = new BoxPanel(BoxPanel.X_AXIS);
        questionPanel.add(Box.createVerticalGlue());
        questionPanel.add(label2);
        questionPanel.add(Box.createVerticalGlue());

        MAIN_PANEL.setBorder(border);
        MAIN_PANEL.setPreferredSize(DIALOG_DIMENSION);
        dialog.setSize(DIALOG_DIMENSION);

        actions = new Action[] { new YesAction(0), new WhyAction(1), new NoAction(2) };

        if (StartupAdSettings.PRO_STARTUP_RANDOM_BUTTONS.getValue()) {
            // Shuffle the buttons around.
            List<Action> shuffled = Arrays.asList(actions);
            Collections.shuffle(shuffled);
            actions = shuffled.toArray(actions);
            
            for(int i = 0; i < actions.length; i++) {
                LocationAction action = (LocationAction) actions[i];
                action.setLocation(i);
            }
        }

        ButtonRow buttons = new ButtonRow(actions, ButtonRow.X_AXIS, ButtonRow.NO_GLUE);

        MAIN_PANEL.add(imagePanel);
        MAIN_PANEL.add(Box.createVerticalStrut(5));
        MAIN_PANEL.add(questionPanel);
        MAIN_PANEL.add(Box.createVerticalStrut(5));
        MAIN_PANEL.add(buttons);
        dialog.getContentPane().add(MAIN_PANEL);
        dialog.pack();
    }
    
    /**
     * Loads the correct background image. If remote images are being used, attempts to
     * connect to the url and load the image remotely. If the image can't be found or 
     * isn't loaded, loads the default image instead
     * @return
     */
    private ImageIcon getBackgroundImage(){
        // checks if we are using remote images, if not just load the local image instead
        if( StartupAdSettings.PRO_STARTUP_REMOTE_IMAGE.getValue() ) { 
            String url = ad.getURLImage();
            if(url == null || url.length() == 0)
                return defaultBackgroundImage;
            
            BackgroundExecutorService.schedule(
            new ImageLoader(url, defaultBackgroundImage, imagePanel));
            return null;
        }
        return defaultBackgroundImage;
    }
    
    /**
     * Opens a url and adds a few parameters to the end so we can learn about 
     * who was clicking this url 
     * 
     * @param url
     * @param buttonLocation
     */
    private void openURL(String url, int buttonLocation) {
        byte[] guid = GuiCoreMediator.getApplicationServices().getMyGUID();
        GUIMediator.openURL( LimeWireUtils.addLWInfoToUrl(url, guid) + "&button=" + buttonLocation); 
    }

    /**
     * Displays the upgrade to pro dialog window to the user.
     */
    static void showProDialog() {
        UpgradeWindow window = new UpgradeWindow();
        window.dialog.setLocationRelativeTo(GUIMediator.getAppFrame());
        try {
            if (StartupAdSettings.PRO_STARTUP_IS_VISIBLE.getValue()) 
                window.dialog.setVisible(true);
        } catch (InternalError ie) {
            // happens occasionally, ignore.
        }
    }

    /**
     * Creates a 'Yes' Button. When clicked will redirect the user to the
     * purchase Pro page
     */
    private class YesAction extends LocationAction {
        public YesAction(int location) {
            super(location);
            putValue(Action.NAME, (ad.getButton1Text() == null) ? I18n.tr("Yes") : ad.getButton1Text());
            putValue(Action.SHORT_DESCRIPTION, (ad.getButton1ToolTip() == null) ? 
                    I18n.tr("Get LimeWire PRO Now") : ad.getButton1ToolTip());
        }

        public void actionPerformed(ActionEvent e) {
            if( ad.getURLButton1() != null )
                openURL(ad.getURLButton1(), location);
            dialog.dispose();
            dialog.setVisible(false);
        }
    }

    /**
     * Creates a 'Why' Button. When clicked will redirect the user to a page
     * describing the features of Pro
     */
    private class WhyAction extends LocationAction {       
        public WhyAction(int location) {
            super(location);
            putValue(Action.NAME, (ad.getButton2Text() == null) ? I18n.tr("Why") : ad.getButton2Text());
            putValue(Action.SHORT_DESCRIPTION, (ad.getButton2ToolTip() == null) ? 
                    I18n.tr("What does PRO give me?") : ad.getButton2ToolTip());
        }

        public void actionPerformed(ActionEvent e) {
            if( ad.getURLButton2() != null )
                openURL(ad.getURLButton2(), location);
            dialog.dispose();
            dialog.setVisible(false);
        }
    }

    /**
     * Creates a 'No' Button. When clicked will close the dialog box
     */
    private class NoAction extends LocationAction {       
        public NoAction(int location) {
            super(location);
            putValue(Action.NAME, (ad.getButton3Text() == null) ? I18n.tr("Later") : ad.getButton3Text());
            putValue(Action.SHORT_DESCRIPTION, (ad.getButton3ToolTip() == null) ? 
                    I18n.tr("Get LimeWire PRO Later") : ad.getButton3ToolTip());
        }

        public void actionPerformed(ActionEvent e) {
            if( ad.getURLButton3() != null )
                openURL(ad.getURLButton3(), location);
            dialog.dispose();
            dialog.setVisible(false);
        }
    }
    
    /**
     * Bizdev wants to know where the button was located in the row when it was clicked
     */
    private abstract class LocationAction extends AbstractAction {
        protected int location;
        
        public LocationAction(int location) {
            this.location = location;
        }
        
        public void setLocation(int location) {
            this.location = location;
        }
    }

    /**
     * Loads the message to be displayed. This contains the title, message,
     * urls, etc.. to be used in this dialog.
     */
    private StartupAd loadAd() {
        try {
            StartupBanner b = new StartupBanner(StartupAdSettings.PRO_STARTUP_ADS.getValue()); 
            return b.getRandomAd();
        } catch (IllegalArgumentException bad) {
            return StartupBanner.getDefaultBanner().getRandomAd();
        }
    }
    
    /**
     * Loads the remote image on a background thread in case the image takes 
     * a while to retrieve
     */
    private static class ImageLoader implements Runnable {
        private final String url;
        private final ImageIcon image;
        private final ImagePanel panel;
        
        public ImageLoader(String url, ImageIcon defaultImage, ImagePanel panel) {
            this.url = url;
            this.image = defaultImage;
            this.panel = panel;
        }
        
        public void run() {
            
            final ImageIcon displayImage;

            ImageIcon icon = new ImageIcon(url); 
            // if no image was loaded, show default instead
            
            if( icon.getIconHeight() <= 0 || icon.getIconWidth() <= 0 || icon.getImage() == null)
                displayImage = image;
            else 
                displayImage = icon;

            SwingUtilities.invokeLater( new Runnable(){
                public void run() { 
                    if( panel != null && panel.isVisible() )
                        panel.setBackgroundImage(displayImage.getImage());
                }                
            });
        }    
    }
}
