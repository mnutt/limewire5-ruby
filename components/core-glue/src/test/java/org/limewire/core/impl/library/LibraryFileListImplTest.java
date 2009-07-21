package org.limewire.core.impl.library;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.core.api.library.LibraryState;
import org.limewire.listener.EventListener;
import org.limewire.util.AssignParameterAction;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.library.FileViewChangeEvent;
import com.limegroup.gnutella.library.Library;

public class LibraryFileListImplTest extends BaseTestCase {

    public LibraryFileListImplTest(String name) {
        super(name);
    }

    @SuppressWarnings("unchecked")
    public void testPropertyChangesUpdateLibraryState() {
        Mockery context = new Mockery();

        final Library managedList = context.mock(Library.class);
        final CoreLocalFileItemFactory coreLocalFileItemFactory = context
                .mock(CoreLocalFileItemFactory.class);

        final AtomicReference<EventListener<FileViewChangeEvent>> fileListListener = new AtomicReference<EventListener<FileViewChangeEvent>>();
        final AtomicReference<PropertyChangeListener> propertyChangeListener = new AtomicReference<PropertyChangeListener>();
        context.checking(new Expectations() {
            {
                one(managedList).addListener(with(any(EventListener.class)));
                will(new AssignParameterAction<EventListener<FileViewChangeEvent>>(
                        fileListListener, 0));
                one(managedList).addPropertyChangeListener(with(any(PropertyChangeListener.class)));
                will(new AssignParameterAction<PropertyChangeListener>(propertyChangeListener, 0));
            }
        });

        LibraryFileListImpl libraryFileListImpl = new LibraryFileListImpl(managedList,
                coreLocalFileItemFactory);

        assertEquals(LibraryState.LOADING, libraryFileListImpl.getState());
        propertyChangeListener.get().propertyChange(
                new PropertyChangeEvent("", "ignoreproperty", Boolean.TRUE, Boolean.FALSE));
        assertEquals(LibraryState.LOADING, libraryFileListImpl.getState());
        propertyChangeListener.get().propertyChange(
                new PropertyChangeEvent("", "hasPending", Boolean.TRUE, Boolean.FALSE));
        assertEquals(LibraryState.LOADED, libraryFileListImpl.getState());
        propertyChangeListener.get().propertyChange(
                new PropertyChangeEvent("", "hasPending", Boolean.FALSE, Boolean.TRUE));
        assertEquals(LibraryState.LOADING, libraryFileListImpl.getState());
        context.assertIsSatisfied();
    }
}
