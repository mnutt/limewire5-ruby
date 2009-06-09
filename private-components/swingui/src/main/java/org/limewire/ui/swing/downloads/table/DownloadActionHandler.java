package org.limewire.ui.swing.downloads.table;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class DownloadActionHandler {
    
    public final static String PAUSE_COMMAND = "pause";
    /**Cancels download, deletes file, and removes download from list*/
    public final static String CANCEL_COMMAND = "cancel";
    public final static String RESUME_COMMAND = "resume";
    public final static String TRY_AGAIN_COMMAND = "try again";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String PREVIEW_COMMAND = "preview";
    public final static String PLAY_COMMAND = "play";
    /**Removes download from list.  Has no other effects.*/
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_COMMAND = "locate";
    public final static String LIBRARY_COMMAND = "library";
    public final static String PROPERTIES_COMMAND = "properties";
    public final static String LINK_COMMAND = "link";
    public final static String SHARE_COMMAND = "share";
    
   // private static final String ERROR_URL = "http://wiki.limewire.org/index.php?title=User_Guide_Download";
    
    private final LibraryNavigator libraryNavigator;
    private DownloadListManager downloadListManager;
    private ShareWidget<File> shareWidget = null;
    private LibraryManager libraryManager;
    private final FileInfoDialogFactory fileInfoFactory;
    private final Provider<ShareWidgetFactory> shareFactory;
    
    @Inject
    public DownloadActionHandler(Provider<ShareWidgetFactory> shareFactory, DownloadListManager downloadListManager, 
            LibraryNavigator libraryNavigator, LibraryManager libraryManager, FileInfoDialogFactory fileInfoFactory){
        this.downloadListManager = downloadListManager;
        this.shareFactory = shareFactory;
        this.libraryNavigator = libraryNavigator;
        this.libraryManager = libraryManager;
        this.fileInfoFactory = fileInfoFactory;
    }

    public void performAction(final String actionCommmand, final DownloadItem item){
        if (actionCommmand == CANCEL_COMMAND) {
            item.cancel();
        } else if (actionCommmand == PAUSE_COMMAND) {
            item.pause();
        } else if (actionCommmand == RESUME_COMMAND) {
            item.resume();
        } else if (actionCommmand == TRY_AGAIN_COMMAND) {
            item.resume();
        } else if (actionCommmand == LINK_COMMAND){
            //Do nothing for now
           // NativeLaunchUtils.openURL(ERROR_URL);
        } else if (actionCommmand == PREVIEW_COMMAND || actionCommmand == LAUNCH_COMMAND || actionCommmand == PLAY_COMMAND){
            if (item.isLaunchable()) {
                DownloadItemUtils.launch(item);
            }
        } else if (actionCommmand == LOCATE_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getDownloadingFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            if(item.getState() != DownloadState.DONE) {
                fileInfoFactory.createFileInfoDialog(item, FileInfoType.DOWNLOADING_FILE);
            } else {
                // if finished downloadng, try showing all the information from the localFileItem
                LocalFileItem localItem = libraryManager.getLibraryManagedList().getFileItem(item.getLaunchableFile());
                if(localItem != null)
                    fileInfoFactory.createFileInfoDialog(localItem, FileInfoType.LOCAL_FILE);
                else  // if can't find the localFileItem, revert to the downloadItem
                    fileInfoFactory.createFileInfoDialog(item, FileInfoType.DOWNLOADING_FILE);
            }
        } else if (actionCommmand == REMOVE_COMMAND){
            downloadListManager.remove(item);
        } else if (actionCommmand == SHARE_COMMAND){
            if(shareWidget == null)
                shareWidget = shareFactory.get().createFileShareWidget();
            shareWidget.setShareable(item.getDownloadingFile());
            shareWidget.show(null);
        } else if( actionCommmand == LIBRARY_COMMAND) {
            File file = item.getState() == DownloadState.DONE ? item.getLaunchableFile() : item.getDownloadingFile();
            URN urn = item.getUrn();
            Category category = item.getCategory();
            
            if(file != null) {
                libraryNavigator.selectInLibrary(file, category);
            } else if (urn != null){
                libraryNavigator.selectInLibrary(urn, category);
            }
        }
    }
}
