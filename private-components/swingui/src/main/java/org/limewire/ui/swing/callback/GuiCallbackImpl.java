package org.limewire.ui.swing.callback;

import javax.swing.JOptionPane;

import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GuiCallbackImpl implements GuiCallback {
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    @Inject
    public GuiCallbackImpl(GuiCallbackService guiCallbackService,
            SaveLocationExceptionHandler saveLocationExceptionHandler) {
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        guiCallbackService.setGuiCallback(this);

    }

    @Override
    public void handleSaveLocationException(DownloadAction downLoadAction,
            SaveLocationException sle, boolean supportsNewSaveDir) {
        saveLocationExceptionHandler.handleSaveLocationException(downLoadAction, sle,
                supportsNewSaveDir);
    }

    private boolean yesNoQuestion(String message) {
        return JOptionPane.showConfirmDialog(GuiUtils.getMainFrame(), I18n.tr(message),
                "", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE, null) == JOptionPane.YES_OPTION;
    }

    @Override
    public boolean promptTorrentDownloading() {
        return yesNoQuestion(I18n
                .tr("If you stop this upload, the torrent download will stop. Are you sure you want to do this?"));
    }

    @Override
    public boolean promptTorrentSeedRatioLow() {
        return yesNoQuestion(I18n
                .tr("This upload is a torrent and it hasn\'t seeded enough. You should let it upload some more. Are you sure you want to stop it?"));
    }
}
