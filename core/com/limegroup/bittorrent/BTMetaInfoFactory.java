package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;

import org.limewire.io.InvalidDataException;

import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

public interface BTMetaInfoFactory {

    /**
     * Creates an instance of BTMetaInfo from the BTMetaInfoMemento passed.
     * @param memento BTMetaInfoMemento we want to use to create a BTMetaInfo
     * @return a new instance of BTMetaInfo
     * @throws InvalidDataException thrown if the memento contained invalid data
     */
    BTMetaInfo createBTMetaInfoFromMemento(BTMetaInfoMemento memento) throws InvalidDataException;       
    
    /**
     * Creates a BTMetaInfo from byte [].
     * 
     * @param torrent byte array with the contents of .torrent
     * @return new instance of BTMetaInfo if all went well
     * @throws IOException if parsing or reading failed.
     */
    BTMetaInfo createBTMetaInfoFromBytes(byte []torrent) throws IOException;
    
    /**
     * Creates meta info object from file.
     * 
     * @throws IOException if there was a problem reading the file or parsing
     * the torrent
     */
    BTMetaInfo createMetaInfo(File torrentFile) throws IOException;
}