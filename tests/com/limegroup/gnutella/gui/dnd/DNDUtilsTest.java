package com.limegroup.gnutella.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.util.LimeTestCase;
import com.limegroup.gnutella.util.EncodingUtils;

public class DNDUtilsTest extends LimeTestCase {

	private List<File> files;
	
	public DNDUtilsTest(String name) {
		super(name);
	}
	
	@Override
	protected void setUp() throws Exception {
		files = Arrays.asList(new File("/test/file with spaces"), 
				new File("/test me/"));
	}
	
	public void testGetURIs() throws UnsupportedFlavorException, IOException {
		assertEquals(1, DNDUtils.getURIs(new URITransferable("magnet:?dn=" + EncodingUtils.encode("compile me") 
		+ "&xt=urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59&xs=http://127.0.0.1:6346/uri-res/N2R?urn:sha1:YNCKHTQCWBTRNJIV4WNAE52SJUQCZO59")).length);
		assertEquals(2, DNDUtils.getURIs(new FileTransferable(files)).length);
	}
	
	public void testGetFiles() throws UnsupportedFlavorException, IOException {
		// use java file list flavor
		assertEquals(files, Arrays.asList(DNDUtils.getFiles(new FileTransferable(files))));
		File file = new File("C:\\test\\file with spaces");
		assertEquals(file, DNDUtils.getFiles(new FileTransferable(Collections.singletonList(file)))[0]);
		// use uri flavor
		if (OSUtils.isWindows()) {
			assertEquals(file, DNDUtils.getFiles(new URITransferable(file.toURI().toString()))[0]);
		}
		else { 
			file = new File("/test/dir with spaces/file");
			assertEquals(file, DNDUtils.getFiles(new URITransferable(file.toURI().toString()))[0]);
		}
	}
	
	private class URITransferable implements Transferable {

		private String uri;
		
		public URITransferable(String uri) { 
			this.uri = uri;
		}
		
		public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
			return uri;
		}

		public DataFlavor[] getTransferDataFlavors() {
			return new DataFlavor[] { FileTransferable.URIFlavor };
		}

		public boolean isDataFlavorSupported(DataFlavor flavor) {
			return flavor.equals(FileTransferable.URIFlavor);
		}
		
	}
	

}
