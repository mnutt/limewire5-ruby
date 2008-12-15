package org.limewire.core.impl.player;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.AudioSource;
import org.limewire.player.api.PlayerState;

public class MockAudioPlayer implements AudioPlayer{

    @Override
    public void addAudioPlayerListener(AudioPlayerListener listener) {
        // TODO Auto-generated method stub        
    }

    @Override
    public PlayerState getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void loadSong(AudioSource source) {
        // TODO Auto-generated method stub        
    }

    @Override
    public void loadSong(File source) {
        // TODO Auto-generated method stub        
    }

    @Override
    public void loadSong(InputStream source) {
        // TODO Auto-generated method stub        
    }

    @Override
    public void loadSong(URL source) {
        // TODO Auto-generated method stub        
    }

    @Override
    public void pause() {
        // TODO Auto-generated method stub        
    }

    @Override
    public void playSong() {
        // TODO Auto-generated method stub        
    }

    @Override
    public void removeAudioPlayerListener(AudioPlayerListener listener) {
        // TODO Auto-generated method stub        
    }

    @Override
    public long seekLocation(long value) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setVolume(double value) {
        // TODO Auto-generated method stub        
    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub
    }

    @Override
    public void unpause() {
        // TODO Auto-generated method stub        
    }

    @Override
    public boolean isPlaying(File file) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isPaused(File file) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public AudioSource getCurrentSong() {
        // TODO Auto-generated method stub
        return null;
    }

}
