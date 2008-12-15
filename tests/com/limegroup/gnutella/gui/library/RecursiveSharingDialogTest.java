package com.limegroup.gnutella.gui.library;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.util.Arrays;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.library.RecursiveSharingDialog.State;

public class RecursiveSharingDialogTest extends GUIBaseTestCase {

	public RecursiveSharingDialogTest(String name) {
		super(name);
	}
	
	public static Test suite() {
        return buildTestSuite(RecursiveSharingDialogTest.class);
    }
	
	@Override
	protected void setUp() throws Exception {
	    super.setUp();
	    LimeTestUtils.createInjector(new AbstractModule() { 
	        @Override
	        protected void configure() {
	            requestStaticInjection(GuiCoreMediator.class);
	        }
	    });
	}
	
	public void testShowRecursiveDialogWithEmptyDirs() {
		File[] dirs = LimeTestUtils.createTmpDirs("emptydir");
		RecursiveSharingDialog dialog = new RecursiveSharingDialog((Frame)null, dirs);
		assertEquals(State.OK, dialog.showChooseDialog((Component)null));
		assertTrue(dialog.getRootsToShare().containsAll(Arrays.asList(dirs)));
		assertTrue(dialog.getFoldersToExclude().isEmpty());
	}
}
