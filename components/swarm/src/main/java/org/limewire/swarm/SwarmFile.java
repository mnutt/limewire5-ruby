package org.limewire.swarm;

import java.io.File;

/**
 * 
 *
 */
public interface SwarmFile {

    /**
     * Returns the local file backing this download.
     * 
     * @return
     */
    public abstract File getFile();

    /**
     * The total file size of the complete file.
     * 
     * @return
     */
    public abstract long getFileSize();

    /**
     * Returns the position of start byte for this file, in whatever file system it is represented
     * by.
     */
    public abstract long getStartBytePosition();

    /**
     * The position of the end byte for this file, in whatever file system it is represented by.
     * This is inclusive, the byte at this position belongs to this file.
     */
    public abstract long getEndBytePosition();

    /**
     * Returns the base path for this file. Can be used for remote and local
     * repositories. The path may contain subfolders to represent the full path
     *  to the file.
     * 
     * @return
     */
    public abstract String getPath();

}