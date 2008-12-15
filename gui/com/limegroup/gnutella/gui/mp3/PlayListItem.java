package com.limegroup.gnutella.gui.mp3;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.limewire.util.CommonUtils;

import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.metadata.audio.AudioMetaData;

/**
 *  Wrapper for a local or remote audio file/stream that is to be added to the 
 *  playlist queue to be played by the local media player. Audio properties 
 *  such as artist, title, etc.. can also be passed in as a Map for audio 
 *  sources that may not contain META-TAG information
 */
public class PlayListItem implements Comparable<PlayListItem>{
    
    // Values to be used in the properties map
    public static final String ALBUM = "Album";
    public static final String ARTIST = "Artist";
    public static final String BITRATE = "BitRate";
    public static final String COMMENT = "Comment";
    public static final String GENRE = "Genre";
    public static final String LENGTH = "Length";
    public static final String TIME = "Time";
    public static final String SIZE = "Size";
    public static final String TITLE = "Title";
    public static final String TRACK = "Track";
    public static final String TYPE = "Type";
    public static final String YEAR = "Year";
    public static final String VBR = "VBR";
    
    /**
     * Location of the audio source
     */
    private final URI uri;
    
    /**
     * Audio Source to pass to the music player
     */
    private final AudioSource audioSource;
    
    /**
     * Default name of the audio source to display if no META information is available
     */
    private final String name;
    
    /**
     * A flag for determining if the audio source is on the local filesystem. 
     */
    private final boolean isLocal;
    
    /**
     * A flag for setting whether this audio source is a preview clip from the LimeWire store.
     * When this flag is set, a buy/info button is presented in the playlist for purchasing
     * this item
     */
    @SuppressWarnings("unused") 
    private final boolean isStorePreview;
    
    /**
     * A map of audio properties that can be displayed about this item. Audio clips from the store
     * for example, may not contain audio properties embeded in the Tag but are transfered in XML 
     * to the client.
     */
    private final Map<String,String> properties;
    
    /**
     * List of all meta data for this song in human readable form for display
     * as a tooltip.
     */
    private String[] toolTips;
    
    
    /**
     * Adds a local file to the playlist. Creation on a PlayListItem will
     * result in disk access to obtain the ID3 tag. This should never be
     * instantiated on the swing event queue.
     * 
     * @param file - file to be added
     */
    public PlayListItem(File file) {
        this( file.toURI(), new AudioSource(file), file.getName(), true);
    }
    
    /**
     * creates a playlistitem instance. Creation of a PlayListItem will
     * often result in disk access or possibly network access to obtain
     * the ID3 tag. This should never be instantiated on the swing event
     * queue
     * 
     * @param uri - location of the audio source
     * @param audioSource - source to pass to the music player
     * @param name - default name to display
     * @param isLocal - true if the source if a file on the local file system
     */
    public PlayListItem(URI uri, AudioSource audioSource, String name, boolean isLocal) {
        this(uri, audioSource, name, isLocal, false);
    }
    
    /**
     * creates a playlistitem instance. Creation of a PlayListItem will
     * often result in disk access or possibly network access to obtain
     * the ID3 tag. This should never be instantiated on the swing event
     * queue
     * 
     * @param url - location of the audio source
     * @param audioSource - source to pass to the music player
     * @param name - default name to display if no other information is available
     * @param isLocal - true if the source is a file and stored locally
     * @param isStorePreview - true if url is from LimeWire Store (LWS), sets a buy/info button
     *                  in the display
     */
    public PlayListItem(URI uri, AudioSource audioSource, String name, boolean isLocal, boolean isStorePreview){
        this(uri,audioSource, name, isLocal, isStorePreview, null);
    }
    
    /**
     * creates a playlistitem instance. Creation of a PlayListItem will
     * often result in disk access or possibly network access to obtain
     * the ID3 tag. This should never be instantiated on the swing event
     * queue
     * 
     * @param url - location of the audio source
     * @param audioSource - source to pass to the music player
     * @param name - default name to display if no other information is available
     * @param isLocal - true if the source is a file and stored locally
     * @param isStorePreview - true if url is from LimeWire Store (LWS), sets a buy/info button
     *                  in the display
     * @param properties - meta data about the song such as Artist, Album, Track, etc.
     *              The map is stored as a reference and not a copy so changes outside
     *              will be reflected in the playlist table
     */
    public PlayListItem(URI uri, AudioSource audioSource, String name, boolean isLocal, boolean isStorePreview,  
            Map<String,String> properties){
        if( uri == null || name == null )
            throw new IllegalArgumentException();
        this.uri = uri;
        this.audioSource = audioSource;
        this.name = name;
        this.isLocal = isLocal;
        this.isStorePreview = isStorePreview;
        if( properties == null )
            this.properties = new HashMap<String,String>();
        else
            this.properties = properties;
        
        // try and get any missing meta data
        initMetaData();
    }

