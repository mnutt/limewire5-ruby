package org.limewire.ui.swing.player;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Properties;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.inject.LazySingleton;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.AudioSource;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.library.LibraryPanel;
import org.limewire.ui.swing.library.navigator.LibraryNavItem;
import org.limewire.ui.swing.library.navigator.LibraryNavItem.NavType;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.settings.MediaPlayerSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.inspection.InspectionHistogram;

import ca.odell.glazedlists.EventList;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Mediator that controls the interaction between the player view, the current
 * playlist, and the audio player.
 */
@LazySingleton
public class PlayerMediator {

    public static final String AUDIO_LENGTH_BYTES = "audio.length.bytes";
    public static final String AUDIO_TYPE = "audio.type";
    
    private static final String MP3 = "mp3";
    private static final String WAVE = "wave";
    
    private final Provider<AudioPlayer> audioPlayerProvider;
    private final LibraryMediator libraryMediator;
    private final List<PlayerMediatorListener> listenerList;
    
    /** Current list of songs. */
    private final List<LocalFileItem> playList;
    
    /** Randomized list of songs to be played. */
    private final List<LocalFileItem> shuffleList;
    
    /** Audio player component. */
    private AudioPlayer audioPlayer;
    
    /** Identifier for current playlist. */
    private PlaylistId playlistId;
    
    /** File item for the last opened song. */
    private LocalFileItem fileItem = null;
    
    /** Map containing properties for the last opened song. */
    private Map audioProperties = null;
    
    /** Progress of current song from 0.0 to 1.0. */
    private float progress;
    
    /** Indicator for shuffle mode. */
    private boolean shuffle = false;
    
    @InspectionPoint(value = "media-player")
    private final PlayerInspector inspectable;
    
    /**
     * Constructs a PlayerMediator using the specified services.
     */
    @Inject
    public PlayerMediator(Provider<AudioPlayer> audioPlayerProvider,
            LibraryMediator libraryMediator) {
        this.audioPlayerProvider = audioPlayerProvider;
        this.libraryMediator = libraryMediator;
        
        this.listenerList = new ArrayList<PlayerMediatorListener>();
        this.playList = new ArrayList<LocalFileItem>();
        this.shuffleList = new ArrayList<LocalFileItem>();
        this.inspectable = new PlayerInspector();
    }
    
    /**
     * Returns the audio player component.  When first called, this method
     * creates the component and registers this mediator as a listener.
     */
    private AudioPlayer getPlayer() {
        if (audioPlayer == null) {
            audioPlayer = audioPlayerProvider.get();
            audioPlayer.addAudioPlayerListener(new PlayerListener());
        }
        return audioPlayer;
    }
    
