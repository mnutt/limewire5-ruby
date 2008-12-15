package com.limegroup.gnutella.gui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.util.Locale;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.licenses.VerificationListener;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A dialog displaying license information.
 */
public abstract class LicenseWindow extends JDialog implements VerificationListener {    
    
    /** The URN that the license is expected to match. */
    protected final URN URN;
    /** The LimeXMLDocument with information about the file this license matches. */
    protected final LimeXMLDocument DOCUMENT;
    /** The License itself. */
    protected final License LICENSE;
    /** The panel where the ongoing status of the license verification is shown */
    protected final JPanel DETAILS;
    /** A listener who wants to receive notification about the license being verified */
    protected final VerificationListener LISTENER;
    /** The value to insert in the key to lookup message bundle info. */
    protected final String KEY_VALUE;
    
    public static LicenseWindow create(License l, URN u, LimeXMLDocument d, VerificationListener v) {
        String name = l.getLicenseName();
        
        if(LicenseFactory.CC_NAME.equals(name))
            return new CCWindow(l, u, d, v);
        else if(LicenseFactory.WEED_NAME.equals(name))
            return new WeedWindow(l, u, d, v);
        else
            return new UnknownWindow(l, u, d, v);
    }
    
    /**
     * Constructs a new LicenseWindow.
     * @param license the License being displayed
     * @param urn the URN the license is validating against
     * @param listener a VerificationListener this license can forward
     *                 licenseVerified events to.
     */
    private LicenseWindow(License license, URN urn,
                          LimeXMLDocument document, VerificationListener listener,
                          String keyValue) {
        super(GUIMediator.getAppFrame());
        URN = urn;
        LICENSE = license;
        DETAILS = new JPanel(new GridBagLayout());
        LISTENER = listener;
        KEY_VALUE = keyValue;
        DOCUMENT = document;
        
        setModal(false);
        setResizable(false);
        setTitle(getTitleString());
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        
        JComponent pane = (JComponent)getContentPane();
        GUIUtils.addHideAction(pane);
        pane.setPreferredSize(new Dimension(400, 230));            
        DETAILS.setPreferredSize(new Dimension(400, 210));

        getContentPane().setLayout(new GridBagLayout());
        constructDialog(getContentPane());
        validate();
    }
    
