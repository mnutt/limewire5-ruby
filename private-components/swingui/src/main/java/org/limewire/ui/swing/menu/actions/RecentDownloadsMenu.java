/**
 * 
 */
package org.limewire.ui.swing.menu.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Action;
import javax.swing.JMenuItem;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.CategoryUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

import com.google.inject.Inject;

public class RecentDownloadsMenu extends MnemonicMenu {
    private final JMenuItem emptyItem;

    private final Action clearMenu;

    @Inject
    public RecentDownloadsMenu(final LibraryManager libraryManager) {
        // TODO fberger
        super(I18n.tr("Recent Downloads"));
        emptyItem = new JMenuItem(I18n.tr("(empty)"));
        emptyItem.setEnabled(false);

        clearMenu = new AbstractAction(I18n.tr("Clear list")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                DownloadSettings.RECENT_DOWNLOADS.clear();
            }
        };

        populateRecentDownloads();

        DownloadSettings.RECENT_DOWNLOADS.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        populateRecentDownloads();
                    }
                });
            }
        });
    }

    private void populateRecentDownloads() {
        removeAll();
        List<File> files = null;
        synchronized (DownloadSettings.RECENT_DOWNLOADS) {
            files = new ArrayList<File>(DownloadSettings.RECENT_DOWNLOADS.getValue());
        }

        Collections.sort(files, new FileDateMostToLeastRecentComparator());

        if (files.size() > 0) {
            for (final File file : files) {
                addRecentDownloadAction(file);
            }
            addSeparator();
            add(clearMenu);
        } else {
            add(emptyItem);
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
    private class FileDateMostToLeastRecentComparator implements Comparator<File> {
        @Override
        public int compare(File o1, File o2) {
            return -1 * Long.valueOf(o1.lastModified()).compareTo(Long.valueOf(o2.lastModified()));
        }
    }
}