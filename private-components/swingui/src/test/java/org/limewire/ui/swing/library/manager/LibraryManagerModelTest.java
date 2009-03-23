package org.limewire.ui.swing.library.manager;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import junit.framework.Test;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.util.SwingTestCase;

public class LibraryManagerModelTest extends SwingTestCase {
    
    private Mockery context;
    private LibraryData libraryData;
    
    private File firstFolder;
    private File secondFolder;
    private File thirdFolder;
    private File fourthFolder;
    
    public LibraryManagerModelTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LibraryManagerModelTest.class);
    }
    
    protected void postTearDown() {
        super.postTearDown();

        if(firstFolder != null) firstFolder.delete();
        if(secondFolder != null) secondFolder.delete();
        if(thirdFolder != null) thirdFolder.delete();
        if(fourthFolder != null) fourthFolder.delete();
    }
    
    /**
     * Single root with no children
     */
    public void testSimpleRoot() throws Exception {
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ExcludedFolderCollectionManagerImpl());
        
        LibraryManagerItem[] items = model.getPathToRoot(root);
        assertEquals(1, items.length);
        assertEquals(root, items[0]);
        
        RootLibraryManagerItem currentRoot = model.getRoot();
        assertEquals(root, currentRoot);
        
        assertEquals(0, model.getAllExcludedSubfolders().size());
        
        assertEquals(0, model.getRootChildrenAsFiles().size());
    }
    
    public void testSingleRootOneChild() throws Exception {
        //single root with one child
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        firstFolder = createFolder(baseDir, "firstFolder");

        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        
        root.addChild(item);
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ExcludedFolderCollectionManagerImpl());
        
        //child returns parent root
        LibraryManagerItem[] items = model.getPathToRoot(item);
        assertEquals(2, items.length);
        assertEquals(root, items[0]);
        
        //find child of root
        LibraryManagerItem childOf = model.getChild(root, 0);
        assertNotNull(childOf);
        assertEquals(item, childOf);
        // should only be one child of root
        childOf = model.getChild(root, 1);
        assertNull(childOf);
        
        //child count
        int numChildren = model.getChildCount(root);
        assertEquals(1, numChildren);
        numChildren = model.getChildCount(item);
        assertEquals(0, numChildren);
        
        //child files
        Collection<File> files = model.getRootChildrenAsFiles();
        assertEquals(1, files.size());
        assertEquals(firstFolder, files.iterator().next());
        
        //excluded folders
        files = model.getAllExcludedSubfolders();
        assertEquals(0, files.size());
        
        assertEquals(0, model.getIndexOfChild(root, item));
        
    }
    
    /**
     * Recurses through the tree using the model to retrieve children
     * of a given node
     */
    public void testChildrenRecursion() throws Exception {
        setupMockery();
        
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(firstFolder, "secondFolder");
        thirdFolder = createFolder(secondFolder, "thirdFolder");
        fourthFolder = createFolder(secondFolder, "fourthFolder");

        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
               
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(thirdFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(fourthFolder);
            will(returnValue(true));
        }});

        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
        LibraryManagerModel model = new LibraryManagerModel(root, excludedFolders);
        
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        
        // add the firstFolder to the root
        model.addChild(firstItem, root);
        assertEquals(1, model.getChildCount(root));

        LibraryManagerItem item = model.getChild(root, 0);
        //look at firstFolder children
        assertEquals(1, model.getChildCount(item));
        assertEquals(firstFolder, item.getFile());
        
        //get firstFolder's child
        item = model.getChild(item, 0);
        assertEquals(2, model.getChildCount(item));
        assertEquals(secondFolder, item.getFile());
        
    }
    
    public void testAddRemoveChild() throws Exception {
        setupMockery();
        
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(firstFolder, "secondFolder");
        
        context.checking(new Expectations() {{
            allowing(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            allowing(libraryData).isDirectoryAllowed(firstFolder);
            will(returnValue(true));
        }});
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ExcludedFolderCollectionManagerImpl());

        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        
        model.addChild(firstItem, root);
        libraryData.isDirectoryAllowed(secondFolder);
        
        assertEquals(1, model.getChildCount(firstItem));
        
        LibraryManagerItem childItem = firstItem.getChildFor(secondFolder);
        
        //remove child
        model.removeChild(childItem);
        assertEquals(0, model.getChildCount(firstItem));
        assertEquals(-1, model.getIndexOfChild(childItem, firstItem));
        
        // add child
        model.addChild(childItem, firstItem);
        assertEquals(1, model.getChildCount(firstItem));
        assertEquals(0, model.getIndexOfChild(firstItem, childItem));
    }    
           
    /**
     * Removes current children of the root and replaces them with
     * new children.
     */
    public void testReplaceRoots() throws Exception {
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(baseDir, "secondFolder");
        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        root.addChild(firstItem);
        
        LibraryManagerItem secondItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, secondFolder);
        
        LibraryManagerModel model = new LibraryManagerModel(root, new ExcludedFolderCollectionManagerImpl());
        
        List<LibraryManagerItem> items = model.getRoot().getChildren();
        
        assertEquals(1, items.size());
        assertEquals(firstItem, items.get(0));
        
        model.setRootChildren(Arrays.asList(new LibraryManagerItem[]{secondItem}));
        items = model.getRoot().getChildren();
        assertEquals(1, items.size());
        assertEquals(secondItem, items.get(0));
    }

    public void testSingleRootExcludedChild() throws Exception {
        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl();
        //single root with one excluded child
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        firstFolder = createFolder(baseDir, "firstFolder");
        
        LibraryManagerItem item = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        
        root.addChild(item);
        root.removeChild(item);
        
        excludedFolders.exclude(firstFolder);
        LibraryManagerModel model = new LibraryManagerModel(root, excludedFolders);
        
        Collection<File> exclusions = model.getAllExcludedSubfolders();
        assertEquals(1, exclusions.size());
        
        exclusions = model.getRootChildrenAsFiles();
        assertEquals(0, exclusions.size());
        
        assertEquals(-1, model.getIndexOfChild(root, item));
        
        model.unexcludeAllSubfolders(baseDir);
        exclusions = model.getAllExcludedSubfolders();
        assertEquals(0, exclusions.size());
    }
    
    /**
     * Unexcludes children of a folder.
     */
    public void testUnexcludeChildren() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(firstFolder, "secondFolder");
        thirdFolder = createFolder(baseDir, "anotherFolder");
        fourthFolder = createFolder(thirdFolder, "anotherExcludedFolder");
        
        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(fourthFolder);
            will(returnValue(true));
        }});
        
        //add two roots
        LibraryManagerItem firstParentItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
