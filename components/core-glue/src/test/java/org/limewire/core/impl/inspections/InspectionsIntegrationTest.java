package org.limewire.core.impl.inspections;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.limewire.core.impl.CoreGlueModule;
import org.limewire.core.settings.InspectionsSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectablePrimitive;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.InvalidDataException;
import org.limewire.util.StringUtils;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.http.handler.NotFoundHandler;
import org.mortbay.http.handler.ResourceHandler;
import org.mortbay.util.Resource;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.limegroup.gnutella.LimeWireCoreModule;

/**
 * Real world push inspections integration test.  Brings up LW Core and
 * performs real inspections
 * 
 */
public class InspectionsIntegrationTest extends LimeTestCase {
    
    private static final String INSPECTIONS_REQUEST = "/request";
    private static final String INSPECTIONS_SUBMIT = "/submit";
    private static final String SPEC_FILENAME = "specFile";
    
    /* test inspection points */
    @InspectablePrimitive("test primitive") public static int INSPECTION_ONE;
    @InspectionPoint("test inspectable") public static Inspectable INSPECTION_TWO;
    @InspectionPoint("throwing inspectable") public static Inspectable INSPECTION_THROW;
    @InspectionPoint("null inspectable") public static Inspectable INSPECTION_NULL;

    protected Injector injector;
    private HttpServer server;
    private InspectionsCommunicatorImpl ic;

    public InspectionsIntegrationTest(String name) {
        super(name);
        server = new HttpServer();
    }


    @Override
    protected void setUp() throws Exception {
        injector = createInjector(getModules());
        initializeInspectionPoints();
        ensureInspectionsCommunicatorStopped();
        initSettings();
    }

    @Override
    public void tearDown() throws Exception {
        server.stop();
        ensureInspectionsCommunicatorStopped();
    }
    
    private void ensureInspectionsCommunicatorStopped() {
        if (ic != null) {
            ic.stop();
            ic = null;
        }    
    }
    
    private void startInspectionsCommunicator() {
        ic = (InspectionsCommunicatorImpl)injector.getInstance(InspectionsCommunicator.class);
        ic.initialize();
        ic.start();   
    }
    
    private void initSettings() {
        InspectionsSettings.INSPECTION_SPEC_REQUEST_URL.set("http://localhost:8123/request");
        InspectionsSettings.INSPECTION_SPEC_SUBMIT_URL.set("http://localhost:8123/submit");
    }

    private void initializeInspectionPoints() {
        INSPECTION_ONE = 1;
        INSPECTION_TWO = new Inspectable() {
            public int count = 0;

            @Override public Object inspect() {
                Map<String, Object> map = new HashMap<String, Object>();
                map.put("integer insp", 5);
                map.put("string insp", "test");
                map.put("count value", count++);
                return map;
            }
        };
        INSPECTION_THROW = new Inspectable() {
            @Override public Object inspect() { throw new RuntimeException("error in inspection"); }
        };
        INSPECTION_NULL = new Inspectable() {
            @Override public Object inspect() { return null; }
        };
        
    }
    
    private Injector createInjector(Module... modules) {
        return Guice.createInjector(Stage.PRODUCTION, modules);
    }
    
