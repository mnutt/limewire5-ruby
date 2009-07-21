package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.Category;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.library.Library;

public class LibraryDataImplTest extends BaseTestCase {

    public LibraryDataImplTest(String name) {
        super(name);
    }

    public void testGetDefaultExtensions() {
        Mockery context = new Mockery();

        final Library managedList = context.mock(Library.class);

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

    public void testExtensionsPerCategory() {
        Mockery context = new Mockery();

        final Library managedList = context.mock(Library.class);

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
}
