package com.limegroup.bittorrent.gui;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationManager;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.Address;
import org.limewire.io.GUID;
import org.limewire.io.IOUtils;
import org.limewire.listener.EventListener;
import org.limewire.nio.observer.Shutdownable;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Endpoint;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.downloader.DownloadStatusEvent;
import com.limegroup.gnutella.downloader.DownloaderType;
import com.limegroup.gnutella.downloader.serial.DownloadMemento;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.download.DownloaderUtils;
import com.limegroup.gnutella.gui.download.GuiDownloaderFactory;
import com.limegroup.gnutella.http.HTTPHeaderName;
import com.limegroup.gnutella.http.HttpClientListener;
import com.limegroup.gnutella.util.LimeWireUtils;

public class TorrentFileFetcher implements HttpClientListener, CoreDownloader {
	
	private static final int TIMEOUT = 5000;
	private final URI torrentURI;
	private volatile boolean stopped, failed;
	
	/** 
	 * The URN for this download - initialized as soon as we fetch
	 * the metadata, but before the downloader object is created.  
	 * See equals(). 
	 */
	private volatile URN urn;

	/**
	 * Something to shutdown if the user cancels the fetching
	 */
	private volatile Shutdownable aborter;
	
	/**
	 * The delegate downloader that will perform the actual download.
	 */
	private volatile CoreDownloader delegate;
	
	public TorrentFileFetcher(URI torrentURI, SaveLocationManager saveLocationManager) {
		this.torrentURI = torrentURI;
	}
	
	public void fetch() {
		final HttpGet get = new HttpGet(torrentURI);
		get.addHeader("User-Agent", LimeWireUtils.getHttpServer());
		get.addHeader(HTTPHeaderName.CONNECTION.httpStringValue(),"close");
        
        HttpParams params = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(params, TIMEOUT);
        HttpConnectionParams.setSoTimeout(params, TIMEOUT);
        HttpClientParams.setRedirecting(params, true);
        aborter = GuiCoreMediator.getHttpExecutor().execute(get, params, this);
	}
	
	
	public boolean requestComplete(HttpUriRequest method, HttpResponse response) {
		aborter = null;
		if (stopped)
			return false;
		BTMetaInfo m = null;
        byte [] body = null;
		try {
            if(response.getEntity() != null) {
                body = IOUtils.readFully(response.getEntity().getContent());
            }
            if (response.getStatusLine().getStatusCode() < 200 || response.getStatusLine().getStatusCode() >= 300)
				throw new IOException("bad status code, downloading .torrent file "
				+ response.getStatusLine().getStatusCode());
			if (body == null || body.length == 0)
				throw new IOException("invalid response");
            
			m = GuiCoreMediator.getBTMetaInfoFactory().createBTMetaInfoFromBytes(body);
			synchronized(this) {
				urn = m.getURN();
			}
		} catch (SaveLocationException security) {
			GUIMediator.showWarning(I18n.tr("The selected .torrent file may contain a security hazard."));
		} catch (IOException iox) {
			GUIMediator.showWarning(I18n.tr("LimeWire could not download a .torrent file from the provided URL."));
		} finally {
			GuiCoreMediator.getHttpExecutor().releaseResources(response);
		}
		
		if (m == null) {
			removeDataLine();
			return false;
		}
        
		if(SharingSettings.SHARE_TORRENT_META_FILES.getValue()) {
            final File tFile = 
                GuiCoreMediator.getTorrentManager().getSharedTorrentMetaDataFile(m);
            GuiCoreMediator.getFileManager().getGnutellaFileList().remove(tFile);

            File backup = null;
            if(tFile.exists()) {
                backup = new File(tFile.getParent(), tFile.getName().concat(".bak"));
                FileUtils.forceRename(tFile, backup);
            }
            OutputStream out = null;
            try {
                out = new BufferedOutputStream(new FileOutputStream(tFile));
                out.write(body);
                out.flush();
                if(backup != null) {
                    backup.delete();
                }
            } catch (IOException ioe) {
                if(backup != null) {
                    //restore backup
                    if(FileUtils.forceRename(backup, tFile)){
                        GuiCoreMediator.getFileManager().getGnutellaFileList().add(tFile);
                    }
                }
            } 
            finally {
                IOUtils.close(out);
            }
        }
        
		final BTMetaInfo toDownload = m;
        Runnable starter = new Runnable() {
            public void run() {
                GuiDownloaderFactory factory = new TorrentDownloadFactory(toDownload);
                CoreDownloader d = (CoreDownloader) DownloaderUtils.createDownloader(factory);
                if (d != null) {
                    delegate = d;
                } else {
                    stopped = true;
                    removeDataLine();
                }
            }
        };
        SwingUtilities.invokeLater(starter);
        return false;
	}

