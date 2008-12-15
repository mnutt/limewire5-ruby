package org.limewire.core.impl.download;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.SwingPropertyChangeSupport;

import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.io.Address;


public class MockDownloadItem implements DownloadItem {

	private volatile String title;
	private volatile long currentSize = 0;
    private volatile long remainingQueueTime = 9999;
	private final long totalSize;
	//guarded by this
	private volatile boolean running = true;
	private volatile DownloadState state = DownloadState.DOWNLOADING;
	private final List<Address> downloadSources;
	private final Category category;
	
	private ErrorState errorState = ErrorState.NONE;
	
	private final PropertyChangeSupport support = new SwingPropertyChangeSupport(this);
    private int queuePostion = 2;

	//TODO: change constructor
	public MockDownloadItem(String title, long totalSize, DownloadState state, Category category) {
		this.title = title;
		this.category = category;
		this.totalSize = totalSize;
		this.state = state;
		downloadSources = new ArrayList<Address>();
		if (this.state == DownloadState.DOWNLOADING) {
			start();
		}
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener){
		support.addPropertyChangeListener(listener);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener listener){
		support.removePropertyChangeListener(listener);
	}

	public String getTitle() {
		return title;
	}

	public int getPercentComplete() {
		//TODO - check for div by zero?
		return (int) (100 * getCurrentSize() / getTotalSize());
	}

	public long getCurrentSize() {
		return currentSize;
	}

	private void setCurrentSize(long newSize) {
		double oldSize = this.currentSize;
		this.currentSize = newSize > getTotalSize() ? getTotalSize() : newSize;
		if (currentSize == getTotalSize()) {
			setState(DownloadState.DONE);
		} else {
			support.firePropertyChange("currentSize", oldSize, currentSize);
		}
//		setChanged();
//		notifyObservers();
	}

	public long getTotalSize() {
		return totalSize;
	}

	public long getRemainingDownloadTime() {
        return (long) ((getTotalSize() - getCurrentSize()) / getDownloadSpeed());
    }

	public void cancel() {
		setRunning(false);
		setState(DownloadState.CANCELLED);
	}

	public void pause() {
		setRunning(false);
		setState(DownloadState.PAUSED);
	}

	public void resume() {
		setRunning(true);
		start();
		setState(DownloadState.DOWNLOADING);
	}

	private boolean isRunning() {
		return running;
	}

	private void setRunning(boolean running) {
		this.running = running;
	}

	private void start() {
		new Thread() {
            @Override
			public void run() {
				while (isRunning() && getCurrentSize() < getTotalSize()) {
					setCurrentSize(getCurrentSize() + 5);
					setRemainingQueueTime(getRemainingQueueTime() - 1);
					try {
						sleep(500);
					} catch (InterruptedException e) {
						// eat InterruptedException
					}
				}
			}
          
		}.start();
	}

	@Override
	public DownloadState getState() {
		return state;
	}

	public void setState(DownloadState state) {
		DownloadState oldState = this.state;
		this.state = state;
		support.firePropertyChange("state", oldState, state);
		//setChanged();
		//notifyObservers();
	}
	
	public void addDownloadSource(Address source){
		downloadSources.add(source);
	}

	@Override
	public int getDownloadSourceCount() {
		return downloadSources.size();
	}
	
	
	/**
	 * @throws ArrayIndexOutOfBoundsException
	 */
	@Override
	public List<Address> getSources() {
		return downloadSources;
	}

	@Override
	public Category getCategory() {
		return category;
	}

    @Override
    public float getDownloadSpeed() {
        return 56;
    }

    @Override
    public int getQueuePosition() {
        return queuePostion;
    }
    
    public void setQueuePosition(int position) {
        queuePostion = position;
    }


    @Override
    public ErrorState getErrorState() {
        return errorState;
    }
    
    public void setErrorState(ErrorState errorState){
        this.errorState = errorState;
    }

    @Override
    public long getRemainingQueueTime() {
        return remainingQueueTime;
    }

    private void setRemainingQueueTime(long l) {
        remainingQueueTime = l;
    }
 
    @Override
    public int getLocalQueuePriority() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean isLaunchable() {
        return true;
    }

    @Override
    public File getDownloadingFile() {
        return new File("A FILE!");
    }
    
    @Override
    public File getLaunchableFile() {
        return getDownloadingFile();
    }

    @Override
    public URN getUrn() {
        return null;
    }
    
    @Override
    public String getFileName() {
        return title;
    }

    @Override
    public Object getProperty(FilePropertyKey key) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPropertyString(FilePropertyKey filePropertyKey) {
        // TODO Auto-generated method stub
        return null;
    }
}
