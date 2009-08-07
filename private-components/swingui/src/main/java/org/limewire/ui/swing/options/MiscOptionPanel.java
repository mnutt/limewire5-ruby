package org.limewire.ui.swing.options;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Locale;

import javax.swing.AbstractAction;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.UrlAction;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LanguageComboBox;
import org.limewire.ui.swing.friends.settings.FriendAccountConfiguration;
import org.limewire.ui.swing.friends.settings.FriendAccountConfigurationManager;
import org.limewire.ui.swing.search.resultpanel.LicenseWarningDownloadPreprocessor;
import org.limewire.ui.swing.settings.QuestionsHandler;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Misc Option View.
 */
public class MiscOptionPanel extends OptionPanel {

    // backwards compatibility
    private static final int SKIP_WARNING_VALUE = LicenseWarningDownloadPreprocessor.SKIP_WARNING_VALUE;
    private static final int SHOW_WARNING_VALUE = LicenseWarningDownloadPreprocessor.SHOW_WARNING_VALUE;
    
    private static final String TRANSLATE_URL = "http://wiki.limewire.org/index.php?title=Translate";
    
    private final Provider<FriendAccountConfigurationManager> accountManager;

    private NotificationsPanel notificationsPanel;
    private FriendsChatPanel friendsChatPanel;
    
    private Locale currentLanguage;
    private JLabel comboLabel;
    private final JComboBox languageDropDown;
    private final HyperlinkButton translateButton;
    // TODO: re-enable this code once the setting does something
    // private final JCheckBox shareUsageDataCheckBox;

    @Inject

    public MiscOptionPanel(Provider<FriendAccountConfigurationManager> accountManager) {
        this.accountManager = accountManager;
        
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("nogrid, insets 15 15 15 15, fillx, gap 4"));

        comboLabel = new JLabel(I18n.tr("Language:"));
        languageDropDown = new LanguageComboBox();
        
        translateButton = new HyperlinkButton(new UrlAction(I18n.tr("Help translate LimeWire!"), TRANSLATE_URL));
        
        add(comboLabel);
        add(languageDropDown);
        add(translateButton, "wrap");
        
        add(getNotificationsPanel(), "growx, wrap");
        add(getFriendChatPanel(), "growx, wrap");

        // TODO: re-enable this code once the setting does something
        /*
        shareUsageDataCheckBox = new JCheckBox((I18n.tr("Help improve LimeWire by sending us anonymous usage data")));
        shareUsageDataCheckBox.setOpaque(false);
        add(shareUsageDataCheckBox);
        add(new LearnMoreButton("http://www.limewire.com/client_redirect/?page=anonymousDataCollection"), "wrap");
        */
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

    @Override
    boolean applyOptions() {
        // TODO: re-enable this code once the setting does something
        // ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.setValue(shareUsageDataCheckBox.isSelected());
        
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        
        boolean restart = getNotificationsPanel().applyOptions();
        restart |= getFriendChatPanel().applyOptions();
        
        // if the language changed, always notify about a required restart
        if(selectedLocale != null && !currentLanguage.equals(selectedLocale)) {
            currentLanguage = selectedLocale;
            LanguageUtils.setLocale(selectedLocale);
            restart = true;
        }
        return restart;
    }

    @Override
    boolean hasChanged() {
        Locale selectedLocale = (Locale) languageDropDown.getSelectedItem();
        return getNotificationsPanel().hasChanged() ||
            getFriendChatPanel().hasChanged() ||
            selectedLocale != currentLanguage;
        // TODO: re-enable this code once the setting does something
        /*
        return getNotificationsPanel().hasChanged() || getFriendChatPanel().hasChanged() ||
                selectedLocale != currentLanguage ||
                ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.getValue() 
                    != shareUsageDataCheckBox.isSelected();
        */
    }

