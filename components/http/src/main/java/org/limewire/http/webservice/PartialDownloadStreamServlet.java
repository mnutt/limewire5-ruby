package org.limewire.http.webservice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.impl.download.CoreDownloadListManager;
import org.mortbay.jetty.Request;

import com.limegroup.gnutella.URN;

public class PartialDownloadStreamServlet extends HttpServlet {
    private CoreDownloadListManager downloadManager;

    public PartialDownloadStreamServlet(CoreDownloadListManager downloadManager) {
        this.downloadManager = downloadManager;
    }
    
    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) 
      throws ServletException, IOException {
        httpServletResponse.setContentType("audio/mpeg");
        
        int offset = 0;
        String urn = httpServletRequest.getParameter("urn");
        DownloadItem download = downloadManager.getDownloadItem(URN.createSHA1Urn("urn:sha1:"+urn));
        ServletOutputStream outputStream = httpServletResponse.getOutputStream();
        
        httpServletResponse.setContentLength((int) download.getTotalSize());
        httpServletResponse.flushBuffer();
        System.out.println("Total size is " + download.getTotalSize());

        while(offset < download.getTotalSize()) {
            File partial = download.getLaunchableFile();
            
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

        outputStream.close();
    }
}
