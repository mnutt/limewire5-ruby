package org.limewire.ui.swing.callback;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;

import org.jdesktop.application.Application;
import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MagnetHandler;
import org.limewire.ui.swing.util.DownloadExceptionHandler;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class GuiCallbackImpl implements GuiCallback {
    private final Provider<DownloadExceptionHandler> downloadExceptionHandler;
    private final Provider<MagnetHandler> magnetHandler;

    @Inject
    public GuiCallbackImpl(GuiCallbackService guiCallbackService,
            Provider<DownloadExceptionHandler> downloadExceptionHandler,
            Provider<MagnetHandler> magnetHandler) {
        this.downloadExceptionHandler = downloadExceptionHandler;
        this.magnetHandler = magnetHandler;
        guiCallbackService.setGuiCallback(this);
    }

    @Override
    public void handleDownloadException(DownloadAction downLoadAction,
            DownloadException e, boolean supportsNewSaveDir) {
        downloadExceptionHandler.get().handleDownloadException(downLoadAction, e, supportsNewSaveDir);
    }

    private boolean yesNoQuestion(String message) {
        return FocusJOptionPane.showConfirmDialog(GuiUtils.getMainFrame(), new MultiLineLabel(I18n.tr(message), 400), "",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    @Override
    public void restoreApplication() {
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                ActionMap actionMap = Application.getInstance().getContext().getActionMap();
                Action restoreView = actionMap.get("restoreView");
                restoreView.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                        "restoreView"));
            }
        });
    }

    @Override
    public String translate(String s) {
        return I18n.tr(s);
    }

    @Override
    public void handleMagnet(MagnetLink magnetLink) {
        magnetHandler.get().handleMagnet(magnetLink);
    }

    @Override
    public boolean promptUserQuestion(String marktr) {
        return yesNoQuestion(I18n.tr(marktr));
    }
    
    @Override
    public void dangerousDownloadDeleted(String filename) {
        final String truncated;
        if(filename.length() < 70)
            truncated = filename;
        else
            truncated = filename.substring(0, 70) + "...";
        SwingUtils.invokeLater(new Runnable() {
            @Override
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(),
                        I18n.tr("{0}\n" +
                                "This file may have been designed to damage your computer.\n" +
                                "LimeWire has cancelled the download for your protection.",
                                truncated),
                        I18n.tr("Dangerous Download Cancelled"),
                        JOptionPane.WARNING_MESSAGE);
            }
        });
    }
}
