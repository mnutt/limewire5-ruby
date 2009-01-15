package org.limewire.ui.swing.player;

import java.io.File;
import java.util.Locale;

import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
//TODO:  this is not the best way to handle player access but it gets it working for now.
public class PlayerUtils {
    @Inject
    private static AudioPlayer player;

    private static void play(File audioFile){
        player.loadSong(audioFile);
        player.playSong();
    }
    
    public static boolean isPlaying(File audioFile){
        return player.isPlaying(audioFile);
    }
    
    public static boolean isPlayableFile(File file) {
        String name = file.getName().toLowerCase(Locale.US);
        return name.endsWith(".mp3") || name.endsWith(".ogg") || name.endsWith(".wav");
    }

    public static void pause() {
        player.pause();
    }
    
    /**Plays file internally if playable.  Launches native player otherwise.
     * 
     * @param file
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
}
