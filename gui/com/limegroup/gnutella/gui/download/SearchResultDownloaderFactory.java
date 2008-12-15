package com.limegroup.gnutella.gui.download;


import java.io.File;
import java.util.List;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.SharingSettings;
import org.limewire.io.GUID;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GuiCoreMediator;

/**
 * Implements the DownloaderFactory interface to start downloads from
 * incoming search results.
 */
public class SearchResultDownloaderFactory implements GuiDownloaderFactory {

	private RemoteFileDesc[] rfds;
	private List<? extends RemoteFileDesc> alts;
	private GUID queryGUID;
	private File saveDir;
	private String fileName;
	
	public SearchResultDownloaderFactory(RemoteFileDesc[] rfds,
			List<? extends RemoteFileDesc> alts, GUID queryGUID,
			File saveDir, String fileName) {
		this.rfds = rfds;
		this.alts = alts;
		this.queryGUID = queryGUID;
		this.saveDir = saveDir;
		this.fileName = fileName != null ? fileName : rfds[0].getFileName();
	}
			
	
	public URN getURN() {
		return rfds[0].getSHA1Urn();
	}

	public Downloader createDownloader(boolean overwrite)
		throws SaveLocationException {
		return GuiCoreMediator.getDownloadServices().download(rfds, alts, queryGUID, overwrite, saveDir, fileName);
	}

	/** 
	 * can be 
	 * <code>null</code>, then the 
	 * {@link org.limewire.core.settings.SharingSettings#getSaveDirectory(String fileName)} 
	 * is used.
	 */
	public File getSaveFile() {
		return new File(saveDir != null ? saveDir : SharingSettings.getSaveDirectory(fileName),
				fileName);
	}

	public void setSaveFile(File saveFile) {
		File parentDir = saveFile.getParentFile();
		fileName = saveFile.getName();
		if (SharingSettings.getSaveDirectory(fileName).equals(parentDir)) {
			saveDir = null;
		} else {
			saveDir = parentDir;
		}
	}


	public long getFileSize() {
		return rfds[0].getSize();
	}
}
