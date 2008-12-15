package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;
import java.util.Dictionary;

import javax.swing.JLabel;
import javax.swing.JSlider;

import org.limewire.core.settings.DaapSettings;

import com.limegroup.gnutella.gui.I18n;

public final class DaapBufferSizePaneItem extends AbstractPaneItem {

    public final static String TITLE = I18n.tr("Buffer Size");
    
    public final static String LABEL = I18n.tr("You can configure the buffer size to use for streaming media. If iTunes or other media players experience frequent rebuffering, increase this value. A higher setting may use more memory.");

    private final JSlider BUFFER_SIZE = new JSlider(512, 1024 * 8);

    /**
     * The constructor constructs all of the elements of this 
     * <tt>AbstractPaneItem</tt>.
     *
     * @param key the key for this <tt>AbstractPaneItem</tt> that the
     *            superclass uses to generate locale-specific keys
     */
    @SuppressWarnings("unchecked")
    public DaapBufferSizePaneItem() {
        super(TITLE, LABEL);
        
        BUFFER_SIZE.setMajorTickSpacing(512);
        BUFFER_SIZE.setMinorTickSpacing(256);
        BUFFER_SIZE.setPaintLabels(true);
        BUFFER_SIZE.setPaintTicks(true);
        BUFFER_SIZE.setSnapToTicks(true);
        Dictionary labels = BUFFER_SIZE.createStandardLabels(1024, 1024);
        labels.put(512, new JLabel("512", JLabel.CENTER));
        BUFFER_SIZE.setLabelTable(labels);
        add(BUFFER_SIZE);
    }
    
    /**
     * Defines the abstract method in <tt>AbstractPaneItem</tt>.<p>
     *
     * Sets the options for the fields in this <tt>PaneItem</tt> when the 
     * window is shown.
     */
    @Override
    public void initOptions() {
        BUFFER_SIZE.setValue(DaapSettings.DAAP_BUFFER_SIZE.getValue());
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
        DaapSettings.DAAP_BUFFER_SIZE.setValue(BUFFER_SIZE.getValue());
        return false;
    }
    
    public boolean isDirty() {
        return DaapSettings.DAAP_BUFFER_SIZE.getValue() != BUFFER_SIZE.getValue();
    }
}
