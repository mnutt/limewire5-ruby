package org.limewire.core.impl.inspections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.concurrent.ScheduledExecutorService;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.limewire.concurrent.SimpleTimer;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.inspection.Inspector;

/**
 * Unit tests to test:
 * 
 * - test inspections communicator scheduling inspections
 */
public class InspectionsSchedulingTest extends LimeTestCase {
    
    private Mockery context;

    public InspectionsSchedulingTest(String name) {
        super(name);
    }


    @Override
    protected void setUp() throws Exception {
        context = new Mockery();
    }
    
    // test that the Inspections Communicator performs the 
    // inspections as specified in the InspectionsSpec objects
    // (3 seconds initial delay, 5 second repeating interval)
    // Confirm by checking time stamp of inspections results.
    //
    public void testScheduledRepeatingInspections() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("sample data", true);
            will(returnValue(0));
        }});
        ScheduledExecutorService scheduler = new SimpleTimer(true);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);

        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Collections.singletonList("sample data");
        long timeToStartInsp = 3L;
        long interval = 5L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        
        long inspSchedulingTime = System.currentTimeMillis();
        ic.initInspectionSpecs(inspSpecs);
        
        Thread.sleep(15000);
        ic.stop();
        List<InspectionDataContainer> inspData = inspectionsProcessor.getInspectionResults();
        
        assertEquals(3, inspData.size());
        assertEquals(3, Math.round(((inspData.get(0).getTimestamp()-inspSchedulingTime)/1000.0)));
        assertEquals(8, Math.round(((inspData.get(1).getTimestamp()-inspSchedulingTime)/1000.0)));
        assertEquals(13, Math.round(((inspData.get(2).getTimestamp()-inspSchedulingTime)/1000.0)));
    }

    // test that the Inspections Communicator performs the 
    // inspections as specified in the InspectionsSpec objects
    // (3 seconds initial delay, no repeating, one time inspection only)
    // Confirm by checking time stamp of inspections results.
    //
    public void testScheduledNonRepeatingInspections() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("sample data", true);
            will(returnValue(0));
        }});
        ScheduledExecutorService scheduler = new SimpleTimer(true);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);    
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Collections.singletonList("sample data");
        long timeToStartInsp = 3L;
        long interval = 0L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        
        long inspSchedulingTime = System.currentTimeMillis();
        ic.initInspectionSpecs(inspSpecs);
        
        Thread.sleep(15000);
        ic.stop();
        List<InspectionDataContainer> inspRes = inspectionsProcessor.getInspectionResults();
        
        assertEquals(1, inspRes.size());
        assertEquals(3, Math.round((inspRes.get(0).getTimestamp()-inspSchedulingTime)/1000.0));       
    }
    
    // test inspections communicator with one InspectionsSpec 
    // with no inspections
    public void testNoInspectionsInOneInspectionsSpec() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        
        ScheduledExecutorService scheduler = new SimpleTimer(true);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);    
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Collections.emptyList();
        long timeToStartInsp = 3L;
        long interval = 0L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        ic.initInspectionSpecs(inspSpecs);
        
        Thread.sleep(15000);
        ic.stop();
        assertEquals(0, inspectionsProcessor.getInspectionResults().size());
    }
    
    // test inspections communicator with one InspectionsSpec 
    // with multiple inspections
    public void testMultipleInspectionsInOneInspectionsSpec() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("sample data", true);
            will(returnValue(0));
            allowing(inspector).inspect("more stuff", true);
            will(returnValue(0));
        }});
        ScheduledExecutorService scheduler = new SimpleTimer(true);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);    
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        List<String> inspections = Arrays.asList("sample data", "more stuff");
        long timeToStartInsp = 3L;
        long interval = 5L;
        inspSpecs.add(new InspectionsSpec(inspections, timeToStartInsp, interval));
        
        long inspSchedulingTime = System.currentTimeMillis();
        ic.initInspectionSpecs(inspSpecs);
        
        Thread.sleep(15000);
        ic.stop();
        
        List<InspectionDataContainer> inspData = inspectionsProcessor.getInspectionResults();
        
        assertEquals(3, inspData.size());
        assertEquals(3, Math.round((inspData.get(0).getTimestamp()-inspSchedulingTime)/1000.0));
        assertEquals(2, inspData.get(0).getResultCount());
        assertEquals(8, Math.round((inspData.get(1).getTimestamp()-inspSchedulingTime)/1000.0));
        assertEquals(2, inspData.get(1).getResultCount());
        assertEquals(13, Math.round((inspData.get(2).getTimestamp()-inspSchedulingTime)/1000.0));
        assertEquals(2, inspData.get(2).getResultCount());
    }
    
    // test inspections communicator with multiple InspectionsSpec 
    // with multiple inspections
    public void testMultipleInspectionsInMultipleInspectionsSpecs() throws Exception {
        final Inspector inspector = context.mock(Inspector.class);
        
        context.checking(new Expectations() {{
            allowing(inspector).inspect("first data", true);
            will(returnValue(0));
            allowing(inspector).inspect("three insp", true);
            will(returnValue(0));
            allowing(inspector).inspect("second data", true);
            will(returnValue(0));
            allowing(inspector).inspect("two insp", true);
            will(returnValue(0));
        }});
        ScheduledExecutorService scheduler = new SimpleTimer(true);
        InspectionsCommunicatorImpl ic = new InspectionsCommunicatorImpl(scheduler, null, null, inspector, null);
        QueuingInspectionsResultProcessor inspectionsProcessor = new QueuingInspectionsResultProcessor();
        ic.setResultProcessor(inspectionsProcessor);
        
        List<InspectionsSpec> inspSpecs = new ArrayList<InspectionsSpec>();
        inspSpecs.add(new InspectionsSpec(Arrays.asList("first data", "three insp"), 3L, 5L));
        inspSpecs.add(new InspectionsSpec(Arrays.asList("second data", "two insp"), 8L, 4L));
        
        long inspSchedulingTime = System.currentTimeMillis();
        ic.initInspectionSpecs(inspSpecs);
        Thread.sleep(15000);
        ic.stop();
        
        List<InspectionDataContainer> inspData = inspectionsProcessor.getInspectionResults();
        assertEquals(5, inspData.size());
        assertEquals(3, Math.round((inspData.get(0).getTimestamp()-inspSchedulingTime)/1000.0));
        assertEquals(8, Math.round((inspData.get(1).getTimestamp()-inspSchedulingTime)/1000.0));
        assertEquals(8, Math.round((inspData.get(2).getTimestamp()-inspSchedulingTime)/1000.0));
        assertEquals(12, Math.round((inspData.get(3).getTimestamp()-inspSchedulingTime)/1000.0));
        assertEquals(13, Math.round((inspData.get(4).getTimestamp()-inspSchedulingTime)/1000.0));
    }
    
    static class QueuingInspectionsResultProcessor implements InspectionsResultProcessor {
        final List<InspectionDataContainer> inspectionResults = new ArrayList<InspectionDataContainer>();
        
        public List<InspectionDataContainer> getInspectionResults() {
            return inspectionResults;
        }

        @Override
        public void inspectionsPerformed(InspectionDataContainer insps) throws InspectionProcessingException {
            inspectionResults.add(insps);
        }
    }
}
