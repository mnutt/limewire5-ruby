package org.limewire.libtorrent;

import org.limewire.bittorrent.TorrentPeer;

import com.sun.jna.Structure;

public class LibTorrentPeer extends Structure implements Structure.ByReference, TorrentPeer {
    public String peer_id;
    public String ip;
    public short source;
    public float up_speed;
    public float down_speed;
    public float payload_up_speed;
    public float payload_down_speed;
    public float progress;
    public String country;

    @Override
    public String getCountry() {
        return country;
    }

    @Override
    public float getDownloadSpeed() {
        return down_speed;
    }

    @Override
    public String getIPAddress() {
        return ip;
    }

    @Override
    public float getPayloadDownloadSpeed() {
        return payload_down_speed;
    }

    @Override
    public float getPayloadUploadSpeed() {
        return payload_up_speed;
    }

    @Override
    public String getPeerId() {
        return peer_id;
    }

    @Override
    public float getProgress() {
        return progress;
    }

    @Override
    public short getSource() {
        return source;
    }

    @Override
    public float getUploadSpeed() {
        return up_speed;
    }
}
