package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.JMenu;

import org.jdesktop.application.Application;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.magnet.MagnetFactory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.event.ExitApplicationEvent;
import org.limewire.ui.swing.menu.actions.AddFileAction;
import org.limewire.ui.swing.menu.actions.AddFolderAction;
import org.limewire.ui.swing.menu.actions.OpenFileAction;
import org.limewire.ui.swing.menu.actions.OpenLinkAction;
import org.limewire.ui.swing.menu.actions.RecentDownloadsMenu;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileMenu extends JMenu {
    final Navigator navigator;

    @Inject
    public FileMenu(DownloadListManager downloadListManager, Navigator navigator,
            LibraryManager libraryManager,
            SaveLocationExceptionHandler saveLocationExceptionHandler, MagnetFactory magnetFactory,
            SearchHandler searchHandler) {
        super(I18n.tr("File"));
        this.navigator = navigator;
        add(new OpenFileAction(navigator, I18n.tr("&Open Torrent..."), downloadListManager,
                saveLocationExceptionHandler));
        add(new OpenLinkAction(navigator, I18n.tr("Open &Link..."), downloadListManager,
                saveLocationExceptionHandler, magnetFactory, searchHandler));
        add(new RecentDownloadsMenu(I18n.tr("Recent Downloads"), libraryManager));
        addSeparator();
        add(new AddFileAction(I18n.tr("Add File to Library..."), libraryManager));
        add(new AddFolderAction(I18n.tr("Add Folder to Library..."), libraryManager));

        // Add exit actions.
        if (!OSUtils.isMacOSX()) {
            addSeparator();
            add(new AbstractAction(I18n.tr("Exit After &Transfers")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    ActionMap map = Application.getInstance().getContext().getActionManager()
                            .getActionMap();
                    map.get("shutdownAfterTransfers").actionPerformed(e);
                }
            });
            add(new AbstractAction(I18n.tr("E&xit")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new ExitApplicationEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                            "Shutdown")).publish();
                }
            });
        }
    }
}
