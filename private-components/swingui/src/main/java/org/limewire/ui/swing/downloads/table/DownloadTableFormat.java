package org.limewire.ui.swing.downloads.table;

import java.util.Comparator;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.ui.swing.table.AbstractAdvancedTableFormat;

class DownloadTableFormat extends AbstractAdvancedTableFormat<DownloadItem> {
    public static final int TITLE = 0;
    public static final int PROGRESS = 1;
    public static final int MESSAGE = 2;
    public static final int NUM_SOURCES = 3;
    public static final int ACTION = 4;
    public DownloadTableFormat(){
        super("title", "progress", "numSources", "message", "action");
    }

    @Override
    public Class getColumnClass(int column) {
        return DownloadItem.class;
    }

    @Override
    public Comparator getColumnComparator(int column) {
        return null;
    }

    @Override
    public Object getColumnValue(DownloadItem baseObject, int column) {
        return baseObject;
    }

   
}
