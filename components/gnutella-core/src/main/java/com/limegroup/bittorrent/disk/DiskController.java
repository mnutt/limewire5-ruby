package com.limegroup.bittorrent.disk;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Interface describing functionality for performing reads and 
 * writes to multiple files on disk.
 */
interface DiskController<F extends File> {

	public void write(long startOffset, byte[] data) throws IOException;

	/**
	 * @return true if this disk storage is open
	 */
	public boolean isOpen();

	/**
	 * 
	 * @param files the files this will be controlling
	 * @param complete true if the torrent is complete
     * @param isVerifying if the file should force verification.
	 * @return List of files that should be verified,
	 * null if none, always a subset of the provided list 
	 * @throws IOException if a problem occurs.
	 */
	public List<F> open(List<F> files, boolean complete, boolean isVerifying)
			throws IOException;

	/**
	 * closes all files under this controller
	 */
	public void close();

	/**
	 * close the given file and reopen it for reading
	 */
	public void setReadOnly(F completed) throws IOException;

	/**
	 * 
	 * @param position
	 *            the position in the file where to start reading
	 * @param buf
	 *            the array to write the read bytes to
	 * @param offset
	 *            the offset in the array where to start storing the bytes read
	 * @param length
	 *            the number of bytes to read to the array
	 * @return the number of bytes read
	 * @throws IOException
	 */
	public int read(long position, byte[] buf, int offset, int length) throws IOException;
	
	/**
	 * Flushes any changes to disk.
	 * @throws IOException if flushing fails.
	 */
	public void flush() throws IOException;

}