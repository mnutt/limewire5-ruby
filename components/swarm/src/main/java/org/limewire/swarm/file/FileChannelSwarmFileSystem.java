package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

import org.limewire.collection.Range;
import org.limewire.io.IOUtils;
import org.limewire.swarm.SwarmFile;
import org.limewire.swarm.SwarmFileSystem;

public class FileChannelSwarmFileSystem implements SwarmFileSystem {
    private final SwarmFile swarmFile;

    private RandomAccessFile raFile;

    private FileChannel fileChannel;

    private final Object LOCK = new Object();

    /**
     * Constructor for this single file filesytem.
     * @param completeSize complete size of this file system.
     * @param file file backing this file system
     */
    public FileChannelSwarmFileSystem(long completeSize, File file) {
        this.swarmFile = new SwarmFileImpl(file, completeSize);
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#write(java.nio.ByteBuffer, long)
     */
    public long write(ByteBuffer byteBuffer, long start) throws IOException {
        synchronized (LOCK) {
            initialize();
            long written = fileChannel.write(byteBuffer, start);
            return written;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#read(java.nio.ByteBuffer, long)
     */
    public long read(ByteBuffer byteBuffer, long position) throws IOException {
        synchronized (LOCK) {
            initialize();
            long read = fileChannel.read(byteBuffer, position);
            return read;
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#getCompleteSize()
     */
    public long getCompleteSize() {
        return swarmFile.getFileSize();
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#close()
     */
    public void close() throws IOException {
        synchronized (LOCK) {
            if (raFile != null) {
                IOUtils.close(fileChannel);
                IOUtils.close(raFile);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#initialize()
     */
    public void initialize() throws IOException {
        synchronized (LOCK) {
            if (raFile == null) {
                raFile = new RandomAccessFile(swarmFile.getFile(), "rw");
                fileChannel = raFile.getChannel();
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#getSwarmFile(long)
     */
    public SwarmFile getSwarmFile(long position) {
        return swarmFile;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#getSwarmFiles()
     */
    public List<SwarmFile> getSwarmFiles() {
        ArrayList<SwarmFile> files = new ArrayList<SwarmFile>();
        files.add(swarmFile);
        return files;
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#closeSwarmFile(org.limewire.swarm.SwarmFile)
     */
    public void closeSwarmFile(SwarmFile swarmFile) throws IOException {
        close();
    }

    /*
     * (non-Javadoc)
     * @see org.limewire.swarm.SwarmFileSystem#getSwarmFiles(org.limewire.collection.Range)
     */
    public List<SwarmFile> getSwarmFiles(Range range) {
        return getSwarmFiles();
    }
}
