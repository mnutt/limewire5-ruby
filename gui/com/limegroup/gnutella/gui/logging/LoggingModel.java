package com.limegroup.gnutella.gui.logging;

import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.gui.tables.BasicDataLineModel;

public final class LoggingModel extends BasicDataLineModel<LoggingDataLine, LogEvent> {
        
    /** Used when the sorting changes off of time. */
    private List<LoggingDataLine> timeSortedList = null;
    
    LoggingModel() {
        super( LoggingDataLine.class );
    }
    
    /**
     * Override default update since we don't cache anything.
     * Specifically, we do NOT want to call update on the DataLine,
     * since that will make it think it's no longer connecting.
     * @return null
     */
    @Override
    public Object refresh() {
        fireTableRowsUpdated(0, getRowCount());
        return null;
    }
    
    /** Creates a new LoggingDataLine */
    @Override
    public LoggingDataLine createDataLine() {
        return new LoggingDataLine();
    }
    
    /** Override default so new ones get added to the end */
    @Override
    public int add(LogEvent o) {
        return add(o, getRowCount());
    }
    
    void removeOldestTime() {
        if(timeSortedList != null) {
            LoggingDataLine o = timeSortedList.remove(0);
            super.remove(o);
        } else {
            super.remove(0);
        }   
    }
    
    @Override
    public void remove(int row) {
        if(timeSortedList != null) {
            LoggingDataLine line = get(row);
            timeSortedList.remove(line);
        }
        
        super.remove(row);
    }

    @Override
    public int add(LoggingDataLine dl, int row) {
        if(timeSortedList != null)
            timeSortedList.add(dl);
        return super.add(dl, row);
    }

    @Override
    public void sort(int col) {
        if(timeSortedList == null)
            timeSortedList = new ArrayList<LoggingDataLine>(_list);
        super.sort(col);
    }

    @Override
    public void clear() {
        if(timeSortedList != null)
            timeSortedList.clear();
        super.clear();
    }
    
    
}