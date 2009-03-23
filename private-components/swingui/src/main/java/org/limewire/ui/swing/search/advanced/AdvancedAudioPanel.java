package org.limewire.ui.swing.search.advanced;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;

/** The panel for advanced audio search. */
class AdvancedAudioPanel extends AdvancedPanel {

    public AdvancedAudioPanel(PropertyDictionary propertyDictionary, FriendAutoCompleterFactory friendAutoCompleterFactory) {
        super(SearchCategory.AUDIO, friendAutoCompleterFactory);
        addField(I18n.tr("Title"), FilePropertyKey.TITLE);
        addField(I18n.tr("Artist"), FilePropertyKey.AUTHOR);
        addField(I18n.tr("Album"), FilePropertyKey.ALBUM);
        addField(I18n.tr("Genre"), FilePropertyKey.GENRE, propertyDictionary.getAudioGenres());
        addField(I18n.tr("Track"), FilePropertyKey.TRACK_NUMBER);
        addField(I18n.tr("Year"), FilePropertyKey.YEAR);
        addField(I18n.tr("Bitrate"), FilePropertyKey.BITRATE);
    }

}
