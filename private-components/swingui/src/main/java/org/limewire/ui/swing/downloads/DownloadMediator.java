package org.limewire.ui.swing.downloads;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.downloads.table.DownloadStateExcluder;
import org.limewire.util.FileUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadMediator {
    
    public static enum SortOrder {ORDER_ADDED, NAME, PROGRESS, TIME_REMAINING, SPEED, STATUS, FILE_TYPE, EXTENSION};

	/**
	 * unfiltered - common to all tables
	 */
    private final SortedList<DownloadItem> commonBaseList;
	private DownloadListManager downloadListManager;
	
	@Inject
	public DownloadMediator(DownloadListManager downloadManager) {
	    this.downloadListManager = downloadManager;
	    EventList<DownloadItem> baseList = GlazedListsFactory.filterList(downloadManager.getSwingThreadSafeDownloads(), new DownloadStateExcluder(DownloadState.CANCELLED));
	    commonBaseList = GlazedListsFactory.sortedList(baseList, new DescendingComparator(new OrderAddedComparator()));
	    
	}
	
	public void setSortOrder(SortOrder order, boolean isAscending){
	    Comparator<DownloadItem> comparator;
	    switch (order) {
        case ORDER_ADDED:
            comparator = new OrderAddedComparator();
            break;
        case NAME:
            comparator = new NameComparator();
            break;
        case PROGRESS:
            comparator = new ProgressComparator();
            break;
        case TIME_REMAINING:
            comparator = new TimeRemainingComparator();
            break;
        case SPEED:
            comparator = new SpeedComparator();
            break;
        case STATUS:
            comparator = new StateComparator();
            break;
        case FILE_TYPE:
            comparator = new FileTypeComparator();
            break;
        case EXTENSION:
            comparator = new FileExtensionComparator();
            break;
        default:
            throw new IllegalArgumentException("Unknown SortOrder: " + order);
        }
	    
	    if(isAscending){
	        commonBaseList.setComparator(comparator);
	    } else {
            commonBaseList.setComparator(new DescendingComparator(comparator));
        }
	}

	public void pauseAll() {
        commonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : commonBaseList) {
                if (item.getState().isPausable()) {
                    item.pause();
                }
            }
        } finally {
            commonBaseList.getReadWriteLock().writeLock().unlock();
        }
    }

	public void resumeAll() {
        commonBaseList.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : commonBaseList) {
                if (item.getState().isResumable()) {
                    item.resume();
                }
            }
        } finally {
            commonBaseList.getReadWriteLock().writeLock().unlock();
        }
    }
	
	public EventList<DownloadItem> getDownloadList() {
	    return this.commonBaseList;
	}
	
	public void clearFinished() {
	    downloadListManager.clearFinished();
	}

    public void fixStalled() {
        List<DownloadItem> items = getMatchingDownloadItems(DownloadState.STALLED);
        for (DownloadItem item : items) {
            item.resume();
        }
    }

    public void cancelStalled() {
        cancelMatchingDownloadItems(DownloadState.STALLED);
    }

    public void cancelError() {
        cancelMatchingDownloadItems(DownloadState.ERROR);
    }

    public void cancelAll() {
        cancelMatchingDownloadItems(null);
    }
    
    /**
     * 
     * @param state The state of the DownloadItems to be canceled.  Null will cancel all.
     */
    private void cancelMatchingDownloadItems(DownloadState state){
        List<DownloadItem> items = getMatchingDownloadItems(state);
        for(DownloadItem item : items){
            item.cancel();
        }
    }
    
    /**
     * 
     * @param state null will return all DownloadItems
     * @return a List of all DownloadItems in the specified DownloadState
     */
    private List<DownloadItem> getMatchingDownloadItems(DownloadState state) {
        if (state == null) {
            return new ArrayList<DownloadItem>(commonBaseList);
        }
        
        List<DownloadItem> matchingItems = new ArrayList<DownloadItem>();
        for (DownloadItem item : commonBaseList) {
            if (item.getState() == state) {
                matchingItems.add(item);
            }
        }
        
        return matchingItems;
    }
    
    private static class StateComparator implements Comparator<DownloadItem>{
        
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
            case RESUMING: return 4;
            case CONNECTING: return 5;
            case PAUSED: return 6;
            case REMOTE_QUEUED: return 7;
            case LOCAL_QUEUED: return 8;
            case TRYING_AGAIN: return 9;
            case STALLED: return 10;
            case ERROR: return 11;       
            case CANCELLED: return 12;
            }
            
           throw new IllegalArgumentException("Unknown DownloadState: " + state);
        }
    }
    
    private static class OrderAddedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) { 
            if (o1 == o2){
                return 0;
            }
            return o2.getStartDate().compareTo(o1.getStartDate());
        }      
    }
    
    private static class NameComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getTitle().compareTo(o2.getTitle());
        }   
     
    } 
  
    private static class ProgressComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getPercentComplete() - o2.getPercentComplete();
        }   
     
    } 
    
    private static class TimeRemainingComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)(o1.getRemainingDownloadTime() - o2.getRemainingDownloadTime());
        }   
     
    }
    
    
    private static class SpeedComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return (int)o2.getDownloadSpeed() - (int)o1.getDownloadSpeed();
        }   
     
    }
    
    
    private static class FileTypeComparator implements Comparator<DownloadItem>{
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return o1.getCategory().compareTo(o2.getCategory());
        }   
     
    } 

    
    private static class FileExtensionComparator implements Comparator<DownloadItem> {
        
        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            if (o1 == o2){
                return 0;
            }

            return FileUtils.getFileExtension(o1.getDownloadingFile()).compareTo(FileUtils.getFileExtension(o2.getDownloadingFile()));
        }   
     
    }
    
    private static class DescendingComparator implements Comparator<DownloadItem>{
        private Comparator<DownloadItem> delegate;

        public DescendingComparator(Comparator<DownloadItem> delegate){
            this.delegate = delegate;
        }

        @Override
        public int compare(DownloadItem o1, DownloadItem o2) {
            return -1 * delegate.compare(o1, o2);
        }
    }
}
