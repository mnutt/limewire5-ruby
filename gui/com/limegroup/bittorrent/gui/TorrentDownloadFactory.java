package com.limegroup.bittorrent.gui;

import java.io.File;
import java.io.IOException;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.util.FileUtils;

import com.limegroup.bittorrent.BTMetaInfo;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.download.GuiDownloaderFactory;

public class TorrentDownloadFactory implements GuiDownloaderFactory {

	private final BTMetaInfo info;
	public TorrentDownloadFactory(File f) throws IOException {
		byte [] b = FileUtils.readFileFully(f);
		if (b == null)
			throw new IOException();
		info = GuiCoreMediator.getBTMetaInfoFactory().createBTMetaInfoFromBytes(b);
	}
	
	public TorrentDownloadFactory(BTMetaInfo info) {
		this.info = info;
	}
	
	public Downloader createDownloader(boolean overwrite) 
	throws SaveLocationException {
		return GuiCoreMediator.getDownloadServices().downloadTorrent(info, overwrite);
	}

	public long getFileSize() {
		return info.getFileSystem().getTotalSize();
	}

	public File getSaveFile() {
		return info.getFileSystem().getCompleteFile();
	}

	public URN getURN() {
		return info.getURN();
	}
    
    public BTMetaInfo getBTMetaInfo() {
        return info;
    }

	public void setSaveFile(File saveFile) {
		info.getFileSystem().setCompleteFile(saveFile);
	}

}
