/**
 * 
 */
package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JMenuItem;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.CategoryUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

import com.google.inject.Inject;

class RecentDownloadsMenu extends MnemonicMenu {
    private static final String emptyText = I18n.tr("(empty)"); 

    @Inject
    public RecentDownloadsMenu(final LibraryManager libraryManager) {
        super(I18n.tr("&Recent Downloads"));
    }

    @Override
    public void createMenuItems() {
        List<File> files = null;
        synchronized (DownloadSettings.RECENT_DOWNLOADS) {
            files = new ArrayList<File>(DownloadSettings.RECENT_DOWNLOADS.get());
        }

        Collections.sort(files, new FileDateMostToLeastRecentComparator());

        if (files.size() > 0) {
            for (final File file : files) {
                addRecentDownloadAction(file);
            }
            addSeparator();
            add(new AbstractAction(I18n.tr("Clear List")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    DownloadSettings.RECENT_DOWNLOADS.clear();
                }
            });
        } else {
            add(new JMenuItem(emptyText)).setEnabled(false);
        }
    }

    private void addRecentDownloadAction(final File file) {
        add(new AbstractAction(file.getName()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                MediaType mediaType = MediaType.getMediaTypeForExtension(FileUtils
                        .getFileExtension(file));
                Category category = CategoryUtils.getCategory(mediaType);

                switch (category) {
                case AUDIO:
                    PlayerUtils.playOrLaunch(file);
                    break;
                case DOCUMENT:
                case IMAGE:
                case VIDEO:
                    NativeLaunchUtils.safeLaunchFile(file);
                    break;
                case PROGRAM:
                case OTHER:
                    NativeLaunchUtils.launchExplorer(file);
                    break;
                default:
                    break;
                }
            }
        });
    }

    /**
     * Orders files from most to least recent.
     */
    private static class FileDateMostToLeastRecentComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return -1 * Long.valueOf(o1.lastModified()).compareTo(Long.valueOf(o2.lastModified()));
        }
    }
}