package org.limewire.ui.swing.pro;

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ProNagController {

    private final boolean isFirstLaunch;
    private final Application application;
    private final ProNag proNag;
    private final Provider<OldStyleProNag> oldProNag;
    
    private JLayeredPane nagContainer;
    private boolean allowNagFromStartup;
    private boolean nagShown;

    @Inject ProNagController(Application application, ProNag proNag, Provider<OldStyleProNag> oldProNag) {
        isFirstLaunch = !InstallSettings.UPGRADED_TO_5.getValue();
        this.proNag = proNag;
        this.application = application;
        this.oldProNag = oldProNag;
    }

    public void allowProNag(JLayeredPane layeredPane) {
        if (!application.isProVersion()) {
            nagContainer = layeredPane;
            allowNagFromStartup = true;
        }
        
        showNagIfOk();
    }
    
    private void showNagIfOk() {
        if(allowNagFromStartup && !nagShown) {
            assert SwingUtilities.isEventDispatchThread();
            nagShown = true;
            
            if(!InstallSettings.USE_OLD_PRO_DIALOG.getValue()) {
                proNag.loadContents(isFirstLaunch).addFutureListener(new EventListener<FutureEvent<Void>>() {
                    @Override
                    @SwingEDTEvent
                    public void handleEvent(FutureEvent<Void> event) {
                        switch (event.getType()) {
                        case SUCCESS:
                            nagContainer.addComponentListener(new PanelResizer(proNag));
                            nagContainer.add(proNag, JLayeredPane.MODAL_LAYER);
                            proNag.resize();
                        }
                    }
                });
            } else {
                OldStyleProNag nag = oldProNag.get();
                JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("Upgrade to Pro!"), null, nag);
                dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
                dialog.setSize(new Dimension(380, 240));
                dialog.getRootPane().setDefaultButton(nag.getDefaultButton());
                dialog.setModal(true);
                dialog.setVisible(true);
            }
        }
    }

}
