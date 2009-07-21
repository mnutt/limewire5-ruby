package org.limewire.ui.swing.options;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.core.settings.SharingSettings;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.core.settings.UploadSettings;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Connections Option View.
 */
public class ConnectionsOptionPanel extends OptionPanel {

    private ConnectionSpeedPanel connectionSpeedPanel;
    private DownloadsPanel downloadsPanel;
    private UploadsPanel uploadPanel;
    
    @Inject
    public ConnectionsOptionPanel() {
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        
        setOpaque(false);
        
        add(getDownloadsPanel(), "pushx, growx");
        add(getUploadPanel(), "pushx, growx");
        add(getConnectionSpeedPanel(), "pushx, growx");
    }
    
    private OptionPanel getConnectionSpeedPanel() {
        if(connectionSpeedPanel == null) {
            connectionSpeedPanel = new ConnectionSpeedPanel();
        }
        return connectionSpeedPanel;
    }
    
    private OptionPanel getDownloadsPanel() {
        if(downloadsPanel == null) {
            downloadsPanel = new DownloadsPanel();
        }
        return downloadsPanel;
    }
    
    private OptionPanel getUploadPanel() {
        if(uploadPanel == null) {
            uploadPanel = new UploadsPanel();
        }
        return uploadPanel;
    }

    @Override
    boolean applyOptions() {
        boolean restart = getConnectionSpeedPanel().applyOptions();
        restart |= getDownloadsPanel().applyOptions();
        restart |= getUploadPanel().applyOptions();

        return restart;
    }

    @Override
    boolean hasChanged() {
        return getConnectionSpeedPanel().hasChanged() ||
                getDownloadsPanel().hasChanged() ||
                getUploadPanel().hasChanged();
    }

    @Override
    public void initOptions() {
        getConnectionSpeedPanel().initOptions();
        getDownloadsPanel().initOptions();
        getUploadPanel().initOptions();
    }
    
    private class ConnectionSpeedPanel extends OptionPanel {

        private ButtonGroup buttonGroup;
        
        private JRadioButton broadBandButton;
        private JRadioButton dialupButton;
        
        public ConnectionSpeedPanel() {
            super(I18n.tr("Connection Speed"));
            
            buttonGroup = new ButtonGroup();
            
            broadBandButton = new JRadioButton(I18n.tr("Broadband"));
            broadBandButton.setContentAreaFilled(false);
            dialupButton = new JRadioButton(I18n.tr("Dial-up"));
            dialupButton.setContentAreaFilled(false);
            
            buttonGroup.add(broadBandButton);
            buttonGroup.add(dialupButton);
            
            add(new JLabel(I18n.tr("Set your connection speed")), "push");
            add(broadBandButton);
            add(dialupButton);
        }
        
        @Override
        boolean applyOptions() {
            ConnectionSettings.CONNECTION_SPEED.setValue(getSpeed(broadBandButton.isSelected()));
            return false;
        }

        @Override
        boolean hasChanged() {
            return ConnectionSettings.CONNECTION_SPEED.getValue() != getSpeed(broadBandButton.isSelected());
        }

        @Override
        public void initOptions() {
            if(ConnectionSettings.CONNECTION_SPEED.getValue() == SpeedConstants.MODEM_SPEED_INT)
                dialupButton.setSelected(true);
            else
                broadBandButton.setSelected(true);
        }
        
        private int getSpeed(boolean isBroadband) {
            if(isBroadband) {
                return SpeedConstants.CABLE_SPEED_INT;
            } else {
                return SpeedConstants.MODEM_SPEED_INT;
            }
        }
    }
    
    private class DownloadsPanel extends OptionPanel {

        private static final int MIN_DOWNLOADS = 1;
        private static final int MAX_DOWNLOADS = 999;
        
        private JSpinner maxDownloadSpinner;
        private JCheckBox limitBandWidthCheckBox;
        private BandWidthSlider bandWidthSlider;
        
        public DownloadsPanel() {
            super(I18n.tr("Downloads"));
            
            maxDownloadSpinner = new JSpinner(new SpinnerNumberModel(MIN_DOWNLOADS, MIN_DOWNLOADS, MAX_DOWNLOADS, 1));
            limitBandWidthCheckBox = new JCheckBox(I18n.tr("Limit your download bandwidth"));
            limitBandWidthCheckBox.setContentAreaFilled(false);

            bandWidthSlider = new BandWidthSlider();
            bandWidthSlider.setVisible(false);
            limitBandWidthCheckBox.addItemListener(new CheckBoxListener(bandWidthSlider, limitBandWidthCheckBox));
            
            add(new JLabel(I18n.tr("Don't allow more than")), "split");
            add(maxDownloadSpinner, "split");
            add(new JLabel(I18n.tr("downloads at once")), "wrap");
            
            add(limitBandWidthCheckBox, "aligny 50%, split");
            add(bandWidthSlider, "aligny 50%, wrap");
        }
        
