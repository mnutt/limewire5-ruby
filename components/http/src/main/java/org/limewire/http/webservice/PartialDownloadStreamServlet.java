package org.limewire.http.webservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.search.SearchManager;
import org.limewire.core.impl.download.CoreDownloadListManager;
import org.limewire.core.impl.search.SearchManagerImpl.SearchWithResults;
import org.limewire.io.GUID;

import com.limegroup.gnutella.URN;

public class PartialDownloadStreamServlet extends HttpServlet {
    private CoreDownloadListManager downloadManager;
    private LibraryManager libraryManager;
    private SearchManager searchManager;

    public PartialDownloadStreamServlet(CoreDownloadListManager downloadManager, SearchManager searchManager, LibraryManager libraryManager) {
        this.downloadManager = downloadManager;
        this.libraryManager = libraryManager;
        this.searchManager = searchManager;
    }
    
    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) 
      throws ServletException, IOException {
        URN urn = URN.createSHA1Urn("urn:sha1:"+httpServletRequest.getParameter("urn"));
        GUID guid = new GUID(httpServletRequest.getParameter("guid"));
        
        SearchWithResults searchWithResults = searchManager.getSearchByGuid(guid);
        
        if(libraryManager.getLibraryManagedList().contains(urn)) {
            
            // File is already in the library; just use that
            File libraryFile = libraryManager.getLibraryManagedList().getFileDescsByURN(urn).get(0).getFile();
            
            // Send library file to client
            this.streamFile(libraryFile, null, httpServletResponse, (int) libraryFile.length());
            
        } else {
            
            // First try to retrieve the in-progress download
            DownloadItem download = downloadManager.getDownloadItem(urn);
            
            // If there wasn't an in-progress download, see if we can start one
            if(download == null) {
                if(searchWithResults != null) {
                    // File hasn't been downloaded yet, initiate download
                    download = downloadManager.addDownload(searchWithResults.getSearch(), searchWithResults.getSearchResultsFromUrn(urn));
                } else {
                    // Nothing found, bail
                    System.out.println("No file found from library, download, or search result!");
                    httpServletResponse.getOutputStream().close();
                    return;
                }
            }
            // Wait until we have some data
            waitForDownload(download);
            
            // Send data to client
            this.streamFile(null, download, httpServletResponse, (int) download.getTotalSize());
        }
        
        httpServletResponse.getOutputStream().close();
    }
    
    private void waitForDownload(DownloadItem downloadItem) {
        int tries = 20;
        while(downloadItem.getLaunchableFile() == null && tries > 0) {
            System.out.println("Stream not ready yet, sleeping 500ms");
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            tries--;
        }
    }
    
    private void streamFile(File file, DownloadItem downloadItem, HttpServletResponse response, int totalSize) throws IOException {
        response.setContentLength(totalSize);
        response.setContentType("audio/mpeg");
        response.flushBuffer();
        System.out.println("Total size is " + totalSize);
        
        ServletOutputStream outputStream = response.getOutputStream();
        
        int offset = 0;
        while(offset < totalSize) {
            File partial = file;
            if(downloadItem != null) {
                partial = downloadItem.getLaunchableFile();
            }
            
            if(partial.length() - offset < 1024) {
                try {
                    System.out.println("File is still " + offset + " bytes, sleeping 500ms...");
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
            
                byte data[] = new byte[1024];
                int read = 0; // Amount read in this session
            
                // Get data from the partial file
                FileInputStream stream = new FileInputStream(partial);
                stream.skip(offset);
                while((read = stream.read(data)) != -1) {
                    outputStream.write(data, 0, read);
                    outputStream.flush();
                }
            
                // Next time, start from where we left off
                offset = (int) partial.length();
                System.out.println("Read "+partial.length()+" bytes, next time starting at" + offset + ".");
            }
        }
    }
}
