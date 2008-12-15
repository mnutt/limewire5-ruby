package com.limegroup.gnutella.version;

/**
 * Simple interface for retrieving the most recent update info.
 */
public interface UpdateInformation extends DownloadInformation {
    
    public String getUpdateURL();
    
    public String getUpdateText();
    
    public String getUpdateTitle();
    
    public String getUpdateVersion();
    
    public String getButton1Text();
    
    public String getButton2Text();
    
    public int getUpdateStyle();
}