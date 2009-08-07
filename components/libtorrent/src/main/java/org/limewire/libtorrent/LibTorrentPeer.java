package org.limewire.libtorrent;

import com.sun.jna.Structure;

public class LibTorrentPeer extends Structure implements Structure.ByReference {
    public String ip;
    public short source;
    
}
