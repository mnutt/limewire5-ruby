package org.limewire.ui.swing.library;

/**
 * Playlist defines a collection of items that can be played by a media player.
 */
class Playlist extends Catalog {

    /**
     * Constructs a Playlist with the specified name.
     */
    public Playlist(String name) {
        super(Type.PLAYLIST, name);
    }

}