    /**
     * If the song is file thats local, attempts to read the ID3 tag of the 
     * file. This updates any missing data in the properties map that
     * was not passed in to the constructor
     */
    private void initMetaData(){
        //if is a file, try to read ID3 tag to fill in missing meta data
        if( isLocal ){
            try {
                File file = new File(uri);
                AudioMetaData amd = (AudioMetaData) GuiCoreMediator.getMetaDataFactory().parse(file);

                // if it has no meta data we can read just return
                if(amd == null)
                    return;                
                if( !properties.containsKey(ARTIST))
                    properties.put(ARTIST, amd.getArtist());
                if( !properties.containsKey(ALBUM))
                    properties.put(ALBUM, amd.getAlbum());
                if( !properties.containsKey(BITRATE))
                    properties.put(BITRATE, Integer.toString(amd.getBitrate()));
                if( !properties.containsKey(COMMENT))
                    properties.put(COMMENT, amd.getComment());
                if( !properties.containsKey(GENRE) && amd.getGenre() != null &&
                        amd.getGenre().length() > 0)
                    properties.put(GENRE, amd.getGenre());
                if( !properties.containsKey(LENGTH))
                    properties.put(LENGTH, Integer.toString(amd.getLength()));
                if( !properties.containsKey(SIZE))
                    properties.put(SIZE, Long.toString(file.length()));
                if( !properties.containsKey(TITLE))
                    properties.put(TITLE, amd.getTitle());
                if( !properties.containsKey(TRACK)&& amd.getTrack() != null)
                    properties.put(TRACK, amd.getTrack());
                if( !properties.containsKey(TYPE))
                    properties.put(TYPE, file.getName().substring(file.getName().length() - 3));
                if( !properties.containsKey(YEAR) && amd.getYear() != null 
                        && amd.getYear().length() > 0)
                    properties.put(YEAR, amd.getYear());
                if( !properties.containsKey(TIME) && properties.containsKey(LENGTH) )
                    properties.put(TIME, CommonUtils.seconds2time(amd.getLength()));
                else
                    properties.put(TIME, "-1");
                if( !properties.containsKey(VBR))
                    properties.put(VBR, Boolean.toString(amd.isVBR()));
                
            } catch (IOException e) { //dont catch
            }
        }
        
    }
    
    /**
     * @return location of the audio source
     */
    public URI getURI(){
        return uri;
    }
    
    public AudioSource getAudioSource(){
        return audioSource;
    }
    
    /**
     * @return default name to display if not meta information 
     */
    public String getName(){
        return name;
    }
           
    /**
     * Looks up a property using a key
     * @param key
     * @return - the String representing the key, null if the key doesn't exist
     */
    public String getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Looks up a property using a key. If the key does not exist in the table,
     * returns the default value passed in.
     * 
     * @param key - key to search the map with
     * @param defaultKey - value to return if the key does not exist in map
     * @return
     */
    public String getProperty(String key, String defaultValue) {
        String value = properties.get(key);
        return value != null ? value : defaultValue;
    }
    
    /**
     * @return true if this is a file on the local filesystem
     */
    public boolean isFile(){
        return isLocal;
    }
    
    /**
     * @return true if this a streaming preview from the LimeWire store. 
     */
    public boolean isStorePreview(){
        // buttons for store songs are currently disabled till later
        return false;//return isStorePreview;
    }
    
    /**
     * Creates a tooltip that displays all the known meta data about this song
     * @return an array of string that contains readable meta data
     */
    public String[] getToolTips(){
        // lazy initializer, doesn't get called until the first request for
        //  a tooltip
        if( toolTips == null ) {
            List<String> allData = new LinkedList<String>();   
            allData.add(getName());
            if( properties.get(TITLE) != null )
                allData.add( I18n.tr("Title") + ": " + properties.get(TITLE));
            if( properties.get(ARTIST) != null )
                allData.add( I18n.tr("Artist") + ": " + properties.get(ARTIST));
            if( properties.get(ALBUM) != null )
                allData.add( I18n.tr("Album") + ": " + properties.get(ALBUM));
            if( properties.get(GENRE) != null )
                allData.add(I18n.tr("Genre") + ": " + properties.get(GENRE));
            if( properties.get(TRACK) != null )
                allData.add(I18n.tr("Track") + ": " + properties.get(TRACK));
            if( properties.get(YEAR) != null )
                allData.add(I18n.tr("Year") + ": " + properties.get(YEAR));
            if( properties.get(TIME) != null )
                allData.add(I18n.tr("Length") + ": " + properties.get(TIME));
            if( properties.get(BITRATE) != null )
                allData.add(I18n.tr("Bitrate") + ": " + properties.get(BITRATE) + " kbps" + (properties.get(VBR).equals("true") ? " (VBR)" : "") );
            
            toolTips = allData.toArray( new String[allData.size()]);
        }
        return toolTips;
    }

    public int compareTo(PlayListItem o) {
        return o.getURI().compareTo(uri);
    }
}
