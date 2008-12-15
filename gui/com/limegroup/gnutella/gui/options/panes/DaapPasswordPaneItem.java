package com.limegroup.gnutella.gui.options.panes;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.limewire.core.settings.DaapSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedPasswordField;
import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GuiCoreMediator;

import de.kapsi.net.daap.DaapUtil;

public final class DaapPasswordPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Password Protection");
    
    public final static String LABEL = I18n.tr("You can limit the access to this service with a password.");

//    private final String USERNAME_CHECK_BOX_LABEL = 
//        "OPTIONS_ITUNES_DAAP_PASSWORD_USERNAME_CHECKBOX_LABEL";
    
    private final String PASSWORD_CHECK_BOX_LABEL = 
            I18nMarker.marktr("Require Password:");

//    private final String USERNAME_BOX_LABEL = 
//        "OPTIONS_ITUNES_DAAP_PASSWORD_USERNAME_LABEL";
    
    private final String PASSWORD_BOX_LABEL = 
            I18nMarker.marktr("Password:");

    private final JCheckBox REQUIRE_USERNAME_CHECK_BOX = new JCheckBox();
    
    /**
     * Constant for the check box that specifies whether or not downloads 
     * should be automatically cleared.
     */
    private final JCheckBox REQUIRE_PASSWORD_CHECK_BOX = new JCheckBox();

    /** The Username */
    private final JTextField USERNAME = new SizedTextField(16, new Dimension(32, 20));
    
    /** The Password */
    private final JTextField PASSWORD = new SizedPasswordField(16, new Dimension(32, 20));

    /**
     * The constructor constructs all of the elements of this 
     * <tt>AbstractPaneItem</tt>.
     *
     * @param key the key for this <tt>AbstractPaneItem</tt> that the
     *            superclass uses to generate locale-specific keys
     */
    public DaapPasswordPaneItem() {
        super(TITLE, LABEL);
            
//            LabeledComponent usernameCheckBox 
//                = new LabeledComponent(USERNAME_CHECK_BOX_LABEL,
//                            REQUIRE_USERNAME_CHECK_BOX,
//                                 LabeledComponent.LEFT_GLUE);
            
            LabeledComponent passwordCheckBox 
                = new LabeledComponent(PASSWORD_CHECK_BOX_LABEL,
                                     REQUIRE_PASSWORD_CHECK_BOX, 
                                     LabeledComponent.LEFT_GLUE,
                                     LabeledComponent.LEFT);

//            LabeledComponent username 
//                = new LabeledComponent(USERNAME_BOX_LABEL,
//                                    USERNAME,
//                                    LabeledComponent.LEFT_GLUE);

            LabeledComponent password 
                = new LabeledComponent(PASSWORD_BOX_LABEL,
                                     PASSWORD,
                                     LabeledComponent.LEFT_GLUE,
                                     LabeledComponent.LEFT);
            
            add(passwordCheckBox.getComponent());
            //add(usernameCheckBox.getComponent());
            //add(username.getComponent());
            add(getVerticalSeparator());
            add(password.getComponent());
            
            REQUIRE_USERNAME_CHECK_BOX.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    enable();
                }
            });
            
            REQUIRE_PASSWORD_CHECK_BOX.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    enable();
                }
            });
    }

    private void enable() {
        PASSWORD.setEnabled(REQUIRE_PASSWORD_CHECK_BOX.isSelected());
        REQUIRE_USERNAME_CHECK_BOX.setEnabled(REQUIRE_PASSWORD_CHECK_BOX.isSelected());
        
        if (!REQUIRE_PASSWORD_CHECK_BOX.isSelected()) {
            REQUIRE_USERNAME_CHECK_BOX.setSelected(false);
        }
        
        USERNAME.setEnabled(REQUIRE_USERNAME_CHECK_BOX.isSelected() 
                && REQUIRE_PASSWORD_CHECK_BOX.isSelected());
    }
    
    /**
     * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
     *
     * Sets the options for the fields in this <tt>PaneItem</tt> when the 
     * window is shown.
     */
    @Override
    public void initOptions() {
        
        REQUIRE_USERNAME_CHECK_BOX.setSelected(DaapSettings.DAAP_REQUIRES_USERNAME.getValue());
        REQUIRE_PASSWORD_CHECK_BOX.setSelected(DaapSettings.DAAP_REQUIRES_PASSWORD.getValue());
        
        if (REQUIRE_USERNAME_CHECK_BOX.isSelected()) {
            USERNAME.setText(DaapSettings.DAAP_USERNAME.getValue());
        }
        
        if (REQUIRE_PASSWORD_CHECK_BOX.isSelected()) {
            PASSWORD.setText(DaapSettings.DAAP_PASSWORD.getValue());
        }
        
        enable();
    }

    /**
     * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
     *
     * Applies the options currently set in this window, displaying an
     * error message to the user if a setting could not be applied.
     *
     * @throws IOException if the options could not be applied for some reason
     */
    @Override
    public boolean applyOptions() throws IOException {

        final boolean prevRequiresUsername = DaapSettings.DAAP_REQUIRES_USERNAME.getValue();
        final String prevUsername = DaapSettings.DAAP_USERNAME.getValue();
        
        final boolean prevRequiresPassword = DaapSettings.DAAP_REQUIRES_PASSWORD.getValue();
        final String prevPassword = DaapSettings.DAAP_PASSWORD.getValue();
        
        final boolean requiresUsername = REQUIRE_USERNAME_CHECK_BOX.isSelected();
        final String username = USERNAME.getText().trim();
        
        final boolean requiresPassword = REQUIRE_PASSWORD_CHECK_BOX.isSelected();
        String password = PASSWORD.getText().trim();
        
        if (username.equals("") && requiresUsername) { 
            throw new IOException(); 
        }
        
        if (password.equals("") && requiresPassword) { 
            throw new IOException(); 
        }

        if ( ! DaapSettings.DAAP_USERNAME.getValue().equals(username)) {
            DaapSettings.DAAP_USERNAME.setValue(username);
        }
        
        if ( ! DaapSettings.DAAP_PASSWORD.equals(password)) {
            DaapSettings.DAAP_PASSWORD.setValue(password);
        }
        
        if (requiresUsername) {
            password = DaapUtil.calculateHA1(username, password);
        }
        
        if (requiresPassword != prevRequiresPassword 
                || (requiresPassword && !password.equals(prevPassword))) {

            DaapSettings.DAAP_REQUIRES_USERNAME.setValue(requiresUsername);
            DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(requiresPassword);

            try {

                // A password is required now or password has changed, 
                // disconnect all users...
                if (requiresPassword) { 
                    GuiCoreMediator.getDaapManager().disconnectAll();
                }
                
                GuiCoreMediator.getDaapManager().updateService();

            } catch (IOException err) {
                
                DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(prevRequiresUsername);
                DaapSettings.DAAP_USERNAME.setValue(prevUsername);
                
                DaapSettings.DAAP_REQUIRES_PASSWORD.setValue(prevRequiresPassword);
                DaapSettings.DAAP_PASSWORD.setValue(prevPassword);

                GuiCoreMediator.getDaapManager().stop();

                initOptions();

                throw err;
            }
        }

        return false;
    }

    public boolean isDirty() {
        return DaapSettings.DAAP_REQUIRES_PASSWORD.getValue() != REQUIRE_PASSWORD_CHECK_BOX.isSelected() ||
                DaapSettings.DAAP_REQUIRES_USERNAME.getValue() != REQUIRE_USERNAME_CHECK_BOX.isSelected() ||
                !DaapSettings.DAAP_PASSWORD.equals(PASSWORD.getText().trim()) ||
                !DaapSettings.DAAP_USERNAME.getValue().equals(USERNAME.getText().trim());
    }    
}
