package com.limegroup.gnutella.gui.playlist;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.limegroup.gnutella.gui.mp3.PlayList;
import com.limegroup.gnutella.gui.mp3.PlayListItem;
import com.limegroup.gnutella.gui.tables.BasicDataLineModel;


/**
 * A model for playlists.  Keeps track of what song is next,
 * what has/hasn't been played during shuffling, etc...
 */
public final class PlaylistModel extends BasicDataLineModel<PlaylistDataLine, PlayListItem> {

    /**
     * Whether or not songs are shuffling.
     */
    private boolean _shuffle = false;
    
    /**
     * Indicates that the next song to return is before
     * the current one.
     */
    private boolean _nextIsBefore = false;
    
    /**
     * The currently playing song.
     */
    private PlayListItem _currentSong;
    
    /**
     * Songs already played -- used for shuffling.
     */
    private List<PlayListItem> _songsNotPlayed;
        
    /**
     * Constructs a new playlist model.
     */
    PlaylistModel() {
        super( PlaylistDataLine.class );
    }
    
    /**
     * Quickly updates all instead of iterating & calling udpate on each one.
     * @return null
     */
    @Override
    public Object refresh() {
        fireTableRowsUpdated(0, getRowCount());
        return null;
    }
    
    /**
     * Creates a new ConnectionDataLine
     */
    @Override
    public PlaylistDataLine createDataLine() {
        return new PlaylistDataLine();
    }
    
    /**
     * Override default so new ones get added to the end
     */
    @Override
    public int add(PlayListItem o) {
        return add(o, getRowCount());
    }
    
    /**
     * If shuffling, adds to the list of songs not played.
     */
    @Override
    public int add(PlaylistDataLine dl, int row) {
        if(_shuffle) {
            _songsNotPlayed.add(dl.getInitializeObject());
            Collections.shuffle(_songsNotPlayed);
        }
        return super.add(dl, row);
    }
    
    /**
     * If shuffling, removes the song from the songs not played.
     */
    @Override
    public void remove(int i) {
        PlayListItem f = get(i).getInitializeObject();
        super.remove(i);
        if(_shuffle)
            _songsNotPlayed.remove(f);
    }
    
    /**
     * If shuffling, clears the songs not played.
     */
    @Override
    public void clear() {    
        super.clear();
        if(_shuffle)
            _songsNotPlayed.clear();
    }
    
    /**
     * Gets the next song to play.
     */
    PlayListItem getNextSong() {
        int rowCount = getRowCount();
        
        if(rowCount == 0)
            return null;
            
        boolean prior = _nextIsBefore;
        _nextIsBefore = false;

        if(_shuffle) {
            //fill up songs not played if empty
            if(_songsNotPlayed.isEmpty()) {
                for(int i = 0; i < rowCount; i++)
                    _songsNotPlayed.add(get(i).getInitializeObject());
                Collections.shuffle(_songsNotPlayed);
            }
            _currentSong = _songsNotPlayed.remove(0);
        } else {
            int idx = getRow(_currentSong);
            // if we haven't played anything, alway get the first one.
            if(idx == -1)
                idx = 0;
            else if(prior)
                idx = (idx - 1 + rowCount) % rowCount;
            else
                idx = (idx + 1 + rowCount) % rowCount;
            _currentSong = get(idx).getInitializeObject();
        }
        
        return _currentSong;
    }
    
    /**
     * Sets the currently playing song.
     */
    void setCurrentSong(PlayListItem f) {
        if(_shuffle)
            _songsNotPlayed.remove(f);
        _currentSong = f;
    }
    
    /**
     * Gets the index of the currently playing song.
     */
    int getCurrentSongIndex() {
        return getRow(_currentSong);
    }
    
    /**
     * Returns the list of all songs.
     */
    List<PlayListItem> getSongs() {
        List<PlayListItem> l = new LinkedList<PlayListItem>();
        for(int i = 0; i < getRowCount(); i++) 
            l.add(get(i).getInitializeObject());
        return l;
    }
    
    /**
     * @return a list of playlist items which are local files
     */
    List<PlayListItem> getLocalFiles() {
        List<PlayListItem> l = new LinkedList<PlayListItem>();
        for(int i = 0; i < getRowCount(); i++)
            if( get(i).getPlayListItem().isFile() )
                l.add(get(i).getPlayListItem());
        return l;
    }
    
    /**
     * Notification that the next song we want to play is BEFORE the current
     * song.
     */
    void setBackwardsMode() {
        _nextIsBefore = true;
    }
    
    /**
     * Adds all songs from the playlist.
     */
    void addSongs(PlayList list) {
        unsort();
        List<PlayListItem> songs = list.getSongs();
        for(PlayListItem item : songs)
            add(item);
    }
    
    /**
     * Sets whether or not shuffle is active.
     */
    void setShuffle(boolean shuffle) {
        if(shuffle)
            _songsNotPlayed = new LinkedList<PlayListItem>();
        else
            _songsNotPlayed = null;
        _shuffle = shuffle;
    }
    
    /**
	 *	Return true if shuffle is set, false otherwise
	 */
    public boolean isShuffleSet(){
        return _shuffle;
    }
    
    /**
     * Determines if a given cell should be passed to a cell editor or not.
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        // we want to allow rows that are names to be editable
        // so that we can forward the mouse events to the buttons if there
        //  are any
        //
        return 
            col == PlaylistDataLine.NAME_IDX;
    }
}