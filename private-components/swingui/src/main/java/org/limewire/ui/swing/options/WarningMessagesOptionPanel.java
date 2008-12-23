package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;
import org.limewire.core.settings.QuestionsHandler;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.search.resultpanel.LicenseWarningDownloadPreprocessor;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.VersionUtils;

public class WarningMessagesOptionPanel extends OptionPanel {

    // backwards compatibility
    private static final int SKIP_WARNING_VALUE = LicenseWarningDownloadPreprocessor.SKIP_WARNING_VALUE;
    private static final int SHOW_WARNING_VALUE = LicenseWarningDownloadPreprocessor.SHOW_WARNING_VALUE;

    private JCheckBox licensedMaterialCheckBox;
    private JCheckBox cancelTorrentCheckBox;
    private JCheckBox oldJavaCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    
    public WarningMessagesOptionPanel(Action okAction, CancelDialogAction cancelAction) {
        setLayout(new MigLayout("gapy 0"));
        
        cancelAction.setOptionPanel(this);
       
        licensedMaterialCheckBox = new JCheckBox(I18n.tr("Warn me when downloading a file without a license"));
        licensedMaterialCheckBox.setContentAreaFilled(false);
        
        cancelTorrentCheckBox = new JCheckBox(I18n.tr("Warn me when I cancel uploading a torrent that has not completely seeded"));
        cancelTorrentCheckBox.setContentAreaFilled(false);
        
        oldJavaCheckBox = new JCheckBox(I18n.tr("Warn me when using an older version of Java"));
        oldJavaCheckBox.setContentAreaFilled(false);
        
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);
        
        add(new JLabel(I18n.tr("Choose which warning messages you'd like to see:")), "span 2, gapbottom 5, gapright 40,  wrap");
        
        add(licensedMaterialCheckBox, "gapleft 25, gapright 40, split, wrap");
        add(cancelTorrentCheckBox, "gapleft 25, gapright 40, split, wrap");
        add(oldJavaCheckBox, "gapleft 25, gapbottom 25, gapright 40, split, wrap");
        
        add(okButton, "split 2, span 2, alignx right");
        add(cancelButton);
    }
    
    @Override
    boolean applyOptions() {
        int skipWarningSettingValue = getLicenseSettingValueFromCheckboxValue(licensedMaterialCheckBox.isSelected());
        QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.setValue(skipWarningSettingValue);
        QuestionsHandler.WARN_TORRENT_SEED_MORE.setValue(cancelTorrentCheckBox.isSelected());
        QuestionsHandler.LAST_CHECKED_JAVA_VERSION.setValue(getJavaVersion(oldJavaCheckBox.isSelected()));
        return false;
    }

    @Override
    boolean hasChanged() {
        return QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.getValue() ==
                getLicenseSettingValueFromCheckboxValue(licensedMaterialCheckBox.isSelected()) ||
                QuestionsHandler.WARN_TORRENT_SEED_MORE.getValue() == cancelTorrentCheckBox.isSelected() ||
                QuestionsHandler.LAST_CHECKED_JAVA_VERSION.getValue().equals(getJavaVersion(oldJavaCheckBox.isSelected()));
    }

    private int getLicenseSettingValueFromCheckboxValue(boolean isSelected) {
        return isSelected ? SHOW_WARNING_VALUE : SKIP_WARNING_VALUE;
    }
    
    private String getJavaVersion(boolean isSelected) {
        return isSelected ? "" : VersionUtils.getJavaVersion();
    }

    @Override
    public void initOptions() {
        boolean warnWhenDownloadingWithNoLicense = QuestionsHandler.SKIP_FIRST_DOWNLOAD_WARNING.getValue() != SKIP_WARNING_VALUE;
        licensedMaterialCheckBox.setSelected(warnWhenDownloadingWithNoLicense);
   
        boolean warnCancelTorrentUpload = QuestionsHandler.WARN_TORRENT_SEED_MORE.getValue();
        cancelTorrentCheckBox.setSelected(warnCancelTorrentUpload);
        
        boolean warnOldJava = QuestionsHandler.LAST_CHECKED_JAVA_VERSION.getValue().equals("");
        oldJavaCheckBox.setSelected(warnOldJava);
    }
}