        @Override
        boolean applyOptions() {
            DownloadSettings.MAX_SIM_DOWNLOAD.setValue((Integer) maxDownloadSpinner.getModel().getValue());
            if(limitBandWidthCheckBox.isSelected()) {
                DownloadSettings.DOWNLOAD_SPEED.setValue(bandWidthSlider.getValue());
            } else {
                DownloadSettings.DOWNLOAD_SPEED.setValue(BandWidthSlider.MAX_SLIDER);
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return DownloadSettings.MAX_SIM_DOWNLOAD.getValue() != (Integer) maxDownloadSpinner.getModel().getValue()
                    || DownloadSettings.DOWNLOAD_SPEED.getValue() != bandWidthSlider.getValue();
        }

        @Override
        public void initOptions() {
            bandWidthSlider.setValue(DownloadSettings.DOWNLOAD_SPEED.getValue());
            if(DownloadSettings.DOWNLOAD_SPEED.getValue() == 100)
                limitBandWidthCheckBox.setSelected(false);
            else
                limitBandWidthCheckBox.setSelected(true);
            maxDownloadSpinner.getModel().setValue(DownloadSettings.MAX_SIM_DOWNLOAD.getValue());
        }
    }
    
    private class UploadsPanel extends OptionPanel {

        private static final int MIN_UPLOADS = 0;
        private static final int MAX_UPLOADS = 50;
        
        private JSpinner maxUploadSpinner;
        private JCheckBox uploadBandwidthCheckBox;
        private JCheckBox clearUploadCheckBox;
        private BandWidthSlider bandWidthSlider;
        
        public UploadsPanel() {
            super(I18n.tr("Uploads"));

            maxUploadSpinner = new JSpinner(new SpinnerNumberModel(MIN_UPLOADS, MIN_UPLOADS, MAX_UPLOADS, 1));
            uploadBandwidthCheckBox = new JCheckBox(I18n.tr("Limit your upload bandwidth"));
            uploadBandwidthCheckBox.setContentAreaFilled(false);
            clearUploadCheckBox = new JCheckBox(I18n.tr("Clear uploads from list when finished"));
            clearUploadCheckBox.setContentAreaFilled(false);
            
            bandWidthSlider = new BandWidthSlider();
            bandWidthSlider.setVisible(false);
            uploadBandwidthCheckBox.addItemListener(new CheckBoxListener(bandWidthSlider, uploadBandwidthCheckBox));
            
            add(new JLabel(I18n.tr("Don't allow more than")), "split");
            add(maxUploadSpinner, "split");
            add(new JLabel(I18n.tr("uploads at once")), "wrap");
            
            add(uploadBandwidthCheckBox, "aligny 50%, split");
            add(bandWidthSlider, "aligny 50%, wrap");
            
            add(clearUploadCheckBox, "split");
        }
        
        @Override
        boolean applyOptions() {
            UploadSettings.HARD_MAX_UPLOADS.setValue((Integer)maxUploadSpinner.getModel().getValue());
            SharingSettings.CLEAR_UPLOAD.setValue(clearUploadCheckBox.isSelected());
            
            if(uploadBandwidthCheckBox.isSelected()) {
                UploadSettings.UPLOAD_SPEED.setValue(bandWidthSlider.getValue());
            } else {
                UploadSettings.UPLOAD_SPEED.setValue(BandWidthSlider.MAX_SLIDER);
            }
            return false;
        }

        @Override
        boolean hasChanged() {
            return  UploadSettings.HARD_MAX_UPLOADS.getValue() != (Integer) maxUploadSpinner.getModel().getValue()
                    || UploadSettings.UPLOAD_SPEED.getValue() != bandWidthSlider.getValue()
                    || SharingSettings.CLEAR_UPLOAD.getValue() != clearUploadCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            bandWidthSlider.setValue(UploadSettings.UPLOAD_SPEED.getValue());
            maxUploadSpinner.getModel().setValue(UploadSettings.HARD_MAX_UPLOADS.getValue());
            clearUploadCheckBox.setSelected(SharingSettings.CLEAR_UPLOAD.getValue());
            
            if(UploadSettings.UPLOAD_SPEED.getValue() == 100)
                uploadBandwidthCheckBox.setSelected(false);
            else
                uploadBandwidthCheckBox.setSelected(true);
        }
    }
    
    private class CheckBoxListener implements ItemListener {

        private BandWidthSlider slider;
        private JCheckBox checkBox;
        
        public CheckBoxListener(BandWidthSlider slider, JCheckBox checkBox) {
            this.slider = slider;
            this.checkBox = checkBox;
        }
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            slider.setVisible(checkBox.isSelected());
        }
    }
}
