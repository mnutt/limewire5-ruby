package com.limegroup.gnutella.gui.options.panes;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import org.limewire.core.settings.ConnectionSettings;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.I18n;

/** Allows the user to pick a custom interface/address to bind to. */
public class NetworkInterfacePaneItem extends AbstractPaneItem {
    
    public final static String TITLE = I18n.tr("Network Interface");
    
    public final static String LABEL = I18n.tr("You can tell LimeWire to bind outgoing connections to an IP address from a specific network interface. Listening sockets will still listen on all available interfaces. This is useful on multi-homed hosts. If you later disable this interface, LimeWire will revert to binding to an arbitrary address.");

    private static final String ADDRESS = "limewire.networkinterfacepane.address";
    
    private final ButtonGroup GROUP = new ButtonGroup();
    
    private final JCheckBox CUSTOM;

	private List<JRadioButton> activeButtons = new ArrayList<JRadioButton>();
    
    public NetworkInterfacePaneItem() {
        super(TITLE, LABEL);
        
        CUSTOM = new JCheckBox(I18n.tr("Use a specific network interface."));
        CUSTOM.setSelected(ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue());
        CUSTOM.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				updateButtons(CUSTOM.isSelected());
			}        	
        });
        add(CUSTOM);
        
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.fill = GridBagConstraints.NONE;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            
            // Add the available interfaces / addresses
            while(interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                JLabel label = new JLabel(ni.getDisplayName());
                gbc.insets = new Insets(5, 0, 2, 0);
                panel.add(label, gbc);
                
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                gbc.insets = new Insets(0, 6, 0, 0);
                while(addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    JRadioButton button = new JRadioButton(address.getHostAddress());
                    GROUP.add(button);
                    if(address.isAnyLocalAddress() || address.isLinkLocalAddress() || address.isLoopbackAddress()) {
                        button.setEnabled(false);
                    } else {
                        activeButtons.add(button);
                    }
                    if(ConnectionSettings.CUSTOM_INETADRESS.getValue().equals(address.getHostAddress()))
                        button.setSelected(true);
                    button.putClientProperty(ADDRESS, address);
                    panel.add(button, gbc);
                }
            }

            initializeSelection();
            
            gbc.weightx = 1;
            gbc.weighty = 1;
            gbc.fill = GridBagConstraints.BOTH;
            gbc.insets = new Insets(0, 0, 0, 0);
            gbc.gridheight = GridBagConstraints.REMAINDER;
            panel.add(Box.createGlue(), gbc);
            //GUIUtils.restrictSize(panel, SizePolicy.RESTRICT_HEIGHT);
            JScrollPane pane = new JScrollPane(panel);
            pane.setBorder(BorderFactory.createEmptyBorder());
            add(pane);
            
            // initialize
            updateButtons(CUSTOM.isSelected());
        } catch(SocketException se) {
            CUSTOM.setSelected(false);
            JPanel labelPanel = new BoxPanel(BoxPanel.X_AXIS);
            labelPanel.add(new JLabel(I18n.tr("LimeWire was unable to determine which network interfaces are available on this machine. Outgoing connections will bind to any arbitrary interface.")));
            labelPanel.add(Box.createHorizontalGlue());
            JPanel outerPanel = new BoxPanel();
            outerPanel.add(labelPanel);
            outerPanel.add(Box.createVerticalGlue());
            add(outerPanel);
        }
    }
    
    protected void updateButtons(boolean enable) {
    	for (JRadioButton button : activeButtons) {
    		button.setEnabled(enable);
		}
    }

	private void initializeSelection() {
        // Make sure one item is selected always.
        Enumeration<AbstractButton> buttons = GROUP.getElements();   
        while(buttons.hasMoreElements()) {
        	AbstractButton bt = buttons.nextElement();
        	if(bt.isSelected())
        		return;
        }
        // Select the first one if nothing's selected.
        buttons = GROUP.getElements();
        while(buttons.hasMoreElements()) {
        	AbstractButton bt = buttons.nextElement();
        	if(bt.isEnabled()) {
        		bt.setSelected(true);
        		return;
        	}
        }
	}

	/**
     * Applies the options currently set in this <tt>PaneItem</tt>.
     *
     * @throws IOException if the options could not be fully applied
     */
    @Override
    public boolean applyOptions() throws IOException {
        ConnectionSettings.CUSTOM_NETWORK_INTERFACE.setValue(CUSTOM.isSelected());
        Enumeration<AbstractButton> buttons = GROUP.getElements();
        while(buttons.hasMoreElements()) {
            AbstractButton bt = buttons.nextElement();
            if(bt.isSelected()) {
                InetAddress addr = (InetAddress)bt.getClientProperty(ADDRESS);
                ConnectionSettings.CUSTOM_INETADRESS.setValue(addr.getHostAddress());
            }
        }
        
        return false;
    }
    
    public boolean isDirty() {
        if(!ConnectionSettings.CUSTOM_NETWORK_INTERFACE.getValue())
            return CUSTOM.isSelected();
        
        String expect = ConnectionSettings.CUSTOM_INETADRESS.getValue();
        Enumeration<AbstractButton> buttons = GROUP.getElements();
        while(buttons.hasMoreElements()) {
            AbstractButton bt = buttons.nextElement();
            if(bt.isSelected()) {
                InetAddress addr = (InetAddress)bt.getClientProperty(ADDRESS);
                if(addr.getHostAddress().equals(expect))
                    return false;
            }
        }
        
        return true;
    }

    /**
     * Sets the options for the fields in this <tt>PaneItem</tt> when the
     * window is shown.
     */
    @Override
    public void initOptions() {
    }
}    
