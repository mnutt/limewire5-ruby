package org.limewire.ui.swing.pro;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.listener.EventListener;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.settings.InstallSettings;

import com.google.inject.Inject;

public class ProNagController {

    private final boolean isFirstLaunch;
    private final Application application;
    private final ProNag proNag;
    
    private JLayeredPane nagContainer;
    private boolean allowNagFromStartup;
    private boolean nagShown;

    @Inject ProNagController(Application application, ProNag proNag) {
        isFirstLaunch = !InstallSettings.UPGRADED_TO_5.getValue();
        this.proNag = proNag;
        this.application = application;
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
        }
    }

}
