package org.limewire.ui.swing.downloads.table;

import java.io.File;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.downloads.DownloadItemUtils;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class DownloadActionHandler {
    
    public final static String PAUSE_COMMAND = "pause";
    public final static String CANCEL_COMMAND = "cancel";
    public final static String RESUME_COMMAND = "resume";
    public final static String TRY_AGAIN_COMMAND = "try again";
    public final static String LAUNCH_COMMAND = "launch";
    public final static String PREVIEW_COMMAND = "preview";
    public final static String REMOVE_COMMAND = "remove";
    public final static String LOCATE_COMMAND = "locate";
    public final static String PROPERTIES_COMMAND = "properties";
    public final static String LINK_COMMAND = "link";
    public final static String SHARE_COMMAND = "share";
    
    private static final String ERROR_URL = "http://wiki.limewire.org/index.php?title=User_Guide_Download";
    
    private EventList<DownloadItem> downloadItems;
    private PropertiesFactory<DownloadItem> propertiesFactory;
    private ShareWidget<File> shareWidget;
    
    @AssistedInject
    public DownloadActionHandler(PropertiesFactory<DownloadItem> propertiesFactory, 
            ShareWidgetFactory shareFactory, @Assisted EventList<DownloadItem> downloadItems){
        this.downloadItems = downloadItems;
        this.propertiesFactory = propertiesFactory;
        this.shareWidget = shareFactory.createFileShareWidget();
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
            NativeLaunchUtils.openURL(ERROR_URL);
        } else if (actionCommmand == PREVIEW_COMMAND || actionCommmand == LAUNCH_COMMAND){
            DownloadItemUtils.launch(item);
        } else if (actionCommmand == LOCATE_COMMAND){
            NativeLaunchUtils.launchExplorer(item.getDownloadingFile());
        } else if (actionCommmand == PROPERTIES_COMMAND){
            propertiesFactory.newProperties().showProperties(item);
        } else if (actionCommmand == REMOVE_COMMAND){
            downloadItems.remove(item);
        } else if (actionCommmand == SHARE_COMMAND){
            shareWidget.setShareable(item.getDownloadingFile());
            shareWidget.show(null);
        }
    }
    

}
