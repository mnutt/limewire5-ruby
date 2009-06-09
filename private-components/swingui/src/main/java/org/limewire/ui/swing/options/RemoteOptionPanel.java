package org.limewire.ui.swing.options;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.http.mongrel.MongrelManager;
import org.limewire.http.mongrel.MongrelGlue;
import org.limewire.http.mongrel.WebSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.action.StartRemoteAction;
import org.limewire.ui.swing.action.StopRemoteAction;

import com.google.inject.Inject;

/**
 * Misc Option View
 */
public class RemoteOptionPanel extends OptionPanel {

    private ServicePanel servicePanel;
    private WebPanel webPanel;
    
    private MongrelManager mongrelManager;
    
    //Language components, does not exist in its own subcomponent
    @Resource private Font font;

    @Inject
    public RemoteOptionPanel(MongrelGlue mongrelGlue) {
        this.mongrelManager = mongrelGlue.getMongrelManager();
        
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getServicePanel(), "pushx, growx");
        add(getWebPanel(), "pushx, growx");
    }

    private OptionPanel getServicePanel() {
        if(servicePanel == null) {
            servicePanel = new ServicePanel();
        }
        return servicePanel;
    }
    
    private OptionPanel getWebPanel() {
        if(webPanel == null) {
            webPanel = new WebPanel();
        }
        return webPanel;
    }

    @Override
    boolean applyOptions() {
        return getServicePanel().applyOptions() || getWebPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {        
        return getServicePanel().hasChanged() || getWebPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getServicePanel().initOptions();
        getWebPanel().initOptions();
    }

    private class ServicePanel extends OptionPanel {

        private JCheckBox startOnLaunchCheckBox;

        public ServicePanel() {
            super(tr("Service"));

            startOnLaunchCheckBox = new JCheckBox(tr("Start on launch"));
            startOnLaunchCheckBox.setContentAreaFilled(false);
            
            JLabel statusLabel = new JLabel(I18n.tr("Service status:"));
            JLabel status = new JLabel(I18n.tr(mongrelManager.getStatus()));
            
            JButton startButton = new JButton(I18n.tr("Start Remote"));
            startButton.addActionListener(new StartRemoteAction(mongrelManager));
            JButton stopButton = new JButton(I18n.tr("Stop Remote"));
            stopButton.addActionListener(new StopRemoteAction(mongrelManager));

            add(statusLabel, "gapbottom 5");
            add(status, "gapbottom 5, wrap");
            add(startButton, "gapbottom 5");
            add(stopButton, "gapbottom 5, wrap");
            add(startOnLaunchCheckBox, "wrap");
        }

        @Override
        boolean applyOptions() {
            SwingUiSettings.START_REMOTE_ON_LAUNCH.setValue(startOnLaunchCheckBox.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return startOnLaunchCheckBox.isSelected() != SwingUiSettings.START_REMOTE_ON_LAUNCH.getValue();
        }

        @Override
        public void initOptions() {
            startOnLaunchCheckBox.setSelected(SwingUiSettings.START_REMOTE_ON_LAUNCH.getValue());
        }
    }
    private class WebPanel extends OptionPanel {

        private JPasswordField passwordField;
        private JTextField usernameField;
        
        public WebPanel() {
            super(tr("WebUI"));

            passwordField = new JPasswordField(30);
            usernameField = new JTextField(30);
            JLabel passwordLabel = new JLabel(I18n.tr("Password:"));
            JLabel usernameLabel = new JLabel(I18n.tr("Username:"));

            add(passwordLabel, "gapbottom 5");
            add(passwordField, "gapbottom 5, wrap");
            add(usernameLabel);
            add(usernameField, "wrap");
        }

        @Override
        boolean applyOptions() {
            WebSettings.WEB_PASSWORD.set(passwordField.getPassword().toString());
            WebSettings.WEB_ADDRESS.set(usernameField.getText());
            return false;
        }

        @Override
        boolean hasChanged() {
            return passwordField.getPassword().toString() != WebSettings.WEB_PASSWORD.getValueAsString() ||
                   usernameField.getText() != WebSettings.WEB_ADDRESS.getValueAsString();
        }

        @Override
        public void initOptions() {
            passwordField.setText(WebSettings.WEB_PASSWORD.getValueAsString()); 
            usernameField.setText(WebSettings.WEB_ADDRESS.getValueAsString());
        }
    }
}