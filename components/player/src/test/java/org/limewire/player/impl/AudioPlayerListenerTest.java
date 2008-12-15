package org.limewire.player.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.util.BaseTestCase;

public class AudioPlayerListenerTest extends BaseTestCase {

    static LimeWirePlayer player;
    static testAudioPlayerListener listener;
    
    public AudioPlayerListenerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AudioPlayerListenerTest.class);
    }
    
    @Override
    public void setUp() {
        player = new LimeWirePlayer();
        listener = new testAudioPlayerListener();
        player.addAudioPlayerListener(listener);
    }
    
    public void testStateUpdated(){
        player.fireStateUpdated( new AudioPlayerEvent(PlayerState.OPENED, 0) );
    
        assertEquals(1, listener.recievedEvents.size());

        player.removeAudioPlayerListener(listener);
        
        player.fireStateUpdated(new AudioPlayerEvent(PlayerState.OPENED, 0) );
        
        assertEquals(1, listener.recievedEvents.size());
    }
    
    public void testOpened(){
        player.fireOpened(null);
        
        assertEquals(1, listener.recievedEvents.size());
        
        player.removeAudioPlayerListener(listener);
        
        player.fireOpened(null);
        
        assertEquals(1, listener.recievedEvents.size());
    }
    
    public void testProgress(){
        player.fireProgress(-1);
        
        assertEquals(1, listener.recievedEvents.size());
        
        player.removeAudioPlayerListener(listener);
        
        player.fireProgress(-1);
        
        assertEquals(1, listener.recievedEvents.size());
    }
    
    public class testAudioPlayerListener implements AudioPlayerListener{

        List<Object> recievedEvents = new ArrayList<Object>();
        
        public void songOpened(Map<String, Object> properties) {
            recievedEvents.add(new opened(properties));
        }

        public void progressChange(int bytesread) {
            recievedEvents.add(new progress(bytesread));
        }

        public void stateChange(AudioPlayerEvent event) {
            recievedEvents.add(event);
        }
    }
    
    private class progress{
        int b;
        Map<String,Object>m;
        
        public progress(int bytes){
            b = bytes;
        }
    }
    
    private class opened{
        Map<String,Object> m;
        
        public opened(Map<String,Object> props){
            m = props;
        }
    }
}
