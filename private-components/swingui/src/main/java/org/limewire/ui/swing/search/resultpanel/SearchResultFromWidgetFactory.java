package org.limewire.ui.swing.search.resultpanel;

import org.limewire.ui.swing.search.RemoteHostActions;

public interface SearchResultFromWidgetFactory {
    
    SearchResultFromWidget create(RemoteHostActions fromActions, boolean isClassicView);
    
}