    @Override
    public void initOptions() {
        // TODO: re-enable this code once the setting does something
        // shareUsageDataCheckBox.setSelected(ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.getValue());
        getNotificationsPanel().initOptions();
        getFriendChatPanel().initOptions();
        currentLanguage = LanguageUtils.getCurrentLocale();
        // if language got corrupted somehow, resave it
        // this shouldn't be possible but somehow currentLanguage can be 
        // null on OSX.
        if(currentLanguage == null) {
            LanguageUtils.setLocale(Locale.ENGLISH);
            currentLanguage = Locale.ENGLISH;
        }
        languageDropDown.setSelectedItem(currentLanguage);
    }

    private class NotificationsPanel extends OptionPanel {

        private JCheckBox showNotificationsCheckBox;
        private JCheckBox playNotificationsCheckBox;
        private JButton resetWarningsButton;

        public NotificationsPanel() {
            super(tr("Notifications and Warnings"));

            showNotificationsCheckBox = new JCheckBox(tr("Show popup system notifications"));
            showNotificationsCheckBox.setContentAreaFilled(false);
            playNotificationsCheckBox = new JCheckBox(tr("Play notification sounds"));
            playNotificationsCheckBox.setContentAreaFilled(false);
            resetWarningsButton = new JButton(new AbstractAction(I18n.tr("Reset")){
                @Override
                public void actionPerformed(ActionEvent e) {
                    resetWarnings();
                }
            });
            
            add(showNotificationsCheckBox, "wrap");
            add(playNotificationsCheckBox, "wrap");
            add(new JLabel(I18n.tr("Reset warning messages")));
            add(resetWarningsButton, "wrap");
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
            for(String label : accountManager.get().getLabels()) {
                if(!label.equals("Facebook")) {
                    serviceComboBox.addItem(label);
                }
            }
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

            add(autoLoginCheckBox, "wrap");
            
            JPanel servicePanel = new JPanel(new MigLayout("insets 0, fill"));
            servicePanel.setOpaque(false);
            
            servicePanel.add(new JLabel(tr("Using:")), "gapleft 25");
            servicePanel.add(serviceComboBox, "wrap");

            servicePanel.add(serviceLabel, "gapleft 25, hidemode 3");
            servicePanel.add(serviceField, "hidemode 3, wrap");

            servicePanel.add(new JLabel(tr("Username:")), "gapleft 25");
            servicePanel.add(usernameField, "wrap");

            servicePanel.add(new JLabel(tr("Password:")), "gapleft 25");
            servicePanel.add(passwordField, "wrap");
            
            add(servicePanel);
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
            FriendAccountConfiguration config = accountManager.get().getConfig(label);
            if(config == accountManager.get().getAutoLoginConfig()) {
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
                accountManager.get().setAutoLoginConfig(null);
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
                    FriendAccountConfiguration config = accountManager.get().getConfig(label);
                    if(label.equals("Jabber")) {
                        String service = serviceField.getText().trim();
                        if(service.equals(""))
                            return false;
                        config.setServiceName(service);
                    }
                    config.setUsername(user);
                    config.setPassword(password);
                    accountManager.get().setAutoLoginConfig(config);
                } else {
                    accountManager.get().setAutoLoginConfig(null);
                }
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            FriendAccountConfiguration auto = accountManager.get().getAutoLoginConfig();

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
            FriendAccountConfiguration auto = accountManager.get().getAutoLoginConfig();
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
        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            FriendAccountConfiguration config = accountManager.get().getConfig(value.toString());
            if(config != null) {
                setIcon(config.getIcon());
            } else {
                setIcon(null);
            }
            return this;
        }
    }
    
    
    
    private static void resetWarnings() {
        int skipWarningSettingValue = getLicenseSettingValueFromCheckboxValue(true);
        QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.setValue(skipWarningSettingValue);
        QuestionsHandler.WARN_TORRENT_SEED_MORE.setValue(true);
        QuestionsHandler.CONFIRM_BLOCK_HOST.setValue(true);
    }
    
    private static int getLicenseSettingValueFromCheckboxValue(boolean isSelected) {
        return isSelected ? SHOW_WARNING_VALUE : SKIP_WARNING_VALUE;
    }
}
