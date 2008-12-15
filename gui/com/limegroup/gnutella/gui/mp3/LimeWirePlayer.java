package com.limegroup.gnutella.gui.mp3;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.ThreadExecutor;

import com.limegroup.gnutella.gui.RefreshListener;
import static com.limegroup.gnutella.gui.mp3.PlayerState.*;

/**
 *  An audio player to play compressed and uncompressed music.
 */
public class LimeWirePlayer implements Runnable, AudioPlayer, RefreshListener {
   
    /**
     * Sleep time is for when the song is loaded or paused but not playing.
     */
    private static final long SLEEP_NONPLAYING = 100;
    
    /**
     * Sleep time for when the song is playing but the sourceLine is full and 
     * no reading/writing occurs
     */
    private static final long SLEEP_PLAYING = 150;

    /**
     *  Maximum size of the buffer to read/write from
     */
    public static final int EXTERNAL_BUFFER_SIZE = 4096 * 4;

    /**
     * Our list of AudioPlayerListeners that are currently listening for events
     * from this player
     */
    private List<AudioPlayerListener> listenerList = new CopyOnWriteArrayList<AudioPlayerListener>();
    
    /**
     * main thread that does the audio IO
     */
    private Thread playerthread;

    /**
     * Synchronized holder for reading/writing the next song to be played
     */
    private final LoadSongBuffer songBuffer;   
    
    /**
     * Used in playerThread to sleep when player is paused
     */
    private final Object threadLock = new Object();
       
    /**
     * Current state of the player
     */
    private volatile PlayerState playerState = UNKNOWN;
    
    
    private Object seekLock = new Object();
    /**
     * byte location to skip to in file
     */
    private long seekValue = -1;
    
    /**
     * true==the thread should close the current song and load the next song
     */
    private volatile boolean loadSong = false;
    
    
    private Object volumeLock = new Object();
    /**
     * true== the thread should update the volume on the sourceDataLine 
     */
    private boolean setVolume = false;
    
    /**
     * the current volume of the player
     */
    private double volume = 0;

    /**
     * Contains the Input and Output streams for the IO
     * only <code>playerThread</code> should touch this
     */
    private LimeAudioFormat currentAudioFormat;
    
    /**
     * The source that the thread is currently reading from
     */
    private AudioSource currentSong;
    
    /**
     * buffer for reading from input stream/ writing to the data line
     */
    private final byte[] buffer = new byte[EXTERNAL_BUFFER_SIZE];
    
    /**
     * bytes read from the input stream
     */
    private int readBytes = 0;
    
    /**
     * available bytes that can be written to the sourceDataLine
     */
    private int avail;

    public LimeWirePlayer() {
        songBuffer = new LoadSongBuffer();
    }

