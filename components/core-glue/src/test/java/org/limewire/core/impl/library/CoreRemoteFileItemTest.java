package org.limewire.core.impl.library;

import java.util.LinkedList;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.URN;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.impl.search.RemoteFileDescAdapter;
import org.limewire.util.BaseTestCase;

import com.limegroup.gnutella.RemoteFileDesc;

/**
 * Test of the mostly delegating CoreRemoteFileItem class.  Ensures
 *  methods are correctly tied and pass on the correct values. 
 */
public class CoreRemoteFileItemTest extends BaseTestCase {

    public CoreRemoteFileItemTest(String name) {
        super(name);
    }
    
    public void testGetRfd() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getRfd();
            will(returnValue(rfd));
        }});
        
        assertEquals(rfd, item.getRfd());
        
        context.assertIsSatisfied();
    }
    
    public void testGetSearchResult() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            // Nothing
        }});
        
        assertEquals(rfdAdapter, item.getSearchResult());
        
        context.assertIsSatisfied();
    }

    public void testGetName() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final String name = "salad";
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getProperty(FilePropertyKey.NAME);
            will(returnValue(name));
        }});
        
        assertEquals(name, item.getName());
        
        context.assertIsSatisfied();
    }
    
    public void testGetFileName() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final String fileName = "caesar";
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getRfd();
            will(returnValue(rfd));
            allowing(rfd).getFileName();
            will(returnValue(fileName));
        }});
        
        assertEquals(fileName, item.getFileName());
        
        context.assertIsSatisfied();
    }
    
    public void testGetSize() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final long size = 1313;
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getSize();
            will(returnValue(size));
        }});
        
        assertEquals(size, item.getSize());
        
        context.assertIsSatisfied();
    }
    
    public void testGetCreationTime() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final Long creationTime = new Long(1234);
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            one(rfdAdapter).getProperty(FilePropertyKey.DATE_CREATED);
            will(returnValue(creationTime));
            one(rfdAdapter).getProperty(FilePropertyKey.DATE_CREATED);
            will(returnValue(null));
        }});
        
        assertEquals(creationTime.longValue(), item.getCreationTime());
        assertEquals(-1, item.getCreationTime());
        
        context.assertIsSatisfied();
    }
    
    public void testGetCategory() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final Category category = Category.PROGRAM;
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getCategory();
            will(returnValue(category));
        }});
        
        assertEquals(category, item.getCategory());
        
        context.assertIsSatisfied();
    }
        
    public void testGetProperty() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final FilePropertyKey key = FilePropertyKey.WIDTH;
        final Object result = "vry wyden";
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getProperty(key);
            will(returnValue(result));
        }});
        
        assertEquals(result, item.getProperty(key));
        
        context.assertIsSatisfied();
    }
   
    public void testGetSources() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final List<RemoteHost> sources = new LinkedList<RemoteHost>();
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getSources();
            will(returnValue(sources));
        }});
        
        assertEquals(sources, item.getSources());
        
        context.assertIsSatisfied();
    }
    
    public void testGetPropertyString() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
            
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final FilePropertyKey key = FilePropertyKey.WIDTH;
        final Object result = new Long(7777777);
            
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
            
        context.checking(new Expectations() {{
            one(rfdAdapter).getProperty(key);
            will(returnValue(result));
            allowing(rfdAdapter).getProperty(key);
            will(returnValue(null));
        }});
        
        assertEquals(result.toString(), item.getPropertyString(key));
        assertNull(item.getPropertyString(key));
            
        context.assertIsSatisfied();
    }
    
    public void testToString() {
        CoreRemoteFileItem item = new CoreRemoteFileItem(null);
        
        assertNotNull(item.toString());
        assertNotEquals("", item.toString());
    }
    
    public void testGetURN() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter = context.mock(RemoteFileDescAdapter.class);
        final URN urn = context.mock(URN.class);
        
        CoreRemoteFileItem item = new CoreRemoteFileItem(rfdAdapter);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter).getUrn();
            will(returnValue(urn));
        }});
        
        assertEquals(urn, item.getUrn());
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the implementation of the Comparable interface
     */
    public void testCompareTo() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final RemoteFileDescAdapter rfdAdapter1 = context.mock(RemoteFileDescAdapter.class);
        final RemoteFileDesc rfd1 = context.mock(RemoteFileDesc.class);

        final RemoteFileDescAdapter rfdAdapter2 = context.mock(RemoteFileDescAdapter.class);
        final RemoteFileDesc rfd2 = context.mock(RemoteFileDesc.class);

        
        CoreRemoteFileItem item1 = new CoreRemoteFileItem(rfdAdapter1);
        CoreRemoteFileItem item2 = new CoreRemoteFileItem(rfdAdapter2);
        
        context.checking(new Expectations() {{
            allowing(rfdAdapter1).getRfd();
            will(returnValue(rfd1));
            allowing(rfdAdapter2).getRfd();
            will(returnValue(rfd2));
            
            // Self compare
            one(rfd1).getFileName();
            will(returnValue("a"));
            one(rfd1).getFileName();
            will(returnValue("a"));
            
            // First pair, compare is case insensitive so these should be equal
            one(rfd1).getFileName();
            will(returnValue("A"));
            one(rfd2).getFileName();
            will(returnValue("a"));
            
            // Second pair A < B : -1
            one(rfd1).getFileName();
            will(returnValue("AAAAA"));
            one(rfd2).getFileName();
            will(returnValue("BBBBB"));
            
            // Second pair B > AAAA : 1
            one(rfd1).getFileName();
            will(returnValue("B"));
            one(rfd2).getFileName();
            will(returnValue("AAAAA"));
            
            // Third pair AAAAAA > AA : positive diff
            one(rfd1).getFileName();
            will(returnValue("AAAAAA"));
            one(rfd2).getFileName();
            will(returnValue("AA"));
            
            // Third pair A > AAAAAAA : negative diff
            one(rfd1).getFileName();
            will(returnValue("a"));
            one(rfd2).getFileName();
            will(returnValue("AAAAAAA"));
        }});
        
        assertEquals(-1, item1.compareTo("aadsad"));
        assertEquals(-1, item1.compareTo(new Long(132)));
        assertEquals(0, item1.compareTo(item1));
        assertEquals(0, item1.compareTo(item2));
        assertEquals(-1, item1.compareTo(item2));
        assertEquals(1, item1.compareTo(item2));
        assertGreaterThan(0, item1.compareTo(item2));
        assertLessThan(0, item1.compareTo(item2));
        
        context.assertIsSatisfied();
    }
}
