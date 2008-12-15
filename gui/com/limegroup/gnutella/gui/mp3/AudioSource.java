package com.limegroup.gnutella.gui.mp3;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *  A wrapper for the source of an audio file that is currently playing
 */
public class AudioSource {
    
    /**
     * current audio source that is loaded in the music player
     */
    private final File file;
    
    private final URL url;
    
    private final InputStream inputStream;
    
    
    public AudioSource(File file){
        if( file == null )
            throw new NullPointerException("File cannot be null");
        
        this.file = file;
        url = null;
        inputStream = null;
    }
    
    public AudioSource(URL url){
        if( url == null )
            throw new NullPointerException("URL cannot be null");
        
        this.url = url;
        file = null;
        inputStream = null;
    }
    
    public AudioSource(InputStream stream){
        if( stream == null )
            throw new NullPointerException("InputStream cannot be null");
        
        this.inputStream = stream;
        file = null;
        url = null;
    }
    
    public File getFile(){
        return file;
    }
    
    public InputStream getStream(){
        return inputStream;
    }

    public URL getURL(){
        if( url != null )
            return url;
        else if( file != null )
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
            }
        return null;
    }
}
