package org.limewire.ui.swing.library.table;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;

/**
 * Table format for the Audio Table for All Friends table
 */
public class AllFriendAudioTableFormat<T extends RemoteFileItem> extends RemoteAudioTableFormat<T> {
    static final int FROM_INDEX = 13;
    
    public AllFriendAudioTableFormat() {
        super( new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "ALL_LIBRARY_AUDIO_TITLE", I18n.tr("Name"), 278, true, true),     
                new ColumnStateInfo(ARTIST_INDEX, "ALL_LIBRARY_AUDIO_ARTIST", I18n.tr("Artist"), 166, true, true), 
                new ColumnStateInfo(ALBUM_INDEX, "ALL_LIBRARY_AUDIO_ALBUM", I18n.tr("Album"), 147, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "ALL_LIBRARY_AUDIO_LENGTH", I18n.tr("Length"), 58, true, true), 
                new ColumnStateInfo(QUALITY_INDEX, "ALL_LIBRARY_AUDIO_QUALITY", I18n.tr("Quality"), 67, true, true), 
                new ColumnStateInfo(GENRE_INDEX, "ALL_LIBRARY_AUDIO_GENRE", I18n.tr("Genre"), 60, false, true),
                new ColumnStateInfo(BITRATE_INDEX, "ALL_LIBRARY_AUDIO_BITRATE", I18n.tr("Bitrate"), 50, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "ALL_LIBRARY_AUDIO_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(TRACK_INDEX, "ALL_LIBRARY_AUDIO_TRACK", I18n.tr("Track"), 50, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "ALL_LIBRARY_AUDIO_YEAR", I18n.tr("Year"), 50, false, true), 
                new ColumnStateInfo(FILENAME_INDEX, "ALL_LIBRARY_AUDIO_FILENAME", I18n.tr("Filename"), 120, false, true),
                new ColumnStateInfo(EXTENSION_INDEX, "ALL_LIBRARY_AUDIO_EXTENSION", I18n.tr("Extension"), 60, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "ALL_LIBRARY_AUDIO_DESCRIPTION", I18n.tr("Description"), 100, false, true),
                new ColumnStateInfo(FROM_INDEX, "ALL_LIBRARY_AUDIO_FROM", I18n.tr("From"), 82, true, true)
        });
    }
    
    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case FROM_INDEX: return baseObject;
        }
        return super.getColumnValue(baseObject, column);
    }
}
