package org.limewire.ui.swing.menu;

import java.awt.event.ActionEvent;
import java.util.Map;

import javax.swing.Action;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.action.MnemonicMenu;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class PlayerMenu extends MnemonicMenu {
    @Inject
    public PlayerMenu(AudioPlayer audioPlayer,
            LibraryNavigator libraryNavigator,
            LibraryManager libraryManager) {
        super(I18n.tr("&Player"));

        add(getPlayPauseAction(audioPlayer));
//
//        add(getNextAction(audioPlayer));
//        add(getPreviousAction(audioPlayer));
//
//        addSeparator();
//        add(getShowCurrentFileAction(audioPlayer, libraryNavigator, libraryManager));
    }

    private Action getPlayPauseAction(final AudioPlayer audioPlayer) {
        final String play = I18n.tr("Play");
        final String pause = I18n.tr("Pause");
        final Action action = new AbstractAction(play) {
            @Override
            public void actionPerformed(ActionEvent e) {
                switch (audioPlayer.getStatus()) {
                case PAUSED:
                case STOPPED:
                case OPENED:
                case SEEKING_PAUSED:
                    audioPlayer.unpause();
                    break;
                case PLAYING:
                case RESUMED:
                case SEEKING_PLAY:
                    audioPlayer.pause();
                    break;
                default:
                    //ignore
                    break;
                }
            }
        };
        
        action.setEnabled(false);

        audioPlayer.addAudioPlayerListener(new AudioPlayerListener() {
            @Override
            public void stateChange(AudioPlayerEvent event) {

                switch (audioPlayer.getStatus()) {
                case PAUSED:
                case STOPPED:
                case OPENED:
                case SEEKING_PAUSED:
                    action.setEnabled(true);
                    action.putValue(Action.NAME, play);
                    break;
                case PLAYING:
                case RESUMED:
                case SEEKING_PLAY:
                    action.setEnabled(true);
                    action.putValue(Action.NAME, pause);
                    break;
                default:
                    //ignore
                    break;
                }
            }
            @Override
            public void songOpened(Map<String, Object> properties) {
    
            }
            @Override
            public void progressChange(int bytesread) {
                
            } 
        });
        return action;
    }

    
//    private Action getNextAction(final AudioPlayer audioPlayer) {
//        //TODO need to have a notion of a playlist for this to work.
//        Action action = new AbstractAction(I18n.tr("Next")) {;
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                throw new UnsupportedOperationException("TODO implement me.");
//            }
//        };
//
//        addAudioListener(audioPlayer, action);
//        return action;
//    }
//
//    private Action getPreviousAction(final AudioPlayer audioPlayer) {
//        Action action = new AbstractAction(I18n.tr("Previous")) {
//          //TODO need to have a notion of a playlist for this to work.
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                throw new UnsupportedOperationException("TODO implement me.");
//            }
//        };
//
//        addAudioListener(audioPlayer, action);
//        return action;
//    }
//
//
//    
//    private Action getShowCurrentFileAction(final AudioPlayer audioPlayer, final LibraryNavigator libraryNavigator,
//            final LibraryManager libraryManager) {
//        Action action = new AbstractAction(I18n.tr("Show current file")) {
//            @Override
//            public void actionPerformed(ActionEvent e) {
//                AudioSource currentSong = audioPlayer.getCurrentSong();
//                if (currentSong != null) {
//                    File currentFile = currentSong.getFile();
//                    if (currentFile != null) {
//                        final LocalFileItem localFileItem = libraryManager.getLibraryManagedList()
//                                .getFileItem(currentFile);
//                        if (localFileItem != null) {
//                            libraryNavigator.selectInLibrary(localFileItem.getUrn(), localFileItem.getCategory());
//                        }
//                    }
//                }
//            }
//        };
//        addAudioListener(audioPlayer, action);
//        return action;
//    }

}
