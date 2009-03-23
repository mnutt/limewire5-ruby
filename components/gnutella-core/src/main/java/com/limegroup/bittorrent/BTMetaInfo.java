package com.limegroup.bittorrent;

import java.net.URI;
import java.security.MessageDigest;

import com.limegroup.bittorrent.disk.BlockRangeMap;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.serial.BTDiskManagerMemento;
import com.limegroup.gnutella.downloader.serial.BTMetaInfoMemento;

/**
 * Defines an interface for a class parsing information in a .torrent file.
 */
public interface BTMetaInfo {

    /**
     * @return piece length for this torrent
     */
    public abstract int getPieceLength();

    public abstract TorrentFileSystem getFileSystem();

    public abstract BTDiskManagerMemento getDiskManagerData();

    public abstract boolean isPrivate();

    public abstract void setContext(TorrentContext context);

    public long getAmountUploaded();

    public void countUploaded(int uploaded);

    public float getRatio();

    /**
     * Verifies whether the given hash matches the expect hash of a Piece
     * 
     * @param sha1 the hash that was computed
     * @param pieceIndex the Piece for which the hash was computed
     * @return true if they match.
     */
    public abstract boolean verify(byte[] sha1, int pieceIndex);

    /**
     * @return info hash
     */
    public abstract byte[] getInfoHash();

    /**
     * @return infohash URN
     */
    public abstract URN getURN();

    /**
     * @return number of pieces in this torrent
     */
    public abstract int getNumBlocks();

    public abstract String getName();

    /**
     * @return array of <tt>URL</tt> storing the addresses of the trackers
     */
    public abstract URI[] getTrackers();

    /**
     * 
     * @return array of <tt>URL</tt> storing the addresses of the webseeds.
     */
    public abstract URI[] getWebSeeds();

    /**
     * Returns which message digest was used to create _hashes.
     * 
     * @return new Instance of the message digest that was used
     * 
     */
    public abstract MessageDigest getMessageDigest();

    /**
     * Serializes this, including information about the written ranges.
     */
    public abstract BTMetaInfoMemento toMemento();

    /**
     * Returns true if this is a multi file torrent download.
     */
    public abstract boolean isMultiFileTorrent();

    /**
     * Setter method for the webseed addresses.
     * 
     * @param uris - webseed addresses
     */
    public abstract void setWebSeeds(URI[] uris);

    /**
     * Returns a BTInterval object representing the given piece.
     * 
     * @param pieceIndex - zero based piece index.
     * @throws IllegalArgumentException when given pieceIndex not within [0-numPieces-1]
     */
    public abstract BTInterval getPiece(int pieceIndex);

    /**
     * Returns the piece size of the given piece. All pieces have pieceLength
     * size, except for potentially the last one.
     * 
     * @param pieceIndex
     * @throws IllegalArgumentException when given pieceIndex not within [0-numPieces-1]
     */
    public abstract int getPieceSize(int pieceIndex);

    /**
     * Helper method to check if the given piece is complete.
     * @param pieceIndex zero based piece index
     * @param toCheck block range map holding completed pieces.
     * 
     *  @throws IllegalArgumentException when given pieceIndex not within [0-numPieces-1]
     */
    public abstract boolean isCompleteBlock(int pieceIndex, BlockRangeMap toCheck);

    /**
     * Returns the BTInterval that is stored at the given byte offset.
     * @param byteLocation
     * 
     * @throws IllegalArgumentException when given byte range not within [0-totalSize-1]
     */
    public abstract BTInterval getPieceAt(long byteLocation);

    /**
     * Gets the highByte of the given BTInterval
     * 
     * @param piece
     */
    public long getHighByte(BTInterval btInterval);

    /**
     * Gets the lowByte of the given BTInterval
     * 
     * @param piece
     */
    public long getLowByte(BTInterval btInterval);

    /**
     * Returns true if this torrent has webseed addresses.
     */
    public abstract boolean hasWebSeeds();

}