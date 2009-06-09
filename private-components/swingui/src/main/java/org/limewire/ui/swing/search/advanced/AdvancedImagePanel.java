package org.limewire.ui.swing.search.advanced;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced image search. */
class AdvancedImagePanel extends AdvancedPanel {
    
    public AdvancedImagePanel(FriendAutoCompleterFactory friendAutoCompleterFactory) {
        super(SearchCategory.IMAGE, friendAutoCompleterFactory);
        addField(FilePropertyKey.NAME);
    }

}
