package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Collections;

import junit.framework.Test;

import org.limewire.ui.swing.util.SwingTestCase;

/**
 * Tests NoChildLibraryManagerItem
 */
public class NoChildLibraryManagerItemTest extends SwingTestCase {

    public NoChildLibraryManagerItemTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(NoChildLibraryManagerItemTest.class);
    }
        
    public void testChild() throws Exception {
        LibraryManagerItem parent = new RootLibraryManagerItem(null);
        
        File childFile = createNewNamedTestFile(3, "test", "txt", baseDir);
        NoChildrenLibraryManagerItem item = new NoChildrenLibraryManagerItem(parent, childFile);
        
        //toString
        item.setShowFullName(true);
        assertEquals(childFile.getPath(), item.toString());
        item.setShowFullName(false);
        assertEquals(childFile.getName(), item.toString());
        
        //adding child
        try {
            item.addChild(new LibraryManagerItemImpl(item, null, null, childFile));
            fail();
        } catch(IllegalStateException e) {
        }
        
        //display name
        item.setShowFullName(true);
        assertEquals(childFile.getPath(), item.displayName());
        item.setShowFullName(false);
        assertEquals(childFile.getName(), item.displayName());
        
        //getChild
        try {
            item.getChildFor(baseDir);
            fail();
        } catch(IllegalStateException e) {
        }
        
        assertEquals(Collections.emptyList(), item.getChildren());
        
        assertEquals(childFile, item.getFile());
        assertEquals(parent, item.getParent());
        
        try {
            item.removeChild(new LibraryManagerItemImpl(item, null, null, childFile));
            fail();
        } catch(IllegalStateException e) {
        }
    }
}
