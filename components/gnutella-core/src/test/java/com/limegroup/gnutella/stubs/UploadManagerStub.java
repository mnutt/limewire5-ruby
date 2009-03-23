
package com.limegroup.gnutella.stubs;

import java.net.InetAddress;

import com.google.inject.Singleton;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.library.FileDesc;

/**
 * stub for easier testing.  Feel free to override more methods/getters
 */
@Singleton
public class UploadManagerStub implements UploadManager {
	
	private int _numQueuedUploads;
    private int _uploadsInProgress;
	
	public void setNumQueuedUploads(int what) {
		_numQueuedUploads =what;
	}
	
	public void setUploadsInProgress(int what) {
		_uploadsInProgress = what;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#getNumQueuedUploads()
	 */
	public synchronized int getNumQueuedUploads() {
		// TODO Auto-generated method stub
		return _numQueuedUploads;
	}
    
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.UploadManager#uploadsInProgress()
	 */
	public synchronized int uploadsInProgress() {
		// TODO Auto-generated method stub
		return _uploadsInProgress;
	}

    public float getLastMeasuredBandwidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    public boolean hadSuccesfulUpload() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isConnectedTo(InetAddress addr) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isServiceable() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean killUploadsForFileDesc(FileDesc fd) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean mayBeServiceable() {
        // TODO Auto-generated method stub
        return false;
    }

    public int measuredUploadSpeed() {
        // TODO Auto-generated method stub
        return 0;
    }

    public void start() {
        // TODO Auto-generated method stub
        
    }

    public void stop() {
        // TODO Auto-generated method stub
        
    }

    public float getAverageBandwidth() {
        // TODO Auto-generated method stub
        return 0;
    }

    public float getMeasuredBandwidth() throws InsufficientDataException {
        // TODO Auto-generated method stub
        return 0;
    }

    public void measureBandwidth() {
        // TODO Auto-generated method stub
        
    }
}