    /**
     * Notification that a license has been verified.
     *
     * Rebuilds the details panel to match the new status.
     */
    public void licenseVerified(License license) {
        if(license == LICENSE) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    buildDetails();
                }
            });
        }

        if(LISTENER != null)
            LISTENER.licenseVerified(license);
    }
    
    protected void createVerifying() {
        GridBagConstraints c = new GridBagConstraints();
        // TODO:  Add a pretty animation.
        JTextArea text = newTextArea(getLocatingString());
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        DETAILS.add(text, c);
    }
    
    protected void createNotVerified() {        
        GridBagConstraints c = new GridBagConstraints();
        JTextArea text = newTextArea(getNotVerifiedString());
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.anchor = GridBagConstraints.NORTHWEST;
        DETAILS.add(text, c);
        
        JButton button = new JButton(getVerifyString());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                GuiCoreMediator.getLicenseVerifier().verify(LICENSE, LicenseWindow.this);
                buildDetails();
            }
        });
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        c.weighty = 0;
        c.anchor = GridBagConstraints.SOUTHWEST;
        DETAILS.add(button, c);
    }
    
    protected void createNotValid() {
        GridBagConstraints c = new GridBagConstraints();
        URI licenseURI = LICENSE.getLicenseURI();
        JComponent comp;

        comp = new JLabel(getWarningIcon());
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 0, 5);
        DETAILS.add(comp, c);
        
        String invalidText = getInvalidString();
        if(licenseURI != null && allowRetryLink())
            invalidText += "  " + getRetryString();
        comp = newTextArea(invalidText);
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        c.insets = new Insets(7, 0, 0, 0);
        DETAILS.add(comp, c);
        
        c.gridwidth = 1;
        c.gridheight = 2;
        c.weightx = 0;
        c.weighty = 0;
        DETAILS.add(javax.swing.Box.createGlue(), c);
        

        JButton button = new JButton(getVerifyString());
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                GuiCoreMediator.getLicenseVerifier().verify(LICENSE, LicenseWindow.this);
                buildDetails();
            }
        });
        button.setVisible(licenseURI != null && allowVerifyLookup());
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 2;
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(0, 0, 0, 100);
        DETAILS.add(button, c);

        
        if(LICENSE.getLicenseDeed(URN) == null || !allowClaimedDeedLink()) {
            comp = new JLabel();
            comp.setVisible(false);
        } else {
            comp = new URLLabel(LICENSE.getLicenseDeed(URN), getClaimedDeedString());
        }
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 0;
        c.weighty = 0;
        c.insets = new Insets(0, 0, 3, 0);
        DETAILS.add(comp, c);
        
        if(licenseURI == null || !allowVerificationLink()) {
            comp = new JLabel();
            comp.setVisible(false);
        } else {
            comp = new URLLabel(licenseURI, getVerificationString());
        }
        c.insets = new Insets(0, 0, 3, 0);
        DETAILS.add(comp, c);
    }
    
    protected void createValid() {
        GridBagConstraints c = new GridBagConstraints();
        JComponent label = new JLabel(getDetailsString());
        Font f = label.getFont();
        label.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.anchor = GridBagConstraints.NORTHWEST;
        DETAILS.add(label, c);
        
        JTextArea text = newTextArea(LICENSE.getLicenseDescription(URN));
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.gridheight = 2;
        c.weighty = 1;
        c.weightx = .7;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        DETAILS.add(text, c);
        
        if(LICENSE.getLicenseDeed(URN) != null) {
            label = new URLLabel(LICENSE.getLicenseDeed(URN), getDeedString());
        } else {
            label = new JLabel();
            label.setVisible(false);
        }
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = 1;
        c.weighty = 0;
        c.weightx = .3;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 3, 0);
        DETAILS.add(label, c);
        
        label = new URLLabel(LICENSE.getLicenseURI(), getVerificationString());
        DETAILS.add(label, c);
    }
    
    
    /**
     * Builds the details panel.
     *
     * This will show either a message that
     * 1) The license is currently being located.
     * 2) The license is not yet verified, asking the user to verify.
     * 3) The license could not be validated (with guess-work links)
     * 4) The license is valid (with the details, and links)
     */
    protected void buildDetails() {
        DETAILS.removeAll();
        
        if(LICENSE.isVerifying())
            createVerifying();
        else if(!LICENSE.isVerified())
            createNotVerified();
        else if(!LICENSE.isValid(URN))
            createNotValid();
        else // LICENSE.isValid()
            createValid();

        validate();
        repaint();
    }
    
    /** Creates the top of the window. */
    protected void createTopOfWindow(Container parent) {
        GridBagConstraints c = new GridBagConstraints();
        JLabel img = new URLLabel(getLargeLicenseURLString(), getLargeLicenseIcon());
        c.insets = new Insets(4, 4, 2, 0);
        parent.add(img, c);
        
        JComponent line = new Line();
        c.insets = new Insets(0, 2, 0, 2);
        c.gridwidth = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.VERTICAL;
        parent.add(line, c);
        
        JTextArea text = newTextArea(LICENSE.getLicense());
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 0, 2, 4);
        parent.add(text, c);
    }
    
    /**
     * Constructs the dialog itself.
     */
    protected void constructDialog(Container parent) {
        // Add the 
        //  [ img ] | [ license ]
        createTopOfWindow(parent);

        GridBagConstraints c = new GridBagConstraints();
        
        // Add a -----------
        // line below img & license
        JComponent line = new Line();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 2, 0);
        parent.add(line, c);

        // Create the details panel.
        buildDetails();
        
        // Add the details panel.
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(4, 4, 0, 4);
        c.weighty = 1;
        c.weightx = 1;
        parent.add(DETAILS, c);
        
        // Add a ------------
        // line below the details.
        line = new Line();
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.gridheight = GridBagConstraints.RELATIVE;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);
        c.ipady = 0;
        c.weighty = 0;
        c.weightx = 0;
        parent.add(line, c);
        
        // Add an OK button out of the window.
        JButton button = new JButton(I18n.tr("OK"));
        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                LicenseWindow.this.dispose();
                LicenseWindow.this.setVisible(false);
           }
        });
        c.gridheight = GridBagConstraints.REMAINDER;
        c.gridwidth = GridBagConstraints.REMAINDER;
        c.insets = new Insets(0, 0, 4, 4);
        c.fill = GridBagConstraints.NONE;
        c.anchor = GridBagConstraints.EAST;
        parent.add(button, c);

        pack();
    }
    
    /**
     * Builds a new JTextArea with the appropriate values set.
     */
    private JTextArea newTextArea(String msg) {
        JTextArea text = new JTextArea();
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setEditable(false);
        text.setFont(UIManager.getFont("Table.font"));
        JLabel label = new JLabel();
        text.setForeground(label.getForeground());
        text.setBackground(label.getBackground());
        text.setText(msg);
        return text;
    }

    protected abstract String getTitleString();
    
    protected abstract String getLocatingString();
    
    protected abstract String getNotVerifiedString();
    
    protected String getVerifyString() {
        return I18n.tr("Verify");
    }
    
    protected Icon getWarningIcon() {
        return GUIMediator.getThemeImage("warning");
    }
    
    protected abstract String getInvalidString();
    
    protected abstract String getRetryString();
    
    protected abstract String getClaimedDeedString();
    
    protected abstract String getVerificationString();
    
    protected abstract String getDetailsString();
    
    protected abstract String getDeedString();
    
    protected Icon getLargeLicenseIcon() {
        return GUIMediator.getThemeImage(KEY_VALUE.toLowerCase(Locale.US) + "_window");
    }
    
    protected boolean allowRetryLink() { return true; }
    protected boolean allowVerificationLink() { return true; }
    protected boolean allowClaimedDeedLink() { return true; }
    protected boolean allowVerifyLookup()  { return true; }
    
    protected abstract String getLargeLicenseURLString();
    
    /**
     * Special stuff for a CC license.
     */
    private static class CCWindow extends LicenseWindow {
        CCWindow(License l, URN u, LimeXMLDocument d, VerificationListener v) {
            super(l, u, d, v, "CC");
        }
        
        @Override
        protected String getLargeLicenseURLString() {
            return "http://www.creativecommons.org";
        }
        
        @Override
        protected String getInvalidString() {
            return I18n.tr("LimeWire was unable to locate the details of this license.");
        }
        
        @Override
        protected String getRetryString() {
            return I18n.tr("You can try validating this license again by clicking \'Verify\'.");
        }
        
        @Override
        protected String getClaimedDeedString() {
            return I18n.tr("View Claimed License Deed");
        }
        
        @Override
        protected String getVerificationString() {
            return I18n.tr("View Verification Page");
        }
        
        @Override
        protected String getDetailsString() {
            return I18n.tr("License Details:");
        }
        
        @Override
        protected String getDeedString() {
            return I18n.tr("View License Deed");
        }

        @Override
        protected String getLocatingString() {
            return I18n.tr("LimeWire is currently locating the details of this license.");
        }

        @Override
        protected String getNotVerifiedString() {
            return I18n.tr("LimeWire has not yet verified this license.\nClick \'verify\' to locate the details of this Creative Commons license.");
        }

        @Override
        protected String getTitleString() {
            return I18n.tr("Creative Commons Details");
        }
    }
    
    /**
     * Special stuff for a Weed license.
     */
    private static class WeedWindow extends LicenseWindow {
        WeedWindow(License l, URN u, LimeXMLDocument d, VerificationListener v) {
            super(l, u, d, v, "WEED");
        }
        
        @Override
        protected String getLargeLicenseURLString() {
            return "http://www.weedshare.com";
        }
        
        @Override
        protected String getInvalidString() {
            return I18n.tr("LimeWire was unable to locate the details of this Weed file.");
        }
        
        @Override
        protected String getRetryString() {
            return I18n.tr("You can try looking up the details again by clicking \'Verify\'.");
        }
        
        @Override
        protected String getClaimedDeedString() {
            return I18n.tr("View Weedshare Usage Rights");
        }
        
        @Override
        protected String getVerificationString() {
            return I18n.tr("View Verification Page");
        }
        
        @Override
        protected String getDetailsString() {
            return I18n.tr("License Details:");
        }
        
        @Override
        protected String getDeedString() {
            return I18n.tr("View Usage Rights");
        }
        
        @Override
        protected String getLocatingString() {
            return I18n.tr("LimeWire is contacting www.weedshare.com for the details of this file.");
        }

        @Override
        protected String getNotVerifiedString() {
            return I18n.tr("LimeWire has not yet verified this license.\nClick \'verify\' to locate the details of this file.");
        }

        @Override
        protected String getTitleString() {
            return I18n.tr("Weedshare Details");
        }
        
        /** Overriden to only show the img, not a license text box. */
        @Override
        protected void createTopOfWindow(Container parent) {
            GridBagConstraints c = new GridBagConstraints();
            JLabel img = new URLLabel(getLargeLicenseURLString(), getLargeLicenseIcon());
            c.insets = new Insets(4, 4, 2, 4);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            parent.add(img, c);
        }
    }
    
    /**
     * Special stuff for an unknown license.
     */
    private static class UnknownWindow extends LicenseWindow {
        UnknownWindow(License l, URN u, LimeXMLDocument d, VerificationListener v) {
            super(l, u, d, v, "UNKNOWN");
        }
        
        @Override
        protected String getLargeLicenseURLString() {
            return "http://www.limewire.com";
        }
        
        @Override
        protected String getInvalidString() {
            return I18n.tr("This file may require a license in order to open and view.");
        }
        
        @Override
        protected String getRetryString() {
            return I18n.tr("You can try validating this license again by clicking \'Verify\'.");
        }
        
        @Override
        protected String getClaimedDeedString() {
            return I18n.tr("View Claimed License Deed");
        }
        
        @Override
        protected String getVerificationString() {
            return I18n.tr("View Verification Page");
        }
        
        @Override
        protected String getDetailsString() {
            return I18n.tr("License Details:");
        }
        
        @Override
        protected String getDeedString() {
            return I18n.tr("View License Deed");
        }
        
        @Override
        protected String getLocatingString() {
            return I18n.tr("LimeWire is currently locating the details of this license.");
        }

        @Override
        protected String getNotVerifiedString() {
            return I18n.tr("LimeWire has not yet verified this license.\nClick \'verify\' to locate the details of this Creative Commons license.");
        }

        @Override
        protected String getTitleString() {
            return I18n.tr("License Details");
        }
        
        @Override
        protected Icon getLargeLicenseIcon() {
            return GUIMediator.getThemeImage("lw_logo");
        }
        
        /** Overriden to only show the img, not a license text box. */
        @Override
        protected void createTopOfWindow(Container parent) {
            GridBagConstraints c = new GridBagConstraints();
            JLabel img = new URLLabel(getLargeLicenseURLString(), getLargeLicenseIcon());
            c.insets = new Insets(4, 4, 2, 4);
            c.gridwidth = GridBagConstraints.REMAINDER;
            c.fill = GridBagConstraints.BOTH;
            parent.add(img, c);
        }
        
        /** Always shows not valid. */
        @Override
        protected void buildDetails() {
            DETAILS.removeAll();
            createNotValid();
            validate();
            repaint();
        }
        
        /** Can't handle unknown licenses. */
        @Override
        protected boolean allowRetryLink() { return false; }
        /** Can't handle unknown licenses. */
        @Override
        protected boolean allowVerificationLink() { return false; }
        /** Can't handle unknown licenses. */
        @Override
        protected boolean allowClaimedDeedLink() { return false; }
        /** Can't handle unknown licenses. */
        @Override
        protected boolean allowVerifyLookup() { return false; }
    }
}