	private synchronized void removeDataLine() {
		failed = true;
		urn = null;
		GuiCoreMediator.getActivityCallback().downloadCompleted(this);
	}
	
	public boolean requestFailed(HttpUriRequest method, HttpResponse response, IOException exc) {
	    assert(delegate == null);
		failed = true;
        return false;
	}

	public void discardCorruptDownload(boolean delete) {
	}

	public long getAmountLost() {
		return delegate == null ? 0 : delegate.getAmountLost();
	}

	public int getAmountPending() {
		return delegate == null ? 0 : delegate.getAmountPending();
	}

	public long getAmountRead() {
		return delegate == null ? 0 : delegate.getAmountRead();
	}

	public long getAmountVerified() {
		return delegate == null ? 0 : delegate.getAmountVerified();
	}

	public Object getAttribute(String key) {
		return delegate == null ? null : delegate.getAttribute(key);
	}

	public RemoteFileDesc getBrowseEnabledHost() {
		return delegate == null ? null : delegate.getBrowseEnabledHost();
	}

	public int getBusyHostCount() {
		return delegate == null ? 0 : delegate.getBusyHostCount();
	}

	public Endpoint getChatEnabledHost() {
		return delegate == null ? null : delegate.getChatEnabledHost();
	}

	public int getChunkSize() {
		return delegate == null ? 1 : delegate.getChunkSize();
	}

	public long getContentLength() {
		return delegate == null ? 0 : delegate.getContentLength();
	}

	public File getDownloadFragment() {
		return delegate == null ? null : delegate.getDownloadFragment();
	}

	public File getFile() {
		return delegate == null ? null : delegate.getFile();
	}

	public int getInactivePriority() {
		return delegate == null ? 0 : delegate.getInactivePriority();
	}

	public int getNumHosts() {
		return delegate == null ? 0 : delegate.getNumHosts();
	}
	
	@Override
	public List<Address> getSourcesAsAddresses() {
	    if(delegate == null) {
	        return Collections.emptyList();
	    } else {
	        return delegate.getSourcesAsAddresses();
	    }
	}

	public int getNumberOfAlternateLocations() {
		return delegate == null ? 0 : delegate.getNumberOfAlternateLocations();
	}

	public int getNumberOfInvalidAlternateLocations() {
		return delegate == null ? 0 : delegate.getNumberOfInvalidAlternateLocations();
	}

	public int getPossibleHostCount() {
		return delegate == null ? 0 : delegate.getPossibleHostCount();
	}

	public int getQueuePosition() {
		return delegate == null ? 0 : delegate.getQueuePosition();
	}

	public int getQueuedHostCount() {
		return delegate == null ? 0 : delegate.getQueuedHostCount();
	}

	public int getRemainingStateTime() {
		return delegate == null ? 0 : delegate.getRemainingStateTime();
	}

	public URN getSha1Urn() {
		return delegate == null ? null : delegate.getSha1Urn();
	}

	public File getSaveFile() {
		if (delegate != null)
			return delegate.getSaveFile();
		
		// try to get a meaningful name out of the URI
		String uri = torrentURI.toString();
		String name = null;
		if (uri.endsWith(".torrent")) {
			int slash = uri.lastIndexOf("/");
			if (slash != -1)
				name = uri.substring(slash);
		}
		
		// can't figure it out?  show the uri
		if (name == null)
			name = uri;
		
		return new File(uri);
	}

	public DownloadStatus getState() {
		if (stopped)
			return DownloadStatus.ABORTED;
		if (failed)
			return DownloadStatus.GAVE_UP;
		if (delegate == null)
			return DownloadStatus.FETCHING;
		
		return delegate.getState();
	}

	public String getVendor() {
		return delegate == null ? null : delegate.getVendor();
	}

	public boolean hasBrowseEnabledHost() {
		return delegate == null ? false : delegate.hasBrowseEnabledHost();
	}

	public boolean hasChatEnabledHost() {
		return delegate == null ? false : delegate.hasChatEnabledHost();
	}

	public boolean isCompleted() {
		return delegate == null ? (failed || stopped) : delegate.isCompleted();
	}

