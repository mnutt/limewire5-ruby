package com.limegroup.bittorrent.disk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Experimental disk controller that uses Memory Mapped files
 * for disk io.  If a file cannot be mapped for some reason, it 
 * falls back to the RAFDiskController behavior.
 */
public class MMDiskController<F extends File> extends RAFDiskController<F> {

	private static final Log LOG = LogFactory.getLog(MMDiskController.class);
	
	/*
	 * Note: because of Bug ID:4724038 System.gc() is being called
	 * explicitly in a few places.
	 */
	
	private Map<RandomAccessFile, MappedByteBuffer> bufMap;

	@Override
	public synchronized List<F> open(List<F> files, boolean complete, boolean isVerifying) throws IOException {
		List<F> ret =  super.open(files, complete, isVerifying);
		
		bufMap = new HashMap<RandomAccessFile, MappedByteBuffer>(files.size());
		
		for (int i = 0; i < _fos.length; i++) {
			
			if (_files.get(i).length() >= Integer.MAX_VALUE)
				continue; // not even in 64 bit jvms
			
			try {
				bufMap.put(_fos[i],
						_fos[i].getChannel().map(
								complete ? MapMode.READ_ONLY : MapMode.READ_WRITE, 
										0, 
										_files.get(i).length()));
				if (LOG.isDebugEnabled())
					LOG.debug("mapped "+_files.get(i));
			} catch (IOException mapFailed) {
				// fallback to regular.
				if (LOG.isDebugEnabled())
					LOG.debug("didn't map "+_files.get(i), mapFailed);
			} 
		}
		
		return ret;
	}

	@Override
	protected void writeImpl(RandomAccessFile f, long fileOffset, byte[] data, int offset, int length) throws IOException {
		MappedByteBuffer buf = getBuf(f);
		if (buf == null) {
			super.writeImpl(f, fileOffset, data, offset, length);
			return;
		}
		buf.position((int)fileOffset);
		buf.put(data, offset, length);
	}

	@Override
	protected int readImpl(RandomAccessFile raf, long fileOffset, byte[] dst, int offset, int length) throws IOException {
		MappedByteBuffer buf = getBuf(raf);
		if (buf == null) 
			return super.readImpl(raf, fileOffset, dst, offset, length);
		
		buf.position((int)fileOffset);
		buf.get(dst, offset, length);
		return buf.position() - (int)fileOffset;
	}
	
	private synchronized MappedByteBuffer getBuf(RandomAccessFile f) {
		return bufMap.get(f);
	}
	
	@Override
    public synchronized void close() {
	    LOG.debug("closing...");
	    boolean allCleaned = false;
	    try {
	        if (bufMap != null) {
	            boolean cleaned = true;
	            for (MappedByteBuffer mbb : bufMap.values()) {
	                safeForce(mbb);
	                cleaned = cleaned && clean(mbb);
	            }
	            allCleaned = cleaned;
	        }
	    } finally {
            if (bufMap != null)
                bufMap.clear();
	        if (!allCleaned)
	            System.gc(); 
	        super.close();
	    }
	}
	
	@Override
    public synchronized void flush() throws IOException {
	    if (bufMap == null)
	        return;
	    for (MappedByteBuffer buf : bufMap.values())
			buf.force(); // do not use safeForce, its ok if an iox throws.
		super.flush();
	}

	@Override
	public synchronized RandomAccessFile setReadOnly(RandomAccessFile raf, String path) throws IOException {
		MappedByteBuffer buf = bufMap.remove(raf);
		try {
			safeForce(buf);
		} finally {
			if (!clean(buf)) {
				buf = null;
				System.gc();
			}
		}
		raf = super.setReadOnly(raf, path);
		buf = raf.getChannel().map(MapMode.READ_ONLY, 0, raf.length());
		bufMap.put(raf, buf);
		return raf;
	}
	
	private void safeForce(MappedByteBuffer buf) {
		try {
			buf.force();
		} catch (Exception iox) {
			if (iox instanceof IOException) 
				return; // BugID 5074836
			RuntimeException r;
			if (iox instanceof RuntimeException) 
				r = (RuntimeException)iox;
			else
				r = new RuntimeException(iox);
			throw r;
		}
	}
	
	public static boolean clean(final Object buffer) {

		return AccessController.doPrivileged(new PrivilegedAction<Boolean>() {

			public Boolean run() {
 				try {
					Method getCleanerMethod = buffer.getClass().getMethod("cleaner");
					if (getCleanerMethod == null)
						return false;
                    
					getCleanerMethod.setAccessible(true);
                    
                    Object cleaner = getCleanerMethod.invoke(buffer);
					if (cleaner == null)
						return false;
                    
                    Method cleanMethod = cleaner.getClass().getMethod("clean");
                    cleanMethod.invoke(cleaner);
					return true;
				} catch(SecurityException e) {
                    LOG.warn("security", e);
                } catch (InvocationTargetException e) {
                    LOG.warn("invocation", e);
                } catch (NoSuchMethodException e) {
                    LOG.warn("no method", e);
                } catch (IllegalAccessException e) {
                    LOG.warn("illegal access", e);
                }
				return false;
			}

		});

	}

}

