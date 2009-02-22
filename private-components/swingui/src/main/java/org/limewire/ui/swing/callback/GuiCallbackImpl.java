package org.limewire.ui.swing.callback;

import java.awt.event.ActionEvent;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.JOptionPane;

import org.jdesktop.application.Application;
import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.callback.GuiCallbackService;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.MagnetHandler;
import org.limewire.ui.swing.util.SaveLocationExceptionHandler;
import org.limewire.ui.swing.util.SwingUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class GuiCallbackImpl implements GuiCallback {
    private final SaveLocationExceptionHandler saveLocationExceptionHandler;

    private final MagnetHandler magnetHandler;

    @Inject
    public GuiCallbackImpl(GuiCallbackService guiCallbackService,
            SaveLocationExceptionHandler saveLocationExceptionHandler,
            MagnetHandler magnetHandler) {
        this.saveLocationExceptionHandler = saveLocationExceptionHandler;
        this.magnetHandler = magnetHandler;
        guiCallbackService.setGuiCallback(this);
    }

    @Override
    public void handleSaveLocationException(DownloadAction downLoadAction,
            SaveLocationException sle, boolean supportsNewSaveDir) {
        saveLocationExceptionHandler.handleSaveLocationException(downLoadAction, sle,
                supportsNewSaveDir);
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
        magnetHandler.handleMagnet(magnetLink);
    }

    @Override
    public boolean promptUserQuestion(String marktr) {
        return yesNoQuestion(I18n.tr(marktr));
    }
}
