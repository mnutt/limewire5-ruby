package org.limewire.ui.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import net.roydesign.event.ApplicationEvent;
import net.roydesign.mac.MRJAdapter;

import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.lifecycle.LifeCycleManager;
import org.limewire.ui.swing.event.AboutDisplayEvent;
import org.limewire.ui.swing.event.ExitApplicationEvent;
import org.limewire.ui.swing.event.OptionsDisplayEvent;
import org.limewire.ui.swing.event.RestoreViewEvent;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.browser.ExternalControl;

/**
 * This class handles Macintosh specific events. The handled events include the
 * selection of the "About" option in the Mac file menu, the selection of the
 * "Quit" option from the Mac file menu, and the dropping of a file on LimeWire
 * on the Mac, which LimeWire would be expected to handle in some way.
 */
public class MacEventHandler {

    private static MacEventHandler INSTANCE;

    public static synchronized MacEventHandler instance() {
        if (INSTANCE == null) {
            INSTANCE = new MacEventHandler();
        }
        return INSTANCE;
    }

    private volatile File lastFileOpened = null;
    private volatile boolean enabled;

    @Inject private volatile ExternalControl externalControl = null;
    @Inject private volatile Initializer initializer = null;
    @Inject private volatile DownloadManager downloadManager = null;
    @Inject private volatile LifeCycleManager lifecycleManager = null;
    @Inject private volatile ActivityCallback activityCallback = null;

    /** Creates a new instance of MacEventHandler */
    private MacEventHandler() {

        MRJAdapter.addAboutListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                new AboutDisplayEvent().publish();
            }
        });

        MRJAdapter.addQuitApplicationListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handleQuit();
            }
        });

        MRJAdapter.addOpenDocumentListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                File file = ((ApplicationEvent) evt).getFile();
                handleOpenFile(file);
            }
        });

        MRJAdapter.addReopenApplicationListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                handleReopen();
            }
        });
    }

    @Inject
    public void startup() {
        this.enabled = true;
        
        if (lastFileOpened != null) {
            runFileOpen(lastFileOpened);
        }
    }

    /**
     * Enable preferences.
     */
    public void enablePreferences() {
        MRJAdapter.setPreferencesEnabled(true);

        MRJAdapter.addPreferencesListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                new OptionsDisplayEvent().publish();
            }
        });
    }

    private void handleQuit() {
        new ExitApplicationEvent(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "Shutdown"))
                .publish();
    }

    /**
     * This method handles a request to open the specified file.
     */
    private void handleOpenFile(File file) {
        if (!enabled) {
            lastFileOpened = file;
        } else {
            runFileOpen(file);
        }
    }

    private void runFileOpen(final File file) {
        String filename = file.getPath();
        if (filename.endsWith("limestart")) {
            initializer.setStartup();
        } else if (filename.endsWith("torrent")) {
            if (!lifecycleManager.isStarted()) {
                externalControl.enqueueControlRequest(file.getAbsolutePath());
            } else {
                try {
                    downloadManager.downloadTorrent(file, null, false);
                } catch (DownloadException e) {
                    activityCallback.handleDownloadException(new DownloadAction() {
                        @Override
                        public void download(File saveDirectory, boolean overwrite)
                                throws DownloadException {
                            downloadManager.downloadTorrent(file, saveDirectory, overwrite);
                        }

                        @Override
                        public void downloadCanceled(DownloadException ignored) {
                            //nothing to do
                        }

                    }, e, false);

                }
            }
        } else {
            NativeLaunchUtils.safeLaunchFile(file);
        }
    }

    private void handleReopen() {
        new RestoreViewEvent().publish();
    }
}
