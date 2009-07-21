package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.JOptionPane;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
class MagnetHandlerImpl implements MagnetHandler {

    private final DownloadListManager downloadListManager;

    private final SearchHandler searchHandler;

    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;
    
    @Inject
    MagnetHandlerImpl(SearchHandler searchHandler,
            DownloadListManager downloadListManager,
            Provider<DownloadExceptionHandler> downloadExceptionHandler) {
        this.downloadListManager = downloadListManager;
        this.searchHandler = searchHandler;
        this.downloadExceptionHandler = downloadExceptionHandler;
    }

    /**
     * Handles the given magnet file by either starting a search or starting to
     * download the file specified in the magnet.
     */
    public void handleMagnet(final MagnetLink magnet) {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (magnet.isDownloadable()) {
                    downloadMagnet(downloadListManager, downloadExceptionHandler, magnet);
                } else if (magnet.isKeywordTopicOnly()) {
                    searchHandler.doSearch(DefaultSearchInfo.createKeywordSearch(magnet
                            .getQueryString(), SearchCategory.ALL));
                } else {
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), I18n
                            .tr("Invalid magnet option."), I18n.tr("Open Link"),
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

    }

    private void downloadMagnet(final DownloadListManager downloadListManager,
            final Provider<DownloadExceptionHandler> downloadExceptionHandler, final MagnetLink magnet) {
        try {
            downloadListManager.addDownload(magnet, null, false);
        } catch (DownloadException e1) {
            downloadExceptionHandler.get().handleDownloadException(new DownloadAction() {
                @Override
                public void download(File saveFile, boolean overwrite) throws DownloadException {
                    downloadListManager
                            .addDownload(magnet, saveFile, overwrite);
                }

                @Override
                public void downloadCanceled(DownloadException ignored) {
                    //nothing to do                    
                }

            }, e1, true);
        }
    }
}
