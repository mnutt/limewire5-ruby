package org.limewire.ui.swing.player;

import java.io.File;
import java.util.Locale;

import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Utility methods to access the audio player.  PlayerUtils accesses the player
 * using an instance of PlayerMediator.  The UI may also work with the 
 * PlayerMediator directly, which maintains a playlist and various play modes
 * like shuffle.
 */
public class PlayerUtils {

    @Inject
    private static Provider<PlayerMediator> playerProvider;

    private static void play(File audioFile){
        playerProvider.get().play(audioFile);
    }
    
    public static boolean isPlaying(File audioFile){
        return playerProvider.get().isPlaying(audioFile);
    }
    
    public static boolean isPlayableFile(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
    }

    public static void pause() {
        playerProvider.get().pause();
    }
    
    public static void stop() {
        playerProvider.get().stop();
    }
    
    /**Plays file internally if playable.  Launches native player otherwise.
     * 
     * @return true if file is played internally, false if played in native player
     */
    public static boolean playOrLaunch(File file) {
        if (SwingUiSettings.PLAYER_ENABLED.getValue() && isPlayableFile(file)) {
            play(file);
            return true;
        } else {    
            NativeLaunchUtils.safeLaunchFile(file);
            return false;
        }

    }
    
    /**
     * @return The current file playing in the player. This may return null if
     *         nothing is playing or if the audio source is not a file.
     */
    public static File getCurrentSongFile(){
        return playerProvider.get().getCurrentSongFile();
    }
}
