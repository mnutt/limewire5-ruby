package org.limewire.ui.swing.options;

import java.awt.Dimension;
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

import org.limewire.bittorrent.TorrentManager;
import org.limewire.bittorrent.TorrentManagerSettings;
import org.limewire.bittorrent.TorrentSettingsAnnotation;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.components.NumericTextField;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * BitTorrent Option View.
 */
public class BitTorrentOptionPanel extends OptionPanel {

    private ButtonGroup buttonGroup;

    private JRadioButton uploadForever;

    private JRadioButton myControl;

    private JLabel seedRatioLabel;

    private SpinnerNumberModel seedRatioModel;

    private JSpinner seedRatio;

    private JLabel seedTimeLabel;

    private SpinnerNumberModel seedTimeModel;

    private JSpinner seedTime;

    private JLabel daysLabel;

    private NumericTextField startPortField;

    private NumericTextField endPortField;

    private JLabel portLabel;

    private JLabel portToLabel;

    private final Provider<TorrentManager> torrentManager;

    private final TorrentManagerSettings torrentSettings;
    
    private JCheckBox prioritizeTorrentPopup;

    @Inject
    public BitTorrentOptionPanel(Provider<TorrentManager> torrentManager,
            @TorrentSettingsAnnotation TorrentManagerSettings torrentSettings) {
        this.torrentManager = torrentManager;
        this.torrentSettings = torrentSettings;

        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        setOpaque(false);

        add(getBitTorrentPanel(), "pushx, growx");
    }

