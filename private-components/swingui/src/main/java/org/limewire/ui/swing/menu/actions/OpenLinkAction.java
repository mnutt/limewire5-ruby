package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URI;

import javax.swing.JOptionPane;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.nav.NavCategory;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.nav.SimpleNavSelectable;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MagnetHandler;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;

/**
 * An action to prompt the user to open and download a link. Two link types are
 * supported: magnet links and torrent links.
 */
public class OpenLinkAction extends AbstractAction {

    private final Navigator navigator;

    private final DownloadListManager downloadListManager;

    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    private final MagnetFactory magnetFactory;


    private final MagnetHandler magnetHandler;

    @Inject
    public OpenLinkAction(Navigator navigator,
            DownloadListManager downloadListManager,
            SaveLocationExceptionHandler saveLocationExceptionHandler, MagnetFactory magnetFactory,
            MagnetHandler magnetHandler) {
        super(I18n.tr("Open &Link..."));
        this.navigator = navigator;
        this.downloadListManager = downloadListManager;
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.magnetFactory = magnetFactory;
        this.magnetHandler = magnetHandler;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Get owner frame.
        // Create dialog.
        final LocationDialog locationDialogue = new LocationDialog();
        locationDialogue.setTitle(I18n.tr("Open Link"));
        locationDialogue.setLocationRelativeTo(GuiUtils.getMainFrame());
        locationDialogue.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final URI uri = locationDialogue.getURI();
                if (uri != null) {
                    if (magnetFactory.isMagnetLink(uri)) {
                        MagnetLink[] magnetLinks = magnetFactory.parseMagnetLink(uri);
                        if (magnetLinks.length == 0) {
                            FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), I18n
                                    .tr("Magnet link contains no options."), I18n.tr("Open Link"),
                                    JOptionPane.INFORMATION_MESSAGE);
                        }

                        for (final MagnetLink magnet : magnetLinks) {
                            magnetHandler.handleMagnet(magnet);
                        }

                    } else {
                        downloadTorrent(downloadListManager, saveLocationExceptionHandler, uri);
                    }
                }
            }

            private void downloadTorrent(final DownloadListManager downloadListManager,
                    final SaveLocationExceptionHandler saveLocationExceptionHandler, final URI uri) {
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
        });

        locationDialogue.setVisible(true);

    }
}