package com.limegroup.gnutella.gui.options.panes;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.limewire.core.settings.SearchSettings;

import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;

/**
 * This class gives the user the option of whether or not to enable LimeWire's
 * internal spam filter
 */
public final class SpamFilterSensivityPaneItem extends AbstractPaneItem {
    
    public final static String TITLE = I18n.tr("Sensitivity");
    
    public final static String LABEL = I18n.tr("Adjust the sensitivity of LimeWire\'s junk filter");

    /** The spam threshold slider */
    private JSlider THRESHOLD = new JSlider(0, 50);

    /** Reset the spam filter */
    private JButton RESET = new JButton();
    
    public SpamFilterSensivityPaneItem() {
        super(TITLE, LABEL);

        RESET.setText(I18n.tr("Forget Training Data"));
        RESET.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                GuiCoreMediator.getSpamManager().clearFilterData();
            }
        });

        Dictionary<Integer, JLabel> labels = new Hashtable<Integer, JLabel>();
        labels.put(0, new JLabel(I18n.tr("Relaxed"), JLabel.CENTER));
        labels.put(50, new JLabel(I18n.tr("Strict"), JLabel.CENTER));
        
        THRESHOLD.setLabelTable(labels);
        THRESHOLD.setPaintLabels(true);
        add(THRESHOLD);

        add(getVerticalSeparator());
        add(getVerticalSeparator());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(RESET);
        GUIUtils.restrictSize(buttonPanel, SizePolicy.RESTRICT_HEIGHT);
        add(buttonPanel);
    }

    @Override
    public void initOptions() {
        float threshold = SearchSettings.FILTER_SPAM_RESULTS.getValue();
        THRESHOLD.setValue((int) (100 - 100 * threshold));
    }

    @Override
    public boolean applyOptions() throws IOException {
        float threshold = (100 - THRESHOLD.getValue()) / 100f;
        SearchSettings.FILTER_SPAM_RESULTS.setValue(threshold);
        return false;
    }

    public boolean isDirty() {
        float threshold = (100 - THRESHOLD.getValue()) / 100f;
        return SearchSettings.FILTER_SPAM_RESULTS.getValue() != threshold;
    }
}
