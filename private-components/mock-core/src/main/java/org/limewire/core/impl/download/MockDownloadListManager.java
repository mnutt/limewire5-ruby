package org.limewire.core.impl.download;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadListManager;
import org.limewire.core.api.download.DownloadState;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.DownloadItem.ErrorState;
import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;

import com.google.inject.Singleton;

@Singleton
public class MockDownloadListManager implements DownloadListManager {
    private final RemoveCancelledListener cancelListener = new RemoveCancelledListener();
	private final EventList<DownloadItem> downloadItems;
	
	public MockDownloadListManager(){
	    ObservableElementList.Connector<DownloadItem> downloadConnector =
            GlazedLists.beanConnector(DownloadItem.class);
	    downloadItems = GlazedLists.threadSafeList(
	        new ObservableElementList<DownloadItem>(
            new BasicEventList<DownloadItem>(), downloadConnector));
		initializeMockData();
	}

	@Override
	public EventList<DownloadItem> getDownloads() {
		return downloadItems;
	}
	
	@Override
	public EventList<DownloadItem> getSwingThreadSafeDownloads() {
	    return GlazedListsFactory.swingThreadProxyEventList(getDownloads());
	}

	@Override
	public DownloadItem addDownload(
            Search search, List<? extends SearchResult> coreSearchResults) {
        String title = coreSearchResults.get(0).getFileName(); 
        long totalSize = coreSearchResults.get(0).getSize();
        DownloadState state = DownloadState.DOWNLOADING;
        Category category = coreSearchResults.get(0).getCategory();
        final MockDownloadItem mdi =
            new MockDownloadItem(title, totalSize, state, category);

        addDownload(mdi);
	    return mdi;
	}

	public void addDownload(DownloadItem downloadItem){
	    downloadItem.addPropertyChangeListener(cancelListener);
		downloadItems.add(downloadItem);
	}
	
	private void initializeMockData(){
	    MockDownloadItem item = new MockDownloadItem("Monkey on ice skates", 4416,
				DownloadState.DOWNLOADING, Category.VIDEO);
		item.addDownloadSource(new MockDownloadSource("134.23.2.7"));
		addDownload(item);

		item = new MockDownloadItem("Psychology 101 Lecture 3", 446,
				DownloadState.DOWNLOADING, Category.AUDIO);
		item.addDownloadSource(new MockDownloadSource("245.2.7.78"));
		addDownload(item);
		
		item = new MockDownloadItem("New England Foliage.bmp", 46,
				DownloadState.DOWNLOADING, Category.IMAGE);
		item.addDownloadSource(new MockDownloadSource("234.2.3.4"));
		addDownload(item);

		item = new MockDownloadItem("Psychology 101 Lecture 2.avi", 55,
				DownloadState.LOCAL_QUEUED, Category.VIDEO);
		item.addDownloadSource(new MockDownloadSource("34.2.7.7"));
		addDownload(item);

        item = new MockDownloadItem("Psychology 101 Lecture 1", 55,
                DownloadState.ERROR, Category.VIDEO);
        item.addDownloadSource(new MockDownloadSource("23.12.33.4"));
        item.setErrorState(ErrorState.DISK_PROBLEM);
        addDownload(item);
        
        item = new MockDownloadItem("Psychology 102 Lecture 1", 55,
                DownloadState.CONNECTING, Category.VIDEO);
        item.addDownloadSource(new MockDownloadSource("23.12.33.4"));
        addDownload(item);
        
        item = new MockDownloadItem("Tester.bmp", 46,
                DownloadState.STALLED, Category.AUDIO);
        item.addDownloadSource(new MockDownloadSource("234.2.2.2"));
        addDownload(item);

	}
	
	private class RemoveCancelledListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getNewValue() == DownloadState.CANCELLED) {
                downloadItems.remove(evt.getSource());
            }
        }
    }

    @Override
    public DownloadItem addDownload(RemoteFileItem fileItem) throws SaveLocationException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DownloadItem addTorrentDownload(URI uri, boolean overwrite) throws SaveLocationException {
        return null;
    }

    @Override
    public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults,
            File saveFile, boolean overwrite) throws SaveLocationException {
       return addDownload(search, coreSearchResults);
    }

    @Override
    public DownloadItem addDownload(RemoteFileItem file, File saveFile, boolean overwrite) {
        return null;
    }

    @Override
    public DownloadItem addTorrentDownload(File file, File saveFile, boolean overwrite)
            throws SaveLocationException {
        return null;
    }

    @Override
    public boolean contains(URN urn) {
        return false;
    }

    @Override
    public DownloadItem addDownload(MagnetLink magnet, File saveFile, boolean overwrite)
            throws SaveLocationException {
        return null;
    }
    
    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    }
    
    @Override
    public void updateDownloadsCompleted() {
    }

    @Override
    public DownloadItem getDownloadItem(URN urn) {
        return null;
    }
    
    @Override
    public void clearFinished() {
        List<DownloadItem> finishedItems = new ArrayList<DownloadItem>();
        downloadItems.getReadWriteLock().writeLock().lock();
        try {
            for (DownloadItem item : downloadItems) {
                if (item.getState() == DownloadState.DONE) {
                    finishedItems.add(item);
                }
            }
            
            for (DownloadItem item : finishedItems) {
                downloadItems.remove(item);
            }
        } finally {
            downloadItems.getReadWriteLock().writeLock().unlock();
        }
    }
    
}