	public boolean isInactive() {
		return delegate == null ? false : delegate.isInactive();
	}

	public boolean isLaunchable() {
		return delegate == null ? false : delegate.isLaunchable();
	}

	public boolean isPausable() {
		return delegate == null ? false : delegate.isPausable();
	}

	public boolean isPaused() {
		return delegate == null ? false : delegate.isPaused();
	}

	public boolean isRelocatable() {
		return delegate == null ? false : delegate.isRelocatable();
	}

	public boolean isResumable() {
		return delegate == null ? false : delegate.isResumable();
	}

	public void pause() {
		if (delegate != null)
			delegate.pause();
	}

	public Object removeAttribute(String key) {
		return delegate == null ? null : delegate.removeAttribute(key);
	}

	public boolean resume() {
		return delegate == null ? false : delegate.resume();
	}

	public Object setAttribute(String key, Object value, boolean serialize) {
		return delegate == null ? null : delegate.setAttribute(key, value, serialize);
	}

	public void setSaveFile(File saveDirectory, String fileName,
			boolean overwrite) throws SaveLocationException {
		if (delegate != null)
			delegate.setSaveFile(saveDirectory, fileName, overwrite);
	}

	public void stop(boolean deleteFile) {
		if (delegate != null)
			delegate.stop(deleteFile);
		else {
			stopped = true;
			if (aborter != null) {
				aborter.shutdown();
				aborter = null;
			}
		}
	}

	public float getAverageBandwidth() {
		return delegate == null ? 0 : delegate.getAverageBandwidth();
	}

	public float getMeasuredBandwidth() throws InsufficientDataException {
		return delegate == null ? 0 : delegate.getMeasuredBandwidth();
	}

	public void measureBandwidth() {
		if (delegate != null)
			delegate.measureBandwidth();
	}
	
	/**
	 * The equals() method ensures that once
	 * the delegate downloader is created, it is considered identical to this.
	 * Until then, this is a regular unique downloader.
	 */
	@Override
    public synchronized boolean equals(Object o) {
		if (urn == null)
			return super.equals(o);
		if (! (o instanceof Downloader))
			return false;
		Downloader d = (Downloader)o;
		return urn.equals(d.getSha1Urn());
	}

	public int getTriedHostCount() {
		return -1;
	}
	
	public String getCustomIconDescriptor() {
		return delegate == null ? null : delegate.getCustomIconDescriptor();
	}

	
	/*
	 * ***********************************************************
	 * Delegate methods for AbstractDownloader.  These should only
	 * be invoked after we have a delegate.
	 * ***********************************************************
	 */

	public boolean conflicts(URN urn, long fileSize, File... files) {
		return delegate.conflicts(urn, fileSize, files);
	}

	public boolean conflictsSaveFile(File saveFile) {
		return delegate.conflictsSaveFile(saveFile);
	}

	public boolean conflictsWithIncompleteFile(File incomplete) {
		return delegate.conflictsWithIncompleteFile(incomplete);
	}

	public void finish() {
		delegate.finish();
	}

	public GUID getQueryGUID() {
		return delegate.getQueryGUID();
	}

	public void handleInactivity() {
		delegate.handleInactivity();
	}

	public void initialize() {
        delegate.initialize();
    }

	public boolean isAlive() {
		return delegate.isAlive();
	}

	public boolean isQueuable() {
		return delegate.isQueuable();
	}

	public void setInactivePriority(int priority) {
		delegate.setInactivePriority(priority);
	}

	public boolean shouldBeRemoved() {
		return delegate.shouldBeRemoved();
	}

	public boolean shouldBeRestarted() {
		return delegate.shouldBeRestarted();
	}

	public void startDownload() {
		delegate.startDownload();
	}

	@Override
    public String toString() {
		return delegate.toString();
	}

    public DownloaderType getDownloadType() {
        return DownloaderType.TORRENTFETCHER;
    }

    public DownloadMemento toMemento() {
        throw new IllegalStateException("should not be serializing!");
    }

    public void initFromMemento(DownloadMemento memento) {
        throw new IllegalStateException("should not be deserialized!");
    }

    public void addListener(EventListener<DownloadStatusEvent> listener) {
    }

    public boolean removeListener(EventListener<DownloadStatusEvent> listener) {
        return false;
    }

    @Override
    public boolean allowRequest(HttpUriRequest request) {
        return true;
    }

    @Override
    public boolean isMementoSupported() {
        return false;
    }
}
