package org.limewire.core.impl.library;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.library.ManagedFileList;

public class LibraryDataImplTest extends BaseTestCase {

    public LibraryDataImplTest(String name) {
        super(name);
    }

    public void testGetDefaultExtensions() {
        Mockery context = new Mockery();

        final ManagedFileList managedList = context.mock(ManagedFileList.class);

        final Collection<String> testExtensions = new ArrayList<String>();
        context.checking(new Expectations() {
            {
                one(managedList).getDefaultManagedExtensions();
                will(returnValue(testExtensions));
            }
        });

        LibraryDataImpl libraryDataImpl = new LibraryDataImpl(managedList);
        Collection<String> defaultExtensions = libraryDataImpl.getDefaultExtensions();
        assertEquals(testExtensions, defaultExtensions);
        context.assertIsSatisfied();
    }

    public void testDirectoriesToExclude() {
        Mockery context = new Mockery();

        final ManagedFileList managedList = context.mock(ManagedFileList.class);

        final List<File> testExcludeDirectories = new ArrayList<File>();
        context.checking(new Expectations() {
            {
                one(managedList).getDirectoriesToExcludeFromManaging();
                will(returnValue(testExcludeDirectories));
            }
        });

        LibraryDataImpl libraryDataImpl = new LibraryDataImpl(managedList);
        List<File> directoriesToExclude = libraryDataImpl.getDirectoriesToExcludeFromManaging();
        assertEquals(testExcludeDirectories, directoriesToExclude);
        context.assertIsSatisfied();
    }

    public void testDirectoriesToManageRecursively() {
        Mockery context = new Mockery();

        final ManagedFileList managedList = context.mock(ManagedFileList.class);

        final List<File> testManageRescursiveDirectories = new ArrayList<File>();
        context.checking(new Expectations() {
            {
                one(managedList).getDirectoriesToManageRecursively();
                will(returnValue(testManageRescursiveDirectories));
            }
        });

        LibraryDataImpl libraryDataImpl = new LibraryDataImpl(managedList);
        List<File> directoriesToManage = libraryDataImpl.getDirectoriesToManageRecursively();
        assertEquals(testManageRescursiveDirectories, directoriesToManage);
        context.assertIsSatisfied();
    }

    public void testDirectoriesWithImportedFiles() {
        Mockery context = new Mockery();

        final ManagedFileList managedList = context.mock(ManagedFileList.class);

        final List<File> testDirectories = new ArrayList<File>();
        context.checking(new Expectations() {
            {
                one(managedList).getDirectoriesWithImportedFiles();
                will(returnValue(testDirectories));
            }
        });

        LibraryDataImpl libraryDataImpl = new LibraryDataImpl(managedList);
        Collection<File> directoriesWithImportedFiles = libraryDataImpl
                .getDirectoriesWithImportedFiles();
        assertEquals(testDirectories, directoriesWithImportedFiles);
        context.assertIsSatisfied();
    }

    public void testExtensionsPerCategory() {
        Mockery context = new Mockery();

        final ManagedFileList managedList = context.mock(ManagedFileList.class);

        final Map<Category, Collection<String>> testCategoryExtensions = new HashMap<Category, Collection<String>>();
        context.checking(new Expectations() {
            {
                one(managedList).getExtensionsPerCategory();
                will(returnValue(testCategoryExtensions));
            }
        });

        LibraryDataImpl libraryDataImpl = new LibraryDataImpl(managedList);
        Map<Category, Collection<String>> categoryExtensions = libraryDataImpl
                .getExtensionsPerCategory();
        assertEquals(testCategoryExtensions, categoryExtensions);
        context.assertIsSatisfied();
    }

    public void testRemoveFolders() {
        Mockery context = new Mockery();

        final ManagedFileList managedList = context.mock(ManagedFileList.class);

        final File folder1 = new File("folder1");
        final File folder2 = new File("folder2");
        final File folder3 = new File("folder3");
        
        
        context.checking(new Expectations() {
            {
                one(managedList).removeFolder(folder1);
                one(managedList).removeFolder(folder2);
                one(managedList).removeFolder(folder3);
            }
        });

        LibraryDataImpl libraryDataImpl = new LibraryDataImpl(managedList);
        Collection<File> foldersToRemove = Arrays.asList(folder1, folder2, folder3);
        libraryDataImpl.removeFolders(foldersToRemove);
        context.assertIsSatisfied();
    }
}
