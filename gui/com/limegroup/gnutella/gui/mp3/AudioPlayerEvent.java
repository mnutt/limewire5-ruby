package com.limegroup.gnutella.gui.mp3;

/**
 * This event is fired by the AudioPlayer everytime the state of the player changes.
 * The changed state along with the updated value is passed along when applicable
 */
public class AudioPlayerEvent {
    
    /**
     * Current state of the player
     */
    private final PlayerState state;

    /**
     * The value that has been changed on a given component ie. current volume
     * of the song, current pan value, etc..
     * 
     * NOTE: the value may not have meaning with everystate, for example 
     * PlayerState.STOPPED has no applicable meaning but with PlayerState.VOLUME,
     * this represents the current volume the player is set to.
     */
    private final double value;

    
    public AudioPlayerEvent(PlayerState state, double value) {
        this.value = value;
        this.state = state;
    }

    public PlayerState getState() {
        return state;
    }

    public double getValue() {
        return value;
    }
}
