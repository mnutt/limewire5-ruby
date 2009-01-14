package org.limewire.ui.swing.downloads;

import java.awt.BorderLayout;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadTableFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;

public class CategoryDownloadPanel extends JPanel {
    
    @AssistedInject
    public CategoryDownloadPanel(DownloadTableFactory downloadTableFactory, @Assisted EventList<DownloadItem> list) {
        super(new BorderLayout());

        SortedList<DownloadItem> sortList = GlazedListsFactory.sortedList(list, new CategoryComparator());
        JTable table = downloadTableFactory.create(sortList);
        table.setTableHeader(null);
        JScrollPane pane = new JScrollPane(table);
        pane.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        add(pane, BorderLayout.CENTER);
    }
    
    private static class CategoryComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return getSortPriority(o1.getState()) - getSortPriority(o2.getState());
        }   
        
        private int getSortPriority(DownloadState state){
            
            switch(state){
            case DONE: return 1;
            case FINISHING: return 2;
            case DOWNLOADING: return 3;
            case CONNECTING: return 4;
            case PAUSED: return 5;
            case REMOTE_QUEUED: return 6;
            case LOCAL_QUEUED: return 7;
            case STALLED: return 8;
            case ERROR: return 9;            
            }
            
           throw new IllegalArgumentException("Unknown DownloadState: " + state);
        }
    }

}
