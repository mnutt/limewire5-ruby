package com.limegroup.gnutella.gui.options.panes;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class defines the panel in the options window that allows the user to
 * set the login data for the proxy.
 */
public final class ProxyLoginPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Login Details");
    
    public final static String LABEL = I18n.tr("Configure username and password to be used for the proxy.");

    /**
     * Constant for the key of the locale-specific <tt>String</tt> for the
     * check box that enables / disables password authentification at the
     * proxy.
     */
    private final String PROXY_AUTHENTICATE_CHECK_BOX_LABEL =
        I18nMarker.marktr("Enable Authentication (Does Not Work for HTTP Proxies):");

    /**
     * Constant for the key of the locale-specific <tt>String</tt> for the
     * label on the username field.
     */
    private final String PROXY_USERNAME_LABEL_KEY =
        I18nMarker.marktr("Username:");

    /**
     * Constant for the key of the locale-specific <tt>String</tt> for the
     * label on the password field.
     */
    private final String PROXY_PASSWORD_LABEL_KEY =
        I18nMarker.marktr("Password:");

    /**
     * Constant <tt>JTextField</tt> instance that holds the username.
     */
    private final JTextField PROXY_USERNAME_FIELD =
        new SizedTextField(12, SizePolicy.RESTRICT_BOTH);

    /**
     * Constant <tt>JTextField</tt> instance that holds the pasword.
     */
    private final JTextField PROXY_PASSWORD_FIELD =
        new SizedTextField(12, SizePolicy.RESTRICT_BOTH);

    /**
     * Constant for the check box that determines whether or not to
     * authenticate at proxy settings
     */
    private final JCheckBox CHECK_BOX = new JCheckBox();

    /**
	 * The constructor constructs all of the elements of this
	 * <tt>AbstractPaneItem</tt>.
	 * 
	 * @param key the key for this <tt>AbstractPaneItem</tt> that the
	 *        superclass uses to generate locale-specific keys
	 */
	public ProxyLoginPaneItem() {
	    super(TITLE, LABEL);

		CHECK_BOX.addItemListener(new LocalAuthenticateListener());

		LabeledComponent checkBox = new LabeledComponent(
				PROXY_AUTHENTICATE_CHECK_BOX_LABEL, CHECK_BOX,
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		LabeledComponent username = new LabeledComponent(
				PROXY_USERNAME_LABEL_KEY, PROXY_USERNAME_FIELD,
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
		LabeledComponent password = new LabeledComponent(
				PROXY_PASSWORD_LABEL_KEY, PROXY_PASSWORD_FIELD,
				LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);

		add(checkBox.getComponent());
		add(getVerticalSeparator());
		add(username.getComponent());
		add(getVerticalSeparator());
		add(password.getComponent());
	}
    /**
	 * Defines the abstract method in <tt>AbstractPaneItem</tt>.
	 * <p>
	 * 
	 * Sets the options for the fields in this <tt>PaneItem</tt> when the
	 * window is shown.
	 */
    @Override
    public void initOptions() {
        String username = ConnectionSettings.PROXY_USERNAME.getValue();
        String password = ConnectionSettings.PROXY_PASS.getValue();
        boolean authenticate = ConnectionSettings.PROXY_AUTHENTICATE.getValue();

        PROXY_USERNAME_FIELD.setText(username);
        PROXY_PASSWORD_FIELD.setText(password);

        //HTTP Authentication is not yet supported
        CHECK_BOX.setSelected(
            authenticate
                && ConnectionSettings.CONNECTION_METHOD.getValue()
                    != ConnectionSettings.C_HTTP_PROXY);
        updateState();
    }

    /**
     * Defines the abstract method in <tt>AbstractPaneItem</tt>.
     * <p>
     * 
     * Applies the options currently set in this window, displaying an error
     * message to the user if a setting could not be applied.
     * 
     * @throws IOException
     *             if the options could not be applied for some reason
     */
    @Override
    public boolean applyOptions() throws IOException {
        final String username = PROXY_USERNAME_FIELD.getText();
        final String password = PROXY_PASSWORD_FIELD.getText();
        final boolean authenticate = CHECK_BOX.isSelected();

        ConnectionSettings.PROXY_USERNAME.setValue(username);
        ConnectionSettings.PROXY_PASS.setValue(password);
        ConnectionSettings.PROXY_AUTHENTICATE.setValue(authenticate);
        return false;
    }
    
    public boolean isDirty() {
        return !ConnectionSettings.PROXY_USERNAME.getValue().equals(PROXY_USERNAME_FIELD.getText()) ||
               !ConnectionSettings.PROXY_PASS.getValue().equals(PROXY_PASSWORD_FIELD.getText()) ||
               ConnectionSettings.PROXY_AUTHENTICATE.getValue() != CHECK_BOX.isSelected();
    }
    
    private void updateState() {
        PROXY_USERNAME_FIELD.setEnabled(CHECK_BOX.isSelected());
        PROXY_PASSWORD_FIELD.setEnabled(CHECK_BOX.isSelected());
        PROXY_USERNAME_FIELD.setEditable(CHECK_BOX.isSelected());
        PROXY_PASSWORD_FIELD.setEditable(CHECK_BOX.isSelected());
    }

    /**
     * Listener class that responds to the checking and the unchecking of the
     * checkbox specifying whether or not to use authentication. It makes the
     * other fields editable or not editable depending on the state of the
     * JCheckBox.
     */
    private class LocalAuthenticateListener implements ItemListener {
        public void itemStateChanged(ItemEvent e) {
            updateState();
        }
    }
}
