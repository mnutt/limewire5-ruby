package com.limegroup.gnutella.library;

import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.collection.IntSet;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.listener.EventListener;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.URN;

public class SharedFilesKeywordIndexImplTest extends BaseTestCase {


    private ServiceRegistry registry;
    private SourcedEventMulticaster multicaster;
    private SharedFilesKeywordIndexImpl keywordIndex;
    private Mockery context;
    private FileView sharedFileList;
    private FileView incompleteFileList;
    private Library library;

    public SharedFilesKeywordIndexImplTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
        multicaster = context.mock(SourcedEventMulticaster.class);
        registry = context.mock(ServiceRegistry.class);
        library = context.mock(Library.class);
        sharedFileList = context.mock(FileView.class);
        incompleteFileList = context.mock(FileView.class);
        keywordIndex = new SharedFilesKeywordIndexImpl(library, null, null, null, null, null, sharedFileList, incompleteFileList);
    }
    
    @SuppressWarnings("unchecked")
    public void testRenamedFilesEvent() throws Exception {
        URN urn = URN.createSHA1Urn("urn:sha1:GLSTHIPQGSSZTS5FJUPAKPZWUGYQYPFB");
        final FileDesc originalFile = new FileDescStub("hello world", urn, 1);
        final FileDesc newFile = new FileDescStub("goodbye world", urn, 2);
        
        final GetterMatcher<Service> serviceGetter = GetterMatcher.create();
        final GetterMatcher<EventListener<FileViewChangeEvent>> listenerGetter = GetterMatcher.create();
        
        context.checking(new Expectations() {
            {
                exactly(1).of(registry).register(with(serviceGetter));                
                exactly(1).of(sharedFileList).addListener(with(listenerGetter));
                exactly(1).of(library).addManagedListStatusListener(with(any(EventListener.class)));
                exactly(1).of(incompleteFileList).addListener(with(any(EventListener.class)));
                exactly(1).of(multicaster).addListener(with(any(EventListener.class)));
                
                atLeast(1).of(incompleteFileList).contains(originalFile);
                will(returnValue(false));
                atLeast(1).of(sharedFileList).contains(originalFile);
                will(returnValue(true));
                
                atLeast(1).of(incompleteFileList).contains(originalFile);
                will(returnValue(false));
                atLeast(1).of(incompleteFileList).contains(newFile);
                will(returnValue(false));
                atLeast(1).of(sharedFileList).contains(newFile);
                will(returnValue(true));
            }
        });
        keywordIndex.register(registry, multicaster);
        serviceGetter.get().initialize();
        listenerGetter.get().handleEvent(new FileViewChangeEvent(sharedFileList, FileViewChangeEvent.Type.FILE_ADDED, originalFile));
        
        IntSet result = keywordIndex.search("world hello", null, false);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(1));
        
        result = keywordIndex.search("goodbye world", null, false);
        assertNull(result);

        listenerGetter.get().handleEvent(new FileViewChangeEvent(sharedFileList, FileViewChangeEvent.Type.FILE_CHANGED, originalFile, newFile));
        
        result = keywordIndex.search("world goodbye", null, false);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.contains(2));
        
        result = keywordIndex.search("hello world", null, false);
        assertNull(result);
    }
    
    private static class GetterMatcher<T> extends TypeSafeMatcher<T> {
        private final AtomicReference<T> ref = new AtomicReference<T>();
        
        public static <T> GetterMatcher<T> create() {
            return new GetterMatcher<T>();
        }
        
        @Override
        public void describeTo(Description description) {
            description.appendText("getter matcher");
        }
        
        @Override
        public boolean matchesSafely(T item) {
            ref.set(item);
            return true;
        }
        
        public T get() {
            return ref.get();
        }
    }

}