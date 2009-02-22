package org.limewire.core.impl.library;

import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.ManagedFileList;

public class LibraryManagerImplTest extends BaseTestCase {

    public LibraryManagerImplTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testInstantiation() {
        Mockery context = new Mockery();

        final ManagedFileList managedList = context.mock(ManagedFileList.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);

        final AtomicReference<EventListener<FileListChangedEvent>> fileListListener = new AtomicReference<EventListener<FileListChangedEvent>>();
        final AtomicReference<PropertyChangeListener> propertyChangeListener = new AtomicReference<PropertyChangeListener>();
        context.checking(new Expectations() {
            {
                one(managedList).addFileListListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<FileListChangedEvent>>(
                        fileListListener, 0));
                one(managedList).addPropertyChangeListener(with(any(PropertyChangeListener.class)));
                will(new AssignParameterAction<PropertyChangeListener>(propertyChangeListener, 0));
            }
        });
        LibraryManagerImpl libraryManagerImpl = new LibraryManagerImpl(managedList,
                coreLocalFileItemFactory);

        LibraryData libraryData = libraryManagerImpl.getLibraryData();
        assertNotNull(libraryData);
        
        LibraryFileList libraryFileList = libraryManagerImpl.getLibraryManagedList();
        assertNotNull(libraryFileList);

        context.assertIsSatisfied();
    }

}
