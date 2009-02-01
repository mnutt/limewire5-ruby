package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.NativeLaunchUtils;

public class UploadActionHandler {

    public final static String PLAY_COMMAND = "play";
    public final static String CANCEL_COMMAND = "cancel";
    public final static String LIBRARY_COMMAND = "library";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_ON_DISK_COMMAND = "locate";
    public final static String PROPERTIES_COMMAND = "properties";
    
    
    private UploadListManager uploadListManager;
    private PropertiesFactory<UploadItem> propertiesFactory;
    private LibraryNavigator libraryNavigator;
    
    public UploadActionHandler(UploadListManager uploadListManager, PropertiesFactory<UploadItem> propertiesFactory, LibraryNavigator libraryNavigator){
        this.uploadListManager = uploadListManager;
        this.propertiesFactory = propertiesFactory;
        this.libraryNavigator = libraryNavigator;
    }

    public void performAction(final String actionCommmand, final UploadItem item){
        if (actionCommmand == CANCEL_COMMAND) {
            //canceled upload items end up in the DONE state so they need to be manually removed.
            uploadListManager.remove(item);
            item.cancel();
        } else if (actionCommmand == LOCATE_ON_DISK_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            propertiesFactory.newProperties().showProperties(item);
        } else if (actionCommmand == REMOVE_COMMAND){
            uploadListManager.remove(item);
        } else if (actionCommmand == LIBRARY_COMMAND){
            libraryNavigator.selectInLibrary(item.getFile(), item.getCategory());
        } else if (actionCommmand == LAUNCH_COMMAND){
            NativeLaunchUtils.safeLaunchFile(item.getFile());
        } else if (actionCommmand == PLAY_COMMAND){
            PlayerUtils.playOrLaunch(item.getFile());
        }
    }
}
