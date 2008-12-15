package com.limegroup.gnutella.downloader.serial;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.io.IOUtils;
import org.limewire.util.ConverterObjectInputStream;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadSerializerImpl implements DownloadSerializer {
    
    private static final Log LOG = LogFactory.getLog(DownloadSerializerImpl.class);
    
    private final DownloadSerializeSettings downloadSerializeSettings;
    
    @Inject
    public DownloadSerializerImpl(DownloadSerializeSettings downloadSerializeSettings) {
        this.downloadSerializeSettings = downloadSerializeSettings;
    }
    
    /**
     * Reads all saved downloads from disk.
     * 
     * This works by first attempting to read from the save file described in the settings,
     * and then attempting to read from the backup file if there were any errors while
     * reading the normal file.  If both files fail, this returns an empty list.
     */
    public List<DownloadMemento> readFromDisk() throws IOException {
        if(!downloadSerializeSettings.getSaveFile().exists() && !downloadSerializeSettings.getSaveFile().exists())
            return Collections.emptyList();
        
        Throwable exception;
        ObjectInputStream in = null;
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getSaveFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            exception = ignored;
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        // Falls through to here only on error with normal file.
        
        try {
            in = new ConverterObjectInputStream(new BufferedInputStream(new FileInputStream(downloadSerializeSettings.getBackupFile())));
            return GenericsUtils.scanForList(in.readObject(), DownloadMemento.class, GenericsUtils.ScanMode.REMOVE);
        } catch(Throwable ignored) {
            LOG.warn("Error reading normal file.", ignored);
        } finally {
            IOUtils.close(in);
        }
        
        if(exception instanceof IOException)
            throw (IOException)exception;
        else
            throw (IOException)new IOException().initCause(exception);
    }
    
    /**
     * Writes the mementos to disk.  This works by first writing to the backup file
     * and then renaming the backup file to the save file.  If the backup file cannot
     * be written, this fails.
     */
    public boolean writeToDisk(List<? extends DownloadMemento> mementos) {
        // Follows this process:
        // 1) Write backup file.
        // 2) Try to rename save file to a temporary file
        //   a) If success, continue.  If failure, delete save file.
        // 3) Rename backup file to save file.
        //   a) If success, return true.  Delete temp file.
        //      If failure, revert temp file back to save file, return false.
        
        File backupFile = downloadSerializeSettings.getBackupFile();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(backupFile)));
            out.writeObject(mementos);
        } catch(IOException iox) {
            LOG.warn("Unable to write to backup file!", iox);
            return false;
        } finally {
            IOUtils.close(out);
        }
        
        File saveFile = downloadSerializeSettings.getSaveFile();
        if(saveFile.equals(backupFile)) {
            LOG.debug("backup == save, nothing more to do");
            return true;
        }
        
        File saveDir = saveFile.getParentFile();
        File tmpFile = null;
        try {
            tmpFile = FileUtils.createTempFile("lwc", "tmp", saveDir);
        } catch(IOException ignored) {
            LOG.warn("Error creating temp file", ignored);
        }
        
        // If we could make a temp file, rename save to that.
        if(tmpFile != null) {
            tmpFile.delete();
            if(!saveFile.renameTo(tmpFile)) {
                LOG.debug("Unable to rename save to temp, deleting instead!");
                saveFile.delete();
            }
        } else {
            saveFile.delete();
        }
        
        boolean renamed = backupFile.renameTo(downloadSerializeSettings.getSaveFile());
        if(tmpFile != null) {
            // If we couldn't rename, but we did create the tmp file,
            // revert that back to the save file.
            if(!renamed)
                tmpFile.renameTo(saveFile);
            tmpFile.delete();
        }
        return renamed;
    }
}
