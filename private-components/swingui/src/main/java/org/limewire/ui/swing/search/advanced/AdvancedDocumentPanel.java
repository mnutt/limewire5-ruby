package org.limewire.ui.swing.search.advanced;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;

/** The panel for advanced document search. */
class AdvancedDocumentPanel extends AdvancedPanel {
    
    public AdvancedDocumentPanel(FriendAutoCompleterFactory friendAutoCompleterFactory) {
        super(SearchCategory.DOCUMENT, friendAutoCompleterFactory);
        addField(I18n.tr("Name"), FilePropertyKey.NAME);
    }

}
