package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.util.Arrays;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.util.SwingTestCase;

public class LibraryManagerItemImplTest extends SwingTestCase {

    private Mockery context;
    private LibraryData libraryData;
    
    public LibraryManagerItemImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LibraryManagerItemImplTest.class);
    }
    
    public void testDisplayName() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test");
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, null, null, itemFile);
        
        item.setShowFullName(true);
        assertEquals(itemFile.getPath(), item.displayName());
        assertEquals(itemFile.getPath(), item.toString());
        
        item.setShowFullName(false);
        assertEquals(itemFile.getName(), item.displayName());
        assertEquals(itemFile.getName(), item.toString());
    }
    
    public void testParentFile() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test");
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, null, null, itemFile);
        
        assertEquals(itemFile, item.getFile());
        assertEquals(root, item.getParent());
    }
    
    public void testEquals() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test");
        File itemFile2 = createNewBaseDirectory("test2");
        
        LibraryManagerItem item1 = new LibraryManagerItemImpl(root, null, null, itemFile);
        LibraryManagerItem item2 = new LibraryManagerItemImpl(root, null, null, itemFile);
        LibraryManagerItem item3 = new LibraryManagerItemImpl(root, null, null, itemFile2);
        
        assertTrue(item1.equals(item1));
        assertTrue(item1.equals(item2));
        assertFalse(item1.equals(item3));
    }
    
    public void testNoChildren() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test");
        File itemFile2 = createNewBaseDirectory("test2");
        LibraryManagerItem item = new LibraryManagerItemImpl(root, null, null, itemFile);
        
        assertEquals(0, item.getChildren().size());
        assertNull(item.getChildFor(itemFile2));
    }
    
    public void testChildrenFiles() throws Exception {
        setupMockery();
               
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test");
        itemFile.mkdir();
        
        //only files exist below this item
        final File f1 = createNewNamedTestFile(3, "test1", "txt", itemFile);
        final File f2 = createNewNamedTestFile(5, "test2", "txt", itemFile);
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(f1);
            will(returnValue(false));
            one(libraryData).isDirectoryAllowed(f2);
            will(returnValue(false));
        }});
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, null, itemFile);
        
        assertEquals(0, item.getChildren().size());
    }
    
    public void testOneChildFolderNotExcluded() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test");
        itemFile.mkdir();
        
        //subfolder of item
        final File childFolder = new File(itemFile, "subFolder");
        childFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(childFolder);
            will(returnValue(true));
        }});
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, new ExcludedFolderCollectionManagerImpl(), itemFile);
    
        assertEquals(1, item.getChildren().size());
        
        //subfolder should be a child
        LibraryManagerItem childItem = item.getChildFor(childFolder);
        assertNotNull(childItem);
        assertEquals(childFolder, childItem.getFile());
    }
    
    public void testOneChildFolderExcluded() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test").getCanonicalFile();
        itemFile.mkdir();
        
        final File childFolder = new File(itemFile, "subFolder").getCanonicalFile();
        childFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(childFolder);
            will(returnValue(true));
        }});
        
        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl();
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, excludedFolders, itemFile);
        
        //exclude folder
        excludedFolders.exclude(childFolder);
    
        //subfolder should not be a child but an excluded directory
        assertEquals(0, item.getChildren().size());
        
        LibraryManagerItem childItem = item.getChildFor(childFolder);
        assertNull(childItem);
    }
    
    public void testRemoveAddChild() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File itemFile = createNewBaseDirectory("test");
        itemFile.mkdir();
        
        final File childFolder = new File(itemFile, "subFolder");
        childFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(childFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryExcluded(childFolder);
            will(returnValue(false)); //subfolder is not excluded 
        }});
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, new ExcludedFolderCollectionManagerImpl(), itemFile);
    
        //subfolder is a child
        assertEquals(1, item.getChildren().size());
        
        LibraryManagerItem childItem = item.getChildFor(childFolder);
        assertNotNull(childItem);
        assertEquals(childFolder, childItem.getFile());
        
        //removing subfolder moves it from child -> excluded
        item.removeChild(childItem);
        assertEquals(0, item.getChildren().size());
        
        //adding subfolder moves it from excluded -> child
        item.addChild(childItem);
        assertEquals(1, item.getChildren().size());
    }
    
    public void testAddExcludedChild() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        File baseFolder = createNewBaseDirectory("test");
        baseFolder.mkdir();
        
        final File childFolder = new File(baseFolder, "subFolder");
        childFolder.mkdir();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(childFolder);
            will(returnValue(true));
        }});
        
        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl();
        LibraryManagerItem baseItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, baseFolder);
        
        //exclude folder
        excludedFolders.exclude(childFolder);
        
        //only child is excluded
        assertEquals(0, baseItem.getChildren().size());
        
        LibraryManagerItem childItem = baseItem.getChildFor(childFolder);
        assertNull(childItem);
        
        LibraryManagerItem excludedChild = new LibraryManagerItemImpl(baseItem, libraryData, excludedFolders, childFolder);
        
        //adding subfolder moves it from excluded -> child
        baseItem.addChild(excludedChild);
        assertEquals(1, baseItem.getChildren().size());

        //removing subfolder moves it from child -> excluded
        baseItem.removeChild(excludedChild);
        assertEquals(0, baseItem.getChildren().size());
    }
    
    private void setupMockery() {
        context = new Mockery();
        libraryData = context.mock(LibraryData.class);
    }
}
