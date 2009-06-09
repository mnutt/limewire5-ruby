package org.limewire.ui.swing.search.advanced;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced program search. */
class AdvancedProgramPanel extends AdvancedPanel {
    
    public AdvancedProgramPanel(FriendAutoCompleterFactory friendAutoCompleterFactory) {
        super(SearchCategory.PROGRAM, friendAutoCompleterFactory);
        addField(FilePropertyKey.NAME);
    }

}
