package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;


/**
 * BitTorrent Option View
 */
public class BitTorrentOptionPanel extends OptionPanel {
    
    private static final int MIN = 1;
    private static final int MAX = 10;
    
    private ButtonGroup buttonGroup;
    
    private JRadioButton limewireControl;
    private JRadioButton myControl;
    
    private JSpinner maxUploadSpinner;
    private JSpinner minUploadSpinner;
    private JCheckBox safeChunkCheckBox;
    private JCheckBox experimentCheckBox;
    
    private JLabel maxUploadLabel;
    private JLabel minUploadLabel;
    
    @Inject
    public BitTorrentOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        add(getBitTorrentPanel(), "pushx, growx");
    }

    private JPanel getBitTorrentPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10"));
        
        limewireControl = new JRadioButton(I18n.tr("Let LimeWire manage my BitTorrent settings (Recommended)"));
        limewireControl.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(!limewireControl.isSelected());
            }
        });
        myControl = new JRadioButton(I18n.tr("Let me manage my BitTorrent settings"));
        myControl.addItemListener(new ItemListener(){
            @Override
            public void itemStateChanged(ItemEvent e) {
                maxUploadSpinner.setVisible(myControl.isSelected());
                minUploadSpinner.setVisible(myControl.isSelected());
                safeChunkCheckBox.setVisible(myControl.isSelected());
                experimentCheckBox.setVisible(myControl.isSelected());
                
                maxUploadLabel.setVisible(myControl.isSelected());
                minUploadLabel.setVisible(myControl.isSelected());
            }
        });
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(limewireControl);
        buttonGroup.add(myControl);
        
        maxUploadSpinner = new JSpinner(new SpinnerNumberModel(MIN, MIN, MAX, 1));
        maxUploadSpinner.setVisible(false);
        minUploadSpinner = new JSpinner(new SpinnerNumberModel(MIN, MIN, MAX, 1));
        minUploadSpinner.setVisible(false);
        safeChunkCheckBox = new JCheckBox(I18n.tr("Safe chunk verification"));
        safeChunkCheckBox.setVisible(false);
        experimentCheckBox = new JCheckBox(I18n.tr("Experimental disk access"));
        experimentCheckBox.setVisible(false);
        
        maxUploadLabel = new JLabel(I18n.tr("Max uploads per torrent"));
        maxUploadLabel.setVisible(false);
        minUploadLabel = new JLabel(I18n.tr("Min uploads per torrent"));
        minUploadLabel.setVisible(false);

        p.add(limewireControl, "wrap");        
        p.add(myControl, "wrap");        


        p.add(maxUploadLabel, "gapleft 35, split");
        p.add(maxUploadSpinner, "wrap");
        p.add(minUploadLabel, "gapleft 35, split");
        p.add(minUploadSpinner, "wrap");
        p.add(safeChunkCheckBox, "gapleft 35, split, wrap");
        p.add(experimentCheckBox, "gapleft 35, split, wrap");
        
        return p;
    }
    
    @Override
    boolean applyOptions() {
        SwingUiSettings.AUTOMATIC_SETTINGS.setValue(limewireControl.isSelected());
        BittorrentSettings.TORRENT_MAX_UPLOADS.setValue((Integer)maxUploadSpinner.getModel().getValue());
        BittorrentSettings.TORRENT_MIN_UPLOADS.setValue((Integer)minUploadSpinner.getModel().getValue());
        BittorrentSettings.TORRENT_FLUSH_VERIRY.setValue(safeChunkCheckBox.isSelected());
        BittorrentSettings.TORRENT_USE_MMAP.setValue(experimentCheckBox.isSelected());
        return false;
    }
    
    @Override
    boolean hasChanged() {
        return SwingUiSettings.AUTOMATIC_SETTINGS.getValue() != limewireControl.isSelected() 
                || (Integer)maxUploadSpinner.getModel().getValue() != BittorrentSettings.TORRENT_MAX_UPLOADS.getValue()
                || (Integer)minUploadSpinner.getModel().getValue()!= BittorrentSettings.TORRENT_MIN_UPLOADS.getValue()
                || BittorrentSettings.TORRENT_FLUSH_VERIRY.getValue() != safeChunkCheckBox.isSelected()
                || BittorrentSettings.TORRENT_USE_MMAP.getValue() != experimentCheckBox.isSelected();
    }
    
    @Override
    public void initOptions() {
        boolean auto = SwingUiSettings.AUTOMATIC_SETTINGS.getValue();
        if(auto)
            limewireControl.setSelected(true);
        else
            myControl.setSelected(true);
        
        maxUploadSpinner.getModel().setValue(BittorrentSettings.TORRENT_MAX_UPLOADS.getValue());
        minUploadSpinner.getModel().setValue(BittorrentSettings.TORRENT_MIN_UPLOADS.getValue());
        safeChunkCheckBox.setSelected(BittorrentSettings.TORRENT_FLUSH_VERIRY.getValue());
        experimentCheckBox.setSelected(BittorrentSettings.TORRENT_USE_MMAP.getValue());
        
        updateState(!auto);
    }
    
    private void updateState(boolean value) {
        maxUploadSpinner.setEnabled(value);
        minUploadSpinner.setEnabled(value);
        safeChunkCheckBox.setEnabled(value);
        experimentCheckBox.setEnabled(value);
    }
}
