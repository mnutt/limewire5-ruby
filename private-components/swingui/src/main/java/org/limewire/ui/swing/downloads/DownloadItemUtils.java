package org.limewire.ui.swing.downloads;

import java.awt.Cursor;
import java.awt.EventQueue;
import java.io.File;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.util.GuiUtils;

public class DownloadItemUtils {
    
    private DownloadItemUtils() {}
    
    /**
     * Launches the download, loading the launchable portion in the background
     * if necessary.
     */
    public static void launch(final DownloadItem downloadItem) {
        assert EventQueue.isDispatchThread();
        assert downloadItem.isLaunchable();
        GuiUtils.getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<File, Void>() {
            @Override
            protected File doInBackground() throws Exception {
                return downloadItem.getLaunchableFile();
            }
            
            @Override
            protected void done() {
                GuiUtils.getMainFrame().setCursor(Cursor.getDefaultCursor());
                File file;
                try {
                    file = get();
                    if(file != null) {
                        PlayerUtils.playOrLaunch(file);
                    }
                } catch (InterruptedException ignored) {
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
        }.execute();
    }

}
