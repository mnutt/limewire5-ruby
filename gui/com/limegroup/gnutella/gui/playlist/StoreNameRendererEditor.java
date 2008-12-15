package com.limegroup.gnutella.gui.playlist;

import java.awt.Color;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import com.limegroup.gnutella.gui.mp3.MediaPlayerComponent;
import com.limegroup.gnutella.gui.search.CompositeCellTableRendererAndTableCellEditor;
import com.limegroup.gnutella.gui.tables.DeletableCellEditor;

/**
 *  Creates both a renderer and an editor for cells in the playlist table that display the name
 *  of the file being played. When displaying a preview item from the LWS, buttons are
 *  displayed which enable the user to purchase the song directly from the playlist
 */
public class StoreNameRendererEditor extends CompositeCellTableRendererAndTableCellEditor implements
        DeletableCellEditor {
    
    /**
     * line containing information about the row being painted
     */
    private PlaylistDataLine line;
    
    public StoreNameRendererEditor(){
        super();
    }
    
    /**
     * Create two buttons in the playlist, a buy and a try button that reference store
     * actions. 
     */
    @Override
    protected AbstractAction[] createActions() {
        return new AbstractAction[] { 
           new AbstractAction("buy") {
            public void actionPerformed(ActionEvent e) {
                PlaylistMediator.getInstance().buyProduct(line);
            }
        }, new AbstractAction("try") {
            public void actionPerformed(ActionEvent e) {
                PlaylistMediator.getInstance().infoProduct(line);
            }
        } };
    }

    /**
     * Returns the default filename for this table line
     */
    @Override
    protected String getNameForValue(Object value) { 
        final StoreName rnh = (StoreName)value;
        this.line = rnh.getLine();
        return line.getSongName();
    }
     
    /**
     * Determines whether to paint the buttons for purchasing a song
     * This should only be true when the item it a LWS preview
     */
    @Override
    protected boolean buttonsVisible(){
        return line.isStoreSong();
    }
    
    /**
     * @return true if this PlayListItem is currently playing, false otherwise
     */
    protected boolean isPlaying(){
        return MediaPlayerComponent.getInstance().getCurrentSong() == line.getPlayListItem();
    }
    
    /**
     * Check what font color to use if this song is playing. 
     */
    @Override
    protected Color getFontColor(Color defaultColor) {
        if( line != null && isPlaying() )
            return line.getColor(true);
        else
            return defaultColor;
    }
    
    @Override
    public String toString() {
        return line.getSongName();
    }
}

