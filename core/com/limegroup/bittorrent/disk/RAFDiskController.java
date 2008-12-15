package com.limegroup.bittorrent.disk;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.FileUtils;

/**
 * implementation of the DiskController interface using
 * <tt>RandomAccessFile</tt> for io.
 */
class RAFDiskController<F extends File> implements DiskController<F> {
	
	private static final Log LOG = LogFactory.getLog(RAFDiskController.class);
	
	/*
	 * The files of this torrent as an array
	 */
	protected List<F> _files;

	/**
	 * The instances RandomAccessFile for all files contained in this torrent
	 */
	protected RandomAccessFile[] _fos = null;
	
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#write(long, byte[])
	 */
	public synchronized void write(long startOffset, byte[] data) throws IOException {
	    if (!isOpen())
	        throw new IOException("file closed");
		int written = 0;
		int filesSize = _files.size();
		for (int i = 0; i < filesSize && written < data.length; i++) {
			File current = _files.get(i);
			if (startOffset < current.length()) {
				int toWrite = (int) Math.min(current.length()- startOffset,
						data.length - written);

				writeImpl(_fos[i], startOffset, data, written, toWrite);
				startOffset += toWrite;
				written += toWrite;
			} 
			startOffset -= current.length();
		}
	}
	
	protected void  writeImpl(RandomAccessFile f, long fileOffset, byte [] data, int offset, int length) 
	throws IOException {
		f.seek(fileOffset);
		f.write(data, offset, length);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#isOpen()
	 */
	public synchronized boolean isOpen() {
		return _fos != null;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#open(boolean, boolean)
	 */
	public synchronized List<F> open(List<F> files, boolean complete, boolean isVerifying) throws IOException {
		_files = files;
		if (_fos != null)
		    throw new IOException("Files already open(ing)!");
		RandomAccessFile [] fos = new RandomAccessFile[_files.size()];
		
		// position of the first byte of a file in the torrent
		long pos = 0;
		
		List<F> filesToVerify = null;
		for (int i = 0; i < _files.size(); i++) {
			F file = _files.get(i);
            // close in case this is a restart loop
			IOUtils.close(fos[i]);
            
			// if the file is complete, just open it for reading and be done
			// with it
			if (complete) {
				LOG.info("opening torrent in read-only mode");
				fos[i] = new RandomAccessFile(file, "r");
			} else {
				LOG.info("opening torrent in read-write");
				if (!file.exists()) {

					// Ensure that the directory this file is in exists & is
					// writeable.
					// TODO: make sure this doesn't allow trickery with names
					File parentFile = file.getParentFile();
					if (parentFile != null) {
						parentFile.mkdirs();
						FileUtils.setWriteable(parentFile);
					}
					
					file.createNewFile();
					
					// a file is missing, so this must be a new download.
					// if it was not, we need to reverify every file.
					if (!isVerifying) {
						isVerifying = true;
						i = -1; // restart the loop
						continue;
					}

				} 
				
				FileUtils.setWriteable(file);
				fos[i] = new RandomAccessFile(file, "rw");
				
				// if a file exists, try to verify it
				if (isVerifying && fos[i].length() > 0) {
					if (filesToVerify == null)
						filesToVerify = new ArrayList<F>(_files.size());
					filesToVerify.add(file);
				}
			}

			// increment pos to point to the first byte of the next file
			pos += file.length();
		}
		
		for (RandomAccessFile raf : fos) {
		    if (!raf.getFD().valid())
		        throw new IOException("file was invalid: "+raf);
		}
            
        _fos = fos;
		return filesToVerify;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#close()
	 */
	public synchronized void close() {
		LOG.debug("closing the file");
		if (!isOpen())
		    return;
		for (RandomAccessFile f : _fos)
			IOUtils.close(f);
		_fos = null;
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#setReadOnly(com.limegroup.bittorrent.TorrentFile)
	 */
	public synchronized void setReadOnly(F completed) throws IOException {
	    if (!isOpen()) 
	        return;
	    // TODO: decide if files should be moved to the save
	    // location as they are completed.. cool but not trivial
	    int index = _files.indexOf(completed);
	    _fos[index] = setReadOnly(_fos[index], completed.getPath());
        if (!_fos[index].getFD().valid())
            throw new IOException("new fd invalid "+completed);
	}
	
	protected RandomAccessFile setReadOnly(RandomAccessFile f, String path) throws IOException {
		f.close();
		return new RandomAccessFile(path, "r");
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#read(long, byte[], int, int)
	 */
	public synchronized int read(long position, byte[] buf, int offset,
			int length) throws IOException {
		
		if (position < 0)
			throw new IllegalArgumentException("cannot seek negative position "+position);
		else if (offset + length > buf.length)
			throw new ArrayIndexOutOfBoundsException(
					"buffer to small to store supplied number of bytes");
		if (!isOpen())
		    throw new IOException("file closed");
        
		
		int read = 0;
		for (int i = 0; i < _files.size() && read < length; i++) {
			File f = _files.get(i);
			while (position < f.length() && read < length) {
				assert _fos[i] != null : "file being read & verified at the same time";
				
				long currentLength = _fos[i].length();
				if (currentLength < f.length() && position >= currentLength)
					return read;
				int toRead = (int) Math.min(currentLength - position, length
						- read);
				int t_read = readImpl(_fos[i], position, buf, read + offset, toRead);
				if (t_read == -1)
					throw new IOException();
				position += t_read;
				read += t_read;
			}
			position -= f.length();
		}
		return read;
	}
	
	protected int readImpl(RandomAccessFile raf, long fileOffset, byte [] dst, int offset, int length) 
	throws IOException {
		raf.seek(fileOffset);
		return raf.read(dst, offset, length);
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#flush()
	 */
	public synchronized void flush() throws IOException {
	    LOG.debug("flushing");
	    if (!isOpen())
	        return;
		for (RandomAccessFile f : _fos) {
            if (f != null)
                f.getChannel().force(false);
            else {
                StringBuilder report = new StringBuilder();
                report.append("flush npe report:");
                report.append("  files:").append(_files).append("  ");
                report.append("fos length ").append(_fos.length).append("  ");
                for (RandomAccessFile f2 : _fos)
                    report.append(String.valueOf(f2)).append("  ");
                throw new IllegalStateException(report.toString());
            }
        }
	}
}
