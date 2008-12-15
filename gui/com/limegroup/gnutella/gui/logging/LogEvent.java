package com.limegroup.gnutella.gui.logging;

import java.io.File;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.uploader.UploadType;

public class LogEvent {
    
    private final long time;
    private final LogType type;
    private final String message;
    
    public LogEvent(Downloader d) {
        this.time = System.currentTimeMillis();
        this.type = LogType.DOWNLOAD;
        File f = d.getSaveFile();
        
        if (d.getAmountRead() == d.getContentLength()) {
            this.message = "<html>Download Finished - <b>"
                + (f == null ? "Unknown" : f.getName())
                + "</b> <i>("
                + GUIUtils.toUnitbytes(d.getContentLength())
                + ")</i></html>";
        }
        else {
            this.message = "<html>Download Cancelled - <b>"
                + (f == null ? "Unknown" : f.getName())
                + "</b> <i>("
                + GUIUtils.toUnitbytes(d.getAmountRead())
                + " of "
                + GUIUtils.toUnitbytes(d.getContentLength())
                + ")</i></html>";
        }
    }
    
    public LogEvent(Uploader u) {
        this.time = System.currentTimeMillis();
        if(!u.getUploadType().isInternal()) {
            this.type = LogType.UPLOAD;
            this.message = "<html>Upload Started - <b>"
                + u.getFileName()
                + "</b> <i>(" 
                + GUIUtils.toUnitbytes(u.getFileSize())
                + ")</i></html>";
        } else if(u.getUploadType() == UploadType.BROWSE_HOST) {
            this.type = LogType.BROWSE_HOST;
            this.message = "<html>Host browsed by <b>"
                + u.getHost()
                + "</b></html>";
        } else {
            this.type = LogType.UPLOAD;
            this.message = "<html>Internal Upload</html>";
        }
    }
    
    long getTime() {
        return time;
    }
    
    LogType getType() {
        return type;
    }
    
    String getMessage() {
        return message;
    }

}
