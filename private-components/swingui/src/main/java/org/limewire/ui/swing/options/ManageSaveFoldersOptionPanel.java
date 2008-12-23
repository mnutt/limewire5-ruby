package org.limewire.ui.swing.options;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.settings.SharingSettings;
import org.limewire.setting.FileSetting;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LabelTextField;
import org.limewire.ui.swing.options.actions.BrowseDirectoryAction;
import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.MediaType;
import org.limewire.util.Objects;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class ManageSaveFoldersOptionPanel extends OptionPanel {
    
    private LabelTextField audioTextField;

    private LabelTextField videoTextField;

    private LabelTextField imageTextField;

    private LabelTextField documentTextField;

    private LabelTextField programTextField;

    private LabelTextField otherTextField;

    private JButton audioBrowseButton;

    private JButton videoBrowseButton;

    private JButton imageBrowseButton;

    private JButton documentBrowseButton;

    private JButton programBrowseButton;

    private JButton otherBrowseButton;

    private JButton defaultButton;

    private JButton okButton;

    private JButton cancelButton;

    @AssistedInject
    public ManageSaveFoldersOptionPanel(CategoryIconManager categoryIconManager,
            @Assisted Action okAction, @Assisted CancelDialogAction cancelAction, IconManager iconManager) {

        GuiUtils.assignResources(this);

        setLayout(new MigLayout("gapy 10"));

        audioTextField = new LabelTextField(iconManager);
        videoTextField = new LabelTextField(iconManager);
        imageTextField = new LabelTextField(iconManager);
        documentTextField = new LabelTextField(iconManager);
        programTextField = new LabelTextField(iconManager);
        otherTextField = new LabelTextField(iconManager);

        audioBrowseButton = new JButton(new BrowseDirectoryAction(this, audioTextField));
        audioTextField.addMouseListener(audioBrowseButton.getAction());
        videoBrowseButton = new JButton(new BrowseDirectoryAction(this, videoTextField));
        videoTextField.addMouseListener(videoBrowseButton.getAction());
        imageBrowseButton = new JButton(new BrowseDirectoryAction(this, imageTextField));
        imageTextField.addMouseListener(imageBrowseButton.getAction());
        documentBrowseButton = new JButton(new BrowseDirectoryAction(this, documentTextField));
        documentTextField.addMouseListener(documentBrowseButton.getAction());
        programBrowseButton = new JButton(new BrowseDirectoryAction(this, programTextField));
        programTextField.addMouseListener(programBrowseButton.getAction());
        otherBrowseButton = new JButton(new BrowseDirectoryAction(this, otherTextField));
        otherTextField.addMouseListener(otherBrowseButton.getAction());

        cancelAction.setOptionPanel(this);

        defaultButton = new JButton(new DefaultAction());
        okButton = new JButton(okAction);
        cancelButton = new JButton(cancelAction);

        add(new JLabel(I18n.tr("Choose where specific file types get saved")), "span, wrap");

        add(new JLabel(I18n.tr("Audio"), categoryIconManager.getIcon(Category.AUDIO),
                SwingConstants.RIGHT), "wrap");

        add(audioTextField, "gapleft 25, gap unrelated");
        add(audioBrowseButton, "alignx right, wrap");

        add(new JLabel(I18n.tr("Video"), categoryIconManager.getIcon(Category.VIDEO),
                SwingConstants.RIGHT), "wrap");

        add(videoTextField, "gapleft 25, gap unrelated");
        add(videoBrowseButton, "alignx right, wrap");

        add(new JLabel(I18n.tr("Images"), categoryIconManager.getIcon(Category.IMAGE),
                SwingConstants.RIGHT), "wrap");

        add(imageTextField, "gapleft 25, gap unrelated");
        add(imageBrowseButton, "alignx right, wrap");

        add(new JLabel(I18n.tr("Documents"), categoryIconManager.getIcon(Category.DOCUMENT),
                SwingConstants.RIGHT), "wrap");

        add(documentTextField, "gapleft 25, gap unrelated");
        add(documentBrowseButton, "alignx right, wrap");

        add(new JLabel(I18n.tr("Programs"), categoryIconManager.getIcon(Category.PROGRAM),
                SwingConstants.RIGHT), "wrap");

        add(programTextField, "gapleft 25, gap unrelated");
        add(programBrowseButton, "alignx right, wrap");

        add(new JLabel(I18n.tr("Other"), categoryIconManager.getIcon(Category.OTHER),
                SwingConstants.RIGHT), "wrap");

        add(otherTextField, "gapleft 25, gap unrelated");
        add(otherBrowseButton, "alignx right, wrap");

        add(defaultButton, "gaptop 10, push");
        add(okButton, "split 2, alignx right, gaptop 10");
        add(cancelButton, "alignx right, gaptop 10");

    }

    @Override
    boolean applyOptions() {
        applyOption(MediaType.getAudioMediaType(), audioTextField);
        applyOption(MediaType.getVideoMediaType(), videoTextField);
        applyOption(MediaType.getImageMediaType(), imageTextField);
        applyOption(MediaType.getDocumentMediaType(), documentTextField);
        applyOption(MediaType.getProgramMediaType(), programTextField);
        applyOption(MediaType.getOtherMediaType(), otherTextField);
        return false;
    }
    
    private void revertToDefault(LabelTextField textField) {
        textField.setText(SharingSettings.getSaveDirectory().getAbsolutePath());
    }

    private void applyOption(MediaType mediaType, LabelTextField textField) {
        if(hasChanged(mediaType, textField)) {
            FileSetting saveDirSetting = SharingSettings.getFileSettingForMediaType(mediaType);
            String newSaveDirString = textField.getText();
            File newSaveDir = new File(newSaveDirString);
            saveDirSetting.setValue(newSaveDir);
         }
    }
    
    @Override
    boolean hasChanged() {
        return hasChanged(MediaType.getAudioMediaType(), audioTextField)
                || hasChanged(MediaType.getVideoMediaType(), videoTextField)
                || hasChanged(MediaType.getImageMediaType(), imageTextField)
                || hasChanged(MediaType.getDocumentMediaType(), documentTextField)
                || hasChanged(MediaType.getProgramMediaType(), programTextField)
                || hasChanged(MediaType.getOtherMediaType(), otherTextField);
    }

    private boolean hasChanged(MediaType mediaType, LabelTextField textField) {
        FileSetting saveDirSetting = SharingSettings.getFileSettingForMediaType(mediaType);
        File saveDir = saveDirSetting.getValue();
        String oldSaveDirString = saveDir.getAbsolutePath();
        String newSaveDirString = textField.getText();
        return !Objects.equalOrNull(oldSaveDirString, newSaveDirString);
    }

    @Override
    public void initOptions() {
        initField(MediaType.getAudioMediaType(), audioTextField);
        initField(MediaType.getVideoMediaType(), videoTextField);
        initField(MediaType.getImageMediaType(), imageTextField);
        initField(MediaType.getDocumentMediaType(), documentTextField);
        initField(MediaType.getProgramMediaType(), programTextField);
        initField(MediaType.getOtherMediaType(), otherTextField);
    }

    private void initField(MediaType mediaType, LabelTextField textField) {
        FileSetting saveDirSetting = SharingSettings.getFileSettingForMediaType(mediaType);
        File saveDir = saveDirSetting.getValue();
        String saveDirString = saveDir.getAbsolutePath();
        textField.setText(saveDirString);
    }
    
    private class DefaultAction extends AbstractAction {

        public DefaultAction() {
            putValue(Action.NAME, I18n.tr("Use Default"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Revert directories to the default"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            revertToDefault(audioTextField);
            revertToDefault(videoTextField);
            revertToDefault(imageTextField);
            revertToDefault(documentTextField);
            revertToDefault(programTextField);
            revertToDefault(otherTextField);
        }
    }
}
