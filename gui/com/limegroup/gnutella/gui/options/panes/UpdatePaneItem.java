package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.limewire.core.api.updates.UpdateStyle;
import org.limewire.core.settings.UpdateSettings;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.I18n;

/** Update options */
public class UpdatePaneItem extends AbstractPaneItem { 
    
    public final static String TITLE = I18n.tr("Update Options");
    
    public final static String LABEL = I18n.tr("You can choose when you want to be notified of new LimeWire releases.");

    /** button for wanting betas */
    private JRadioButton beta;
    
    /** button for wanting service releases */
    private JRadioButton service;
    
    /** button for wanting major releases */
    private JRadioButton major;

    /** Creates the UpdatePaneItem */
    public UpdatePaneItem() {
        super(TITLE, LABEL);
        
        beta = new JRadioButton(I18n.tr("Beta Releases"));
        service = new JRadioButton(I18n.tr("Service Releases"));
        major = new JRadioButton(I18n.tr("Major Releases"));
        
        ButtonGroup bg = new ButtonGroup();
        bg.add(beta);
        bg.add(service);
        bg.add(major);


        JPanel panel = new BoxPanel();
        panel.add(major);
        panel.add(service);
        panel.add(beta);
        
        JPanel outer = new BoxPanel(BoxPanel.X_AXIS);
        outer.add(panel);
        outer.add(Box.createHorizontalGlue());
        
        add(outer);
    }

    /**
     * Applies the options currently set in this <tt>PaneItem</tt>.
     *
     * @throws IOException if the options could not be fully applied
     */
    @Override
    public boolean applyOptions() throws IOException {
        if(beta.isSelected())
            UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_BETA);
        else if(service.isSelected())
            UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_MINOR);
        else // if beta.isSelected())
            UpdateSettings.UPDATE_STYLE.setValue(UpdateStyle.STYLE_MAJOR);
        return false;
    }
    
    /**
     * Sets the options for the fields in this <tt>PaneItem</tt> when the
     * window is shown.
     */
    @Override
    public void initOptions() {
        int style = UpdateSettings.UPDATE_STYLE.getValue();
        if(style <= UpdateStyle.STYLE_BETA)
            beta.setSelected(true);
        else if(style == UpdateStyle.STYLE_MINOR)
            service.setSelected(true);
        else // if style >= UpdateInformation.STYLE_MAJOR
            major.setSelected(true);
    }
    
    public boolean isDirty() {
        int style = UpdateSettings.UPDATE_STYLE.getValue();
        if(style <= UpdateStyle.STYLE_BETA)
            return !beta.isSelected();
        else if(style == UpdateStyle.STYLE_MINOR)
            return !service.isSelected();
        else // if style >= UpdateInformation.STYLE_MAJOR
            return !major.isSelected();
    }
}
