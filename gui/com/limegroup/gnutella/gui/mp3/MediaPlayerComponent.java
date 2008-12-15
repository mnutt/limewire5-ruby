package com.limegroup.gnutella.gui.mp3;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.LWSSettings;
import org.limewire.lws.server.LWSDispatcherSupport;
import org.limewire.lws.server.LWSServerUtil;
import org.limewire.service.ErrorService;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MediaButton;
import com.limegroup.gnutella.gui.MediaSlider;
import com.limegroup.gnutella.gui.RefreshListener;
import com.limegroup.gnutella.gui.playlist.PlaylistMediator;
import com.limegroup.gnutella.gui.themes.ThemeMediator;
import com.limegroup.gnutella.gui.themes.ThemeObserver;
import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.lws.server.LWSManagerCommandResponseHandlerWithCallback;
import com.limegroup.gnutella.lws.server.LWSUtil;
import com.limegroup.gnutella.util.Tagged;
import com.limegroup.gnutella.util.URLDecoder;

/**
 * This class sets up JPanel with MediaPlayer on it, and takes care of GUI
 * MediaPlayer events.
 */
public final class MediaPlayerComponent implements AudioPlayerListener, RefreshListener,
        ThemeObserver {

    private static final Log LOG = LogFactory.getLog(MediaPlayerComponent.class);

    private static final String MP3 = "mp3";

    private static final String WAVE = "wave";

    public static final String STREAMING_AUDIO = "Streaming Audio";

    /**
     * The sole instance.
     */
    private static MediaPlayerComponent INSTANCE = null;

    /**
     * How fast to scroll the song title if it is too long in milliseconds
     */
    private static final long SCROLL_RATE = 200;

    /**
     * The maximum characters to show in the progress bar.
     */
    private static final int STRING_SIZE_TO_SHOW = 20;
    
    /**
     * Width needed to fully display everything in the media player. If this width
     * isn't available, the progress and volume bar collapse
     */
    public final int fullSizeWidth = 351;
    
    /**
     *  Minimum width needed to display just the buttons
     */
    public final int minWidth = 240;

    /**
     * Constant for the play button.
     */
    private static final MediaButton PLAY_BUTTON = new MediaButton(I18n.tr("Play"),
            "play_up", "play_dn");

    /**
     * Constant for the pause button.
     */
    private static final MediaButton PAUSE_BUTTON = new MediaButton(I18n.tr("Pause"),
            "pause_up", "pause_dn");

    /**
     * Constant for the stop button.
     */
    private static final MediaButton STOP_BUTTON = new MediaButton(I18n.tr("Stop"),
            "stop_up", "stop_dn");

    /**
     * Constant for the forward button.
     */
    private static final MediaButton NEXT_BUTTON = new MediaButton(I18n.tr("Next"),
            "forward_up", "forward_dn");

    /**
     * Constant for the rewind button.
     */
    private static final MediaButton PREV_BUTTON = new MediaButton(I18n.tr("Previous"),
            "rewind_up", "rewind_dn");

    /**
     * Constant for the volume control
     */
    private static final MediaSlider VOLUME = new MediaSlider("volume_track_left",
            "volume_track_center", "volume_track_right", "volume_thumb_up", "volume_thumb_dn");

    /**
     * Constant for the progress bar
     */
    private static final SongProgressBar PROGRESS = new SongProgressBar("progress_track_left",
            "progress_track_center", "progress_track_right", "progress_thumb_up",
            "progress_thumb_dn", "progress_bar");

    /**
     * Executor to ensure all thread creation on the limewireplayer is called from
     * a single thread
     */
    private static final ExecutorService SONG_QUEUE = ExecutorsHelper
            .newProcessingQueue("SongProcessor");

    /**
     * The MP3 player.
     */
    private final AudioPlayer PLAYER;

    /**
     * The ProgressBar dimensions for showing the name & play progress.
     */
    private final Dimension progressBarDimension = new Dimension(129, 19);

    /**
     * Volume slider dimensions for adjusting the audio level of a song
     */
    private final Dimension volumeSliderDimension = new Dimension(63, 19);

    /**
     * Index of where to display the name in the progress bar.
     */
    private volatile int currBeginIndex = -1;

    /**
     * Current properties of the song being played
     */
    private Map audioProperties;

    /**
     * The last time the scrolling song was shifted
     */
    private long lastScroll = System.currentTimeMillis();

    /**
     * The current song that is playing
     */
    private PlayListItem currentPlayListItem;

    /**
     * The lazily constructed media panel.
     */
    private JPanel myMediaPanel = null;

    /**
     * Variable for the name of the current file being played.
     */
    private String currentFileName;
    
    /**
     * If true, will only play current song and stop, regradless of position
     * in the playlist or value of continous or random. If false, continous
     * and random control the play feature
     */
    private boolean playOneTime = false;

    /**
     * Lock for access to the above String.
     */
    private final Object cfnLock = new Object();

    /**
     * Constructs a new <tt>MediaPlayerComponent</tt>.
     */
    private MediaPlayerComponent() {
        PLAYER = new LimeWirePlayer();
        PLAYER.addAudioPlayerListener(this);

        GUIMediator.addRefreshListener(this);
        ThemeMediator.addThemeObserver(this);       
    }

    /**
     * Gets the sole instance.
     */
    public static MediaPlayerComponent getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MediaPlayerComponent();
        }
        return INSTANCE;
    }

    /**
     * Gets the media panel, constructing it if necessary.
     */
    public JPanel getMediaPanel() {
        if (myMediaPanel == null)
            myMediaPanel = constructMediaPanel();
        return myMediaPanel;
    }

    /**
     * Constructs the media panel.
     */
    private JPanel constructMediaPanel() {
        int tempWidth = 0, tempHeight = 0;
        tempHeight += PLAY_BUTTON.getIcon().getIconHeight() + 2;
        tempWidth += PLAY_BUTTON.getIcon().getIconWidth() + 2
                + PAUSE_BUTTON.getIcon().getIconWidth() + 2 + STOP_BUTTON.getIcon().getIconWidth()
                + 2 + NEXT_BUTTON.getIcon().getIconWidth() + 2
                + PREV_BUTTON.getIcon().getIconWidth() + 2;

        // create sliders
        PROGRESS.setMaximumSize(progressBarDimension);
        PROGRESS.setPreferredSize(progressBarDimension);
        PROGRESS.setString( I18n.tr("LimeWire Media Player"));
        PROGRESS.setMaximum(3600);
        PROGRESS.setEnabled(false);

        VOLUME.setMaximumSize(volumeSliderDimension);
        VOLUME.setPreferredSize(volumeSliderDimension);
        VOLUME.setMinimum(0);
        VOLUME.setValue(50);
        VOLUME.setMaximum(100);
        VOLUME.setEnabled(true);

        // setup buttons
        registerListeners();

        // add everything
        JPanel buttonPanel = new MediaPlayerPanel(BoxPanel.X_AXIS);
        buttonPanel.setMaximumSize(new Dimension(tempWidth + PROGRESS.getWidth()
                + VOLUME.getWidth(), tempHeight));
        buttonPanel.setMinimumSize(new Dimension(tempWidth, tempHeight));
        buttonPanel.add(Box.createHorizontalGlue());
        buttonPanel.add(VOLUME);
        buttonPanel.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 3));
        buttonPanel.add(PREV_BUTTON);
        buttonPanel.add(PLAY_BUTTON);
        buttonPanel.add(PAUSE_BUTTON);
        buttonPanel.add(STOP_BUTTON);
        buttonPanel.add(NEXT_BUTTON);
        buttonPanel.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR / 3));
        buttonPanel.add(PROGRESS);
        if (OSUtils.isMacOSX())
            buttonPanel.add(Box.createHorizontalStrut(16));
        buttonPanel.add(Box.createHorizontalGlue());

        return buttonPanel;
    }

    public void registerListeners() {
        PLAY_BUTTON.addActionListener(new PlayListener());
        PAUSE_BUTTON.addActionListener(new PauseListener());
        STOP_BUTTON.addActionListener(new StopListener());
        NEXT_BUTTON.addActionListener(new NextListener());
        PREV_BUTTON.addActionListener(new BackListener());
        VOLUME.addChangeListener(new VolumeSliderListener());
        PROGRESS.addChangeListener(new ProgressBarListener());
        
        // For the LimeWire Store
        LWSManager lwsManager = GuiCoreMediator.getLWSManager();
        //
        // Back command
        // INPUT
        //  --
        // OUTPUT
        //  OK
        //
        lwsManager.registerHandler("Back", new LWSManagerCommandResponseHandlerWithCallback("Back") {
            @Override
            protected String handleRest(Map<String, String> args) {
                back();
                return LWSDispatcherSupport.Responses.OK;
            }            
        });
        //
        // Stop command
        // INPUT
        //  --
        // OUTPUT
        //  OK
        //        
        lwsManager.registerHandler("Stop", new LWSManagerCommandResponseHandlerWithCallback("Stop") {
            @Override
            protected String handleRest(Map<String, String> args) {
                stopSong();
                return LWSDispatcherSupport.Responses.OK;
            }            
        });
        //
        // Pause command
        // INPUT
        //  --
        // OUTPUT
        //  OK
        //
        lwsManager.registerHandler("Pause", new LWSManagerCommandResponseHandlerWithCallback("Pause") {
            @Override
            protected String handleRest(Map<String, String> args) {
                pauseSong();
                return LWSDispatcherSupport.Responses.OK;
            }            
        });
        //
        // Play command
        // INPUT
        //  --
        // OUTPUT
        //  OK
        //
        lwsManager.registerHandler("Play", new LWSManagerCommandResponseHandlerWithCallback("Play") {
            @Override
            protected String handleRest(Map<String, String> args) {
                return playSong(args);
            }            
        });
        //
        // INPUT:
        //  url
        // RETURNS:
        //  ok
        lwsManager.registerHandler("PlayURL", new LWSManagerCommandResponseHandlerWithCallback("Play") {
            @Override
            protected String handleRest(Map<String, String> args) {
                playURL(args);
                return LWSDispatcherSupport.Responses.OK;
            }            
        });        
        //
        // Next command
        // INPUT
        //  --
        // OUTPUT
        //  OK
        //
        lwsManager.registerHandler("Next", new LWSManagerCommandResponseHandlerWithCallback("Next") {
            @Override
            protected String handleRest(Map<String, String> args) {
                next();
                return LWSDispatcherSupport.Responses.OK;
            }            
        });
        //
        // INPUT
        //  url
        //  bitrate
        //  name
        //  length
        //
        lwsManager.registerHandler("AddToPlaylist", new LWSManagerCommandResponseHandlerWithCallback("AddToPlaylist") {
            @Override
            protected String handleRest(Map<String, String> args) {
                return addToPlaylist(args);
            }     
        });        
        //
        // RETURNS
        //     <secondsSinceStart> '\t' <lengthOfCurrentTrack>
        //   | 'stopped'
        //
        lwsManager.registerHandler("GetProgress", new LWSManagerCommandResponseHandlerWithCallback("GetProgress") {
            @Override
            protected String handleRest(Map<String, String> args) {
                return getProgress();
            }            
        });
        //
        // RETURNS
        //     [ <song> '\t' <url> '\n' <length> ]*
        //
        lwsManager.registerHandler("GetPlaylist", new LWSManagerCommandResponseHandlerWithCallback("GetPlaylist") {
            @Override
            protected String handleRest(Map<String, String> args) {
                return getSongs();
            }            
        });  
        //
        // INPUT:
        //      index
        // RETURNS:
        //      ok
        //
        lwsManager.registerHandler("RemoveFromPlaylist", new LWSManagerCommandResponseHandlerWithCallback("RemoveFromPlaylist") {
            @Override
            protected String handleRest(Map<String, String> args) {
                Tagged<String> indexString = LWSUtil.getArg(args, "index", "RemoveFromPlaylist");
                if (!indexString.isValid()) return indexString.getValue();
                
                int index = -1;
                try {
                    index = Integer.parseInt(indexString.getValue());
                } catch (NumberFormatException e) {
                    return "invalid.index: " + indexString.getValue();
                }
                return removeFromPlaylist(index);
            }            
        }); 
        //
        // INPUT:
        //      index
        // RETURNS:
        //      ok
        //
        lwsManager.registerHandler("PlayIndexInPlaylist", new LWSManagerCommandResponseHandlerWithCallback("PlayIndexInPlaylist") {
            @Override
            protected String handleRest(Map<String, String> args) {
                Tagged<String> indexString = LWSUtil.getArg(args, "index", "PlayIndexInPlaylist");
                if (!indexString.isValid()) return indexString.getValue();
                
                int index = -1;
                try {
                    index = Integer.parseInt(indexString.getValue());
                } catch (NumberFormatException e) {
                    return "invalid.index: " + indexString.getValue();
                }
                return playIndexInPlaylist(index);
            }            
        });         
        //
        // INPUT:
        //   v - percetage to go
        //
        lwsManager.registerHandler("SetProgress", new LWSManagerCommandResponseHandlerWithCallback("SetProgress") {
            @Override
            protected String handleRest(Map<String, String> args) {
                if (isPlaying()) {
                    double v = Double.parseDouble(args.get("v"));
                    PROGRESS.setValue((int)(v * PROGRESS.getMaximum()));
                    skip(PROGRESS.getValue() * 1.0 / PROGRESS.getMaximum());
                    setProgressValue(PROGRESS.getValue());
                }
                return LWSDispatcherSupport.Responses.OK;
            }            
        });         
    }

    public void unregisterListeners() {
        PLAY_BUTTON.removeActionListener(new PlayListener());
        PAUSE_BUTTON.removeActionListener(new PauseListener());
        STOP_BUTTON.removeActionListener(new StopListener());
        NEXT_BUTTON.removeActionListener(new NextListener());
        PREV_BUTTON.removeActionListener(new BackListener());
        VOLUME.removeChangeListener(new VolumeSliderListener());
        PROGRESS.removeChangeListener(new ProgressBarListener());
        
        // For the LimeWire Store
        LWSManager lwsManager = GuiCoreMediator.getLWSManager();
        lwsManager.unregisterHandler("Back");
        lwsManager.unregisterHandler("Stop");
        lwsManager.unregisterHandler("Pause");
        lwsManager.unregisterHandler("Play");
        lwsManager.unregisterHandler("Next");
        lwsManager.unregisterHandler("GetProgress");
        lwsManager.unregisterHandler("SetProgress");
        lwsManager.unregisterHandler("GetPlaylist");
        lwsManager.unregisterHandler("RemoveFromPlaylist");
    }

    /**
     * Updates the audio player.
     */
    public void refresh() {
        PLAYER.refresh();

        if (getMediaPanel().getSize().width < fullSizeWidth) {
            GUIMediator.safeInvokeLater(new Runnable() {
                public void run() {
                    PROGRESS.setVisible(false);
                    VOLUME.setVisible(false);
                }
            });
        } else {
            GUIMediator.safeInvokeLater(new Runnable() {
                public void run() {
                    PROGRESS.setVisible(true);
                    VOLUME.setVisible(true);
                }
            });
        }
    }

    // inherit doc comment
    public void updateTheme() {
        ((MediaPlayerPanel) getMediaPanel()).updateTheme();
        PLAY_BUTTON.updateTheme();
        PAUSE_BUTTON.updateTheme();
        STOP_BUTTON.updateTheme();
        NEXT_BUTTON.updateTheme();
        PREV_BUTTON.updateTheme();
        PROGRESS.updateTheme();
        PROGRESS.setString( I18n.tr("LimeWire Media Player"));
        VOLUME.updateTheme();
    }

    /**
     * Updates the displayed String on the progress bar, on the Swing thread.
     */
    private void setProgressString(final String update) {
        GUIMediator.safeInvokeLater(new Runnable() {
            public void run() {
                PROGRESS.setString(update);
            }
        });
    }

    /**
     * Updates the current progress of the progress bar, on the Swing thread.
     */
    private void setProgressValue(final int update) {
        GUIMediator.safeInvokeLater(new Runnable() {
            public void run() {
                PROGRESS.setValue(update);
            }
        });
    }
    
    /**
     * Enables or disables the skipping action on the progress bar
     * safely from the swing event queue
     * 
     * @param enabled - true to allow skipping, false otherwise
     */
    private void setProgressEnabled(final boolean enabled) {
        GUIMediator.safeInvokeLater(new Runnable(){ 
            public void run(){
                PROGRESS.setEnabled(enabled);
            }
        });
    }

    /**
     * Updates the volume based on the position of the volume slider
     */
    private void setVolumeValue() {
        VOLUME.repaint();
        PLAYER.setVolume(((float) VOLUME.getValue()) / VOLUME.getMaximum());
    }
           
    /**
     * Public accessor for loading a song to be played. 
     */
    public void loadSong(final PlayListItem item) { 
        loadSong(item, false);
    }
    
    /**
	 * Public accessor for loading a song to be played,
	 * @playOnce - if true, play song one time regardless of continous
     *			and random values and stop the player after completing, 
	 *			if false, observe the continous and	random control 
	 */
    public void loadSong(final PlayListItem item, boolean playOnce) {
        // fail silently if there's nothing to play
        if( item == null || item.getAudioSource() == null)
            return;
        currentPlayListItem = item;
        playOneTime = playOnce;
        
        loadSong(item.getAudioSource(), item.getName() );
    }

    /**
     * Loads an audiosource to be played. 
     */
    private void loadSong(final AudioSource audioSource, String displayName) {
        if( audioSource == null )
            return;

        // load song on Executor thread
        SONG_QUEUE.execute(new SongLoader(audioSource));
        
        // try using the name passed in to the function, if not fallback on
        //  the a default string
        if( displayName != null && displayName.length() > 0)
            currentFileName = generateNameDisplay(displayName);
        else
            currentFileName = generateNameDisplay(STREAMING_AUDIO);
    }
    

    /**
     * Begins playing the loaded song
     */
    public void play() {       
        if (PLAYER.getStatus() == PlayerState.PAUSED || PLAYER.getStatus() == PlayerState.PLAYING)
            PLAYER.unpause();
        else {
            loadSong(currentPlayListItem, playOneTime);
        }
    }

    /**
     * Pauses the currently playing audio file.
     */
    public void pauseSong() {
        if (PLAYER.getStatus() == PlayerState.PAUSED)
            PLAYER.unpause();
        else
            PLAYER.pause();
    }

    /**
     * Stops the currently playing audio file.
     */
    public void stopSong() {
        PLAYER.stop();
    }

    /**
     * Skips the current song to a new position in the song. If the song's
     * length is unknown (streaming audio), then ignore the skip
     * 
     * @param percent of the song frames to skip from begining of file
     */
    public void skip(double percent) {
        // need to know something about the audio type to be able to skip
        if (audioProperties != null && audioProperties.containsKey(LimeAudioFormat.AUDIO_TYPE)) {
            String songType = (String) audioProperties.get(LimeAudioFormat.AUDIO_TYPE);
            
            // currently, only mp3 and wav files can be seeked upon
            if ( isSeekable(songType)
                    && audioProperties.containsKey(LimeAudioFormat.AUDIO_LENGTH_BYTES)) {
                final long skipBytes = Math.round((Integer) audioProperties
                        .get(LimeAudioFormat.AUDIO_LENGTH_BYTES)
                        * percent);
                PLAYER.seekLocation(skipBytes);
            }
        }
    }
    
    private boolean isSeekable(String songType) {
        if( songType == null )
            return false;
        return songType.equalsIgnoreCase(MP3) || songType.equalsIgnoreCase(WAVE);
    }

    /**
     * @return the current song that is playing, null if there is no song loaded
     *         or the song is streaming audio
     */
    public PlayListItem getCurrentSong() {
        return currentPlayListItem;
    }

    /**
     * Modifies the filename that is displayed in the progress bar. If the name
     * is too large to display in one string, *** are added to the end of the
     * name and it rotates like a ticker display
     */
    private String generateNameDisplay(String filename) {
        if (filename.length() > STRING_SIZE_TO_SHOW) {
            filename += " *** " + filename + " *** ";
        }
        currBeginIndex = -1;
        return filename;
    }

    /**
     * This event is thrown everytime a new song is opened and is ready to be
     * played.
     */
    public void songOpened(Map<String, Object> properties) {
        audioProperties = properties;
        setVolumeValue();
        // if we don't know the length of the song, hide the thumb to prevent
        // skipping
        if (audioProperties.containsKey(LimeAudioFormat.AUDIO_LENGTH_BYTES) && 
                isSeekable((String) audioProperties.get(LimeAudioFormat.AUDIO_TYPE))) {
            setProgressEnabled(true);
        } else {
            setProgressEnabled(false);
        }
        
        // notify the playlist to repaint since a new song has started playing
        GUIMediator.getPlayList().playStarted();
    }

    /**
     * This event is thrown a number of times a second. It updates the current
     * frames that have been read, along with position and bytes read
     */
    public void progressChange(int bytesread) {

        // if we know the length of the song, update the progress bar
        if (audioProperties.containsKey(LimeAudioFormat.AUDIO_LENGTH_BYTES)) {
            int byteslength = ((Integer) audioProperties.get(LimeAudioFormat.AUDIO_LENGTH_BYTES))
                    .intValue();

            float progressUpdate = bytesread * 1.0f / byteslength * 1.0f;

            if (!(PROGRESS.getValueIsAdjusting() || PLAYER.getStatus() == PlayerState.SEEKING))
                setProgressValue((int) (PROGRESS.getMaximum() * progressUpdate));
        }

        // if the display name is too long, increment it
        // TODO: this should be replaced by the TimingFramework Animator
        if (currentFileName.length() <= STRING_SIZE_TO_SHOW) {
            setProgressString(currentFileName);
        } else if ((System.currentTimeMillis() - lastScroll) > SCROLL_RATE) {

            synchronized (cfnLock) {
                lastScroll = System.currentTimeMillis();
                if (currentFileName == null)
                    return;

                currBeginIndex = currBeginIndex + 1;
                if (currBeginIndex > currentFileName.length() / 2)
                    currBeginIndex = 0;
                setProgressString(currentFileName.substring(currBeginIndex, currBeginIndex
                        + STRING_SIZE_TO_SHOW));
            }
        }
    }

    /**
     * This event is generated everytime the song state changes. ie.
     * OPENED->PLAYING->PAUSED->PLAYING->STOPPED->EOF, etc..
     */
    public void stateChange(AudioPlayerEvent event) {

        if (event.getState() == PlayerState.UNKNOWN) {
            setProgressEnabled(false);
        }
        else if (event.getState() == PlayerState.OPENED || event.getState() == PlayerState.SEEKED) {
            setVolumeValue();
        }
        else if( event.getState() == PlayerState.STOPPED) {
            setProgressValue(PROGRESS.getMinimum());
        }
        // end of the song reached
        else if (event.getState() == PlayerState.EOM) {  
            if (LOG.isDebugEnabled())
                LOG.debug("play completed for: " + currentFileName);

            // complete progress bar
            setProgressValue(PROGRESS.getMinimum());

            PlaylistMediator playlist = GUIMediator.getPlayList();
            if (playlist == null)
                return;
            // inform the GUI on whether or not we're going to continue playing
            // if the end of the playlist has been reached, stop even if continous play is selected
            if (playOneTime || !playlist.isContinuous() && (playlist.getSize() <= 0 || playlist.isEndOfList()) ) {
                playlist.playComplete();
                PLAYER.stop();
            }
            else {
                playlist.playComplete();
                // if we don't already have another song to play,
                // get one.
                loadSong(playlist.getNextSong());
            }
        }
    }
    
    /**
     * @return [ <song> '\t' <url> '\t' <length> '\t' <isStorePreview> '|' ]*
     */    
    String getSongs() {
        PlaylistMediator pl = GUIMediator.getPlayList();
        List<PlayListItem> songs = pl.getSongs();
        StringBuffer res = new StringBuffer();
        if (songs != null) {
            for (PlayListItem s : songs) {
                res.append(s.getName())
                   .append('\t')
                   .append(s.getURI())
                   .append('\t')
                   .append(s.getProperty(PlayListItem.LENGTH))
                   .append('\t')
                   .append(s.isStorePreview())
                   .append('|');
                   
            }
        }System.out.println("songs:"+res);
        return res.toString();
    }
    
    /**
     * Begins playing the loaded song in url of args.
     */
    String playSong(Map<String,String> args) {
        
        Tagged<String> urlString = LWSUtil.getArg(args, "url", "AddToPlaylist");
        if (!urlString.isValid()) return urlString.getValue();
        String url = urlString.getValue();
        
        // Find the song with this url
        PlaylistMediator pl = GUIMediator.getPlayList();
        List<PlayListItem> songs = pl.getSongs();
        PlayListItem targetTrack = null;
        for (PlayListItem it : songs) {
            try {
                String thatOne = URLDecoder.decode(it.getURI().toString());
                String thisOne = URLDecoder.decode(url);
                if (thatOne.equals(thisOne)) {
                    targetTrack = it;
                    break;
                }
            } catch (IOException e) {
                // ignore
            }
        }
        
        if (targetTrack != null) {
            loadSong(targetTrack);
            return LWSDispatcherSupport.Responses.OK;
        }
        
        
        if (PLAYER.getStatus() == PlayerState.PAUSED || PLAYER.getStatus() == PlayerState.PLAYING)
            PLAYER.unpause();
        else {
            loadSong(currentPlayListItem);
        }
        
        return LWSDispatcherSupport.Responses.OK;
    }        
    
    /**
     * Returns {@link LWSDispatcherSupport.Responses#OK} on success and a
     * failure message on failure after taking an index into the playlist and
     * remove it.
     * 
     * @param index index of the item to remove
     * @return {@link LWSDispatcherSupport.Responses#OK} on success and a
     *         failure message on failure after taking an index into the
     *         playlist and remove it;
     */
    String removeFromPlaylist(int index) {
        PlaylistMediator pl = GUIMediator.getPlayList();
        if (pl.removeFileFromPlaylist(index)) {
            return LWSDispatcherSupport.Responses.OK;
        }
        return "invalid.index: " + index;
    }
    
    /**
     * Returns {@link LWSDispatcherSupport.Responses#OK} on success and a
     * failure message on failure after taking an index into the playlist and
     * remove it.
     * 
     * @param index index of the item to remove
     * @return {@link LWSDispatcherSupport.Responses#OK} on success and a
     *         failure message on failure after taking an index into the
     *         playlist and remove it;
     */
    String playIndexInPlaylist(int index) {
        PlaylistMediator pl = GUIMediator.getPlayList();
//        pl.
        if (pl.removeFileFromPlaylist(index)) {
            return LWSDispatcherSupport.Responses.OK;
        }
        return "invalid.index: " + index;
    }    
    
    /**
     * @return <code>PROGRESS.getValue() + "\t" + PROGRESS.getMaximum()</code>
     *         or <code>"stopped"</code> if we're not playing
     */
    String getProgress() {
        String res;
        if (isPlaying()) {
            int secs = PROGRESS.getValue();
            int length = PROGRESS.getMaximum();
            System.out.println(secs + ":" + length);
            res = secs + "\t" + length;
        } else {
            res = "stopped";
        }
        return res.toString();        
    }
    
    /**
     * @return {@link  LWSDispatcherSupport.Responses#OK}
     */
    String addToPlaylist(Map<String,String> args) {

        Tagged<String> urlString = LWSUtil.getArg(args, "url", "AddToPlaylist");
        if (!urlString.isValid()) return urlString.getValue();
        
        Tagged<String> nameString = LWSUtil.getArg(args, "name", "AddtoPlaylist");
        if (!nameString.isValid()) return nameString.getValue();
        
        Tagged<String> lengthString = LWSUtil.getArg(args, "length", "AddtoPlaylist");
        if (!lengthString.isValid()) return lengthString.getValue();
        
        Tagged<String> artistString = LWSUtil.getArg(args, "artist", "AddtoPlaylist");
        if (!artistString.isValid()) return artistString.getValue();  
        
        Tagged<String> albumString = LWSUtil.getArg(args, "album", "AddtoPlaylist");
        if (!albumString.isValid()) return albumString.getValue();  
        
        // We won't accept full URLs
        String baseDir = "http://" + LWSSettings.LWS_ADD_TO_PLAYLIST_HOSTNAME.getValue();
        int port = LWSSettings.LWS_ADD_TO_PLAYLIST_PORT.getValue();
        if (port > 0) {
            baseDir += ":" + port;
        }
        
        String url = baseDir + urlString.getValue();
        try {
            String decodedURL = URLDecoder.decode(url);
            URL u = new URL(decodedURL);
            Map<String,String> props = new HashMap<String,String>();
            //props.put(PlayListItem.BITRATE, bitrateString.getValue());
            props.put(LimeAudioFormat.AUDIO_LENGTH_BYTES, lengthString.getValue());
            props.put(LimeAudioFormat.AUDIO_TYPE, "MP3");
            props.put(PlayListItem.ARTIST, artistString.getValue());
            if (albumString.isValid()) {
                props.put(PlayListItem.ALBUM, albumString.getValue());
            }
            PlayListItem song = new PlayListItem(u.toURI(), new AudioSource(u), 
                                                 nameString.getValue(), false, true, props);           
            GUIMediator.instance().launchAudio(song);
        } catch (IOException e) {
            ErrorService.error(e, "invalid URL:" + url);
            return LWSServerUtil.wrapError("invalid.url:" + url);
        } catch (URISyntaxException e) {
            ErrorService.error(e, "invalid URL:" + url);
            return LWSServerUtil.wrapError("invalid.url:" + url);
        }
        return LWSDispatcherSupport.Responses.OK;        
    }
    
    String playURL(Map<String,String> args) {
        
        Tagged<String> urlString = LWSUtil.getArg(args, "url", "PlayURL");
        if (!urlString.isValid()) return urlString.getValue();
        
        // We won't accept full URLs
        String baseDir = "http://" + LWSSettings.LWS_ADD_TO_PLAYLIST_HOSTNAME.getValue();
        int port = LWSSettings.LWS_ADD_TO_PLAYLIST_PORT.getValue();
        if (port > 0) {
            baseDir += ":" + port;
        }        
        
        String url = baseDir + urlString.getValue();
        String name = getName(url);
        try {
            String decodedURL = URLDecoder.decode(url);
            URL u = new URL(decodedURL);
            Map<String,String> props = new HashMap<String,String>();
            PlayListItem song = new PlayListItem(u.toURI(), new AudioSource(u), 
                                                 name, false, true, props);
            GUIMediator.instance().launchAudio(song);
        } catch (IOException e) {
            ErrorService.error(e, "invalid URL:" + url);
            return LWSServerUtil.wrapError("invalid.url:" + url);
        } catch (URISyntaxException e) {
            ErrorService.error(e, "invalid URL:" + url);
            return LWSServerUtil.wrapError("invalid.url:" + url);
        }
        return LWSDispatcherSupport.Responses.OK;          
    }
    
    private String getName(String url) {
        int ilast = url.lastIndexOf('/');
        if (ilast == -1) {
            ilast = url.lastIndexOf('\\');
        }
        if (ilast == -1) {
            return url;
        }
        return url.substring(ilast+1);
    }
    
    private boolean isPlaying() {
        return !(PLAYER.getStatus() == PlayerState.STOPPED || 
                PLAYER.getStatus() == PlayerState.UNKNOWN  ||
                PLAYER.getStatus() == PlayerState.PAUSED   ); 
    }   
    

    /**
     * Listens for the play button being pressed.
     */
    private class PlayListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            play();
        }
    }

    /**
     * Listens for the stopped button being pressed.
     */
    private class StopListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            stopSong();
        }
    }

    /**
     * Listens for the next button being pressed.
     */
    private class NextListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            next();
        }
    }
    
    private void next() {
        loadSong(GUIMediator.getPlayList().getNextSong());
    }

    /**
     * Listens for the back button being pressed.
     */
    private class BackListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            back();
        }
    }
    
    private void back() {
        loadSong(GUIMediator.getPlayList().getPrevSong());
    }

    /**
     * Listens for the pause button being pressed.
     */
    private class PauseListener implements ActionListener {
        public void actionPerformed(ActionEvent ae) {
            pauseSong();
        }
    }

    /**
     * This listener is added to the progressbar to process when the user has
     * skipped to a new part of the song with a mouse
     */
    private class ProgressBarListener implements ChangeListener {

        /**
         * This is set to true when the user has manipulated the mouse position
         * but has yet to "letgo" of the thumb
         */
        boolean dragging = false;

        /**
         * If the user has moved the thumb and let go of it, process a skip
         */
        public void stateChanged(ChangeEvent arg0) {
            if (PROGRESS.getValueIsAdjusting()) {
                dragging = true;
            } else {
                if (dragging) {
                    dragging = false;
                    if (PLAYER.getStatus() != PlayerState.SEEKING) {
                        skip(PROGRESS.getValue() * 1.0 / PROGRESS.getMaximum());
                        setProgressValue(PROGRESS.getValue());
                    }
                }
            }
        }
    }

    /**
     * This listener is added to the volume slider to process whene the user has
     * adjusted the volume of the audio player
     */
    private class VolumeSliderListener implements ChangeListener {
        /**
         * If the user moved the thumb, adjust the volume of the player
         */
        public void stateChanged(ChangeEvent e) {
            setVolumeValue();
        }
    }

    /**
     * Ensures that all songs will be loaded/played from the same thread.
     */
    private class SongLoader implements Runnable {

        /**
         * Audio source to load
         */
        private final AudioSource audio;

        public SongLoader(AudioSource audio) {
            this.audio = audio;
        }

        public void run() {
            if (audio != null)
                PLAYER.loadSong(audio);
            PLAYER.playSong();
        }
    }

}