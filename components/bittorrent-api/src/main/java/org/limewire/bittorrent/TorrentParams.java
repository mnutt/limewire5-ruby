package org.limewire.bittorrent;

import java.io.File;

public class TorrentParams {

    private String name;

    private String sha1;

    private String trackerURL;

    private File fastResumeFile;

    private File torrentFile;

    private File torrentDataFile;

    private Boolean isPrivate;
    
    public TorrentParams(File torrentFile) {
        setTorrentFile(torrentFile);
    }
    
    public TorrentParams(String name, String sha1) {
        setName(name);
        setSha1(sha1);
    }

    public TorrentParams name(String name) {
        setName(name);
        return this;
    }

    public TorrentParams trackerURL(String trackerURL) {
        setTrackerURL(trackerURL);
        return this;
    }

    public TorrentParams sha1(String sha1) {
        setSha1(sha1);
        return this;
    }

    public TorrentParams torrentFile(File torrentFile) {
        setTorrentFile(torrentFile);
        return this;
    }

    public TorrentParams torrentDataFile(File torrentDataFile) {
        setTorrentDataFile(torrentDataFile);
        return this;
    }

    public TorrentParams fastResumeFile(File fastResumeFile) {
        setFastResumeFile(fastResumeFile);
        return this;
    }
    
    public TorrentParams isPrivate(boolean isPrivate) {
        setPrivate(isPrivate);
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSha1() {
        return sha1;
    }

    public void setSha1(String sha1) {
        this.sha1 = sha1;
    }

    public String getTrackerURL() {
        return trackerURL;
    }

    public void setTrackerURL(String trackerURL) {
        this.trackerURL = trackerURL;
    }

    public File getFastResumeFile() {
        return fastResumeFile;
    }

    public void setFastResumeFile(File fastResumeFile) {
        this.fastResumeFile = fastResumeFile;
    }

    public File getTorrentFile() {
        return torrentFile;
    }

    public void setTorrentFile(File torrentFile) {
        this.torrentFile = torrentFile;
    }

    public File getTorrentDataFile() {
        return torrentDataFile;
    }

    public void setTorrentDataFile(File torrentDataFile) {
        this.torrentDataFile = torrentDataFile;
    }

    public Boolean getPrivate() {
        return isPrivate;
    }

    public void setPrivate(Boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
