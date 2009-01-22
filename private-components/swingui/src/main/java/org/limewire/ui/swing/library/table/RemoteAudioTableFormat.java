package org.limewire.ui.swing.library.table;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jdesktop.swingx.decorator.SortKey;
import org.jdesktop.swingx.decorator.SortOrder;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.ui.swing.table.ColumnStateInfo;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.FileUtils;

/**
 * Table format for the Audio Table for LW buddies and Browse hosts
 */
public class RemoteAudioTableFormat<T extends RemoteFileItem> extends AbstractRemoteLibraryFormat<T> {
    static final int NAME_INDEX = 0;
    static final int ARTIST_INDEX = 1;
    static final int ALBUM_INDEX = 2;
    static final int LENGTH_INDEX = 3;
    static final int QUALITY_INDEX = 4;
    static final int GENRE_INDEX = 5;
    static final int BITRATE_INDEX = 6;
    static final int SIZE_INDEX = 7;
    static final int TRACK_INDEX = 8;
    static final int YEAR_INDEX = 9;
    static final int FILENAME_INDEX = 10;
    static final int EXTENSION_INDEX = 11;
    static final int DESCRIPTION_INDEX = 12;
    
    public RemoteAudioTableFormat(ColumnStateInfo[] columnInfo) {
        super(columnInfo);
    }
    
    public RemoteAudioTableFormat() {
        super(new ColumnStateInfo[] {
                new ColumnStateInfo(NAME_INDEX, "REMOTE_LIBRARY_AUDIO_TITLE", I18n.tr("Name"), 278, true, true),     
                new ColumnStateInfo(ARTIST_INDEX, "REMOTE_LIBRARY_AUDIO_ARTIST", I18n.tr("Artist"), 120, true, true), 
                new ColumnStateInfo(ALBUM_INDEX, "REMOTE_LIBRARY_AUDIO_ALBUM", I18n.tr("Album"), 161, true, true), 
                new ColumnStateInfo(LENGTH_INDEX, "REMOTE_LIBRARY_AUDIO_LENGTH", I18n.tr("Length"), 60, true, true), 
                new ColumnStateInfo(QUALITY_INDEX, "REMOTE_LIBRARY_AUDIO_QUALITY", I18n.tr("Quality"), 80, true, true), 
                new ColumnStateInfo(GENRE_INDEX, "REMOTE_LIBRARY_AUDIO_GENRE", I18n.tr("Genre"), 60, false, true),
                new ColumnStateInfo(BITRATE_INDEX, "REMOTE_LIBRARY_AUDIO_BITRATE", I18n.tr("Bitrate"), 50, false, true), 
                new ColumnStateInfo(SIZE_INDEX, "REMOTE_LIBRARY_AUDIO_SIZE", I18n.tr("Size"), 60, false, true),
                new ColumnStateInfo(TRACK_INDEX, "REMOTE_LIBRARY_AUDIO_TRACK", I18n.tr("Track"), 50, false, true), 
                new ColumnStateInfo(YEAR_INDEX, "REMOTE_LIBRARY_AUDIO_YEAR", I18n.tr("Year"), 50, false, true), 
                new ColumnStateInfo(FILENAME_INDEX, "REMOTE_LIBRARY_AUDIO_FILENAME", I18n.tr("Filename"), 120, false, true),
                new ColumnStateInfo(EXTENSION_INDEX, "REMOTE_LIBRARY_AUDIO_EXTENSION", I18n.tr("Extension"), 60, false, true), 
                new ColumnStateInfo(DESCRIPTION_INDEX, "REMOTE_LIBRARY_AUDIO_DESCRIPTION", I18n.tr("Description"), 100, false, true)
        });
    }

    @Override
    public Object getColumnValue(T baseObject, int column) {
        switch(column) {
            case NAME_INDEX: return baseObject;
            case ARTIST_INDEX: return baseObject.getProperty(FilePropertyKey.AUTHOR);
            case ALBUM_INDEX: return baseObject.getProperty(FilePropertyKey.ALBUM);
            case LENGTH_INDEX: return baseObject.getProperty(FilePropertyKey.LENGTH);
            case QUALITY_INDEX: return baseObject;
            case GENRE_INDEX: return baseObject.getProperty(FilePropertyKey.GENRE);
            case BITRATE_INDEX: return baseObject.getProperty(FilePropertyKey.BITRATE);
            case SIZE_INDEX: return baseObject.getSize();
            case TRACK_INDEX: return baseObject.getProperty(FilePropertyKey.TRACK_NUMBER);
            case YEAR_INDEX: return baseObject.getProperty(FilePropertyKey.YEAR);
            case FILENAME_INDEX: return baseObject.getProperty(FilePropertyKey.NAME);
            case EXTENSION_INDEX: return FileUtils.getFileExtension(baseObject.getFileName());
            case DESCRIPTION_INDEX: return baseObject.getProperty(FilePropertyKey.DESCRIPTION);
        }
        throw new IllegalArgumentException("Unknown column:" + column);
    }
    
    @Override
    public List<SortKey> getDefaultSortKeys() {
        return Arrays.asList(
                new SortKey(SortOrder.ASCENDING, ARTIST_INDEX),
                new SortKey(SortOrder.ASCENDING, ALBUM_INDEX),
                new SortKey(SortOrder.ASCENDING, TRACK_INDEX),
                new SortKey(SortOrder.ASCENDING, NAME_INDEX));
    }

    @Override
    public List<Integer> getSecondarySortColumns(int column) {
        switch (column) {
        case ARTIST_INDEX:
            return Arrays.asList(ALBUM_INDEX, TRACK_INDEX, NAME_INDEX);
        case ALBUM_INDEX:
            return Arrays.asList(TRACK_INDEX, NAME_INDEX);
        default:
            return Collections.emptyList();
        }
    }
}
