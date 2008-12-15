package com.limegroup.gnutella.gui.download;

import java.io.File;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.settings.SharingSettings;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;

/**
 * Creates a Downloader from a magnet
 *
 */
public class MagnetDownloaderFactory implements GuiDownloaderFactory {

	private MagnetOptions magnet;
	private File saveFile;
	
	/**
	 * Constructs a factory for a magnet
	 * @param magnet
	 * @throws IllegalArgumentException if the magnet is not 
	 * {@link MagnetOptions#isDownloadable() valid for download}
	 */
	public MagnetDownloaderFactory(MagnetOptions magnet) {
		this.magnet = magnet;
		String fileName = magnet.getFileNameForSaving();
		if (fileName == null) { 
			fileName = I18n.tr("No Filename");
		}
		this.saveFile = new File(SharingSettings.getSaveDirectory(fileName), fileName);
		if (!magnet.isDownloadable()) {
			throw new IllegalArgumentException("Invalid magnet");
		}
	}
	
	public File getSaveFile() {
		return saveFile;
	}

	public void setSaveFile(File saveFile) {
		this.saveFile = saveFile;
	}

	public long getFileSize() {
		return 0;
	}

	public URN getURN() {
		return magnet.getSHA1Urn();
	}

	public Downloader createDownloader(boolean overwrite)
			throws SaveLocationException {
		return GuiCoreMediator.getDownloadServices().download(magnet, overwrite, 
									  saveFile.getParentFile(),
									  getSaveFile().getName());
	}

}
