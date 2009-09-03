package org.limewire.core.impl.inspections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.StringUtils;
import org.limewire.io.InvalidDataException;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.inspection.Inspector;
import org.limewire.inspection.InspectionException;

/**
 * represents an inspections instruction - a response 
 * sent from the lw server telling the client:
 * 
 * Contains all inspection points to be collected 
 * after the same delay and at the same intervals, is one 
 * of a list of instructions sent from the server.
 * 
 */
public class InspectionsSpec {
    
    private static final Log LOG = LogFactory.getLog(InspectionsSpec.class);

    /**
     * inspection points specified by name.
     */
    private final List<String> inspectionPoints;

    /**
     * delay (seconds) before the inspections in this spec are first executed.
     */
    private final long startDelay;

    /**
     * Delay (seconds) between executions of the inspections in this spec
     */
    private final long interval;

    /**
     * A handle to the scheduled inspections so we can cancel
     * or reschedule. 
     */
    private ScheduledFuture scheduledInspections;

    /**
     * Creates based on parameters: inspection key list, 
     * start delay, and interval between inspections executed.
     * 
     * @param inspectionPoints inspection points specified by name.
     * @param startDelay delay (in seconds) before the inspections in this spec are first executed.
     * @param interval delay (in seconds) between executions of inspections in this spec.
     */
    InspectionsSpec(List<String> inspectionPoints, long startDelay, long interval) {
        this.inspectionPoints = new ArrayList<String>(inspectionPoints);
        this.startDelay = startDelay;
        this.interval = interval;
    }

    /**
     *  Creates given a bdecoded Object. The Object must be in
     *  the following format, or else it will
     *  throw an InvalidDataException. bdecoded must be a Map,
     *  containing "startdelay" -> int, "interval" -> int,
     *  "insp" -> list of Strings
     *
     *  Map
     *     "startdelay" -> integer
     *     "interval"   -> integer
     *     "insp"       -> list of inspection point strings
     *         point1 (String),
     *         point2 (String),
     *         point3 (String), ...
     *
     * @param bdecoded BDecoded Object
     * @throws InvalidDataException if bdecoded Object has wrong format
     * (see above for proper format)
     */
    InspectionsSpec(Object bdecoded) throws InvalidDataException {
        try {
            Map<?, ?> specMap = (Map<?, ?>) bdecoded;
            long startDelay = (Long) specMap.get("startdelay");
            long interval = (Long) specMap.get("interval");
            List<?> inspPointsEncoded = (List<?>) specMap.get("insp");
            List<String> inspectionPoints = new ArrayList<String>();
            for (Object inspPt : inspPointsEncoded) {
                inspectionPoints.add(StringUtils.getUTF8String((byte[]) inspPt));
            }
            this.inspectionPoints = new ArrayList<String>(inspectionPoints);
            this.startDelay = startDelay;
            this.interval = interval;
        } catch (ClassCastException e) {
            throw new InvalidDataException("invalid inspections specification data", e);
        }
    }
        
    Map<String, Object> asBencodedMap() {
        // "startdelay" -> integer
        // "interval"   -> integer
        // "insp"       -> list of inspection point strings
        Map<String, Object> inspectionSpecEncoded = 
            new HashMap<String, Object>(3);
        
        inspectionSpecEncoded.put("startdelay", startDelay);
        inspectionSpecEncoded.put("interval", interval);
        inspectionSpecEncoded.put("insp", inspectionPoints);
        return inspectionSpecEncoded;
    }


    long getInitialDelay() {
        return startDelay;
    }
    
    
    long getInterval() {
        return interval;    
    }
    
    List<String> getInspectionPoints() {
        return Collections.unmodifiableList(inspectionPoints);
    }
    
    private InspectionDataContainer inspect(Inspector inspector, boolean collectUsageData) {
        InspectionDataContainer inspectionResults = new InspectionDataContainer();
        
        // inspect individual inspection points
        for (String inspectionKey : inspectionPoints) {
            try {
                inspectionResults.addInspectionResult(inspectionKey,
                        inspector.inspect(inspectionKey, collectUsageData));
            } catch (InspectionException e) { 
                LOG.error("Error performing inspection", e);
                Map<String, String> errorMap = new HashMap<String, String>();
                errorMap.put("error", e.getMessage());
                inspectionResults.addInspectionResult(inspectionKey, errorMap);
            }
        }
        return inspectionResults;
    }

    // todo: consider creating with InspectionsSpecFactory, injecting in Inspector, InspectionsCommunicator, ScheduledExecutorService
    synchronized void schedule(final InspectionsResultProcessor processor, final Inspector inspector, 
                               ScheduledExecutorService scheduler) {
        ensureCancelled();
        Runnable doInspection = new InspectionsSpecProcessing(processor, inspector);
        
        if (interval == 0) {
            scheduledInspections = scheduler.schedule(doInspection, startDelay, TimeUnit.SECONDS);
        } else {
            scheduledInspections = scheduler.scheduleWithFixedDelay(doInspection, startDelay,
                    interval, TimeUnit.SECONDS);
        }
    }

    synchronized void ensureCancelled() {
        // if necessary, cancel future representing scheduled inspections
        if (scheduledInspections != null) {
            this.scheduledInspections.cancel(false);
            this.scheduledInspections = null;
        }
    }
    
    private class InspectionsSpecProcessing implements Runnable {
        
        private final Inspector inspector;
        private final InspectionsResultProcessor processor;
        
        InspectionsSpecProcessing(InspectionsResultProcessor processor, 
                                  Inspector inspector) {
            this.processor = processor;
            this.inspector = inspector;    
        }
        
        @Override
        public void run() {
            // inspect and add all finished inspections to queue
            InspectionDataContainer inspResults = inspect(inspector,
                ApplicationSettings.ALLOW_ANONYMOUS_STATISTICS_GATHERING.get());
            try {
                processor.inspectionsPerformed(inspResults);
            } catch (InspectionProcessingException e) {
                ensureCancelled();
            }
        }
    }
}
