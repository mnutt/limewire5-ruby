package com.limegroup.gnutella.gui.mp3;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;


public class PlayListItemTest extends BaseTestCase {

    public PlayListItemTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(PlayListItemTest.class);
    }
    
    public void testItem() throws URISyntaxException{
        URL url = null;
        try {
            url = new URL("http://test.txt");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        assert(url != null );
        URI uri = url.toURI();

        PlayListItem item = new PlayListItem(uri, new AudioSource(url),"test", false, false);
        
        assertEquals(item.getURI(), uri);
        assertEquals(item.getName(), "test");
        assertEquals(item.isFile(), false);
        assertEquals(item.isStorePreview(), false);
        assertEquals(item.getProperty(PlayListItem.ARTIST), null);
        assertEquals(item.getProperty(PlayListItem.BITRATE), null);
    }
    
    public void testConstructor() throws URISyntaxException{
        URL url = null;
        try {
            url = new URL("http://test.txt");
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        String album = "album";
        String artist = "artist";
        String bitrate = "128";
        String comment = "comments";
        String genre = "genre";
        String length = "min:sec";
        String size = "200000";
        String title = "title";
        String track = "3";
        String type = "mp3";
        String year = "2000";
        
        
        Map<String,String> map = new HashMap<String,String>();
        map.put(PlayListItem.ALBUM, album);
        map.put(PlayListItem.ARTIST, artist);
        map.put(PlayListItem.BITRATE, bitrate);
        map.put(PlayListItem.COMMENT, comment);
        map.put(PlayListItem.GENRE, genre);
        map.put(PlayListItem.LENGTH, length);
        map.put(PlayListItem.SIZE, size);
        map.put(PlayListItem.TITLE, title);
        map.put(PlayListItem.TRACK, track);
        map.put(PlayListItem.TYPE, type);
        map.put(PlayListItem.YEAR, year);
        
        assert(url != null );
        URI uri = url.toURI();
        
        PlayListItem item = new PlayListItem(uri, new AudioSource(url),"test", false, false, map);
        
        assertEquals(item.getURI(), uri);
        assertEquals(item.getName(), "test");
        assertEquals(item.isStorePreview(), false);
        assertEquals(item.isFile(), false);

        assertEquals(item.getProperty(PlayListItem.ALBUM),album);
        assertEquals(item.getProperty(PlayListItem.ARTIST),artist);
        assertEquals(item.getProperty(PlayListItem.BITRATE),bitrate);
        assertEquals(item.getProperty(PlayListItem.COMMENT),comment);
        assertEquals(item.getProperty(PlayListItem.GENRE),genre);
        assertEquals(item.getProperty(PlayListItem.LENGTH),length);
        assertEquals(item.getProperty(PlayListItem.SIZE),size);
        assertEquals(item.getProperty(PlayListItem.TITLE),title);
        assertEquals(item.getProperty(PlayListItem.TRACK),track);
        assertEquals(item.getProperty(PlayListItem.TYPE),type);
        assertEquals(item.getProperty(PlayListItem.YEAR),year);
    }

}
