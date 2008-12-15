package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.settings.XMPPSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.components.LimePromptPasswordField;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class LoginPanel extends JXPanel implements SettingListener {

    // private static final Log LOG = LogFactory.getLog(LoginPanel.class);

    private static final String SIGNIN_ENABLED_TEXT = tr("Sign in");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in ...");

    private static final String AUTHENTICATION_ERROR = tr("Incorrect username\nor password.");
    private static final String NETWORK_ERROR = tr("Network error.");
    
    private static final String CONFIG = "limewire.configProperty";

    private LimeComboBox serviceComboBox;
    private JTextField serviceField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JLabel authFailedLabel;
    private JXButton signInButton;
    private final XMPPAccountConfigurationManager accountManager;
    private final XMPPService xmppService;
    private final SignInAction signinAction = new SignInAction();
    
    @Resource private Icon loginHide;
    @Resource private Icon loginHideHover;

    @Inject
    LoginPanel(XMPPAccountConfigurationManager accountManager,
            XMPPService xmppService,
            LimeComboBoxFactory comboFactory,
            ButtonDecorator buttonDecorator,
            BarPainterFactory barPainterFactory) {
        this.accountManager = accountManager;
        this.xmppService = xmppService;
        GuiUtils.assignResources(this);
        XMPPSettings.XMPP_AUTO_LOGIN.addSettingListener(this);
        initComponents(comboFactory, buttonDecorator, barPainterFactory);
    }
    
    private Action getActionForConfig(XMPPAccountConfiguration config) {
        for(Action action : serviceComboBox.getActions()) {
            if(action.getValue(CONFIG).equals(config)) {
                return action;
            }
        }
        return null;
    }
    
    private Action getActionForLabel(String label) {
        for(Action action : serviceComboBox.getActions()) {
            if(((XMPPAccountConfiguration)action.getValue(CONFIG)).getLabel().equals(label)) {
                return action;
            }
        }
        return null;
    }

    void autoLogin(XMPPAccountConfiguration auto) {
        serviceComboBox.setSelectedAction(getActionForConfig(auto));
        login(auto);
    }    

    @Override
    public void setVisible(boolean flag) {
        boolean becameVisible = flag && !isVisible();
        super.setVisible(flag);
        if(becameVisible) {
            populateInputs();
            // Give focus to the first visible, empty text field
            if(serviceField.isVisible()) {
                if(serviceField.getText().trim().equals(""))
                    serviceField.requestFocusInWindow();
            } else {
                if(usernameField.getText().trim().equals(""))
                    usernameField.requestFocusInWindow();
            }
        }
    }

    private void initComponents(LimeComboBoxFactory comboFactory, ButtonDecorator buttonDecorator,
            BarPainterFactory barPainterFactory) {
        List<Action> actions = new ArrayList<Action>();
        for (XMPPAccountConfiguration config : accountManager.getConfigurations()) {
            Action action = new AbstractAction(config.getLabel(), config.getIcon()) {            
                @Override
                public void actionPerformed(ActionEvent e) {}
            };
            action.putValue(CONFIG, config);
            actions.add(action);
        }        
        serviceComboBox = comboFactory.createDarkFullComboBox(actions, AccentType.NONE);
        serviceComboBox.addSelectionListener(new LimeComboBox.SelectionListener() {
            @Override
            public void selectionChanged(Action item) {
                populateInputs();
            }
        });
        
        serviceField = new LimePromptTextField("Server", AccentType.NONE);
        usernameField =  new LimePromptTextField("Username", AccentType.NONE);
        passwordField = new LimePromptPasswordField("Password", AccentType.NONE);
        passwordField.setAction(signinAction);

        autoLoginCheckBox = new JCheckBox(tr("Remember me"));
        autoLoginCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // When the user clears the auto-login checkbox,
                // forget the auto-login config
                if(!autoLoginCheckBox.isSelected()) {
                    serviceField.setText("");
                    usernameField.setText("");
                    passwordField.setText("");
                    accountManager.setAutoLoginConfig(null);
                }
            }
        });
        autoLoginCheckBox.setOpaque(false);
        autoLoginCheckBox.setBorder(BorderFactory.createEmptyBorder());

        signInButton = new JXButton(signinAction);
        buttonDecorator.decorateDarkFullButton(signInButton, AccentType.NONE);
        signInButton.setOpaque(false);

        authFailedLabel = new MultiLineLabel();
        authFailedLabel.setVisible(false);
        authFailedLabel.setForeground(Color.RED);

        JButton hideButton = new IconButton(loginHide, loginHideHover);
        hideButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginPanel.this.setVisible(false);
            }
        });

        setLayout(new MigLayout("nocache, gap 0, fill"));
        add(authFailedLabel, "alignx left, gapleft 2, wmin 0, hidemode 3");
        add(hideButton, "alignx right, aligny top, gaptop 4, gapright 4, wrap");
        add(new JLabel(tr("Sign in with")), "alignx left, gapleft 2, gapbottom 4, wrap");
        add(serviceComboBox, "alignx left, gapbottom 8, wmin 0, wrap");
        add(serviceField, "alignx left, gapbottom 8, hidemode 3, grow, wmin 0, wrap");
        add(usernameField, "alignx left, gapbottom 8, grow, wmin 0, wrap");
        add(passwordField, "alignx left, gapbottom 8, grow, wmin 0, wrap");
        add(autoLoginCheckBox, "alignx left, gapbottom 8, wmin 0, wrap");
        add(signInButton, "alignx left, wmin 0");

        setBackgroundPainter(barPainterFactory.createFriendsBarPainter());

        serviceComboBox.setSelectedAction(getActionForLabel("GMail"));
        setSignInComponentsEnabled(true);
    }

    private void populateInputs() {
        XMPPAccountConfiguration config = (XMPPAccountConfiguration)serviceComboBox.getSelectedAction().getValue(CONFIG);
        if(config.getLabel().equals("Jabber")) {
            serviceField.setVisible(true);
        } else {
            serviceField.setVisible(false);
        }
        
        if(config == accountManager.getAutoLoginConfig()) {
            serviceField.setText(config.getServiceName());
            usernameField.setText(config.getUserInputLocalID());
            passwordField.setText(config.getPassword());
            autoLoginCheckBox.setSelected(true);
        } else {
            serviceField.setText("");
            usernameField.setText("");
            passwordField.setText("");
            autoLoginCheckBox.setSelected(false);
        }
        validate();
        repaint();
    }

    void disconnected(Exception reason) {
        setSignInComponentsEnabled(true);
        populateInputs();
        if(reason.getMessage().toLowerCase(Locale.US).contains("auth")) {
            authFailedLabel.setText(AUTHENTICATION_ERROR);
            passwordField.setText("");
        } else {
            authFailedLabel.setText(NETWORK_ERROR);
        }
        authFailedLabel.setVisible(true);
        validate();
        repaint();
    }

    private void setSignInComponentsEnabled(boolean isEnabled) {
        signinAction.setEnabled(isEnabled);
        signinAction.putValue(Action.NAME, isEnabled ? SIGNIN_ENABLED_TEXT : SIGNIN_DISABLED_TEXT);
        serviceField.setEnabled(isEnabled);
        usernameField.setEnabled(isEnabled);
        passwordField.setEnabled(isEnabled);
        autoLoginCheckBox.setEnabled(isEnabled);
        serviceComboBox.setEnabled(isEnabled);
    }

    void connected(XMPPConnectionConfiguration config) {
        setSignInComponentsEnabled(true);
    }

    private void login(final XMPPAccountConfiguration config) {
        setSignInComponentsEnabled(false);
        authFailedLabel.setVisible(false);
        validate();
        repaint();
        BackgroundExecutorService.execute(new Runnable() {
            public void run() {
                try {
                    xmppService.login(config);
                } catch (XMPPException e) {
                    // Ignored
                }
            }
        });            
    }

    @Override
    public void settingChanged(SettingEvent evt) {
        populateInputs();
    }

    class SignInAction extends AbstractAction {
        public SignInAction() {
            super();
        }

        public void actionPerformed(ActionEvent e) {
            String user = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());
            if(user.equals("") || password.equals("")) {
                return;
            }            
            XMPPAccountConfiguration config = (XMPPAccountConfiguration)serviceComboBox.getSelectedAction().getValue(CONFIG);
            if(config.getLabel().equals("Jabber")) {
                String service = serviceField.getText().trim();
                if(service.equals(""))
                    return;
                config.setServiceName(service);
            }
            config.setUsername(user);
            config.setPassword(password);
            if(autoLoginCheckBox.isSelected()) {
                // Set this as the auto-login account
                accountManager.setAutoLoginConfig(config);
            } else {
                // If there was previously an auto-login account, delete it
                accountManager.setAutoLoginConfig(null);
            }
            login(config);
        }
    }

    public void connecting(XMPPConnectionConfiguration config) {
        setSignInComponentsEnabled(false);
    }
}
