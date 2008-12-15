package com.limegroup.gnutella.templates;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import com.limegroup.gnutella.templates.StoreTemplateProcessor.IllegalTemplateException;
import com.limegroup.gnutella.util.LimeTestCase;

public class StoreFileNameTemplateProcessorTest extends LimeTestCase {

    public StoreFileNameTemplateProcessorTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(StoreFileNameTemplateProcessorTest.class);
    }
    
    private static final String ALBUM       = "album";
    private static final String ARTIST      = "artist";
    private static final String TITLE       = "title";
    private static final String TRACK       = "track";
    private static final String ALBUM_VAR   = "<" + StoreTemplateProcessor.ALBUM_LABEL  +   ">";
    private static final String ARTIST_VAR  = "<" + StoreTemplateProcessor.ARTIST_LABEL +   ">";
    private static final String TITLE_VAR   = "<" + StoreTemplateProcessor.TITLE_LABEL  +   ">";
    private static final String TRACK_VAR   = "<" + StoreTemplateProcessor.TRACK_LABEL  +   ">";
    
    private final static Map<String,String> SUBSTITUTIONS = new HashMap<String,String>();
    
    static {
        SUBSTITUTIONS.put(StoreTemplateProcessor.ALBUM_LABEL,   ALBUM);
        SUBSTITUTIONS.put(StoreTemplateProcessor.ARTIST_LABEL,  ARTIST);
        SUBSTITUTIONS.put(StoreTemplateProcessor.TITLE_LABEL,   TITLE);
        SUBSTITUTIONS.put(StoreTemplateProcessor.TRACK_LABEL,   TRACK);
    }
    
    public void testNull(){
        try {
            new StoreFileNameTemplateProcessor().getFileName(null, SUBSTITUTIONS);
            fail();
        } catch (IllegalTemplateException e) {
        }
    }

    public void testEmpty(){
        runTest("", "", SUBSTITUTIONS);
    }
    
    public void testAlbum(){
        runTest(ALBUM_VAR, ALBUM, SUBSTITUTIONS);
    }
    
    public void testArtist(){
        runTest(ARTIST_VAR, ARTIST, SUBSTITUTIONS);
    }
    
    public void testTitle(){
        runTest(TITLE_VAR, TITLE, SUBSTITUTIONS);
    }
    
    public void testArtistTitle(){
        runTest(ARTIST_VAR + " - " + TITLE_VAR, ARTIST + " - " + TITLE, SUBSTITUTIONS);
    }
    
    public void testArtistTitleAlbumTrack(){
        runTest(ARTIST_VAR + " - " + ALBUM_VAR + " - " + TRACK_VAR + " - " + TITLE_VAR, 
                ARTIST + " - " + ALBUM + " - " + TRACK + " - " + TITLE, SUBSTITUTIONS);
    }
    
    public void runTest(String template, String correctOutput, Map<String,String> subs) {
        try {
            String fileName = new StoreFileNameTemplateProcessor().getFileName(template, SUBSTITUTIONS);
            assertEquals(correctOutput, fileName);
        } catch (IllegalTemplateException e) {
            fail();
        }
    }

}
