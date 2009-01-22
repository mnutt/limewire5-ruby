/**
 * 
 */
package org.limewire.core.api;

import java.util.Collection;
import java.util.HashSet;

public enum FilePropertyKey {
    TITLE,//for audio this is the track name
    AUTHOR,//for audio files this represents the artists name
    BITRATE, // ex. 128, 160, 192, 224, 320
    DESCRIPTION,
    COMPANY,//for video files this is the studio, for applications the publisher
    DATE_CREATED,
    FILE_SIZE, // in bytes
    GENRE,
    HEIGHT,
    LENGTH, // in seconds
    NAME,
    PLATFORM,
    QUALITY,
    RATING,
    TRACK_NUMBER,
    ALBUM,
    WIDTH,
    YEAR;
    
    private final static Collection<FilePropertyKey> indexableKeys = new HashSet<FilePropertyKey>(); 
    private final static Collection<FilePropertyKey> editableKeys = new HashSet<FilePropertyKey>(); 
    private final static Collection<FilePropertyKey> longKeys = new HashSet<FilePropertyKey>(); 

    
    static {
        indexableKeys.add(ALBUM);
        indexableKeys.add(TITLE);
        indexableKeys.add(AUTHOR);
        indexableKeys.add(DESCRIPTION);
        indexableKeys.add(COMPANY);
        indexableKeys.add(GENRE);
        indexableKeys.add(NAME);
        indexableKeys.add(PLATFORM);
    };

    static {
        editableKeys.add(ALBUM);
        editableKeys.add(AUTHOR);
        editableKeys.add(DESCRIPTION);
        editableKeys.add(COMPANY);
        editableKeys.add(GENRE);
        editableKeys.add(PLATFORM);
        editableKeys.add(TITLE);
        editableKeys.add(TRACK_NUMBER);
        editableKeys.add(YEAR);
        editableKeys.add(RATING);
    };
    
    static {
        longKeys.add(LENGTH);
        longKeys.add(YEAR);
        longKeys.add(WIDTH);  
        longKeys.add(TRACK_NUMBER);
        longKeys.add(BITRATE);
        longKeys.add(FILE_SIZE);
    };
    
    public static Collection<FilePropertyKey> getIndexableKeys() {
        return indexableKeys;
    }
    
    public static Collection<FilePropertyKey> getEditableKeys() {
        return editableKeys;
    }
    
    public static boolean isLong(FilePropertyKey key){
        return longKeys.contains(key);
    }
}

