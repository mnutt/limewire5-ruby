/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.search.DefaultSearchInfo;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.util.NotImplementedException;

public class OpenLinkAction extends AbstractAction {

    private final Navigator navigator;

    private final DownloadListManager downloadListManager;

    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    private final MagnetFactory magnetFactory;

    private final SearchHandler searchHandler;

    public OpenLinkAction(Navigator navigator, String name, DownloadListManager downloadListManager,
            SaveLocationExceptionHandler saveLocationExceptionHandler, MagnetFactory magnetFactory,
            SearchHandler searchHandler) {
        super(name);
        this.navigator = navigator;
        this.downloadListManager = downloadListManager;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.magnetFactory = magnetFactory;
        this.searchHandler = searchHandler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Get owner frame.
        Frame frame = GuiUtils.getMainFrame();
        
        // Create dialog.
        final LocationDialog locationDialogue = new LocationDialog(frame);
        locationDialogue.setTitle(I18n.tr("Open Link"));
        locationDialogue.setLocationRelativeTo(frame);
        locationDialogue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final URI uri = locationDialogue.getURI();
                if (uri != null) {
                    if (magnetFactory.isMagnetLink(uri)) {
                        MagnetLink[] magnetLinks = magnetFactory.parseMagnetLink(uri);
                        if (magnetLinks.length == 0) {
                            throw new NotImplementedException("need user feedback");
                        }

                        for (final MagnetLink magnet : magnetLinks) {
                            if (magnet.isDownloadable()) {
                                downloadMagnet(downloadListManager, saveLocationExceptionHandler,
                                        magnet);
                            } else if (magnet.isKeywordTopicOnly()) {
                                searchHandler.doSearch(DefaultSearchInfo.createKeywordSearch(magnet
                                        .getQueryString(), SearchCategory.ALL));
                            } else {
                                throw new NotImplementedException("need user feedback");
                            }
                        }
                    } else {
                        downloadTorrent(downloadListManager, saveLocationExceptionHandler,
                                 uri);
                    }
                }
            }

            private void downloadTorrent(final DownloadListManager downloadListManager,
                    final SaveLocationExceptionHandler saveLocationExceptionHandler,
                    final URI uri) {
                try {
                    DownloadItem item = downloadListManager.addTorrentDownload(uri, false);
                    navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME).select(
                            SimpleNavSelectable.create(item));
                } catch (SaveLocationException sle) {
                    saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {
                            DownloadItem item = downloadListManager.addTorrentDownload(uri,
                                    overwrite);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select(SimpleNavSelectable.create(item));
                        }
                    }, sle, false);
                }
            }

            private void downloadMagnet(final DownloadListManager downloadListManager,
                    final SaveLocationExceptionHandler saveLocationExceptionHandler,
                    final MagnetLink magnet) {
                try {
                    downloadListManager.addDownload(magnet, null, false);
                } catch (SaveLocationException e1) {
                    saveLocationExceptionHandler.handleSaveLocationException(new DownloadAction() {
                        @Override
                        public void download(File saveFile, boolean overwrite)
                                throws SaveLocationException {

                            DownloadItem item = downloadListManager.addDownload(magnet, saveFile,
                                    overwrite);
                            navigator.getNavItem(NavCategory.DOWNLOAD, MainDownloadPanel.NAME)
                                    .select(SimpleNavSelectable.create(item));
                        }
                    }, e1, true);
                }
            }
        });

        locationDialogue.setVisible(true);

    }
}