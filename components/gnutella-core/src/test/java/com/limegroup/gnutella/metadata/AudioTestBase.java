package com.limegroup.gnutella.metadata;

import java.io.IOException;

import org.limewire.gnutella.tests.LimeTestCase;

import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 * Not actually a runnable test. Creates a convienence test for testing meta-data.
 * With the new meta-data reader, all classes can use the same interface allowing them
 * all to be tested with the same test case as well.
 */
public abstract class AudioTestBase extends LimeTestCase {

    String dir = "com/limegroup/gnutella/metadata/";
    
    protected static final String TITLE = "title test";
    protected static final String ARTIST = "artist test";
    protected static final String ALBUM = "album test";
    protected static final String COMMENT = "comment test";
    protected static final String TRACK = "1";
    protected static final String YEAR = "2007";
    protected static final String GENRE = "Bass";
    
    public AudioTestBase(String name) {
        super(name);
    }
    
    /**
     * Tests the ID3 tag for the audio file
     */
    protected void validateTag(AudioMetaData data) throws IOException {    
        assertEquals(TITLE, data.getTitle());
        assertEquals(ARTIST, data.getArtist());
        assertEquals(ALBUM, data.getAlbum());
        assertEquals(COMMENT, data.getComment());
        assertEquals(TRACK, data.getTrack());
        assertEquals(YEAR, data.getYear());
        assertEquals(GENRE, data.getGenre());
    }

}
