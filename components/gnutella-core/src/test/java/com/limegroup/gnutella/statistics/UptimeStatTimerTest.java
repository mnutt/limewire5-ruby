package com.limegroup.gnutella.statistics;

import java.util.concurrent.ScheduledExecutorService;

import junit.framework.Test;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.setting.StringArraySetting;

import com.limegroup.gnutella.ClockStub;
import com.limegroup.gnutella.Statistics;
import com.limegroup.gnutella.stubs.ScheduledExecutorServiceStub;

public class UptimeStatTimerTest extends LimeTestCase {

    private ScheduledExecutorService service;
    private ClockStub clock;
    private Statistics stats;
    private UptimeStatTimer timer;

    public UptimeStatTimerTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(UptimeStatTimerTest.class);
    }

    @Override
    public void setUp() {
        ApplicationSettings.UPTIME_HISTORY.revertToDefault();
        ApplicationSettings.DOWNTIME_HISTORY.revertToDefault();
        ApplicationSettings.LAST_SHUTDOWN_TIME.revertToDefault();
        service = new ScheduledExecutorServiceStub();
        clock = new ClockStub();
        stats = new Statistics(clock);
        timer = new UptimeStatTimer(service, stats, clock);
    }

    public void testUpdateUptimeHistory() {
        StringArraySetting uptimeHistory = ApplicationSettings.UPTIME_HISTORY;
        assertEquals(new String[0], uptimeHistory.get());

        // Test for initialization
        timer.updateUptimeHistory(10);
        assertEquals(new String[] { "10" }, uptimeHistory.get());

        // Regular update, should replace most recent value
        timer.updateUptimeHistory(20);
        assertEquals(new String[] { "20" }, uptimeHistory.get());

        // New first time update, should append
        timer = new UptimeStatTimer(service, stats, clock);
        timer.updateUptimeHistory(10);
        assertEquals(new String[] { "20", "10" }, uptimeHistory.get());

        // Regular update, should replace most recent value
        timer.updateUptimeHistory(30);
        assertEquals(new String[] { "20", "30" }, uptimeHistory.get());

        // Several new first time updates, should append and shift array
        uptimeHistory.set(new String[0]);
        for(int i = 0; i < UptimeStatTimer.HISTORY_LENGTH + 1; i++) {
            timer = new UptimeStatTimer(service, stats, clock);
            timer.updateUptimeHistory(i);
        }
        String[] history = uptimeHistory.get();
        assertEquals(UptimeStatTimer.HISTORY_LENGTH, history.length);
        for(int i = 0; i < history.length; i++) {
            // Array should be shifted by one
            assertEquals(Integer.toString(i + 1), history[i]);
        }
    }

    public void testInitializationIncrementsSessionCounter() {
        ApplicationSettings.SESSIONS.revertToDefault();
        assertEquals(0, ApplicationSettings.SESSIONS.getValue());
        timer.initialize();
        assertEquals(1, ApplicationSettings.SESSIONS.getValue());
    }

    public void testInitializationRecordsDefaultDowntime() {
        assertEquals(new String[0], ApplicationSettings.DOWNTIME_HISTORY.get());
        timer.initialize();
        String down = String.valueOf(UptimeStatTimer.DEFAULT_DOWNTIME);
        assertEquals(new String[] { down },
                ApplicationSettings.DOWNTIME_HISTORY.get());
    }

    public void testInitializationRecordsNonDefaultDowntime() {
        assertEquals(new String[0], ApplicationSettings.DOWNTIME_HISTORY.get());
        ApplicationSettings.LAST_SHUTDOWN_TIME.setValue(1000);
        clock.setNow(3000);
        timer.initialize();
        assertEquals(new String[] { "2" },
                ApplicationSettings.DOWNTIME_HISTORY.get());
    }

    public void testRecordingDowntimeAppendsIfHistoryLengthsAreEqual() {
        ApplicationSettings.DOWNTIME_HISTORY.set(new String[] { "1" });
        ApplicationSettings.UPTIME_HISTORY.set(new String[] { "3" });
        ApplicationSettings.LAST_SHUTDOWN_TIME.setValue(1000);
        clock.setNow(3000);
        timer.initialize();
        assertEquals(new String[] { "1", "2" },
                ApplicationSettings.DOWNTIME_HISTORY.get());
    }

    public void testRecordingDowntimeOverwritesIfDowntimeHistoryIsLonger() {
        ApplicationSettings.DOWNTIME_HISTORY.set(new String[] { "1" });
        ApplicationSettings.LAST_SHUTDOWN_TIME.setValue(1000);
        clock.setNow(3000);
        timer.initialize();
        assertEquals(new String[] { "2" },
                ApplicationSettings.DOWNTIME_HISTORY.get());
    }

    public void testRefreshRecordsUptimeHistory() {
        timer.initialize();
        assertEquals(new String[0], ApplicationSettings.UPTIME_HISTORY.get());
        clock.setNow(3000);
        timer.refreshStats();
        assertEquals(new String[] { "3" },
                ApplicationSettings.UPTIME_HISTORY.get());
    }

    public void testRefreshUpdatesTotalUptime() {
        ApplicationSettings.TOTAL_UPTIME.revertToDefault();
        timer.initialize();
        clock.setNow(1000);
        timer.refreshStats();
        assertEquals(1, ApplicationSettings.TOTAL_UPTIME.getValue());
        clock.setNow(3000);
        timer.refreshStats();
        assertEquals(3, ApplicationSettings.TOTAL_UPTIME.getValue());
    }

    public void testRefreshUpdatesAverageUptime() {
        ApplicationSettings.TOTAL_UPTIME.revertToDefault();
        ApplicationSettings.AVERAGE_UPTIME.revertToDefault();
        // First session: one second
        timer.initialize();
        clock.setNow(1000);
        timer.refreshStats();
        assertEquals(1, ApplicationSettings.TOTAL_UPTIME.getValue());
        assertEquals(1, ApplicationSettings.AVERAGE_UPTIME.getValue());
        // Second session: three seconds
        clock.setNow(2000);
        timer.initialize();
        clock.setNow(5000);
        timer.refreshStats();
        // Total four seconds, average two seconds
        assertEquals(4, ApplicationSettings.TOTAL_UPTIME.getValue());
        assertEquals(2, ApplicationSettings.AVERAGE_UPTIME.getValue());
    }

    public void testRefreshUpdatesFractionalUptime() {
        ApplicationSettings.FRACTIONAL_UPTIME.revertToDefault();
        ApplicationSettings.UPTIME_HISTORY.set(new String[] { "1", "1" });
        ApplicationSettings.DOWNTIME_HISTORY.set(new String[] { "2" });
        ApplicationSettings.LAST_SHUTDOWN_TIME.setValue(1000);
        clock.setNow(2000); // One second of downtime since last shutdown
        timer.initialize();
        clock.setNow(3000); // One more second of uptime
        timer.refreshStats();
        float frac = stats.calculateFractionalUptime();
        // Up = 1 + 1 + 1, down = 2 + 1, up / (up + down) = 0.5 
        assertEquals(0.5f, frac);
        assertEquals(0.5f, ApplicationSettings.FRACTIONAL_UPTIME.getValue());
    }

    public void testRefreshUpdatesLastShutdownTimeInCaseWeCrash() {
        ApplicationSettings.LAST_SHUTDOWN_TIME.setValue(12345);
        timer.initialize();
        clock.setNow(54321);
        timer.refreshStats();
        assertEquals(54321, ApplicationSettings.LAST_SHUTDOWN_TIME.getValue());
    }

    public void testRefreshIsIgnoredIfTimeGoesBackwards() {
        ApplicationSettings.TOTAL_UPTIME.revertToDefault();
        timer.initialize();
        clock.setNow(3000);
        timer.refreshStats();
        assertEquals(3, ApplicationSettings.TOTAL_UPTIME.getValue());
        clock.setNow(1000);
        timer.refreshStats();
        assertEquals(3, ApplicationSettings.TOTAL_UPTIME.getValue());
    }
}
