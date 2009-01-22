package org.limewire.ui.swing.options;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfiguration;
import org.limewire.ui.swing.friends.settings.XMPPAccountConfigurationManager;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;

/**
 * Misc Option View
 */
public class MiscOptionPanel extends OptionPanel {

    private final XMPPAccountConfigurationManager accountManager;

    private NotificationsPanel notificationsPanel;
    private FriendsChatPanel friendsChatPanel;
    
    //Language components, does not exist in its own subcomponent
    @Resource private Font font;
    private Locale currentLanguage;
    private JLabel comboLabel;
    private JComboBox languageDropDown;

    @Inject
    public MiscOptionPanel(XMPPAccountConfigurationManager accountManager) {
        this.accountManager = accountManager;
        
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));

        comboLabel = new JLabel(I18n.tr("Language:"));
        languageDropDown = new JComboBox();
        createLanguageComboBox();
        
        add(comboLabel, "split, gapbottom 5");
        add(languageDropDown, "gapbottom 5, wrap");
        
        add(getNotificationsPanel(), "pushx, growx");
        add(getFriendChatPanel(), "pushx, growx");
    }

    private OptionPanel getNotificationsPanel() {
        if(notificationsPanel == null) {
            notificationsPanel = new NotificationsPanel();
        }
        return notificationsPanel;
    }

    private OptionPanel getFriendChatPanel() {
        if(friendsChatPanel == null) {
            friendsChatPanel = new FriendsChatPanel();
        }
        return friendsChatPanel;
    }
    
    private void createLanguageComboBox() {
        languageDropDown.setRenderer(new LocaleRenderer());
        languageDropDown.setFont(font);
        
        Locale[] locales = LanguageUtils.getLocales(font);
        languageDropDown.setModel(new DefaultComboBoxModel(locales));
    }

    @Override
    boolean applyOptions() {
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        
        boolean restart = getNotificationsPanel().applyOptions();
        restart |= getFriendChatPanel().applyOptions();
        
        // if the language changed, always notify about a required restart
        if(!selectedLocale.equals(currentLanguage)) {
            currentLanguage = selectedLocale;
            LanguageUtils.setLocale(selectedLocale);
            restart = true;
        }
        return restart;
    }

    @Override
    boolean hasChanged() {
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        
        return getNotificationsPanel().hasChanged() || getFriendChatPanel().hasChanged() ||
                selectedLocale != currentLanguage;
    }

    @Override
    public void initOptions() {
        getNotificationsPanel().initOptions();
        getFriendChatPanel().initOptions();
        
        currentLanguage = LanguageUtils.getCurrentLocale();
        languageDropDown.setSelectedItem(currentLanguage);
    }

    private class NotificationsPanel extends OptionPanel {

        private JCheckBox showNotificationsCheckBox;
        private JCheckBox playNotificationsCheckBox;

        public NotificationsPanel() {
            super(tr("Notifications"));

            showNotificationsCheckBox = new JCheckBox(tr("Show popup system notifications"));
            showNotificationsCheckBox.setContentAreaFilled(false);
            playNotificationsCheckBox = new JCheckBox(tr("Play notification sounds"));
            playNotificationsCheckBox.setContentAreaFilled(false);

            add(showNotificationsCheckBox, "wrap");
            add(playNotificationsCheckBox, "wrap");
        }

        @Override
        boolean applyOptions() {
            SwingUiSettings.SHOW_NOTIFICATIONS.setValue(showNotificationsCheckBox.isSelected());
            SwingUiSettings.PLAY_NOTIFICATION_SOUND.setValue(playNotificationsCheckBox.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return showNotificationsCheckBox.isSelected() != SwingUiSettings.SHOW_NOTIFICATIONS.getValue() ||
            playNotificationsCheckBox.isSelected() != SwingUiSettings.PLAY_NOTIFICATION_SOUND.getValue(); 
        }

        @Override
        public void initOptions() {
            playNotificationsCheckBox.setSelected(SwingUiSettings.PLAY_NOTIFICATION_SOUND.getValue());
            showNotificationsCheckBox.setSelected(SwingUiSettings.SHOW_NOTIFICATIONS.getValue());
        }
    }

    private class FriendsChatPanel extends OptionPanel implements SettingListener {

        private JCheckBox autoLoginCheckBox;
        private JComboBox serviceComboBox;
        private JLabel serviceLabel;
        private JTextField serviceField;
        private JTextField usernameField;
        private JPasswordField passwordField;

        public FriendsChatPanel() {
            super(tr("Friends and Chat"));
            
            SwingUiSettings.XMPP_AUTO_LOGIN.addSettingListener(this);

            autoLoginCheckBox = new JCheckBox(tr("Sign into Friends when LimeWire starts"));            
            autoLoginCheckBox.setContentAreaFilled(false);
            autoLoginCheckBox.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    setComponentsEnabled(autoLoginCheckBox.isSelected());
                }
            });

            serviceComboBox = new JComboBox();
            for(String label : accountManager.getLabels())
                serviceComboBox.addItem(label);
            serviceComboBox.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    populateInputs();
                }
            });
            serviceComboBox.setRenderer(new Renderer());
            serviceLabel = new JLabel(tr("Jabber Server:"));
            serviceField = new JTextField(18);
            usernameField = new JTextField(18);
            passwordField = new JPasswordField(18);

            add(autoLoginCheckBox, "split, wrap");

            add(new JLabel(tr("Using")), "gapleft 25, split");
            add(serviceComboBox, "wrap");

            add(serviceLabel, "gapleft 25, hidemode 3, split");
            add(serviceField, "hidemode 3, wrap");

            add(new JLabel(tr("Username:")), "gapleft 25, split");
            add(usernameField, "wrap");

            add(new JLabel(tr("Password:")), "gapleft 25, split");
            add(passwordField);
        }

        private void populateInputs() {
            String label = (String)serviceComboBox.getSelectedItem();
            if(label.equals("Jabber")) {
                serviceLabel.setVisible(true);
                serviceField.setVisible(true);
            } else {
                serviceLabel.setVisible(false);
                serviceField.setVisible(false);
            }
            XMPPAccountConfiguration config = accountManager.getConfig(label);
            if(config == accountManager.getAutoLoginConfig()) {
                serviceField.setText(config.getServiceName());
                usernameField.setText(config.getUserInputLocalID());
                passwordField.setText(config.getPassword());
            } else {
                serviceField.setText("");
                usernameField.setText("");
                passwordField.setText("");
            }
        }

        private void setComponentsEnabled(boolean enabled) {
            serviceComboBox.setEnabled(enabled);
            serviceField.setEnabled(enabled);
            usernameField.setEnabled(enabled);
            passwordField.setEnabled(enabled);
            if(!enabled) {
                serviceField.setText("");
                usernameField.setText("");
                passwordField.setText("");
                accountManager.setAutoLoginConfig(null);
            }
        }

        @Override
        boolean applyOptions() {
            if(hasChanged()) {
                if(autoLoginCheckBox.isSelected()) {
                    // Set this as the auto-login account
                    String user = usernameField.getText().trim();
                    String password = new String(passwordField.getPassword());
                    if(user.equals("") || password.equals("")) {
                        return false;
                    }            
                    String label = (String)serviceComboBox.getSelectedItem();
                    XMPPAccountConfiguration config = accountManager.getConfig(label);
                    if(label.equals("Jabber")) {
                        String service = serviceField.getText().trim();
                        if(service.equals(""))
                            return false;
                        config.setServiceName(service);
                    }
                    config.setUsername(user);
                    config.setPassword(password);
                    accountManager.setAutoLoginConfig(config);
                } else {
                    accountManager.setAutoLoginConfig(null);
                }
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
            if(auto == null) {
                return autoLoginCheckBox.isSelected();
            } else {
                if(!autoLoginCheckBox.isSelected())
                    return true;
                String label = (String)serviceComboBox.getSelectedItem();
                if(!label.equals(auto.getLabel()))
                    return true;
                String serviceName = serviceField.getText().trim();
                if(!serviceName.equals(auto.getServiceName()))
                    return true;
                String username = usernameField.getText().trim();
                if(!username.equals(auto.getUserInputLocalID()))
                    return true;
                String password = new String(passwordField.getPassword());
                if(!password.equals(auto.getPassword()))
                    return true;
            }
            return false;
        }

        @Override
        public void initOptions() {
            XMPPAccountConfiguration auto = accountManager.getAutoLoginConfig();
            if(auto == null) {
                serviceComboBox.setSelectedItem("Gmail");
                setComponentsEnabled(false);
                autoLoginCheckBox.setSelected(false);
            } else {
                serviceComboBox.setSelectedItem(auto.getLabel());
                setComponentsEnabled(true);
                autoLoginCheckBox.setSelected(true);
            }
            populateInputs();
        }
        
        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtils.invokeLater(new Runnable() {
                @Override
                public void run() {
                    initOptions();
                }
            });
        }
    }

    private class Renderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            XMPPAccountConfiguration config = accountManager.getConfig(value.toString());
            if(config != null) {
                setIcon(config.getIcon());
            } else {
                setIcon(null);
            }
            return this;
        }
    }
    
    private static class LocaleRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean hasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, hasFocus);
            
            if (value instanceof Locale) {
                Locale locale = (Locale) value;
                setText(locale.getDisplayName(locale));
            } else {
                setIcon(null);
            }
            
            return this;
        }
    }
}
