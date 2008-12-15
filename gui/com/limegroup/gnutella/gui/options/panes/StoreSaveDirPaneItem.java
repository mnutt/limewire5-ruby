package com.limegroup.gnutella.gui.options.panes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.limewire.core.settings.SharingSettings;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;

/**
 * Defines the panel in the options window that allows the user to
 * change the directory for saving files.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class StoreSaveDirPaneItem extends AbstractDirPaneItem {

    /**
     * Title of the item in the pane, this name will be displayed in a line border
     */
    public final static String TITLE = I18n.tr("LimeWire Store - Save Folder");
    
    public final static String LABEL = I18n.tr("You can choose the folder for saving files purchased from the LimeWire Store.");
    
    /**
     * Determines if the directory needs to be updated
     */
    private boolean isDirty = false;
    
    public StoreSaveDirPaneItem() {
        super(TITLE, LABEL);
    }

    @Override
    public void initOptions() {
        try {
            File file = SharingSettings.getSaveLWSDirectory();
            if (file == null) {
                throw (new FileNotFoundException());
            }
            setDirectoryPath( file.getCanonicalPath() );
        } catch (FileNotFoundException fnfe) {
        // simply use the empty string if we could not get the save
        // directory.
            setDirectoryPath("");
        } catch (IOException ioe) {
            setDirectoryPath("");
        }
    }

    @Override
    public String getDefaultPath() {
        return SharingSettings.DEFAULT_SAVE_LWS_DIR.getAbsolutePath();
    }

    @Override
    public boolean applyOptions() throws IOException {
      final String save = saveField.getText();
      if(!save.equals(saveDirectory)) {
          try {
              final File saveDir = new File(save);
              if(!saveDir.isDirectory()) {
                  if (!saveDir.mkdirs())
                      throw new IOException();
              }
              SharingSettings.setSaveLWSDirectory(saveDir);
              saveDirectory = save;
              isDirty = true;
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
      return false;
    }
    
    /**
     * Resets the dirty state to false
     */
    public void resetDirtyState() {
        isDirty = false;
    }
    
    @Override
    public boolean isDirty() {
        return isDirty;
    }
}