    private JPanel getBitTorrentPanel() {
        JPanel p = new JPanel();
        p.setBorder(BorderFactory.createTitledBorder(""));
        p.setLayout(new MigLayout("gapy 10, fill, hidemode 3"));
        p.setOpaque(false);

        uploadForever = new JRadioButton("<html>" + I18n.tr("Upload torrents forever") + "</html>");
        uploadForever.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadForever.isSelected());
            }
        });
        myControl = new JRadioButton(I18n.tr("Limit torrent upload "));
        myControl.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateState(uploadForever.isSelected());
            }
        });

        uploadForever.setOpaque(false);
        myControl.setOpaque(false);

        buttonGroup = new ButtonGroup();
        buttonGroup.add(uploadForever);
        buttonGroup.add(myControl);

        seedRatioLabel = new JLabel(I18n.tr("until ratio: "));

        seedRatioModel = new SpinnerNumberModel(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                .get().doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMinValue()
                .doubleValue(), BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.getMaxValue()
                .doubleValue(), .05);

        seedRatio = new JSpinner(seedRatioModel);
        seedRatio.setPreferredSize(new Dimension(50, 20));
        seedRatio.setMaximumSize(new Dimension(50, 20));

        seedTimeLabel = new JLabel(I18n.tr("OR uploading for: "));
        daysLabel = new JLabel(I18n.tr("days"));

        seedTimeModel = new SpinnerNumberModel(
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get()),
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMinValue()),
                getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getMaxValue()), .05);

        seedTime = new JSpinner(seedTimeModel);
        seedTime.setPreferredSize(new Dimension(50, 20));
        seedTime.setMaximumSize(new Dimension(50, 20));
        portLabel = new JLabel(I18n.tr("Use ports:"));
        portToLabel = new JLabel(I18n.tr("to"));
        startPortField = new NumericTextField(5, 1, 0xFFFF);
        endPortField = new NumericTextField(5, 1, 0xFFFF);
        
        prioritizeTorrentPopup = new JCheckBox(I18n.tr("Show torrent file dialog when starting new torrents"));
        prioritizeTorrentPopup.setOpaque(false);
        
        
        if (torrentManager.get().isValid()) {
            p.add(uploadForever, "wrap");
            p.add(myControl, "wrap");
            p.add(seedRatioLabel, "gapleft 26, split");
            p.add(seedRatio, "split");
            p.add(seedTimeLabel, "split");
            p.add(seedTime, "split");
            p.add(daysLabel, "alignx left, wrap");
            p.add(portLabel, "split");
            p.add(startPortField, "split");
            p.add(portToLabel, "split");
            p.add(endPortField, "alignx right, wrap");
            p.add(prioritizeTorrentPopup, "wrap");

        } else {
            p
                    .add(new MultiLineLabel(
                            I18n
                                    .tr("There was an error loading bittorrent. You will not be able to use bittorrent capabilities until this is resolved."),
                            500));
        }
        return p;
    }

    @Override
    boolean applyOptions() {
        BittorrentSettings.UPLOAD_TORRENTS_FOREVER.setValue(uploadForever.isSelected());
        if (!uploadForever.isSelected()) {
            BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.setValue(seedRatioModel.getNumber()
                    .floatValue());
            BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.setValue(((Double) Math
                    .ceil(((Double) seedTime.getValue()).doubleValue() * 60 * 60 * 24)).intValue());
        }

        int startPort = startPortField.getValue(BittorrentSettings.LIBTORRENT_LISTEN_START_PORT
                .getValue());

        int endPort = endPortField.getValue(BittorrentSettings.LIBTORRENT_LISTEN_END_PORT
                .getValue());

        if (startPort > endPort) {
            int temp = startPort;
            startPort = endPort;
            endPort = temp;
        }

        BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.setValue(startPort);
        BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.setValue(endPort);
        
        BittorrentSettings.SHOW_POPUP_BEFORE_DOWNLOADING.setValue(prioritizeTorrentPopup.isSelected());

        if (torrentManager.get().isValid()) {
            torrentManager.get().setTorrentManagerSettings(torrentSettings);
        }
        return false;
    }

    @Override
    boolean hasChanged() {
        return BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue() != uploadForever.isSelected()
                || ((Float) seedRatio.getValue()).floatValue() != BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT
                        .getValue()
                || ((Double) Math.ceil(((Double) seedTime.getValue()).doubleValue() * 60 * 60 * 24))
                        .intValue() != BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.getValue()
                || startPortField.getValue(BittorrentSettings.LIBTORRENT_LISTEN_START_PORT
                        .getValue()) != BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.getValue()
                || endPortField.getValue(BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.getValue()) != BittorrentSettings.LIBTORRENT_LISTEN_END_PORT
                        .getValue() 
                || prioritizeTorrentPopup.isSelected() != BittorrentSettings.SHOW_POPUP_BEFORE_DOWNLOADING.getValue();
    }

    @Override
    public void initOptions() {
        boolean auto = BittorrentSettings.UPLOAD_TORRENTS_FOREVER.getValue();
        if (auto) {
            uploadForever.setSelected(true);
        } else {
            myControl.setSelected(true);
        }

        seedRatio.setValue(BittorrentSettings.LIBTORRENT_SEED_RATIO_LIMIT.get().doubleValue());
        seedTime.setValue(getDays(BittorrentSettings.LIBTORRENT_SEED_TIME_LIMIT.get()));
        startPortField.setValue(BittorrentSettings.LIBTORRENT_LISTEN_START_PORT.getValue());
        endPortField.setValue(BittorrentSettings.LIBTORRENT_LISTEN_END_PORT.getValue());
        prioritizeTorrentPopup.setSelected(BittorrentSettings.SHOW_POPUP_BEFORE_DOWNLOADING.getValue());

        updateState(auto);
    }

    private double getDays(Integer integer) {
        return integer.doubleValue() / (60 * 60 * 24);
    }

    /**
     * Updates the state of the components based on whether the user has opted
     * to control the bittorrent settings manually, or let limewire control
     * them.
     */
    private void updateState(boolean uploadForever) {
        seedRatioLabel.setVisible(!uploadForever);
        seedRatio.setVisible(!uploadForever);
        seedTime.setVisible(!uploadForever);
        seedTimeLabel.setVisible(!uploadForever);
        daysLabel.setVisible(!uploadForever);
    }

}