    /**
     * Adds the specified AudioPlayer listener to the list
     */
    public void addAudioPlayerListener(AudioPlayerListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes the specified AudioPlayer listener from the list
     */
    public void removeAudioPlayerListener(AudioPlayerListener listener) {
        listenerList.remove(listener);
    }

    /**
     * Converts the playerstate from ints to PlayerState enums
     */
    public PlayerState getStatus() {
        return playerState;
    }
    
    /**
     * Loads a AudioSource into the player to play next
     */
    public void loadSong(AudioSource source) {
        if( source == null )
            throw new IllegalArgumentException();
        songBuffer.setSong(source);
    }

    /**
     * Begins playing a song
     */
    public void playSong() {
        loadSong = true;
        playerState = PLAYING;
        if( (playerthread == null || !playerthread.isAlive()) ){ 
            playerthread = ThreadExecutor.newManagedThread(this, "LimewirePlayer");
            playerthread.setDaemon(true);
            playerthread.start();
        }
        notifyEvent(PLAYING, -1);
    }

    /**
     * Pausing the current song
     */
    public void pause() {
        if( !(playerState == UNKNOWN || playerState == STOPPED)){
            playerState = PAUSED;
            notifyEvent(PAUSED, -1);
        }
    }

    /**
     * Unpauses the current song
     */
    public void unpause() {
        if( !(playerState == UNKNOWN || playerState == STOPPED)){
            playerState = PLAYING;
            notifyEvent(PLAYING, -1);
        }
    }

    /**
     * Stops the current song
     */
    public void stop() {
        if( !(playerState == UNKNOWN || playerState == STOPPED)) {
            playerState = STOPPED;
            notifyEvent(STOPPED, -1);
        }
    }

    /**
     * Seeks to a new location in the current song
     */
    public long seekLocation(long value) {
        if( !(playerState == UNKNOWN || playerState == STOPPED) ) {
            if( playerState == PAUSED || playerState == SEEKING_PAUSED )
                playerState = SEEKING_PAUSED;
            else
                playerState = SEEKING_PLAY;
            synchronized (seekLock) {
                seekValue = value;
            }
            notifyEvent(SEEKING,value);
        }
        return value;
    }
    
    /**
     * Sets the gain(volume) for the outputline
     * 
     * @param gain - [0.0 <-> 1.0]
     * @throws IOException - thrown when the soundcard does not support this
     *         operation
     */
    public void setVolume(double fGain) {
        synchronized (volumeLock) {
            volume = fGain;
            setVolume = true;
        }
    }  

    /**
     * Handles all the IO for reading and writing a song to the sound card.
     */
    public void run() {
        // while the thread is not stopped or unknown, keep alive
        while( playerState != UNKNOWN ) {

            // after stopping, wait a few milliseconds in case a new song is loaded to
            //  avoid recreating the thread
            if( playerState == STOPPED ){
                for(int i = 0; i < 3; i++) {
                    if( playerState == STOPPED )
                        try {
                            synchronized (threadLock) {
                                threadLock.wait(SLEEP_NONPLAYING);
                            }
                        } catch (InterruptedException e) { // dont catch
                        }
                }
                if( playerState == STOPPED )
                    playerState = UNKNOWN;
            }
            // update the volume if changed
            if( currentAudioFormat != null && setVolume) {
                try {                  
                    double vol = 0;
                    synchronized (volumeLock) {
                        vol = volume;
                        setVolume = false;
                    }
                    currentAudioFormat.setGain(vol);
                    notifyEvent(GAIN, volume);
                    
                } catch (IOException e) { 
                } 
            }
            // load a new song
            if( loadSong ){
                if( currentAudioFormat != null )
                    currentAudioFormat.closeStreams();    
                loadFromSongBuffer();
                if( currentSong != null ) 
                    loading();
                else
                    playerState = STOPPED;
            }
            // play the song
            if( playerState == PLAYING ){
                if(currentAudioFormat == null )
                    playerState = STOPPED;
                else 
                    playing();
            }
            // pause the song
            else if( playerState == PAUSED ){
                if( currentAudioFormat == null )
                    playerState = STOPPED;
                else
                    pausing();
            } 
            // seek to a new position in the song
            else if( playerState == SEEKING || playerState == SEEKING_PAUSED ||
                    playerState == SEEKING_PLAY){   
                seeking();
            }
        }
        if( currentAudioFormat != null)
            currentAudioFormat.closeStreams();
        currentAudioFormat = null;
        playerState = UNKNOWN;
    }
    
    /**
     * Attempts to remove the next song for playing from the songBuffer.
     * If there is no song waiting, the current song is placed back on
     * the buffer in case play() is pressed again prior to loading a new
     * song
     */
    private void loadFromSongBuffer(){
        // if another song to play, load it and keep playing
        if( songBuffer.hasSong() ) { 
            currentSong = songBuffer.getSong();
            loadSong = false;
        }
        // if no other song to play
        else {
            loadSong = false;
            currentSong = null;
            playerState = STOPPED;
        }
    }
    
    /**
     *  Processes loading the current song
     */
    private void loading(){          
        try {
            currentAudioFormat = new LimeAudioFormat(currentSong, 0);
            readBytes = 0;
            notifyOpened(currentAudioFormat.getProperties());
            playerState = PLAYING;
        } catch (IllegalArgumentException e ) {
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        } catch (UnsupportedAudioFileException e) {
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        } catch (IOException e) {
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        } catch (LineUnavailableException e) {
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        } catch (NullPointerException e) {
            playerState = STOPPED;
            notifyEvent(EOM, -1);
        }
    }
    
    /**
     * Processes playing the current song
     */
    private void playing() {
        currentAudioFormat.startSourceDataLine();
        avail = currentAudioFormat.getSourceDataLine().available();

        // if we can write without blocking to the audio card, do so
        if (avail > 0) {
            try {
                readBytes = currentAudioFormat.getAudioInputStream().read(
                        buffer, 0, Math.min(avail,buffer.length));
            } 
            // TODO: this is a hack for capturing problems in the JLayer decoder. The problems
            //  should really fixed there but for the time being, this will hide the error from
            //  the user and just proceed to the next song. 
            catch (ArrayIndexOutOfBoundsException e) {
                playerState = STOPPED;  
                notifyEvent(EOM, -1);
                loadSong = true;
            } 
            catch (IOException e) {
                playerState = STOPPED;
            }
            // write whatever we were able to read
            if (readBytes > 0) {
                currentAudioFormat.getSourceDataLine().write(buffer, 0,
                        readBytes);
                notifyProgress(currentAudioFormat.getEncodedStreamPosition());
            }
            // if the end of the song has been reached, see if there's a new song
            //  waiting to be played
            else if (readBytes == -1) { 
                notifyEvent(EOM, -1);
                loadSong = true;
            }
        } else {
            try {
                synchronized (threadLock) {
                    threadLock.wait(SLEEP_PLAYING);
                }
            } catch (InterruptedException e) {
                // dont catch
            }
        }
    }
    
    /**
     * Processes pausing the current song
     */
    private void pausing(){
        currentAudioFormat.stopSourceDataLine();
        try {
            synchronized (threadLock) {
                threadLock.wait(SLEEP_NONPLAYING);  
            }           
        } catch (InterruptedException e) {
            // dont catch
        }
    }
    
    /**
     * Processes a seek to a location in the song
     */
    private void seeking(){
        try {
            
            long seekLocation = 0;
            synchronized (seekLock) {
                seekLocation = seekValue;
                seekValue = 0;
            }

            if( currentAudioFormat != null )
                currentAudioFormat.closeStreams();    
            //open new file and seek to location
            currentAudioFormat = new LimeAudioFormat(currentSong, seekLocation);
            //TODO: the above should be replaced once the mp3 decoder is fixed

            // reload new volume
            synchronized (volumeLock) {
                setVolume = true;
            }
            if( playerState == SEEKING_PAUSED )
                playerState = PAUSED;
            else
                playerState = PLAYING;
        } 
        catch (UnsupportedAudioFileException e) {
            playerState = STOPPED;
        } 
        catch (IOException e) {
            playerState = STOPPED;
        } 
        catch (LineUnavailableException e) {
            playerState = STOPPED;
        } 
        catch (NullPointerException e) {
            playerState = STOPPED;
        }
    }
    
    /**
     * Notify listeners when a new audio source has been opened. 
     * 
     * @param properties - any properties about the source that we extracted
     */
    protected void notifyOpened(final Map<String,Object> properties){
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                fireOpened(properties);
            }
        });
    }

    /**
     * Notify listeners about an AudioPlayerEvent. This creates general state
     * modifications to the player such as the transition from opened to 
     * playing to paused to end of song.
     * 
     * @param code - the type of player event.
     * @param position in the stream when the event occurs.
     * @param value if the event was a modification such as a volume update,
     *        list the new value
     */
    protected void notifyEvent(final PlayerState state, final double value) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireStateUpdated(new AudioPlayerEvent(state,value));
            }
        });
    }

    /**
     * fires a progress event off a new thread. This lets us safely fire events
     * off of the player thread while using a lock on the input stream
     */
    protected void notifyProgress(final int bytesread) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireProgress(bytesread);
            }
        });
    }

    /**
     * This is fired everytime a new song is loaded and ready to play. The
     * properties map contains information about the type of song such as bit
     * rate, sample rate, media type(MPEG, Streaming,etc..), etc..
     */
    protected void fireOpened(Map<String, Object> properties) {
        for (AudioPlayerListener listener : listenerList)
            listener.songOpened(properties);
    }

    /**
     * Fired everytime a byte stream is written to the sound card. This lets 
     * listeners be aware of what point in the entire file is song is currnetly
     * playing. This also returns a copy of the written byte[] so it can get
     * passed along to objects such as a FFT for visual feedback of the song
     */
    protected void fireProgress(int bytesread) {
        for (AudioPlayerListener listener : listenerList)
            listener.progressChange(bytesread);
    }

    /**
     * Fired everytime the state of the player changes. This allows a listener
     * to be aware of state transitions such as from OPENED -> PLAYING ->
     * STOPPED -> EOF
     */
    protected void fireStateUpdated(AudioPlayerEvent event) {
        for (AudioPlayerListener listener : listenerList)
            listener.stateChange(event);
    }


    /**
     * returns the current state of the player and position of the song being
     * played
     */
    public void refresh() {
        notifyEvent(getStatus(), -1);
    }

    
    /**
     * Holds a reference to the next song to be played
     */
    private class LoadSongBuffer {
        
        private AudioSource nextItem;
        
        public synchronized void setSong(AudioSource song){ 
            nextItem = song;
        }
        
        /**
         * @return the next song to be played, returns null if no new song is 
         * awaiting play
         */
        public synchronized AudioSource getSong(){
            AudioSource next = nextItem; 
            nextItem = null;
            return next;
        }
        
        public synchronized boolean hasSong(){
            return nextItem != null;
        }
    }
    
}