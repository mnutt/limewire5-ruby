package com.limegroup.gnutella.templates;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.limewire.gnutella.tests.LimeTestCase;

import junit.framework.Test;


public class StoreSubDirectoryTemplateProcessorTest extends LimeTestCase {

    public StoreSubDirectoryTemplateProcessorTest(String name) {
        super(name);
    }
    
    public static Test suite() { 
        return buildTestSuite(StoreSubDirectoryTemplateProcessorTest.class);
    }
    
    private final static Map<String,String> SUBSTITUTIONS = new HashMap<String,String>();
    private static final String ALBUM       = "album";
    private static final String ARTIST      = "artist";
    private static final String ALBUM_VAR   = "<" + StoreTemplateProcessor.ALBUM_LABEL +   ">";
    private static final String ARTIST_VAR  = "<" + StoreTemplateProcessor.ARTIST_LABEL +  ">";

    private static final File OUTDIR = new File(".");
    static {
        SUBSTITUTIONS.put(StoreTemplateProcessor.ALBUM_LABEL,   ALBUM);
        SUBSTITUTIONS.put(StoreTemplateProcessor.ARTIST_LABEL,ARTIST);
    }
    
    public void testNull() {
        runTestInternal(null, new File("."));
    }
    
    public void testEmpty() {
        runTestInternal("", new File("."));
    }
    
    public void testA() {
        runTestInternal("a", new File(OUTDIR,"a"));
    }
    
    public void testASlashB() {
        runTestInternal("a\b", new File(OUTDIR,"a\b"));
    }
    
    public void testArtistSub() { 
        runTestInternal(ARTIST_VAR, new File(OUTDIR,ARTIST));
    }
    
    public void testAArtistSub() {
        runTestInternal("A" + ARTIST_VAR, new File(OUTDIR,"A" + ARTIST));
    }
    
    public void testArtistSubA() {
        runTestInternal(ARTIST_VAR + "A", new File(OUTDIR,ARTIST + "A"));
    }
    
    public void testAArtistSubA() {
        runTestInternal("A" + ARTIST_VAR + "A", new File(OUTDIR,"A" + ARTIST + "A"));
    }
    
    public void testAlbumSub() {
        runTestInternal(ALBUM_VAR, new File(OUTDIR,ALBUM));
    }

    private void runTestInternal(final String template, final File want) {
        try {
            final File have = new StoreSubDirectoryTemplateProcessor().getOutputDirectory(template, SUBSTITUTIONS, OUTDIR); 
            assertEquals(template + ":" + want + " != " + have, want, have);
        } catch (StoreSubDirectoryTemplateProcessor.IllegalTemplateException e) {
            fail(e.getMessage());
        }
    }
        
}
