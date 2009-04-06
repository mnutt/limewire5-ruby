package com.limegroup.gnutella.statistics;

import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.setting.StringArraySetting;

import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;


public class UptimeStatTimerTest extends LimeTestCase {

    public UptimeStatTimerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UptimeStatTimerTest.class);
    }
    
    public void testUpdateUptimeHistory() {
        ScheduledExecutorService service = new ScheduledExecutorServiceStub();
        
        UptimeStatTimer refreshTimer = new UptimeStatTimer(service);
        StringArraySetting uptimeHistory = ApplicationSettings.LAST_N_UPTIMES;
        assertEquals(new String[0], uptimeHistory.get());
        
        // ensure history is not updated
        for (int i = 0; i < 5; i++) { 
            refreshTimer.updateUptimeHistory(i, 10, 10);
            assertEquals(new String[0], uptimeHistory.get());
        }
        
        // test for initialization
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "10" }, uptimeHistory.get());
        
        // regular update, should replace old value
        refreshTimer.updateUptimeHistory(20, 10, 2);
        assertEquals(new String[] { "20" }, uptimeHistory.get());
        
        // new first time update, should append
        refreshTimer = new UptimeStatTimer(service);
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "20", "10" }, uptimeHistory.get());
        
        // new first time update should shift array
        refreshTimer = new UptimeStatTimer(service);
        refreshTimer.updateUptimeHistory(10, 10, 2);
        assertEquals(new String[] { "10", "10" }, uptimeHistory.get());
        
        // regular update, should replace
        refreshTimer.updateUptimeHistory(30, 10, 2);
        assertEquals(new String[] { "10", "30" }, uptimeHistory.get());
        
        // go back to shorter history length
        refreshTimer = new UptimeStatTimer(service);
        refreshTimer.updateUptimeHistory(10, 10, 1);
        assertEquals(new String[] { "10" }, uptimeHistory.get());
                
        refreshTimer.updateUptimeHistory(20, 10, 1);
        assertEquals(new String[] { "20" }, uptimeHistory.get());
        
        // now with longer history, but not first interval
        refreshTimer.updateUptimeHistory(30, 10, 2);
        assertEquals(new String[] { "30" }, uptimeHistory.get());
    }

    public void testUpdateUptimeHistoryWithFirstUptimeGreaterThanInterval() {
        ScheduledExecutorService service = new ScheduledExecutorServiceStub();

        UptimeStatTimer refreshTimer = new UptimeStatTimer(service);
        StringArraySetting uptimeHistory = ApplicationSettings.LAST_N_UPTIMES;
        assertEquals(new String[0], uptimeHistory.get());

        refreshTimer.updateUptimeHistory(20, 10, 2);
        assertEquals(new String[] { "20" }, uptimeHistory.get());

        refreshTimer.updateUptimeHistory(30, 10, 2);
        assertEquals(new String[] { "30" }, uptimeHistory.get());
    }
   
}
