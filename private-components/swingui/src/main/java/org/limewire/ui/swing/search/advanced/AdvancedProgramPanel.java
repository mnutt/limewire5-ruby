package org.limewire.ui.swing.search.advanced;

import javax.swing.Action;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;

/** The panel for advanced program search. */
class AdvancedProgramPanel extends AdvancedPanel {
    
    public AdvancedProgramPanel(FriendAutoCompleterFactory friendAutoCompleterFactory, Action enterKeyAction) {
        super(SearchCategory.PROGRAM, friendAutoCompleterFactory, enterKeyAction);
        addField(FilePropertyKey.NAME);
    }

}
