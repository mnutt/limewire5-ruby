package org.limewire.ui.swing.mainframe;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavMediator;
import org.limewire.ui.swing.nav.Navigator;

import com.google.inject.Inject;

@LazySingleton
public class StoreMediator implements NavMediator<StorePanel> {
    public static final String NAME = "LimeWire Store";
    
    private StorePanel store;
    
    @Inject
    public StoreMediator(StorePanel storePanel, final Navigator navigator) {
        this.store = storePanel;
    }
    
    @Override
    public StorePanel getComponent() {
        return store;
    }
}
