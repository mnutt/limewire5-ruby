package org.limewire.core.api.download;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.limewire.core.api.library.RemoteFileItem;
import org.limewire.core.api.search.Search;
import org.limewire.core.api.search.SearchResult;

public interface ResultDownloader {

    /**
     * Adds a download triggered by the given search results. The search results
     * must all be for the same item, otherwise an
     * {@link IllegalArgumentException} may be thrown.
     * 
     * @param search The search that triggered these results. This may be null.
     * @param coreSearchResults The results for the file that should be
     *        downloaded. A list is used to indicate that multiple sources can
     *        be swarmed from at the same time. The list is not intended to
     *        provide different downloads.
     * @param saveFile The location to save this file to.
     * @param overwrite Whether or not to automatically overwrite any other files at the saveFileLocation
     */
    public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults, File saveFile, boolean overwrite)
        throws SaveLocationException;
    
    /**
     * Adds a download triggered by the given search results. The search results
     * must all be for the same item, otherwise an
     * {@link IllegalArgumentException} may be thrown.
     * 
     * @param search The search that triggered these results. This may be null.
     * @param coreSearchResults The results for the file that should be
     *        downloaded. A list is used to indicate that multiple sources can
     *        be swarmed from at the same time. The list is not intended to
     *        provide different downloads.
     */
    public DownloadItem addDownload(Search search, List<? extends SearchResult> coreSearchResults)
            throws SaveLocationException;

    /**
     * Adds a download specified by the given RemoteFileItem
     * 
     * @throws IOException if an error occurs while downloading and saving the
     *         file
     * 
     */
    public DownloadItem addDownload(RemoteFileItem fileItem) throws SaveLocationException;
    

    /**
     * Adds a download specified by the given RemoteFileItem
     * 
     * @param saveFile, if non null this file is used as teh file name to save as.
     * @param overwrite if true the downloader will overwrite preexisting downloads with the same name.
     * @throws SaveLocationException 
     * 
     * @throws IOException if an error occurs while downloading and saving the
     *         file
     * 
     */
    public DownloadItem addDownload(RemoteFileItem file, File saveFile, boolean overwrite) throws SaveLocationException;

    
    
}