package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;

import com.google.inject.Inject;
import net.miginfocom.swing.MigLayout;
import org.limewire.core.api.network.NetworkManager;
import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.NetworkSettings;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.NumericTextField;
import org.limewire.ui.swing.util.I18n;

/**
 * Firewall Option Panel
 */
public class FirewallOptionPanel extends OptionPanel {

    private NetworkManager networkManager;
    
    private ListeningPortPanel listeningPortPanel;
    private RouterConfigPanel routerConfigPanel;
    
    @Inject
    public FirewallOptionPanel(NetworkManager networkManager) {
        super();
        this.networkManager = networkManager;
        
        setLayout(new MigLayout("insets 10 10 10 10, fillx, wrap", "", ""));
        
        add(getListeningPortPanel(), "pushx, growx");
        add(getRouterConfigPanel(), "pushx, growx");
    }
    
    private OptionPanel getListeningPortPanel() {
        if(listeningPortPanel == null) {
            listeningPortPanel = new ListeningPortPanel();
        }
        return listeningPortPanel;
    }
    
    private OptionPanel getRouterConfigPanel() {
        if(routerConfigPanel == null) {
            routerConfigPanel = new RouterConfigPanel();
        }
        return routerConfigPanel;
    }
    
    @Override
    boolean applyOptions() {
        boolean restart = getListeningPortPanel().applyOptions();
        restart |= getRouterConfigPanel().applyOptions();
        return restart;
    }

    @Override
    boolean hasChanged() {
        return getListeningPortPanel().hasChanged() || getRouterConfigPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getListeningPortPanel().initOptions();
        getRouterConfigPanel().initOptions();
    }
    
    private class ListeningPortPanel extends OptionPanel {

        private final String description = I18n.tr("You can set the local network port that listens for incoming connections. This port may be changed in case of conflict with another program or if a specific port number is required for direct incoming connections by your firewall.");
        private NumericTextField portField;
        
        private int port;
        
