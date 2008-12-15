package com.limegroup.gnutella.gui.options.panes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.limewire.core.settings.SharingSettings;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog.State;
import com.limegroup.gnutella.gui.options.OptionsMediator;

/**
 * This class defines the panel in the options window that allows the user to
 * change the directory for saving files.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class SaveDirPaneItem extends AbstractDirPaneItem {

    public final static String TITLE = I18n.tr("Save Folder");
    
    /**
     * The mediatype table mediator that handles the per mediatype download
     * directories table.
     */
    private MediaTypeDownloadDirMediator _mtddMediator; 
    
    /**
     * The SharedDirPaneItem, so new save directories can be shared.
     */
    private final SharedDirPaneItem shareData;
    
    /**
     *  The mediator's description label id.
     */ 
    private final String MEDIA_OPTION_LABEL = I18nMarker.marktr("You can specify a download location for each media type.");

    
    public SaveDirPaneItem(SharedDirPaneItem shareStuff) {
        super(TITLE);
        
        shareData = shareStuff;
        
        add(getVerticalSeparator());
        
        _mtddMediator = new MediaTypeDownloadDirMediator(saveField);
        
        LabeledComponent comp = new LabeledComponent(MEDIA_OPTION_LABEL,
                _mtddMediator.getComponent(), LabeledComponent.NO_GLUE, LabeledComponent.TOP_LEFT);
        add(comp.getComponent());

        Action[] actions = new AbstractAction[2];
        actions[0] = _mtddMediator.getBrowseDirectoryAction();
        actions[1] = _mtddMediator.getResetDirectoryAction();
        ButtonRow br = new ButtonRow(actions, ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
        add(getVerticalSeparator());        
        add(br);
    }

    @Override
    public String getDefaultPath() {
        return SharingSettings.DEFAULT_SAVE_DIR.getAbsolutePath();
            }
            
    @Override
    public void initOptions() {
        try {
            File file = SharingSettings.getSaveDirectory();
            if (file == null) {
                throw (new FileNotFoundException());
            }
            setDirectoryPath( file.getCanonicalPath());
        } catch (FileNotFoundException fnfe) {
            // simply use the empty string if we could not get the save
            // directory.
            setDirectoryPath("");
        } catch (IOException ioe) {
            setDirectoryPath("");
        }
        _mtddMediator.initOptions();
    }

    /**
     * Defines the abstract method in <tt>AbstractPaneItem</tt>.
     * <p>
     *
     * Applies the options currently set in this window, displaying an error
     * message to the user if a setting could not be applied.
     *
     * @throws IOException
     *             if the options could not be applied for some reason
     */
    @Override
    public boolean applyOptions() throws IOException {
        final String save = saveField.getText();
        Set<File> newDirs = new HashSet<File>();
        if(!save.equals(saveDirectory)) {
            try {
                File saveDir = new File(save);
                if(!saveDir.isDirectory()) {
                    if (!saveDir.mkdirs())
                        throw new IOException();
                }
                if(!shareData.isAlreadyGoingToBeShared(saveDir))
                    newDirs.add(saveDir);
                SharingSettings.setSaveDirectory(saveDir);
                saveDirectory = save;
            } catch(IOException ioe) {
                GUIMediator.showError(I18n.tr("Invalid folder for saving files. Please use another folder or revert to the default."));
                saveField.setText(saveDirectory);
                throw new IOException();
            } catch(NullPointerException npe) {
                GUIMediator.showError(I18n.tr("Invalid folder for saving files. Please use another folder or revert to the default."));
                saveField.setText(saveDirectory);
                throw new IOException();
            }
        }
        
        boolean restart = _mtddMediator.applyOptions(newDirs);
        if(!newDirs.isEmpty()) {
            RecursiveSharingDialog dialog = new RecursiveSharingDialog(OptionsMediator.instance().getMainOptionsComponent(), newDirs.toArray(new File[newDirs.size()]));
            dialog.setTitleText(I18n.tr("Would you like to share your new save folders? The following new folders will be shared:"));
            if (dialog.showChooseDialog(OptionsMediator.instance().getMainOptionsComponent(), false) == State.OK) {
                shareData.addAndKeepDirtyStatus(dialog.getRootsToShare(), dialog.getFoldersToExclude());
            }
        }
        return restart;
    }
    
    @Override
    public boolean isDirty() {
        return !SharingSettings.getSaveDirectory().equals(
              new File(saveField.getText())) || _mtddMediator.isDirty();
    }
}
