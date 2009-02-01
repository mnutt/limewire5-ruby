package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JPopupMenu;
import javax.swing.JSeparator;

import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.BackgroundExecutorService;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

public class FriendLibraryPopupMenu extends JPopupMenu {

    private List<RemoteFileItem> fileItems;

    final private JSeparator separator = new JSeparator();

    final private DownloadListManager downloadListManager;

    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    private PropertiesFactory<RemoteFileItem> remoteItemPropertiesFactory;

    private PropertiesFactory<DownloadItem> downloadItemPropertiesFactory;

    private final LibraryManager libraryManager;

    public FriendLibraryPopupMenu(DownloadListManager downloadListManager,
            PropertiesFactory<RemoteFileItem> remoteItemPropertiesFactory,
            SaveLocationExceptionHandler saveLocationExceptionHandler,
            PropertiesFactory<DownloadItem> downloadItemPropertiesFactory,
            LibraryManager libraryManager) {
        this.downloadListManager = downloadListManager;
        this.remoteItemPropertiesFactory = remoteItemPropertiesFactory;
        this.downloadItemPropertiesFactory = downloadItemPropertiesFactory;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.libraryManager = libraryManager;
    }

    public void setFileItems(List<RemoteFileItem> items) {
        this.fileItems = items;
        initialize();
    }

    private void initialize() {
        removeAll();
        boolean isSingleSelection = fileItems.size() == 1;
        boolean downloadActionEnabled = false;
        for (RemoteFileItem remoteFileItem : fileItems) {
            URN urn = remoteFileItem.getUrn();
            if (!libraryManager.getLibraryManagedList().contains(urn)
                    && !downloadListManager.contains(urn)) {
                downloadActionEnabled = true;
                break;
            }
        }

        add(new DownloadFromFriendAction()).setEnabled(downloadActionEnabled);
        if (isSingleSelection) {
            add(separator);
            add(new ViewFileInfoAction());
        }

    }

    private RemoteFileItem[] createFileItemArray() {
        return fileItems.toArray(new RemoteFileItem[fileItems.size()]);
    }

    private final class ViewFileInfoAction extends AbstractAction {
        private ViewFileInfoAction() {
            super(I18n.tr("View File Info..."));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            RemoteFileItem propertiable = fileItems.get(0);
            DownloadItem item = downloadListManager.getDownloadItem(propertiable.getUrn());
            if (item != null) {
                downloadItemPropertiesFactory.newProperties().showProperties(item);
            } else {
                remoteItemPropertiesFactory.newProperties().showProperties(propertiable);
            }
        }
    }

    private final class DownloadFromFriendAction extends AbstractAction {
        private DownloadFromFriendAction() {
            super(I18n.tr("Download"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final RemoteFileItem[] fileItemArray = createFileItemArray();

            BackgroundExecutorService.execute(new Runnable() {
                @Override
                public void run() {
                    for (final RemoteFileItem fileItem : fileItemArray) {

                        URN urn = fileItem.getUrn();
                        if (!libraryManager.getLibraryManagedList().contains(urn)
                                && !downloadListManager.contains(urn)) {
                            try {
                                downloadListManager.addDownload(fileItem);
                            } catch (SaveLocationException e) {
                                saveLocationExceptionHandler.handleSaveLocationException(
                                        new DownloadAction() {
                                            @Override
                                            public void download(File saveFile, boolean overwrite)
                                                    throws SaveLocationException {
                                                downloadListManager.addDownload(fileItem, saveFile,
                                                        overwrite);
                                            }
                                        }, e, true);
                            }
                        }
                    }
                    GuiUtils.getMainFrame().repaint();
                }
            });
        }
    }

}
