package com.limegroup.gnutella.messages.vendor;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.Inflater;

import org.hamcrest.Matchers;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.inspection.Inspector;
import org.limewire.io.GGEP;
import org.limewire.io.GUID;
import org.limewire.util.CommonUtils;

import junit.framework.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.limegroup.bittorrent.bencoding.Token;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.util.FECUtils;
import com.limegroup.gnutella.util.LimeTestCase;

public class InspectionResponseFactoryImplTest extends LimeTestCase {

    public InspectionResponseFactoryImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(InspectionResponseFactoryImplTest.class);
    }
    
    private Mockery mockery;
    private Inspector inspector;
    private Injector injector;
    private FECUtils fecUtils;
    
    @Override
    public void setUp() throws Exception {
        mockery = new Mockery();
        inspector = mockery.mock(Inspector.class);
        fecUtils = mockery.mock(FECUtils.class);
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(Inspector.class).toInstance(inspector);
                bind(FECUtils.class).toInstance(fecUtils);
            }
        };
        injector = LimeTestUtils.createInjector(m);
    }
    
    public void testLoadsProperties() throws Exception {
        final File props = new File(CommonUtils.getCurrentDirectory(),"inspection.props");
        mockery.checking(new Expectations() {{
            one(inspector).load(props);
        }});
        injector.getInstance(InspectionResponseFactoryImpl.class); // loads the props
        mockery.assertIsSatisfied();
    }
    
    private Expectations createExpectations(InspectionRequest request,
            final boolean supported,
            final Object inspectedValue) throws Exception {
        return createExpectations(request, supported, inspectedValue, (new GUID()).bytes());
    }
    
    private Expectations createExpectations(final InspectionRequest request, 
            final boolean supported,
            final Object inspectedValue,
            final byte[] guid)
    throws Exception {
        return new Expectations() {{
            allowing(inspector).load(with(Matchers.any(File.class)));
            one(request).getRequestedFields();
            will(returnValue(new String[]{"asdf"}));
            one(request).requestsTimeStamp();
            will(returnValue(Boolean.TRUE));
            allowing(request).supportsEncoding();
            will(returnValue(supported));
            allowing(request).getGUID();
            will(returnValue(guid));
            one(inspector).inspect("asdf");
            will(returnValue(inspectedValue));
        }};
    }
    
    @SuppressWarnings("unchecked")
    public void testRespondsToRequest() throws Exception {
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        mockery.checking(createExpectations(request, false, "inspected"));
        InspectionResponseFactory factory = injector.getInstance(InspectionResponseFactoryImpl.class);
        InspectionResponse[] resp = factory.createResponses(request);
        mockery.assertIsSatisfied();
        assertEquals(1, resp.length);
        assertNotNull(resp[0]);
        byte [] payload = resp[0].getPayload();
        Inflater i = new Inflater();
        i.setInput(payload);
        i.finished();
        byte [] uncompressed = new byte[1024];
        i.inflate(uncompressed);
        Map<String,Object> o = (Map<String,Object>) Token.parse(uncompressed);
        assertTrue(o.containsKey("-1"));
        assertTrue(Arrays.equals("inspected".getBytes(),(byte[])o.get("0")));
    }
    
    @SuppressWarnings("unchecked")
    public void testTooSmallNotEncoded() throws Exception {
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        mockery.checking(createExpectations(request, true, "inspected"));
        InspectionResponseFactory factory = injector.getInstance(InspectionResponseFactoryImpl.class);
        InspectionResponse[] resp = factory.createResponses(request);
        mockery.assertIsSatisfied();
        assertEquals(1, resp.length);
        assertNotNull(resp[0]);
        byte [] payload = resp[0].getPayload();
        Inflater i = new Inflater();
        i.setInput(payload);
        i.finished();
        byte [] uncompressed = new byte[1024];
        i.inflate(uncompressed);
        Map<String,Object> o = (Map<String,Object>) Token.parse(uncompressed);
        assertTrue(o.containsKey("-1"));
        assertTrue(Arrays.equals("inspected".getBytes(),(byte[])o.get("0")));
    }
    
    @SuppressWarnings("unchecked")
    public void testNotSupported() throws Exception {
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        byte [] data = new byte[10000];
        Random r = new Random();
        r.nextBytes(data);
        mockery.checking(createExpectations(request, false, data));
        InspectionResponseFactory factory = injector.getInstance(InspectionResponseFactoryImpl.class);
        InspectionResponse[] resp = factory.createResponses(request);
        mockery.assertIsSatisfied();
        assertEquals(1, resp.length);
        assertNotNull(resp[0]);
        byte [] payload = resp[0].getPayload();
        Inflater i = new Inflater();
        i.setInput(payload);
        i.finished();
        byte [] uncompressed = new byte[1024 * 20];
        i.inflate(uncompressed);
        Map<String,Object> o = (Map<String,Object>) Token.parse(uncompressed);
        assertTrue(o.containsKey("-1"));
        assertTrue(Arrays.equals(data,(byte[])o.get("0")));
    }
    
    public void testEncoded() throws Exception {
        final InspectionRequest request = mockery.mock(InspectionRequest.class);
        final byte [] data = new byte[3000];
        Random r = new Random();
        r.nextBytes(data);
        byte [] guid = new byte[16];
        r.nextBytes(guid);
        mockery.checking(createExpectations(request, true, data, guid));
        
        final List<byte []> chunks= new ArrayList<byte[]> ();
        for (int i = 0; i < 5; i++) {
            byte [] b = new byte[1];
            b[0] = (byte)i;
            chunks.add(b);
        }
        
        mockery.checking(new Expectations() {{
            one(fecUtils).encode(with(Matchers.any(byte[].class)), // this is the bencoded & compressed data
                    with(Matchers.equalTo(1300)), 
                    with(Matchers.equalTo(1.2f)));
            will(returnValue(chunks));
        }});
        
        InspectionResponseFactory factory = injector.getInstance(InspectionResponseFactoryImpl.class);
        InspectionResponse[] resp = factory.createResponses(request);
        mockery.assertIsSatisfied();
        
        assertEquals(5, resp.length);
        for (int i = 0; i < resp.length; i++) {
            InspectionResponse response = resp[i];
            // make sure its definitely smaller than the fragmentation limit
            assertLessThan(1400, response.getTotalLength());
            assertEquals(2, response.getVersion());
            assertTrue(Arrays.equals(guid, response.getGUID()));
            
            GGEP g = new GGEP(response.getPayload(),0);
            assertEquals(i, g.getInt("I")); // chunk id
            assertEquals(5, g.getInt("T")); // total chunks
            assertGreaterThan(3000, g.getInt("L")); // length = 3000 bytes+ overhead
            assertLessThan(3200, g.getInt("L")); 
            byte [] chunk = g.get("D");
            assertEquals(1, chunk.length);
            assertEquals(i, chunk[0]);
        }
    }
}
