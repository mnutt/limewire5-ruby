package com.limegroup.gnutella.uploader.authentication;

import java.util.Collections;

import org.apache.http.protocol.HttpContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.SharedFileList;

/**
 * Returns the file list for a public Gnutella browse of the client's shared
 * files.
 */
@Singleton
public class GnutellaBrowseFileListProvider implements HttpRequestFileListProvider {

    private final FileManager fileManager;

    @Inject
    public GnutellaBrowseFileListProvider(FileManager fileManager) {
        this.fileManager = fileManager;
    }
    
    /**
     * @return {@link FileManager#getGnutellaFileList()}.
     */
    @Override
    public Iterable<SharedFileList> getFileLists(String userID, HttpContext httpContext) {
        // Broken into two lines so it returns the correct type.
        SharedFileList fileList = fileManager.getGnutellaFileList();
        return Collections.singletonList(fileList);
    }

}