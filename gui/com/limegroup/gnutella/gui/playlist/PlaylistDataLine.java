package com.limegroup.gnutella.gui.playlist;

import java.awt.Color;
import java.io.File;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.dnd.FileTransfer;
import com.limegroup.gnutella.gui.mp3.MediaPlayerComponent;
import com.limegroup.gnutella.gui.mp3.PlayListItem;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.ColoredCell;
import com.limegroup.gnutella.gui.tables.ColoredCellImpl;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.SizeHolder;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;


public final class PlaylistDataLine extends AbstractDataLine<PlayListItem>
                                    implements FileTransfer {

    /**
     * Index Column
     */
    static final int NUMBER_IDX = 0;
    private static final LimeTableColumn NUMBER_COLUMN =
        new LimeTableColumn(NUMBER_IDX, "", I18n.tr("#"),
                30, true, NumberCell.class);
    
    /**
     * Name column
     */
    static final int NAME_IDX = 1;
    private static final LimeTableColumn NAME_COLUMN =
        new LimeTableColumn(NAME_IDX, "PLAYLIST_TABLE_NAME", I18n.tr("Name"),
                700, true, StoreName.class);
    
    /**
     * Artist column
     */
    static final int ARTIST_IDX = 2;
    private static final LimeTableColumn ARTIST_COLUMN =
        new LimeTableColumn(ARTIST_IDX, "PLAYLIST_TABLE_ARTIST", I18n.tr("Artist"),
                 80, false, ColoredCell.class);
    /**
     * Title column
     */
    static final int TITLE_IDX = 3;
    private static final LimeTableColumn TITLE_COLUMN =
        new LimeTableColumn(TITLE_IDX, "PLAYLIST_TABLE_TITLE", I18n.tr("Title"),
                 80, false, ColoredCell.class);
    
    /**
     * Length column (in hour:minutes:seconds format)
     */
    static final int LENGTH_IDX = 4;
    private static final LimeTableColumn LENGTH_COLUMN =
        new LimeTableColumn(LENGTH_IDX, "PLAYLIST_TABLE_LENGTH", I18n.tr("Length"),
                150, true, ColoredCell.class);
    
    /**
     * Album column
     */
    static final int ALBUM_IDX = 5;
    private static final LimeTableColumn ALBUM_COLUMN =
        new LimeTableColumn(ALBUM_IDX, "PLAYLIST_TABLE_ALBUM", I18n.tr("Album"),
                120, false, ColoredCell.class);
       
    /**
     * Track column
     */
    static final int TRACK_IDX = 6;
    private static final LimeTableColumn TRACK_COLUMN =
        new LimeTableColumn(TRACK_IDX, "PLAYLIST_TABLE_TRACK", I18n.tr("Track"),
                20, false, ColoredCell.class);
    
    
    /**
     * Bitrate column info
     */
    static final int BITRATE_IDX = 7;
    private static final LimeTableColumn BITRATE_COLUMN =
        new LimeTableColumn(BITRATE_IDX, "PLAYLIST_TABLE_BITRATE",I18n.tr("Bitrate"),
                60, true, ColoredCell.class);
    
    /**
     * Comment column info
     */
    static final int COMMENT_IDX = 8;
    private static final LimeTableColumn COMMENT_COLUMN =
        new LimeTableColumn(COMMENT_IDX, "PLAYLIST_TABLE_COMMENT", I18n.tr("Comment"),
                20, false, ColoredCell.class);
    
    /**
     * Genre column
     */
    static final int GENRE_IDX = 9;
    private static final LimeTableColumn GENRE_COLUMN =
        new LimeTableColumn(GENRE_IDX, "PLAYLIST_TABLE_GENRE", I18n.tr("Genre"),
                 80, false, ColoredCell.class);
           
    /**
     * Size column (in bytes)
     */
    static final int SIZE_IDX = 10;
    private static final LimeTableColumn SIZE_COLUMN =
        new LimeTableColumn(SIZE_IDX, "PLAYLIST_TABLE_SIZE", I18n.tr("Size"),
                80, false, ColoredCell.class);
    
    
    /**
     * TYPE column
     */
    static final int TYPE_IDX = 11;
    private static final LimeTableColumn TYPE_COLUMN = 
        new LimeTableColumn(TYPE_IDX, "PLAYLIST_TABLE_TYPE", I18n.tr("Type"),
                 40, false, ColoredCell.class);
    
    /**
     * YEAR column
     */
    static final int YEAR_IDX = 12;
    private static final LimeTableColumn YEAR_COLUMN =
        new LimeTableColumn(YEAR_IDX, "PLAYLIST_TABLE_YEAR", I18n.tr("Year"),
                 30, false, ColoredCell.class);
    
    /**
     * Total number of columns
     */
    static final int NUMBER_OF_COLUMNS = 13;

    /**
     * Number of columns
     */
    public int getColumnCount() { return NUMBER_OF_COLUMNS; }
       
	/**
	 * The colors for cells.
	 */
	private Color _cellColor;
	private Color _othercellColor;    

    /**
     * Holder for painting the filename/buttons in a single cell
     */
    private StoreName name;
    
    /**
     *  Coverts the size of the PlayListItem into readable form postfixed with
     *  Kb or Mb
     */
    private SizeHolder holder;
    
    /**
     * Place holder for writing the row number in each row. The cell renderer
     * looks up dynamically what row of the table each cell is in so no information
     * is passed into the NumberCell. This is simply a place holder so the correct
     * CellRenderer is called by the paint manager
     */
    private static NumberCell numberCell = new NumberCell();

    /**
     * Sets up the dataline for use with the playlist.
     */
    @Override
    public void initialize(PlayListItem item) {
        super.initialize(item);

        name = new StoreName(this);
        if(item.getProperty(PlayListItem.SIZE) != null )
            holder = new SizeHolder(Integer.parseInt(item.getProperty(PlayListItem.SIZE)));
        else
            holder = new SizeHolder(0);
        
        updateTheme();
    }

    /**
     * Returns the value for the specified index.
     */
    public Object getValueAt(int idx) {
        Color color = getColor(MediaPlayerComponent.getInstance().getCurrentSong() == initializer);
        switch(idx) {
            case ALBUM_IDX:
                return new ColoredCellImpl( initializer.getProperty(PlayListItem.ALBUM), color);
            case ARTIST_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.ARTIST), color);
            case BITRATE_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.BITRATE, ""), color);
            case COMMENT_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.COMMENT), color);
            case GENRE_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.GENRE),color);
            case LENGTH_IDX:
                return new ColoredCellImpl( initializer.getProperty(PlayListItem.TIME, ""), color);
            case NAME_IDX:
                return name;
            case NUMBER_IDX:
                return numberCell;
            case SIZE_IDX:
                return new ColoredCellImpl(holder, color);
            case TITLE_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.TITLE), color);
            case TRACK_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.TRACK, ""), color);
            case TYPE_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.TYPE), color);
            case YEAR_IDX:
                return new ColoredCellImpl(initializer.getProperty(PlayListItem.YEAR), color);
        }
        return null;
    }
    
    /**
     * Gets the color for whether or not the row is playing.
     */
    protected Color getColor(boolean playing) {
        return playing ? _othercellColor : _cellColor;
    }
    
	// inherit doc comment
	public void updateTheme() {
		_cellColor = ThemeFileHandler.WINDOW8_COLOR.getValue();
		_othercellColor = ThemeFileHandler.SEARCH_RESULT_SPEED_COLOR.getValue();
	}    

	/**
	 * Return the table column for this index.
	 */
	public LimeTableColumn getColumn(int idx) {
        switch(idx) {
            case ALBUM_IDX:         return ALBUM_COLUMN;
            case ARTIST_IDX:        return ARTIST_COLUMN;
            case BITRATE_IDX:       return BITRATE_COLUMN;
            case COMMENT_IDX:       return COMMENT_COLUMN;
            case GENRE_IDX:         return GENRE_COLUMN;
            case LENGTH_IDX:        return LENGTH_COLUMN;
            case NAME_IDX:          return NAME_COLUMN;
            case NUMBER_IDX:        return NUMBER_COLUMN;
            case SIZE_IDX:          return SIZE_COLUMN;
            case TITLE_IDX:         return TITLE_COLUMN;
            case TRACK_IDX:         return TRACK_COLUMN;
            case TYPE_IDX:          return TYPE_COLUMN;
            case YEAR_IDX:          return YEAR_COLUMN;
        }
        return null;
    }
    
    public boolean isClippable(int idx) {
        return false;
    }
    
    public int getTypeAheadColumn() {
        return NAME_IDX;
    }

	public boolean isDynamic(int idx) {
	    return false;
	}
	
    /**
     * @return an instance of the file referenced by the URI or null
     *      if the URI is not on the local file system
     */
	public File getFile() {
        if( initializer.isFile() ){
            return new File(initializer.getURI());
        }
        return null;
    }
    
    /**
     * @return the PlayListItem for this table row
     */
    public PlayListItem getPlayListItem() {
	    return initializer;
	}
    
    /**
     * @return true if this is a preview of a song from the LWS
     */
    public boolean isStoreSong() {
        return initializer.isStorePreview();
    }
    
    /**
     * @return the file name of the song
     */
    public String getSongName() {
        return initializer.getName();
    }
    
    /**
     * Creates a tool tip for each row of the playlist. Tries to grab any information
     * that was extracted from the Meta-Tag or passed in to the PlaylistItem as 
     * a property map
     */
    @Override
    public String[] getToolTipArray(int col) {
        return initializer.getToolTips();
    }
}
