package org.limewire.ui.swing.search.advanced;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;

/** The panel for an advanced video search. */
class AdvancedVideoPanel extends AdvancedPanel {

    public AdvancedVideoPanel(PropertyDictionary dictionary, FriendAutoCompleterFactory friendAutoCompleterFactory) {
        super(SearchCategory.VIDEO, friendAutoCompleterFactory);
        addField(I18n.tr("Title"), FilePropertyKey.TITLE);
        addField(I18n.tr("Type"), FilePropertyKey.GENRE, dictionary.getVideoGenres());
        addField(I18n.tr("Year"), FilePropertyKey.YEAR);
        addField(I18n.tr("Rating"), FilePropertyKey.RATING, dictionary.getVideoRatings());
    }
}
