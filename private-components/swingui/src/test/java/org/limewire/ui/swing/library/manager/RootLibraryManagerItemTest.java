package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import junit.framework.Test;

import org.limewire.ui.swing.util.SwingTestCase;

public class RootLibraryManagerItemTest extends SwingTestCase {

    public RootLibraryManagerItemTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(RootLibraryManagerItemTest.class);
    }

    public void testNullConstructor() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(null);
        
        assertNull(root.getParent());
        assertNull(root.getFile());
        assertEquals("root", root.displayName());
        assertEquals(0, root.getChildren().size());
        assertEquals(Collections.emptyList(), root.getChildren());
        assertNull(root.getChildFor(new File("test")));
    }
    
    public void testSingleRoot() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        assertNull(root.getParent());
        assertNull(root.getFile());
        assertEquals("root", root.displayName());
        assertEquals(Collections.emptyList(), root.getChildren());
        
        //add new child
        File f1 = createNewNamedDir("test", baseDir);
        LibraryManagerItem item = new LibraryManagerItemImpl(root, null, null, f1);
        root.addChild(item);
        
        assertEquals(1, root.getChildren().size());
        assertEquals(f1, root.getChildren().get(0).getFile());
        
        assertEquals(item, root.getChildFor(f1));
        
        //remove child
        root.removeChild(item);
        assertEquals(0, root.getChildren().size());
        assertNull(root.getChildFor(f1));

    }
    
    public void testMultipleRoots() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        assertNull(root.getParent());
        assertNull(root.getFile());
        assertEquals("root", root.displayName());
        assertEquals(Collections.emptyList(), root.getChildren());
        
        //add new child
        File f1 = createNewNamedDir("test", baseDir);
        LibraryManagerItem item = new LibraryManagerItemImpl(root, null, null, f1);
        root.addChild(item);
        
        File f2 = createNewNamedDir("test2", baseDir);
        LibraryManagerItem item2 = new LibraryManagerItemImpl(root, null, null, f2);
        root.addChild(item2);
        
        
        assertEquals(2, root.getChildren().size());
        
        assertEquals(item, root.getChildFor(f1));
        assertEquals(item2, root.getChildFor(f2));
        
        //remove child
        root.removeChild(item);
        assertEquals(1, root.getChildren().size());
        assertNull(root.getChildFor(f1));
        assertEquals(item2, root.getChildFor(f2));
        
        root.removeChild(item2);
        assertEquals(0, root.getChildren().size());
    }
    
    public void testRead() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        //add new child
        File f1 = createNewNamedDir("test", baseDir);
        LibraryManagerItem item = new LibraryManagerItemImpl(root, null, null, f1);
        root.addChild(item);
        
        assertEquals(1, root.getChildren().size());
    
        try {
            root.addChild(item);
            fail("root should exist already");
        } catch(IllegalStateException e) {
        }
    }
    
   
    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name & extension, in the given directory.
     */
    public static File createNewNamedDir(String name, File directory) throws Exception {
        File f = new File(directory, name);
                
        f.mkdirs();
        f.deleteOnExit();
        
        return f.getCanonicalFile();
    }
    
}
