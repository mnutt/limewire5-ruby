package com.limegroup.gnutella.gui.mp3;

/**
 * Different states reachable by the LimeWirePlayer
 */
public enum PlayerState {
    
    /** the player state is unknown
     */
    UNKNOWN, 
    
    /** the player is attempting to open a file for reading
     */
    OPENING, 
    
    /** the player has opened the song and sourcedataline and 
     *  is ready to be read
     */
    OPENED, 
    
    /** the player is playing the current song
     */
    PLAYING, 
    
    /** the player has stopped the current song
     */
    STOPPED, 
    
    /** the player has paused the current song
     */
    PAUSED, 
    
    /** the player has resumed playing the current song
     */
    RESUMED, 
    
    /** the player is seeking to a new location in the song
     */
    SEEKING, 
    
    /** seeking with intent to pause after seek
     */
    SEEKING_PAUSED,
    
    /** seeking with intent to begin playing after seek
     */
    SEEKING_PLAY,
    
    /** the end of the song has been reached
     */
    EOM,
    
    /** the volume on the outputstream has changed
     */
    GAIN, 
    
    /** the player has reached the new song location
     */
    SEEKED;
}
