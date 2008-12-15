package org.limewire.swarm.file;

import java.io.File;

import org.limewire.swarm.SwarmFile;
import org.limewire.util.Objects;

public class SwarmFileImpl implements SwarmFile {
    private final File file;

    private final long completeSize;

    private long startByte = 0;

    private String path;

    /**
     * Contructor for the SwarmFileImpl. If the given path is null, the given
     * file's name is uses as the path instead.
     * 
     * @param file
     * @param path
     * @param completeSize
     */
    public SwarmFileImpl(File file, String path, long completeSize) {
        this.file = Objects.nonNull(file, "file");
        assert completeSize >= 0;
        this.completeSize = completeSize;
        this.path = path;
        if (this.path == null) {
            this.path = file.getName();
        }
    }

    /**
     * Contructor for the SwarmFileImpl. The given file's name is uses as the
     * path.
     * 
     * @param file
     * @param completeSize
     */
    public SwarmFileImpl(File file, long completeSize) {
        this(file, file.getName(), completeSize);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getFile()
     */
    public File getFile() {
        return file;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getPath()
     */
    public String getPath() {
        return path;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getCompleteSize()
     */
    public long getFileSize() {
        return completeSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getStartByte()
     */
    public long getStartBytePosition() {
        return startByte;
    }

    /**
     * Method to allow the SwarmFileSystemImpl to set the startByte of its
     * files.
     * 
     * @param startByte
     */
    public void setStartByte(long startByte) {
        this.startByte = startByte;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.limewire.swarm.file.SwarmFile#getEndByte()
     */
    public long getEndBytePosition() {
        long endByte = getFileSize() > 0 ? getStartBytePosition() + getFileSize() - 1 : getStartBytePosition();
        return endByte;
    }
}
