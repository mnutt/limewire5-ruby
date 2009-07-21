package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.core.api.upload.UploadState;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class UploadActionHandler {

    public final static String PLAY_COMMAND = "play";
    public final static String CANCEL_COMMAND = "cancel";
    public final static String LIBRARY_COMMAND = "library";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_ON_DISK_COMMAND = "locate";
    public final static String PROPERTIES_COMMAND = "properties";
    
    
    private final UploadListManager uploadListManager;
    private final FileInfoDialogFactory fileInfoFactory;
    private final LibraryMediator libraryMediator;
    
    public UploadActionHandler(UploadListManager uploadListManager, LibraryMediator libraryMediator,
            FileInfoDialogFactory fileInfoFactory){
        this.uploadListManager = uploadListManager;
        this.libraryMediator = libraryMediator;
        this.fileInfoFactory = fileInfoFactory;
    }

    public void performAction(final String actionCommmand, final UploadItem item){
        if (actionCommmand == CANCEL_COMMAND) {
            item.cancel();
            //canceled upload items end up in the DONE state so they need to be manually removed.
            if (item.getState() == UploadState.CANCELED || item.getState() == UploadState.DONE) {
                uploadListManager.remove(item);
            }
        } else if (actionCommmand == LOCATE_ON_DISK_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            fileInfoFactory.createFileInfoDialog(item, FileInfoType.LOCAL_FILE);
        } else if (actionCommmand == REMOVE_COMMAND){
            uploadListManager.remove(item);
        } else if (actionCommmand == LIBRARY_COMMAND){
            libraryMediator.selectInLibrary(item.getFile());
        } else if (actionCommmand == LAUNCH_COMMAND){
            NativeLaunchUtils.safeLaunchFile(item.getFile());
        } else if (actionCommmand == PLAY_COMMAND){
            PlayerUtils.playOrLaunch(item.getFile());
        }
    }
}