    /**
     * Adds the specified listener to the list that is notified about 
     * mediator events.
     */
    public void addMediatorListener(PlayerMediatorListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes the specified listener from the list that is notified about
     * mediator events.
     */
    public void removeMediatorListener(PlayerMediatorListener listener) {
        listenerList.remove(listener);
    }
    
    /**
     * Notifies registered listeners that the progress is updated to the 
     * specified value.
     */
    private void fireProgressUpdated(float progress) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).progressUpdated(progress);
        }
    }
    
    /**
     * Notifies registered listeners that the song is changed to the specified
     * song name.
     */
    private void fireSongChanged(String name) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).songChanged(name);
        }
    }
    
    /**
     * Notifies registered listeners that the player state is changed to the
     * specified state.
     */
    private void fireStateChanged(PlayerState state) {
        for (int i = 0, size = listenerList.size(); i < size; i++) {
            listenerList.get(i).stateChanged(state);
        }
    }
    
    /**
     * Returns the current status of the audio player.
     */
    public PlayerState getStatus() {
        return getPlayer().getStatus();
    }
    
    /**
     * Returns true if the specified library item is the active playlist.
     */
    public boolean isActivePlaylist(LibraryNavItem navItem) {
        if (navItem != null) {
            PlaylistId newId = new PlaylistId(navItem);
            return newId.equals(playlistId);
        }
        return false;
    }
    
    /**
     * Sets the active playlist using the specified library item.
     */
    public void setActivePlaylist(LibraryNavItem navItem) {
        if (navItem != null) {
            PlaylistId oldPlaylist = playlistId;
            playlistId = new PlaylistId(navItem);
            if (!playlistId.equals(oldPlaylist)) {
                inspectable.newListStarted();
            }
        } else {
            playlistId = null;
            playList.clear();
        }
    }
    
    /**
     * Returns the current playlist.  The method may return an empty list, but
     * never a null list.
     */
    private List<LocalFileItem> getPlaylist() {
        // Get selected list and category.
        LibraryNavItem selectedNavItem = libraryMediator.getSelectedNavItem();
        Category selectedCategory = libraryMediator.getSelectedCategory();
        
        // No selected list so return internal playlist.
        if (selectedNavItem == null) {
            return playList;
        }
        
        // Compare selected list to playlist.
        PlaylistId selectedId = new PlaylistId(selectedNavItem);
        if (selectedId.equals(playlistId) && 
                LibraryPanel.isPlayable(selectedCategory)) {
            // Selected list is same so return list from library.
            List<LocalFileItem> libraryList = libraryMediator.getPlayableList();
            if (libraryList == null) libraryList = Collections.emptyList();
            return libraryList;
        } else {
            // Selected list is different so return internal playlist.
            return playList;
        }
    }
    
    /**
     * Sets the internal playlist using the specified list of file items.  If
     * the specified list is empty or null, then the playlist is cleared.
     */
    public void setPlaylist(EventList<LocalFileItem> fileList) {
        // Clear current playlist.
        playList.clear();
        
        if (fileList == null) return;
        
        // Copy files into playlist.
        for (int i = 0, size = fileList.size(); i < size; i++) {
            playList.add(fileList.get(i));
        }
    }
    
    /**
     * Returns true if shuffle mode is enabled.
     */
    public boolean isShuffle() {
        return shuffle;
    }
    
    /**
     * Sets an indicator to enable shuffle mode.
     */
    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        
        // Update shuffle list.
        if (shuffle) {
            updateShuffleList();
        } else {
            shuffleList.clear();
        }
    }
    
    /**
     * Sets the volume (gain) value on a linear scale from 0.0 to 1.0.
     */
    public void setVolume(double value) {
        getPlayer().setVolume(value);
    }
    
    /**
     * Pauses the current song in the audio player. 
     */
    public void pause() {
        getPlayer().pause();
    }
    
    /**
     * Resumes playing the current song in the audio player.  If the player is
     * stopped, then attempt to play the first selected item in the library. 
     */
    public void resume() {
        PlayerState status = getPlayer().getStatus();
        if ((status == PlayerState.STOPPED) || (status == PlayerState.UNKNOWN)) {
            // Get first selected item.
            List<LocalFileItem> selectedItems = libraryMediator.getSelectedItems();
            if (selectedItems.size() > 0) {
                LocalFileItem selectedItem = selectedItems.get(0);
                if (PlayerUtils.isPlayableFile(selectedItem.getFile())) {
                    // Set active playlist and play file item.
                    setActivePlaylist(libraryMediator.getSelectedNavItem());
                    play(selectedItem);
                }
            }
            
        } else {
            getPlayer().unpause();
        }
    }
    
    /**
     * Starts playing the specified file in the audio player.  The playlist
     * is automatically cleared so the player will stop when the song finishes.
     */
    public void play(File file) {
        // Stop current song.
        stop();
        
        // Play new song.
        this.fileItem = null;
        loadAndPlay(file);
        
        // Clear play and shuffle lists.
        setActivePlaylist(null);
        shuffleList.clear();
    }
    
    /**
     * Starts playing the specified file item in the audio player.
     */
    public void play(LocalFileItem localFileItem) {
        // Stop current song.
        stop();
        
        // Play new song.
        this.fileItem = localFileItem;
        loadAndPlay(localFileItem.getFile());
        
        // Update shuffle list when enabled.
        if (shuffle) {
            updateShuffleList();
        }
    }
    
    private void loadAndPlay(File fileToPlay) {
        AudioPlayer player = getPlayer();
        player.loadSong(fileToPlay);
        player.playSong();
        inspectable.started(fileToPlay);
    }
    
    /**
     * Skips the current song to a new position in the song. If the song's
     * length is unknown (streaming audio), then ignore the skip.
     * 
     * @param percent of the song frames to skip from begining of file
     */
    public void skip(double percent) {
        // need to know something about the audio type to be able to skip
        if (audioProperties != null && audioProperties.containsKey(AUDIO_TYPE)) {
            String songType = (String) audioProperties.get(AUDIO_TYPE);
            
            // currently, only mp3 and wav files can be seeked upon
            if (isSeekable(songType) && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                final long skipBytes = Math.round((Integer) audioProperties.get(AUDIO_LENGTH_BYTES)* percent);
                getPlayer().seekLocation(skipBytes);
            }
        }
    }
    
    /**
     * Stops playing the current song in the audio player.
     */
    public void stop() {
        getPlayer().stop();
    }
    
    /**
     * Plays the next song in the playlist.
     */
    public void nextSong() {
        // Stop current song.
        stop();

        // Get next file item.
        fileItem = getNextFileItem();

        // Play song.
        if (fileItem != null) {
            loadAndPlay(fileItem.getFile());
        }
    }
    
    /**
     * Plays the previous song in the playlist.
     */
    public void prevSong() {
        // Stop current song.
        stop();

        // If near beginning of current song, then get previous song.
        // Otherwise, restart current song.
        if (progress < 0.1f) {
            fileItem = getPrevFileItem();
        }

        // Play song.
        if (fileItem != null) {
            loadAndPlay(fileItem.getFile());
        }
    }
    
    /**
     * Returns the current playing file.
     */
    public File getCurrentSongFile() {
        AudioSource source = getPlayer().getCurrentSong();
        return (source != null) ? source.getFile() : null;
    }
    
    /**
     * Returns true if this file is currently playing, false otherwise
     */
    public boolean isPlaying(File file) {
        return getPlayer().isPlaying(file);
    }
    
    /**
     * Returns true if this file is currently loaded and paused, false otherwise.
     */
    public boolean isPaused(File file) {
        return getPlayer().isPaused(file);
    }
    
    /**
     * Returns true if the currently playing song is seekable.
     */
    public boolean isSeekable() {
        if (audioProperties != null) {
            return isSeekable((String) audioProperties.get(AUDIO_TYPE));
        }
        return false;
    }
    
    /**
     * Returns true if the specified song type is seekable, which means that 
     * the progress position can be set.  At present, only MP3 and Wave files 
     * are seekable.
     */
    private boolean isSeekable(String songType) {
        if (songType == null) {
            return false;
        }
        return songType.equalsIgnoreCase(MP3) || songType.equalsIgnoreCase(WAVE);
    }
    
    /**
     * Updates the shuffle list of items to be played.
     */
    private void updateShuffleList() {
        // Clear shuffle list.
        shuffleList.clear();
        
        // Get current playlist.
        List<LocalFileItem> playlist = getPlaylist();
        if (playlist.size() == 0) return;
        
        // Set shuffle list elements and randomize.
        for (int i = 0; i < playlist.size(); i++) {
            shuffleList.add(playlist.get(i));
        }
        Collections.shuffle(shuffleList);
        
        // Move currently playing song to the beginning.
        int index = shuffleList.indexOf(fileItem);
        if (index > 0) {
            shuffleList.remove(index);
            shuffleList.add(0, fileItem);
        }
    }
    
    /**
     * Returns the next file item in the current playlist.
     */
    private LocalFileItem getNextFileItem() {
        // Get file list.
        List<LocalFileItem> fileList = shuffle ? shuffleList : getPlaylist();
        
        if ((fileItem != null) && (fileList.size() > 0)) {
            int index = fileList.indexOf(fileItem);
            if (index < (fileList.size() - 1)) {
                return fileList.get(index + 1);
            }
        }
        return null;
    }
    
    /**
     * Returns the previous file item in the current playlist.
     */
    private LocalFileItem getPrevFileItem() {
        // Get file list.
        List<LocalFileItem> fileList = shuffle ? shuffleList : getPlaylist();
        
        if ((fileItem != null) && (fileList.size() > 0)) {
            int index = fileList.indexOf(fileItem);
            if (index > 0) {
                return fileList.get(index - 1);
            }
        }
        return null;
    }
    
    /**
     * Returns the name of the current song.
     */
    private String getSongName() {
        // Use audio properties if available.
        if (audioProperties != null) {
            Object author = audioProperties.get("author");
            Object title = audioProperties.get("title");
            if ((author != null) && (title != null)) {
                return author + " - " + title;
            }
        }
        
        // Use file item if available.
        if (fileItem != null) {
            return fileItem.getFile().getName();
        } else {
            return I18n.tr("Unknown");
        }
    }
    
    /**
     * Listener to handle audio player events.
     */
    private class PlayerListener implements AudioPlayerListener {

        @Override
        public void progressChange(int bytesread) {
            // If we know the length of the song, update progress value.
            if ((audioProperties != null) && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                float byteslength = ((Integer) audioProperties.get(AUDIO_LENGTH_BYTES)).floatValue();
                progress = bytesread / byteslength;
                
                // Notify UI about progress.
                fireProgressUpdated(progress);
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
            // Save properties.
            audioProperties = properties;
            
            // Notify UI about new song.
            fireSongChanged(getSongName());
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            // Go to next song when finished.
            if (event.getState() == PlayerState.EOM) {
                nextSong();
            } else if (event.getState() == PlayerState.STOPPED) {
                inspectable.stopped();
            }
            
            // Notify UI about state change.
            fireStateChanged(event.getState());
        }
    }
    
    /**
     * An identifier for a playlist.
     */
    private static class PlaylistId {
        private final NavType type;
        private final int id;
        
        public PlaylistId(LibraryNavItem navItem) {
            this.type = navItem.getType();
            this.id = navItem.getId();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PlaylistId) {
                return (type == ((PlaylistId) obj).type) && (id == ((PlaylistId) obj).id);
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + type.hashCode();
            result = 31 * result + id;
            return result;
        }
    }

    /**
     * Media Player inspections are cumulative, spanning all limewire sessions.
     * Statistics are updated and stored via PropertiesSetting objects. During inspection,
     * statistics are extracted from the properties. 
     */
    private class PlayerInspector implements Inspectable {
        
        // key == percent, value == num times user stopped playing at percent
        private final Properties percentPlayedProp = MediaPlayerSettings.MEDIA_PLAYER_PERCENT_PLAYED.get();
        
        // key == file name, value = num times file played
        private final Properties filesPlayed = MediaPlayerSettings.MEDIA_PLAYER_NUM_PLAYS.get();
        
        // key == playlist size, value == num times playlist of this size was played
        private final Properties playListSizeProp = MediaPlayerSettings.MEDIA_PLAYER_LIST_SIZE.get();

        /**
         * Called when media player stops playing a file.
         */
        void stopped() {
            incrementIntProperty(percentPlayedProp, Integer.toString(Math.round(progress*100)));
            MediaPlayerSettings.MEDIA_PLAYER_PERCENT_PLAYED.set(percentPlayedProp);
        }

        /**
         * Called when media player plays a file.
         * @param filePlayed File that is played
         */
        void started(File filePlayed) {
            incrementIntProperty(filesPlayed, filePlayed.getName());
            MediaPlayerSettings.MEDIA_PLAYER_NUM_PLAYS.set(filesPlayed);
        }

        /**
         * Called when media player plays a playlist.
         */
        void newListStarted() {
            List<LocalFileItem> fileList = shuffle ? shuffleList : getPlaylist();
            incrementIntProperty(playListSizeProp, Integer.toString(fileList.size()));
            MediaPlayerSettings.MEDIA_PLAYER_LIST_SIZE.set(playListSizeProp);
        }
        
        private void incrementIntProperty(Properties properties, String key) {
            String value = properties.getProperty(key);
            Integer currentNum = (value == null) ? 0 : Integer.valueOf(value);
            properties.setProperty(key, Integer.toString(currentNum+1));        
        }

        /**
         * Convert the PropertiesSetting data into inspectable data
         */
        @Override
        public Object inspect() {
            int repeats = 0;
            int numPlayStarts = 0;
            for (String fileName : filesPlayed.stringPropertyNames()) {
                int numOfPlaysForFile = Integer.parseInt(filesPlayed.getProperty(fileName));
                numPlayStarts += numOfPlaysForFile;
                if (numOfPlaysForFile > 1) {
                    repeats += numOfPlaysForFile - 1;
                }
            }
            int numPlayStops = 0;
            InspectionHistogram<Integer> percentPlayed = new InspectionHistogram<Integer>();
            for (String percentPlayedStr : percentPlayedProp.stringPropertyNames()) {
                Integer percent = Integer.valueOf(percentPlayedStr);
                int numPlayedAtPercent = Integer.parseInt(percentPlayedProp.getProperty(percentPlayedStr));
                numPlayStops += numPlayedAtPercent;
                percentPlayed.count(percent, numPlayedAtPercent);
            }
            int numberOfTimesListPlayed = 0;
            InspectionHistogram<Integer> playListsize = new InspectionHistogram<Integer>();
            for (String numFilesInPlayListAsStr : playListSizeProp.stringPropertyNames()) {
                Integer numFilesInPlayList = Integer.valueOf(numFilesInPlayListAsStr);
                int numPlayListPlays = Integer.parseInt(playListSizeProp.getProperty(numFilesInPlayListAsStr));
                numberOfTimesListPlayed += numPlayListPlays;
                playListsize.count(numFilesInPlayList, numPlayListPlays);
            }
            Map<String,Object> ret = new HashMap<String,Object>();
            ret.put("play_starts", numPlayStarts);
            ret.put("repeats", repeats);
            ret.put("play_stops", numPlayStops);
            ret.put("percent_played", percentPlayed.inspect());
            ret.put("total_list_plays", numberOfTimesListPlayed);
            ret.put("list_size", playListsize.inspect());
            return ret;    
        }
    }
}
