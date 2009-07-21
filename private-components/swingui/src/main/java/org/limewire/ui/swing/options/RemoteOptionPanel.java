package org.limewire.ui.swing.options;

import static org.limewire.ui.swing.util.I18n.tr;


import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.http.mongrel.MongrelManager;
import org.limewire.http.mongrel.MongrelGlue;
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
    
    private MongrelManager mongrelManager;

    @Inject
    public RemoteOptionPanel(MongrelGlue mongrelGlue) {
        this.mongrelManager = mongrelGlue.getMongrelManager();
        
        GuiUtils.assignResources(this);

        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getServicePanel(), "pushx, growx");
    }

    private OptionPanel getServicePanel() {
        if(servicePanel == null) {
            servicePanel = new ServicePanel();
        }
        return servicePanel;
    }

    @Override
    boolean applyOptions() {
        return getServicePanel().applyOptions();
    }

    @Override
    boolean hasChanged() {        
        return getServicePanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getServicePanel().initOptions();
    }

    private class ServicePanel extends OptionPanel {

        private JCheckBox startOnLaunchCheckBox;

        public ServicePanel() {
            super(tr("Service"));

            startOnLaunchCheckBox = new JCheckBox(tr("Start on launch"));
            startOnLaunchCheckBox.setContentAreaFilled(false);
            
            JLabel statusLabel = new JLabel(I18n.tr("Service status:"));
            
            JButton startButton = new JButton(I18n.tr("Start Remote"));
            startButton.addActionListener(new StartRemoteAction(mongrelManager));
            JButton stopButton = new JButton(I18n.tr("Stop Remote"));
            stopButton.addActionListener(new StopRemoteAction(mongrelManager));

            add(statusLabel, "gapbottom 5");
            // add(status, "gapbottom 5, wrap");
            // add(startButton, "gapbottom 5");
            // add(stopButton, "gapbottom 5, wrap");
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
}