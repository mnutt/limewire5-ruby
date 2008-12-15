package com.limegroup.gnutella.gui.logging;

import java.util.Date;

import javax.swing.Icon;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;


public final class LoggingDataLine extends AbstractDataLine<LogEvent> {

    /** icon column */
    static final int ICON_IDX = 0;
    private static final LimeTableColumn ICON_COLUMN =
        new LimeTableColumn(ICON_IDX, "LOGGING_COLUMN_ICON", I18n.tr("Icon"),
            GUIMediator.getThemeImage("question_mark"),
                    30, true, Icon.class);

    /** Message column */
    static final int MESSAGE_IDX = 1;
    private static final LimeTableColumn MESSAGE_COLUMN =
        new LimeTableColumn(MESSAGE_IDX, "LOGGING_COLUMN_MESSAGE", I18n.tr("Message"),
                        800, true, String.class);

    /** Time */
    static final int TIME_IDX = 2;
    private static final LimeTableColumn TIME_COLUMN =
        new LimeTableColumn(TIME_IDX, "LOGGING_COLUMN_TIME", I18n.tr("Time"),
                        200, true, Date.class);

    /** Total number of columns */
    static final int NUMBER_OF_COLUMNS = 3;

    /** Number of columns */
    public int getColumnCount() { return NUMBER_OF_COLUMNS; }    

    /** Returns the value for the specified index. */
    public Object getValueAt(int idx) {
        switch(idx) {
            case ICON_IDX:
                return initializer.getType().getIcon();
            case MESSAGE_IDX:
                return initializer.getMessage();
            case TIME_IDX:
                return initializer.getTime();
        }
        return null;
    }

	/** Return the table column for this index. */
	public LimeTableColumn getColumn(int idx) {
        switch(idx) {
            case ICON_IDX:      return ICON_COLUMN;
            case MESSAGE_IDX:   return MESSAGE_COLUMN;
            case TIME_IDX:      return TIME_COLUMN;
        }
        return null;
    }
    
    public boolean isClippable(int idx) {
        return true;
    }
    
    public int getTypeAheadColumn() {
        return MESSAGE_IDX;
    }

	public boolean isDynamic(int idx) {
        return false;
	}
}
