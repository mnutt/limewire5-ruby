package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.MagnetLinkCopier;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

public class FriendLibraryPopupMenu extends JPopupMenu {
   
    private List<RemoteFileItem> fileItems;

    final private JSeparator separator = new JSeparator();
    final private JMenuItem linkItem;
    final private JMenuItem propertiesItem;

    final private DownloadListManager downloadListManager;
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    private MagnetLinkFactory magnetFactory;
    private PropertiesFactory<RemoteFileItem> remoteItemPropertiesFactory;
    private PropertiesFactory<DownloadItem> downloadItemPropertiesFactory;

    public FriendLibraryPopupMenu(DownloadListManager downloadListManager, MagnetLinkFactory magnetFactory, 
            PropertiesFactory<RemoteFileItem> remoteItemPropertiesFactory, SaveLocationExceptionHandler saveLocationExceptionHandler,
            PropertiesFactory<DownloadItem> downloadItemPropertiesFactory) {
        this.downloadListManager = downloadListManager;
        this.magnetFactory = magnetFactory;
        this.remoteItemPropertiesFactory = remoteItemPropertiesFactory;
        this.downloadItemPropertiesFactory = downloadItemPropertiesFactory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        linkItem = new JMenuItem(linkAction);
        propertiesItem = new JMenuItem(propertiesAction);

        add(downloadAction);
        add(linkItem);
        add(separator);
        add(propertiesItem);

    }

    public void setFileItems(List<RemoteFileItem> items) {
        this.fileItems = items;   
          
        boolean isSingleSelection = fileItems.size() == 1;
        linkItem.setVisible(isSingleSelection);
        separator.setVisible(isSingleSelection);
        propertiesItem.setVisible(isSingleSelection);        
    }


    private RemoteFileItem[] createFileItemArray(){
        return fileItems.toArray(new RemoteFileItem[fileItems.size()]);
    }
    
   private Action downloadAction = new AbstractAction(I18n.tr("Download Selected Files")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            final RemoteFileItem[] fileItemArray = createFileItemArray();
            
            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (final RemoteFileItem fileItem : fileItemArray) {
                        try {
                            downloadListManager.addDownload(fileItem);
                        } catch (SaveLocationException e) {
                            saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                                @Override
                                public void download(File saveFile, boolean overwrite)
                                        throws SaveLocationException {
                                    downloadListManager.addDownload(fileItem, saveFile, overwrite);
                                }
                            }, e, true);
                        }
                    }
                }
            });
        }
    };

    private Action linkAction = new AbstractAction(I18n.tr("Copy Link to Clipboard")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            new MagnetLinkCopier().copyLinkToClipBoard(fileItems.get(0), magnetFactory);
        }
    };
    
    private Action propertiesAction = new AbstractAction(I18n.tr("View File Info")) {
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO - Is this correct? Only to show props for first one?
            RemoteFileItem propertiable = fileItems.get(0);
            DownloadItem item = downloadListManager.getDownloadItem(propertiable.getUrn());
            if(item != null) {
                downloadItemPropertiesFactory.newProperties().showProperties(item);
            } else {
                remoteItemPropertiesFactory.newProperties().showProperties(propertiable);
            }
        }
    };
    

}
