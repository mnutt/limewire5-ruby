package org.limewire.ui.swing.pro;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLayeredPane;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.core.api.search.SearchEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
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
    private boolean allowNagFromConnectOrSearch;
    private boolean nagShown;

    @Inject ProNagController(Application application, ProNag proNag) {
        isFirstLaunch = !InstallSettings.UPGRADED_TO_5.getValue();
        this.proNag = proNag;
        this.application = application;
    }
    
    @Inject void register(final GnutellaConnectionManager gnutellaConnectionManager, 
                          final ListenerSupport<SearchEvent> searchEventSupport) {
        gnutellaConnectionManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals(GnutellaConnectionManager.CONNECTION_STRENGTH)) {
                    switch((ConnectionStrength)evt.getNewValue()) {
                    case FULL:
                    case TURBO:
                        allowNagFromConnectOrSearch = true;
                        showNagIfOk();
                        gnutellaConnectionManager.removePropertyChangeListener(this);
                    }
                }
            }
        });
        
        searchEventSupport.addListener(new EventListener<SearchEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(SearchEvent event) {
                switch(event.getType()) {
                case STARTED:
                    allowNagFromConnectOrSearch = true;
                    showNagIfOk();
                    searchEventSupport.removeListener(this);
                }
            }
        });
    }

    public void allowProNag(JLayeredPane layeredPane) {
        if (!application.isProVersion() && !isFirstLaunch) {
            nagContainer = layeredPane;
            allowNagFromStartup = true;
        }
        
        showNagIfOk();
    }
    
    private void showNagIfOk() {
        if(allowNagFromStartup && allowNagFromConnectOrSearch && !nagShown) {
            assert SwingUtilities.isEventDispatchThread();
            nagShown = true;
            proNag.loadContents().addFutureListener(new EventListener<FutureEvent<Void>>() {
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
