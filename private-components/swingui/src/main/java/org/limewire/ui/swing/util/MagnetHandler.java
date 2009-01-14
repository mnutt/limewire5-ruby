package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.JOptionPane;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MagnetHandler {

    private final Navigator navigator;

    private final DownloadListManager downloadListManager;

    private final SearchHandler searchHandler;

    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    @Inject
    public MagnetHandler(Navigator navigator, SearchHandler searchHandler,
            DownloadListManager downloadListManager,
            SaveLocationExceptionHandler saveLocationExceptionHandler) {
        this.navigator = navigator;
        this.downloadListManager = downloadListManager;
        this.searchHandler = searchHandler;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
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
                    downloadMagnet(downloadListManager, saveLocationExceptionHandler, magnet);
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
            final SaveLocationExceptionHandler saveLocationExceptionHandler, final MagnetLink magnet) {
        try {
            downloadListManager.addDownload(magnet, null, false);
        } catch (SaveLocationException e1) {
            saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                @Override
                public void download(File saveFile, boolean overwrite) throws SaveLocationException {

                    DownloadItem item = downloadListManager
                            .addDownload(magnet, saveFile, overwrite);
                    navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select(
                            SimpleNavSelectable.create(item));
                }
            }, e1, true);
        }
    }
}
