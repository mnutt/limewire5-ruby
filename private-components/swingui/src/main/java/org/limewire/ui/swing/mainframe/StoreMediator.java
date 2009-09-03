package org.limewire.ui.swing.mainframe;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class StoreMediator implements NavMediator<StorePanel> {
    public static final String NAME = "LimeWire Store";
    
    private Provider<StorePanel> store;
    private StorePanel storePanel;
    
    @Inject
    public StoreMediator(Provider<StorePanel> storePanel, final Navigator navigator) {
        this.store = storePanel;
    }
    
    @Override
    public StorePanel getComponent() {
        if(storePanel == null)
            storePanel = store.get();
        return storePanel;
    }
}