//        LibraryManagerItem secondItem = new LibraryManagerItemImpl(firstItem, libraryData, excludedFolders, secondFolder);
//        firstItem.addChild(secondItem);
        root.addChild(firstParentItem);
        
        LibraryManagerItem secondParentItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, thirdFolder);
        root.addChild(secondParentItem);
        
        //add both children of roots as excluded folders
        excludedFolders.exclude(secondFolder);
        excludedFolders.exclude(fourthFolder);
        
        LibraryManagerModel model = new LibraryManagerModel(root, excludedFolders);
        
        assertEquals(2, model.getRootChildrenAsFiles().size());
        assertEquals(2, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        assertContains(model.getRootChildrenAsFiles(), thirdFolder);

        assertEquals(0, firstParentItem.getChildren().size());
        assertEquals(0, secondParentItem.getChildren().size());
        assertTrue(model.getAllExcludedSubfolders().contains(secondFolder));
        assertTrue(model.getAllExcludedSubfolders().contains(fourthFolder));
        
        //unexclude the child
        model.unexcludeAllSubfolders(firstFolder);
        
        //only anotherExcludedChild should still exist in the list
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertTrue(model.getAllExcludedSubfolders().contains(fourthFolder));
    }
    
    /**
     * restoreChildren is called when the children haven't been explicitely added to the parent
     * and all its children are to be unexcluded.
     */
    public void testRestoreChildren() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(firstFolder, "secondFolder");
        thirdFolder = createFolder(baseDir, "anotherFolder");
        fourthFolder = createFolder(thirdFolder, "anotherExcludedFolder");

        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryExcluded(secondFolder);
            will(returnValue(true));
        }});
                
        //add two roots
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        root.addChild(firstItem);
        LibraryManagerItem anotherItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, thirdFolder);
        root.addChild(anotherItem);
        
        //add both children of roots as excluded folders
        excludedFolders.exclude(secondFolder);
        excludedFolders.exclude(fourthFolder);
        
        LibraryManagerModel model = new LibraryManagerModel(root, excludedFolders);
        
        assertEquals(2, model.getRootChildrenAsFiles().size());
        assertEquals(2, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        
        assertEquals(0, firstItem.getChildren().size());
        assertTrue(model.getAllExcludedSubfolders().contains(secondFolder));
        
        //unexclude the child
        model.addChild(new LibraryManagerItemImpl(firstItem, libraryData, excludedFolders, secondFolder), firstItem);
        model.getAllExcludedSubfolders().remove(firstItem.getFile());
        model.unexcludeAllSubfolders(firstItem.getFile());
        
        //only anotherExcludedChild should still exist in the list
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertTrue(model.getAllExcludedSubfolders().contains(fourthFolder));
    }
    
    /**
     * Add firstFolder to root, its child is excluded.
     * Excluding the firstFolder will remove it from root since its a parent,
     * its child will also be removed from the exclusion list.
     */
    public void testExcludeRootChild() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(firstFolder, "secondFolder");
        
        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
                
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
        }});
        
        //add two roots
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        root.addChild(firstItem);
        
        //add child of root as excluded folder
        excludedFolders.exclude(secondFolder);
        
        LibraryManagerModel model = new LibraryManagerModel(root, excludedFolders);
        
        //firstFolder is a child of the root
        assertEquals(1, model.getRootChildrenAsFiles().size());
        assertTrue(model.getRootChildrenAsFiles().contains(firstItem.getFile()));
        //firstFolder's child is in the exclusion list
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertTrue(model.getAllExcludedSubfolders().contains(secondFolder));
        
        // remove firstFolder
        model.excludeChild(firstItem);
        
        // firstFolder should be removed from root and its children from exclusion list
        assertEquals(0, model.getRootChildrenAsFiles().size());
        assertEquals(0, model.getAllExcludedSubfolders().size());
    }
    
    /**
     * root -> firstFolder -> secondFolder -> thirdFolder -> fourthFolder
     * Step 1) exclude secondFolder (will remove all its children also)
     * Step 2) unexclude thirdFolder (only second Folder is excluded)
     * Step 3) exclude fourthFolder
     */
    public void testFourSubFolders() throws Exception {
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(firstFolder, "secondFolder");
        thirdFolder = createFolder(secondFolder, "thirdFolder");
        fourthFolder = createFolder(thirdFolder, "fourthFolder");
        
        final ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl();
        
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(thirdFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(fourthFolder);
            will(returnValue(true));
        }});
        
        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);

        LibraryManagerModel model = new LibraryManagerModel(root, excludedFolders);

        model.addChild(firstItem, root);

        
        assertEquals(1, model.getRootChildrenAsFiles().size());
        assertEquals(0, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        
        //exclude first folder's child, second folder
        LibraryManagerItem secondItem = firstItem.getChildFor(secondFolder);
        model.excludeChild(secondItem);
        
        assertEquals(1, model.getRootChildrenAsFiles().size());
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        assertContains(model.getAllExcludedSubfolders(), secondItem.getFile());
        
        //readd second folder's child, third folder
        LibraryManagerItem thirdItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, thirdFolder);
        model.addChild(thirdItem, root);
        
        assertEquals(1, model.getAllExcludedSubfolders().size());
        assertContains(model.getAllExcludedSubfolders(), secondItem.getFile());
        assertEquals(2, model.getRootChildrenAsFiles().size());
        assertContains(model.getRootChildrenAsFiles(), firstFolder);
        assertContains(model.getRootChildrenAsFiles(), thirdFolder);

        //exclude third folder's child, fourth folder
        LibraryManagerItem fourthItem = thirdItem.getChildFor(fourthFolder);
        model.excludeChild(fourthItem);

        assertEquals(2, model.getAllExcludedSubfolders().size());
        assertContains(model.getAllExcludedSubfolders(), secondItem.getFile());
        assertContains(model.getAllExcludedSubfolders(), fourthItem.getFile());
    }
    
    public void testNestedExclusion() throws Exception{
        setupMockery();
        
        RootLibraryManagerItem root = new RootLibraryManagerItem(Arrays.asList(new File[]{baseDir}));
        
        firstFolder = createFolder(baseDir, "firstFolder");
        secondFolder = createFolder(firstFolder, "secondFolder");
        thirdFolder = createFolder(secondFolder, "thirdFolder");
        fourthFolder = createFolder(thirdFolder, "fourthFolder");
        
        ExcludedFolderCollectionManager excludedFolders = new ExcludedFolderCollectionManagerImpl(); 
                
        context.checking(new Expectations() {{
            one(libraryData).isDirectoryAllowed(firstFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(secondFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(thirdFolder);
            will(returnValue(true));
            one(libraryData).isDirectoryAllowed(fourthFolder);
            will(returnValue(true));
        }});
        

        LibraryManagerItem firstItem = new LibraryManagerItemImpl(root, libraryData, excludedFolders, firstFolder);
        LibraryManagerItem secondItem = new LibraryManagerItemImpl(firstItem, libraryData, excludedFolders, secondFolder);
        LibraryManagerItem thirdItem = new LibraryManagerItemImpl(secondItem, libraryData, excludedFolders, thirdFolder);
        LibraryManagerItem fourthItem = new LibraryManagerItemImpl(thirdItem, libraryData, excludedFolders, fourthFolder);
        root.addChild(firstItem);        
        
        LibraryManagerModel model = new LibraryManagerModel(root, excludedFolders);
                   
        // remove fourthFolder
        model.excludeChild(fourthItem);        
        assertTrue(model.getAllExcludedSubfolders().contains(fourthFolder));
        
        // remove secondFolder
        model.excludeChild(secondItem);     
        //fourthFolder should be removed from excluded list
        assertFalse(model.getAllExcludedSubfolders().contains(fourthFolder));
    
    }
    

    
    private void setupMockery() {
        context = new Mockery();
        libraryData = context.mock(LibraryData.class);
    }
    
    private File createFolder(File parent, String name) throws IOException {
        File file = new File(parent, name);
        file.mkdir();
        return file.getCanonicalFile();
    }
}
