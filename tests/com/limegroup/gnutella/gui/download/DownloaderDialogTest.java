package com.limegroup.gnutella.gui.download;

import java.io.File;

import org.limewire.core.api.download.SaveLocationException;
import org.limewire.core.api.download.SaveLocationException.LocationCode;

import junit.framework.Test;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GUIBaseTestCase;

public class DownloaderDialogTest extends GUIBaseTestCase {

	private MockDownloaderFactory factory;

	public DownloaderDialogTest(String name) {
		super(name);
	}

	@Override
	protected void setUp() throws Exception {
		factory = new MockDownloaderFactory();
	}
	
    public static Test suite() { 
        return buildTestSuite(DownloaderDialogTest.class); 
    } 
    
	public void testCreateUniqueFilenameDownloader() throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "limedownload.txt");
		file.createNewFile();
		file.deleteOnExit();
		
		createUniqueFilenameDownloader(file);
		assertEquals("limedownload(1).txt", factory.getSaveFile().getName());

		factory.getSaveFile().createNewFile();
		factory.getSaveFile().deleteOnExit();
		
		createUniqueFilenameDownloader(file);
		assertEquals("limedownload(2).txt", factory.getSaveFile().getName());
		
		file.delete();
		createUniqueFilenameDownloader(file);
		assertEquals("limedownload.txt", factory.getSaveFile().getName());		
	}

	public void testCreateUniqueFilenameDownloaderNoExtension() throws Exception {
		// no extension
		File file = new File(System.getProperty("java.io.tmpdir") + File.separator + "limedownload");
		file.delete();
		
		createUniqueFilenameDownloader(file);
		assertEquals(file, factory.getSaveFile());

		file = new File(System.getProperty("java.io.tmpdir") + File.separator + "limedownload.");
		file.delete();
		
		createUniqueFilenameDownloader(file);
		assertEquals(file, factory.getSaveFile());

		// extension only
		file = new File(System.getProperty("java.io.tmpdir") + File.separator + ".limedownload");
		file.delete();
		
		createUniqueFilenameDownloader(file);
		assertEquals(file, factory.getSaveFile());
	}

	public void testCreateUniqueFilenameDownloaderInvalidPath() throws Exception {
		File file = new File(File.separator + "invalidpath" + File.separator + "limedownload.txt");
		createUniqueFilenameDownloader(file);
		assertEquals(file, factory.getSaveFile());
	}

	public void testCreateUniqueFilenameDownloaderEmptyFilename() throws Exception {
		File file = new File(System.getProperty("java.io.tmpdir"), "");
		createUniqueFilenameDownloader(file);
		assertEquals(new File(System.getProperty("java.io.tmpdir") + "(1)"), factory.getSaveFile());
	}

	private void createUniqueFilenameDownloader(File file) {
		factory.setSaveFile(file);
		SaveLocationException e = new SaveLocationException(LocationCode.FILE_ALREADY_EXISTS, file);
		DownloaderDialog d = new DownloaderDialog(factory, e);
		d.createUniqueFilenameDownloader();
	}
	
	private class MockDownloaderFactory implements GuiDownloaderFactory {

		private File saveFile;

		public Downloader createDownloader(boolean overwrite) throws SaveLocationException {
			return null;
		}

		public long getFileSize() {
			return 0;
		}

		public File getSaveFile() {
			return saveFile;
		}

		public URN getURN() {
			return null;
		}

		public void setSaveFile(File saveFile) {
			this.saveFile = saveFile;
		}
		
	}
	
}
