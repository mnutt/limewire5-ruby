package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;
import javax.swing.JCheckBox;
import javax.swing.JTextField;

import org.limewire.core.settings.DaapSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SizedTextField;

public final class DaapSupportPaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Sharing");
    
    public final static String LABEL = I18n.tr("You can share your Music on the Local Area Network and make it accessible for iTunes and other Digital Audio Access Protocol (DAAP) enabled Players.");
    
    private final String DAAP_ENABLED_LABEL = 
            I18nMarker.marktr("Share My Music:");
    
    private final String SERVICE_NAME_LABEL = 
            I18nMarker.marktr("Shared Name:");

    private final JCheckBox DAAP_ENABLED = new JCheckBox();

    private final JTextField SERVICE_NAME = new SizedTextField(25, SizePolicy.RESTRICT_HEIGHT);

    /**
     * The constructor constructs all of the elements of this 
     * <tt>AbstractPaneItem</tt>.
     *
     * @param key the key for this <tt>AbstractPaneItem</tt> that the
     *            superclass uses to generate locale-specific keys
     */
    public DaapSupportPaneItem() {
        super(TITLE, LABEL);
        
        LabeledComponent comp = new LabeledComponent(DAAP_ENABLED_LABEL, DAAP_ENABLED,
        		LabeledComponent.LEFT_GLUE, LabeledComponent.LEFT);
        add(comp.getComponent());

        add(getVerticalSeparator());
        
        comp = new LabeledComponent(SERVICE_NAME_LABEL, SERVICE_NAME);
        
        add(comp.getComponent());
    }
    
    /**
     * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
     *
     * Sets the options for the fields in this <tt>PaneItem</tt> when the 
     * window is shown.
     */
    @Override
    public void initOptions() {
        DAAP_ENABLED.setSelected(DaapSettings.DAAP_ENABLED.getValue() && 
                    GuiCoreMediator.getDaapManager().isServerRunning());
        
        SERVICE_NAME.setText(DaapSettings.DAAP_SERVICE_NAME.getValue());
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

        final boolean prevEnabled = DaapSettings.DAAP_ENABLED.getValue();
        final String prevServiceName = DaapSettings.DAAP_SERVICE_NAME.getValue();

        String serviceName = SERVICE_NAME.getText().trim();

        if (serviceName.length()==0 && DAAP_ENABLED.isSelected()) { 
            throw new IOException(); 
        }

        DaapSettings.DAAP_ENABLED.setValue(DAAP_ENABLED.isSelected());
        DaapSettings.DAAP_SERVICE_NAME.setValue(serviceName);
        DaapSettings.DAAP_LIBRARY_NAME.setValue(serviceName);

        try {
            
            if (DAAP_ENABLED.isSelected()) {
                
                if (!prevEnabled) {
                    GuiCoreMediator.getDaapManager().restart();
                   
                } else if (!serviceName.equals(prevServiceName)) {
                    GuiCoreMediator.getDaapManager().updateService();
                }
                    
            } else if (prevEnabled) {
                
                GuiCoreMediator.getDaapManager().stop();
            }

        } catch (IOException err) {

            DaapSettings.DAAP_ENABLED.setValue(prevEnabled);
            DaapSettings.DAAP_SERVICE_NAME.setValue(prevServiceName);
            DaapSettings.DAAP_LIBRARY_NAME.setValue(prevServiceName);

            GuiCoreMediator.getDaapManager().stop();

            initOptions();

            throw err;
        }

        return false;
    }
    
    public boolean isDirty() {
        return DaapSettings.DAAP_ENABLED.getValue() != DAAP_ENABLED.isSelected() ||
               !DaapSettings.DAAP_SERVICE_NAME.getValue().equals(SERVICE_NAME.getText()) ||
               !DaapSettings.DAAP_LIBRARY_NAME.getValue().equals(SERVICE_NAME.getText());
    }
}
