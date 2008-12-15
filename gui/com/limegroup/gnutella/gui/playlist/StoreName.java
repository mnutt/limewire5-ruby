package com.limegroup.gnutella.gui.playlist;

/**
 *  Wraps the current dataline to be displayed in the table to pass it
 *  to the {@link StoreNameRendererEditor}
 */
public class StoreName implements Comparable<Object>{
    
    /**
     * The current line to display in the table
     */
    private final PlaylistDataLine line;
    
    public StoreName(PlaylistDataLine line ){
        this.line = line;
    }
    
    /**
     * @return the current dataline 
     */
    public PlaylistDataLine getLine(){
        return line;
    }

    public int compareTo(Object o) {
        return line.getSongName().compareTo(((StoreName)o).line.getSongName() );
    }
}
