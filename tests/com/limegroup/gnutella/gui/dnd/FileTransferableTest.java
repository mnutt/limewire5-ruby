package com.limegroup.gnutella.gui.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.limewire.util.OSUtils;

import com.limegroup.gnutella.util.LimeTestCase;

public class FileTransferableTest extends LimeTestCase {

	public FileTransferableTest(String name) {
		super(name);
	}
	
	public void testGetTransferData() throws UnsupportedFlavorException, IOException {
		List<File> files = Arrays.asList(new File("/test/file 1"), new File("/Test Dir/file2"));
		Transferable transferable = new FileTransferable(files);
		assertEquals(files, transferable.getTransferData(DataFlavor.javaFileListFlavor));
		if (OSUtils.isWindows()) {
			files = Arrays.asList(new File("C:\\test\file 1"),
					new File("C:\\Test Dir\\file2"));
			assertEquals("file:/C:/test/file%201" + System.getProperty("line.separator")
					+ "file:/C:/Test%20Dir/file2",
					transferable.getTransferData(FileTransferable.URIFlavor));
		}
		else {
			assertEquals("file:/test/file%201" + System.getProperty("line.separator")
					+ "file:/Test%20Dir/file2",
					transferable.getTransferData(FileTransferable.URIFlavor));
		}
	}

}
