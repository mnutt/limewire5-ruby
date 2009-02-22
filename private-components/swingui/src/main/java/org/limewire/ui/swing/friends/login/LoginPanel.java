package org.limewire.ui.swing.friends.login;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Dimension;
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
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.PromptPasswordField;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.ComboBoxDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.painter.factories.BarPainterFactory;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.ResizeUtils;
import org.limewire.xmpp.api.client.XMPPConnectionConfiguration;
import org.limewire.xmpp.api.client.XMPPException;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
class LoginPanel extends JXPanel implements SettingListener {

    private static final String SIGNIN_ENABLED_TEXT = tr("Sign in");
    private static final String SIGNIN_DISABLED_TEXT = tr("Signing in ...");
    private static final String AUTHENTICATION_ERROR = tr("Incorrect username or password.");
    private static final String NETWORK_ERROR = tr("Network error.");
    
    private static final String CONFIG = "limewire.configProperty";

    private LimeComboBox serviceComboBox;
    private PromptTextField serviceField;
    private PromptTextField usernameField;
    private PromptPasswordField passwordField;
    private JCheckBox autoLoginCheckBox;
    private JLabel authFailedLabel;
    private JXButton signInButton;
    private final XMPPAccountConfigurationManager accountManager;
    private final XMPPService xmppService;
    private final SignInAction signinAction = new SignInAction();
    
    @Inject
    LoginPanel(XMPPAccountConfigurationManager accountManager,
            XMPPService xmppService,
            ComboBoxDecorator comboFactory,
            ButtonDecorator buttonDecorator,
            BarPainterFactory barPainterFactory,
            TextFieldDecorator textFieldDecorator) {
        
        GuiUtils.assignResources(this);
        
        this.accountManager = accountManager;
        this.xmppService = xmppService;

        SwingUiSettings.XMPP_AUTO_LOGIN.addSettingListener(this);
        initComponents(comboFactory, buttonDecorator, textFieldDecorator, barPainterFactory);
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

    private void initComponents(ComboBoxDecorator comboFactory, ButtonDecorator buttonDecorator,
            TextFieldDecorator textFieldDecorator, BarPainterFactory barPainterFactory) {
        
        JLabel titleLabel = new JLabel(tr("Sign in with"));
        titleLabel.setName("LoginPanel.titleLabel");
        
        List<Action> actions = new ArrayList<Action>();
        for (XMPPAccountConfiguration config : accountManager.getConfigurations()) {
            Action action = new AbstractAction(config.getLabel(), config.getIcon()) {            
                @Override
                public void actionPerformed(ActionEvent e) {}
            };
            action.putValue(CONFIG, config);
            actions.add(action);
        }        
        serviceComboBox = new LimeComboBox(actions);
        comboFactory.decorateDarkFullComboBox(serviceComboBox, AccentType.NONE);
        
        serviceComboBox.addSelectionListener(new LimeComboBox.SelectionListener() {
            @Override
            public void selectionChanged(Action item) {
                populateInputs();
            }
        });
        ResizeUtils.looseForceHeight(serviceComboBox, 22);
        
        serviceField = new PromptTextField(tr("Domain"));
        textFieldDecorator.decoratePromptField(serviceField, AccentType.NONE);
        
        usernameField = new PromptTextField(tr("Username"));
        textFieldDecorator.decoratePromptField(usernameField, AccentType.NONE);
        passwordField = new PromptPasswordField(tr("Password"));
        textFieldDecorator.decoratePromptField(passwordField, AccentType.NONE);
        passwordField.setAction(signinAction);
        
        ResizeUtils.forceSize(usernameField, new Dimension(139, 22));
        ResizeUtils.forceSize(passwordField, new Dimension(139, 22));

        autoLoginCheckBox = new JCheckBox(tr("Remember me")); 
        autoLoginCheckBox.setSelected(SwingUiSettings.REMEMBER_ME_CHECKED.getValue());
        autoLoginCheckBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                // When the user clears the auto-login checkbox,
                // forget the auto-login config
                if(!autoLoginCheckBox.isSelected()) {
                    accountManager.setAutoLoginConfig(null);
                }
                SwingUiSettings.REMEMBER_ME_CHECKED.setValue(autoLoginCheckBox.isSelected());
            }
        });
        autoLoginCheckBox.setName("LoginPanel.autoLoginCheckBox");
        autoLoginCheckBox.setOpaque(false);

        signInButton = new JXButton(signinAction);
        buttonDecorator.decorateDarkFullButton(signInButton, AccentType.NONE);
        signInButton.setBorder(BorderFactory.createEmptyBorder(0,15,2,15));
        ResizeUtils.looseForceHeight(signInButton, 22);
        
        authFailedLabel = new MultiLineLabel();
        authFailedLabel.setVisible(false);
        authFailedLabel.setName("LoginPanel.authFailedLabel");

        JButton hideButton = new IconButton();
        hideButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LoginPanel.this.setVisible(false);
            }
        });
        hideButton.setName("LoginPanel.hideButton");

        setLayout(new MigLayout("nogrid, gap 0, insets 4 5 8 4, fill, alignx left"));
        
        add(titleLabel, "gaptop 1, gapleft 2, gapbottom 4");
        add(hideButton, "alignx right, aligny top, gapbefore push, wrap");
        add(authFailedLabel, "gapleft 2, wmin 0, hidemode 3, gapbottom 3, wrap");
        add(serviceComboBox, "gapbottom 8, wmin 0, wrap");
        add(serviceField, "gapbottom 8, hidemode 3, grow, wmin 0, wrap");
        add(usernameField, "gapbottom 8, grow, wrap");
        add(passwordField, "gapbottom 4, grow, wrap");
        add(autoLoginCheckBox, "gapbottom 3, wmin 0, wrap");
        add(signInButton);

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
            // Preserve prior state.
//            autoLoginCheckBox.setSelected(false);
        }
        validate();
        repaint();
    }

    void disconnected(Exception reason) {
        setSignInComponentsEnabled(true);
        populateInputs();
        if(reason !=null && reason.getMessage() != null && reason.getMessage().toLowerCase(Locale.US).contains("auth")) {
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
        SwingUtilities.invokeLater(new Runnable(){
            public void run() {
                populateInputs();                
            }
        });
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
