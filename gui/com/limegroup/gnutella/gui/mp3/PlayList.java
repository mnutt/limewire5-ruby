package com.limegroup.gnutella.gui.mp3;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.FileUtils;


/** 
 * Ecapsulates an audio playlist (.m3u).  Thread-safe.
 */
public class PlayList {
    
    private static final Log LOG = LogFactory.getLog(PlayList.class);

    /**
     * Used when reading/writing playlist to backing store.
     */
    private File playListFile;

    /**
     * Contains the list of PlayListItems in the song
     */
    private Vector<PlayListItem> songs;
    
    /**
     * Whether or not a a song has changed in the list after the last save.
     */
    private boolean isDirty;

    /**
     * Creates a PlayList accessible from the given filename.
     * If the File 'filename' exists, the playlist is loaded from that file.
     */
    public PlayList(String filename) {
        LOG.trace("PlayList(): entered.");

        playListFile = new File(filename);
        if (playListFile.isDirectory())
            throw new IllegalArgumentException(filename + " is a directory");

        songs = new Vector<PlayListItem>();
        if (playListFile.exists()) {
            try {
                loadM3UFile(); // load the playlist entries....
            } catch(IOException ignored) {
                LOG.warn("Unable to load file: " + filename, ignored);
            }
        }
        
        if(LOG.isTraceEnabled()) {
            LOG.trace("songs = " + songs);
            LOG.trace("returning.  size is now " + getNumSongs());
        }
    }

    private static final String M3U_HEADER = "#EXTM3U";
    private static final String SONG_DELIM = "#EXTINF";
    private static final String SEC_DELIM  = ":";

    /**
     * @exception IOException Thrown if load failed.<p>
     *
     * Format of playlist (.m3u) files is:<br>
     * ----------------------<br>
     * #EXTM3U<br>
     * #EXTINF:numSeconds<br>
     * /path/of/file/1<br>
     * #EXTINF:numSeconds<br>
     * /path/of/file/2<br>
     * ----------------------<br>
     */
    private void loadM3UFile() throws IOException {
        BufferedReader m3uFile = null;
        try {
            m3uFile = new BufferedReader(new FileReader(playListFile));
            String currLine = null;
            currLine = m3uFile.readLine();
            if (currLine == null || !(currLine.startsWith(M3U_HEADER) || currLine.startsWith(SONG_DELIM)))
                throw new IOException();
            if(currLine.startsWith(M3U_HEADER))
                currLine = m3uFile.readLine();
            
            for (; currLine != null; currLine = m3uFile.readLine()) {
                if (currLine.startsWith(SONG_DELIM)) {
                    currLine = m3uFile.readLine();
                    if(currLine == null)
                        break;
                    File toAdd = new File(currLine);
                    if (toAdd.exists() && !toAdd.isDirectory())
                        songs.add( new PlayListItem(toAdd));
                    else {
                        // try relative path to the playlist
                        toAdd = new File(playListFile.getParentFile().getAbsolutePath(), toAdd.getPath());
                        if (toAdd.exists() && !toAdd.isDirectory() && 
                                FileUtils.isReallyInParentPath(playListFile.getParentFile(), toAdd))
                            songs.add(new PlayListItem(toAdd));
                    }
                }
            }
        } finally {
            IOUtils.close(m3uFile);
        }
    }

    /**
     * Call this when you want to save the contents of the playlist.
     * NOTE: only local files can be saved in M3U format, filters out URLs 
     * that are not part of the local filesystem
     * @exception IOException Throw when save failed.
     * @exception IOException Throw when save failed.
     */
    public synchronized void save() throws IOException {
        if(!isDirty)
            return;
        
        // if all songs are new, just get rid of the old file.  this may
        // happen if a delete was done....
        if (songs.size() == 0) {
            LOG.debug("No songs, deleting file");
            if (playListFile.exists())
                playListFile.delete();
            return;
        }

        PrintWriter m3uFile = null;
        try {
            m3uFile = new PrintWriter(
                        new FileWriter(playListFile.getCanonicalPath(), false)
                      );

            m3uFile.write(M3U_HEADER);
            m3uFile.println();
            
            for(PlayListItem currFile : songs) {
                // only save files that are local to the file system
                if( currFile.isFile() ){
                    File locFile;
                    locFile = new File( currFile.getURI() );

                    // first line of song description...
                    m3uFile.write(SONG_DELIM);
                    m3uFile.write(SEC_DELIM);
                    // try to write out seconds info....
                    if( currFile.getProperty(PlayListItem.LENGTH) != null )
                        m3uFile.write("" + currFile.getProperty(PlayListItem.LENGTH) + ",");
                    else
                        m3uFile.write("" + -1 + ",");
                    m3uFile.write(currFile.getName());
                    m3uFile.println();
                    // canonical path follows...
                    m3uFile.write(locFile.getCanonicalPath());
                    m3uFile.println();
                }
            }
        } finally {
            isDirty = false;
            IOUtils.close(m3uFile);
        }
    }

    /**
     * Get the total number of songs in current playlist, including those
     * that were recently added.
     */
    public int getNumSongs() {
        return songs.size();
    }
    
    /**
     * Gets all songs in the list.
     */
    public synchronized List<PlayListItem> getSongs() {
        return new LinkedList<PlayListItem>(songs);
    }
    
    /**
     * Sets all the active songs.
     */
    public synchronized void setSongs(List<? extends PlayListItem> l) {
        isDirty = true;
        songs.clear();
        songs.addAll(l);
    }

    /**
     * Get a reference to the PlayListItem at the indicated index in the playlist.
     */
    public PlayListItem getSong(int index) {
        return songs.get(index);
    }
}