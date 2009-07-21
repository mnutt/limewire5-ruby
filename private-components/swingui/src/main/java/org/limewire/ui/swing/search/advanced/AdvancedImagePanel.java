package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced image search. */
class AdvancedImagePanel extends AdvancedPanel {
    
    public AdvancedImagePanel(FriendAutoCompleterFactory friendAutoCompleterFactory, Action enterKeyAction) {
        super(SearchCategory.IMAGE, friendAutoCompleterFactory, enterKeyAction);
        addField(FilePropertyKey.NAME);
    }

}
