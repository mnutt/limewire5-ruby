package org.limewire.ui.swing.components;

import org.limewire.ui.swing.components.TabActionMap;

public interface FancyTabListFactory {
    
    FancyTabList create(Iterable<? extends TabActionMap> actionMaps);
 
    FancyTabList create(TabActionMap... actionMaps);
}