    private Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireCoreModule());
        modules.add(new CoreGlueModule());
        return modules.toArray(new Module[modules.size()]);
    }
    
    public void testScheduledRepeatingInspections() throws Exception {
                
        List<String> inspections = 
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE");
        long timeToStartInsp = 3L;
        long interval = 5L;
        List<InspectionsSpec> specs = Arrays.asList(new InspectionsSpec(inspections, timeToStartInsp, interval));
        
        // start web server and lw services
        ServerController serverController = startServerWithInspectionSpecs(specs);
        startInspectionsCommunicator();
        
        // wait for inspection data to arrive at web server
        //long origTime = System.currentTimeMillis();
        
        // Changing the inspected values in between scheduled inspections
        // 0-start, 3-first insp, 5-var change, 8-second insp., 10-var change, 13-third insp.
        Thread.sleep(5000);
        INSPECTION_ONE++;
        Thread.sleep(5000);
        INSPECTION_ONE++;
        Thread.sleep(5000);
        
        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(3, listInspDataEncoded.size());
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        InspectionDataContainer listInspData2 = parseInspectionData(listInspDataEncoded.get(1));
        InspectionDataContainer listInspData3 = parseInspectionData(listInspDataEncoded.get(2));
        assertEquals(1, listInspData1.getResultCount());
        assertEquals(1, listInspData2.getResultCount());
        assertEquals(1, listInspData3.getResultCount());
        
        // verify each inspection result
        // TODO make test less fragile and remove sleeps
//        assertEquals(3, Math.round(((listInspData1.getTimestamp()-origTime)/1000.0)));
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData1.getData(
                "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE"))));
        //assertEquals(8, Math.round(((listInspData2.getTimestamp()-origTime)/1000.0)));
        assertEquals(Integer.valueOf(2), Integer.valueOf(new String((byte[])listInspData2.getData(
                "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE"))));
        //assertEquals(13, Math.round(((listInspData3.getTimestamp()-origTime)/1000.0)));
        assertEquals(Integer.valueOf(3), Integer.valueOf(new String((byte[])listInspData3.getData(
                "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE"))));
    }
    
    public void testMultipleInspectionsInOneInspectionsSpec() throws Exception {
        List<String> inspections = 
            Arrays.asList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_ONE",
                          "org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_TWO");
        List<InspectionsSpec> specs = Arrays.asList(new InspectionsSpec(inspections, 5, 0));
        
        // start web server and lw services
        ServerController serverController = startServerWithInspectionSpecs(specs);
        startInspectionsCommunicator();
        
        // wait for inspection data to arrive at web server
        long origTime = System.currentTimeMillis();
        Thread.sleep(15000);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        assertEquals(1, listInspDataEncoded.size());
        InspectionDataContainer listInspData1 = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(2, listInspData1.getResultCount());
        assertEquals(5, Math.round(((listInspData1.getTimestamp()-origTime)/1000.0)));
        assertEquals(Integer.valueOf(1), Integer.valueOf(new String((byte[])listInspData1.getData(
                inspections.get(0)))));
        
        Map<?,?> map = (Map<?,?>)listInspData1.getData(inspections.get(1));
        int integerInsp = ((Long)map.get("integer insp")).intValue();
        String stringInsp = new String((byte[])map.get("string insp"));
        int countValue = ((Long)map.get("count value")).intValue();
        assertEquals(5, integerInsp);
        assertEquals("test", stringInsp);
        assertEquals(0, countValue);
    }

    
    @SuppressWarnings("unchecked")
    public void testScheduledNonExistentInspections() throws Exception {
        List<String> inspections = 
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:DOES_NOT_EXIST");
        long timeToStartInsp = 3L;
        long interval = 0;
        List<InspectionsSpec> specs = Arrays.asList(new InspectionsSpec(inspections, timeToStartInsp, interval));
        
        // start web server and lw services
        ServerController serverController = startServerWithInspectionSpecs(specs);
        startInspectionsCommunicator();       
        Thread.sleep(5000);
        
        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        
        // expecting no inspections results
        assertEquals(1, listInspDataEncoded.size());
        InspectionDataContainer inspData = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(1, inspData.getResultCount());
        Map<String, Object> inspresult = (Map<String, Object>)inspData.getData(inspections.get(0));
        assertEquals(1, inspresult.size());
        assertEquals("java.lang.NoSuchFieldException: DOES_NOT_EXIST", 
                StringUtils.toUTF8String((byte[])inspresult.get("error")));
    }

    /**
     * Test an inspection point which throws an Exception during the inspect() call.
     */
    public void testThrowingInspection() throws Exception {
        List<String> inspections = 
            Collections.singletonList("org.limewire.core.impl.inspections.InspectionsIntegrationTest:INSPECTION_THROW");
        List<InspectionsSpec> specs = Arrays.asList(new InspectionsSpec(inspections, 5, 0));
        
        // start web server and lw services
        ServerController serverController = startServerWithInspectionSpecs(specs);
        startInspectionsCommunicator();       
        Thread.sleep(15000);
        
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        
        // expecting inspection result with error
        assertEquals(1, listInspDataEncoded.size());
        InspectionDataContainer inspData = parseInspectionData(listInspDataEncoded.get(0));
        assertEquals(1, inspData.getResultCount());
        Map<?, ?> map = (Map<?, ?>)inspData.getData(inspections.get(0));
        assertEquals(1, map.size());
        assertEquals("java.lang.RuntimeException: error in inspection", 
                     StringUtils.toUTF8String((byte[])map.get("error")));
    }
    
    public void testServerSendsNoInspections() throws Exception {
        List<InspectionsSpec> specs = Collections.emptyList();
        ServerController serverController = startServerWithInspectionSpecs(specs);
        startInspectionsCommunicator();       
        Thread.sleep(15000);
        
        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        
        // expecting no inspections results
        assertEquals(0, listInspDataEncoded.size());
    }
    
    public void testServerNotUp() throws Exception {
        InspectionsCommunicatorImpl ic = 
                (InspectionsCommunicatorImpl)injector.getInstance(InspectionsCommunicator.class);
        ic.initialize();
        ic.start();       
        Thread.sleep(15000);
        
        // todo: insp: what exactly do i test for here?
    }
    
    public void testBadDataFromServer() throws Exception {
        ServerController serverController = startServerWithContent("sdukfdfgdhsdkfhskhdfsd".getBytes());
        startInspectionsCommunicator();       
        Thread.sleep(15000);
        
        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        
        // expecting no inspections results
        assertEquals(0, listInspDataEncoded.size());
    }
    
    public void testErrorFromServer() throws Exception {
        ServerController serverController = startServer(_baseDir.getAbsolutePath());
        startInspectionsCommunicator();       
        Thread.sleep(15000);
        
        // get all the bytes received by webserver
        List<byte[]> listInspDataEncoded = serverController.getReceivedInspectionData();
        
        // expecting no inspections results
        assertEquals(0, listInspDataEncoded.size());        
    }
    
    private ServerController startServerWithContent(byte[] bytes) throws Exception {
        String serverDir = _baseDir.getAbsolutePath();
        writeFile(bytes, serverDir + "/" + SPEC_FILENAME);
        return startServer(serverDir);
    }
    
    private ServerController startServerWithInspectionSpecs(List<InspectionsSpec> specs) throws Exception {
        byte[] dataForServerToReturn = InspectionsTestUtils.getGzippedAndBencoded(specs);
        return startServerWithContent(dataForServerToReturn);
    }
    
    private void writeFile(byte[] bytes, String pathToFileName) throws IOException {
        FileOutputStream os = new FileOutputStream(pathToFileName);
        os.write(bytes);
        os.close();
    }
    
    private ServerController startServer(String rootContentDir) throws Exception {
        ServerController requestHandler = new ServerController();
        SocketListener listener = new SocketListener();
        listener.setPort(8123);
        listener.setMinThreads(1);
        server.addListener(listener);

        HttpContext context = server.addContext("");

        context.setResourceBase(rootContentDir);
        requestHandler.setAcceptRanges(true);
        requestHandler.setDirAllowed(true);
         
        context.addHandler(requestHandler);
        context.addHandler(new NotFoundHandler());
        server.start();
        return requestHandler;    
    }
    
    private InspectionDataContainer parseInspectionData(byte[] data) throws InvalidDataException {
        return new InspectionDataContainer(data);
    }

    /**
     * HTTP Request handler code. May override {@link #sendInspectionSpecs}
     * method to specify behavior than just uploading the expected file.
     */
    private class ServerController extends ResourceHandler {
        
        private List<byte[]> inspectionDataEncoded = new ArrayList<byte[]>();

        @SuppressWarnings("unchecked")
        @Override
        public void handleGet(org.mortbay.http.HttpRequest httpRequest,
                              org.mortbay.http.HttpResponse httpResponse,
                              String s, java.lang.String s1,
                              org.mortbay.util.Resource resource) throws java.io.IOException {
            super.handleGet(httpRequest, httpResponse, s, s1, resource);
            Set<String> params = httpRequest.getURI().getParameterNames();
            String path = httpRequest.getURI().getPath();
            assertTrue(params.containsAll(Arrays.asList("lv", "guid", "urs")));
            if (path.equals(INSPECTIONS_REQUEST)) {
                sendInspectionSpecs(httpRequest, httpResponse);
            } else if (path.equals(INSPECTIONS_SUBMIT)) {
                // set success on response
                httpResponse.setStatus(org.mortbay.http.HttpResponse.__200_OK);
                int lengthOfData = httpRequest.getContentLength();
                byte[] b = new byte[lengthOfData];
                assertEquals(lengthOfData, httpRequest.getInputStream().read(b));
                inspectionDataEncoded.add(b);
            } else {
                fail("Invalid request: " + path);
            }
            httpResponse.commit();
            httpRequest.setHandled(true);
        }

        protected void sendInspectionSpecs(org.mortbay.http.HttpRequest httpRequest,
                                           org.mortbay.http.HttpResponse httpResponse) 
                                           throws IOException {
            // send back gzipped-bencoded list of maps
            httpResponse.setStatus(org.mortbay.http.HttpResponse.__200_OK);
            httpResponse.setContentType("binary/octet-stream");
            Resource res = getResource(SPEC_FILENAME);
            sendData(httpRequest, httpResponse, null, res, true);
            httpResponse.commit();
            httpRequest.setHandled(true);
        }
        
        List<byte []> getReceivedInspectionData() {
            return inspectionDataEncoded;
        }
    }
}