        public ListeningPortPanel() {
            super(I18n.tr("Listening Port"));
            
            portField = new NumericTextField(5, 1, 0xFFFF);
            
            add(new MultiLineLabel(description, ReallyAdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "wrap");
            
            add(new JLabel(I18n.tr("Listen on port:")), "split");
            add(portField);
        }
        
        @Override
        boolean applyOptions() {
            int newPort = portField.getValue(port);
            if(newPort != port) {
                try {
                    NetworkSettings.PORT.setValue(newPort);
                    networkManager.setListeningPort(newPort);
                    port = newPort;
                    networkManager.portChanged();
                } catch(IOException ioe) {
                    FocusJOptionPane.showMessageDialog(FirewallOptionPanel.this, 
                            I18n.tr("The port chosen {0}, is already in use.", newPort),
                            I18n.tr("Network Port Error"),
                            JOptionPane.ERROR_MESSAGE);
                    NetworkSettings.PORT.setValue(port);
                    portField.setValue(port);
                }
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            int portSetting = NetworkSettings.PORT.getValue();
            return portSetting != portField.getValue(portSetting);
        }

        @Override
        public void initOptions() {
            port = NetworkSettings.PORT.getValue();
            portField.setValue(port);
        }
    }
    
    private class RouterConfigPanel extends OptionPanel {

        private final String description = I18n.tr("Using Universal Plug n' Play, LimeWire can automatically configure your router or firewall for optimal performance. If your router does not support Universal Plug n' Play, LimeWire can be set to advertise an external port manually.");
        private final JRadioButton plugAndPlayRadioButton;
        private final JRadioButton portForwardRadioButton;
        private final NumericTextField portTextField;
        private final JLabel starLabel;
        private final JRadioButton doNothingRadioButton;
        
        private final ButtonGroup buttonGroup;
        
        public RouterConfigPanel() {
            super(I18n.tr("Router Configuration"));
            
            plugAndPlayRadioButton = new JRadioButton(I18n.tr("Use Universal Plug n' Play (Recommended)"));
            portForwardRadioButton = new JRadioButton(I18n.tr("Manual Port Forward:"));
            doNothingRadioButton = new JRadioButton(I18n.tr("Do Nothing"));
            
            buttonGroup = new ButtonGroup();
            buttonGroup.add(plugAndPlayRadioButton);
            buttonGroup.add(portForwardRadioButton);
            buttonGroup.add(doNothingRadioButton);
            
            portTextField = new NumericTextField(5, 1, 0xFFFF);
            starLabel = new JLabel(I18n.tr("* You must also configure your router"));
            starLabel.setVisible(false);
            
            
            add(new MultiLineLabel(description, ReallyAdvancedOptionPanel.MULTI_LINE_LABEL_WIDTH), "wrap");
            
            add(plugAndPlayRadioButton, "split, wrap");
            
            add(portForwardRadioButton, "split");
            add(portTextField,"split");
            add(starLabel, "wrap");
            
            add(doNothingRadioButton, "split");
            
            portForwardRadioButton.addItemListener(new ItemListener(){
                @Override
                public void itemStateChanged(ItemEvent e) {
                    updateTextField();
                }
            });
        }
        
        @Override
        boolean applyOptions() {
            boolean restart = false;
            boolean oldUPNP = ConnectionSettings.DISABLE_UPNP.getValue();
            int oldPort = ConnectionSettings.FORCED_PORT.getValue();
            boolean oldForce = ConnectionSettings.FORCE_IP_ADDRESS.getValue();

            if(plugAndPlayRadioButton.isSelected()) {
                if(!ConnectionSettings.UPNP_IN_USE.getValue()) {
                    ConnectionSettings.FORCE_IP_ADDRESS.setValue(false);
                }
                ConnectionSettings.DISABLE_UPNP.setValue(false);
                if(oldUPNP || oldForce) {
                    restart = true;
                }
            } else if(doNothingRadioButton.isSelected()) {
                ConnectionSettings.FORCE_IP_ADDRESS.setValue(false);
                ConnectionSettings.DISABLE_UPNP.setValue(true);
            } else { // PORT.isSelected()
                int forcedPort = portTextField.getValue(oldPort);
                
                ConnectionSettings.DISABLE_UPNP.setValue(false);
                ConnectionSettings.FORCE_IP_ADDRESS.setValue(true);
                ConnectionSettings.UPNP_IN_USE.setValue(false);
                ConnectionSettings.FORCED_PORT.setValue(forcedPort);
            }
            
            // Notify that the address changed if:
            //    1) The 'forced address' status changed.
            // or 2) We're forcing and the ports are different.
            boolean newForce = ConnectionSettings.FORCE_IP_ADDRESS.getValue();
            int newPort = ConnectionSettings.FORCED_PORT.getValue();        
            if(oldForce != newForce)
                networkManager.addressChanged();
            if(newForce && (oldPort != newPort))
                networkManager.portChanged();

            return restart;
        }

        @Override
        boolean hasChanged() {
            if(ConnectionSettings.FORCE_IP_ADDRESS.getValue() && !ConnectionSettings.UPNP_IN_USE.getValue()) {
                if (!portForwardRadioButton.isSelected())
                    return true;
            }
            else if(ConnectionSettings.DISABLE_UPNP.getValue()) {
                if (!doNothingRadioButton.isSelected())
                    return true;
            } else {
                if (!plugAndPlayRadioButton.isSelected()) 
                    return true;
            }
            int forcedPortSetting = ConnectionSettings.FORCED_PORT.getValue();
            return portForwardRadioButton.isSelected() &&
                    portTextField.getValue(forcedPortSetting) != forcedPortSetting;
        }

        @Override
        public void initOptions() {
            if(ConnectionSettings.FORCE_IP_ADDRESS.getValue() && !ConnectionSettings.UPNP_IN_USE.getValue())
                portForwardRadioButton.setSelected(true);
            else if(ConnectionSettings.DISABLE_UPNP.getValue())
                doNothingRadioButton.setSelected(true);
            else
                plugAndPlayRadioButton.setSelected(true);
                      
            portTextField.setValue(ConnectionSettings.FORCED_PORT.getValue());
            
            updateTextField();
        }
        
        private void updateTextField() {
            portTextField.setEnabled(portForwardRadioButton.isSelected());
            portTextField.setEditable(portForwardRadioButton.isSelected());
            starLabel.setVisible(portForwardRadioButton.isSelected());
        }
    }
}
