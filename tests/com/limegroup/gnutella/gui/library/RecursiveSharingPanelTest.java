package com.limegroup.gnutella.gui.library;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import junit.framework.Test;

import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;
import com.limegroup.gnutella.gui.LimeWireModule;

public class RecursiveSharingPanelTest extends GUIBaseTestCase {
    
    public RecursiveSharingPanelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(RecursiveSharingPanelTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        LimeTestUtils.createInjector(new LimeWireModule());
    }
    
    public void testRetainAncestorsEmptyInput() {
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        assertEquals(Collections.emptySet(), panel.retainAncestors(new File[0]));
    }
    
    public void testRetainAncestorsSingleFile() {
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        panel.setRootFilter(new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        });
        File file = new File("/test/file");
        assertEquals(Collections.singleton(file), panel.retainAncestors(file));
    }
    
    public void testRetainAncestorsNoAncestors() {
        File[] files = new File[] {
                new File("/test/test/test"),
                new File("/test/test/hello"),
                new File("/test/dir"),
                new File("/test/tmp/dir"),
                new File("/blah")
        };
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        panel.setRootFilter(new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        });        
        assertEquals(new HashSet<File>(Arrays.asList(files)), panel.retainAncestors(files));
    }
    
    public void testRetainAncestorsWithSubfoldersToRemove() {
        File[] files = new File[] {
                new File("/test/test/"),
                new File("/test/test/hello"),
                new File("/test/dir"),
                new File("/test/dir"),
                new File("/blah")
        };
        File[] resulting = new File[] {
                new File("/test/test/"),
                new File("/test/dir"),
                new File("/blah")
        };
        RecursiveSharingPanel panel = new RecursiveSharingPanel();
        panel.setRootFilter(new FileFilter() {
            public boolean accept(File pathname) {
                return true;
            }
        });
        assertEquals(new HashSet<File>(Arrays.asList(resulting)), panel.retainAncestors(files));
    }

}
