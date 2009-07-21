package com.limegroup.gnutella;

import org.limewire.core.settings.ApplicationSettings;

import junit.framework.Test;

public class StatisticsTest extends org.limewire.gnutella.tests.LimeTestCase { 

    private ClockStub clock;
    private Statistics stats;

    public StatisticsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(StatisticsTest.class);
    }

    @Override
    public void setUp() {
        ApplicationSettings.UPTIME_HISTORY.revertToDefault();
        ApplicationSettings.DOWNTIME_HISTORY.revertToDefault();
        clock = new ClockStub();
        stats = new Statistics(clock);
    }

    public void testClock() {
        clock.addNow(12);
        assertEquals(12, stats.getUptime());
    }

    public void testUptimeInitializedToZero() {
        assertEquals(0, stats.calculateDailyUptime());
        assertEquals(0f, stats.calculateFractionalUptime());
    }

    public void testZeroUptime() {
        ApplicationSettings.UPTIME_HISTORY.set(new String[] { "0", "0" });
        ApplicationSettings.DOWNTIME_HISTORY.set(new String[] { "123", "123" });
        assertEquals(0, stats.calculateDailyUptime());
        assertEquals(0f, stats.calculateFractionalUptime());
    }

    public void testZeroDowntime() {
        ApplicationSettings.UPTIME_HISTORY.set(new String[] { "123", "123" });
        ApplicationSettings.DOWNTIME_HISTORY.set(new String[] { "0", "0" });
        assertEquals(Statistics.SECONDS_PER_DAY, stats.calculateDailyUptime());
        assertEquals(1f, stats.calculateFractionalUptime());
    }

    public void testZeroUptimeAndDowntime() {
        ApplicationSettings.UPTIME_HISTORY.set(new String[] { "0", "0" });
        ApplicationSettings.DOWNTIME_HISTORY.set(new String[] { "0", "0" });
        assertEquals(0, stats.calculateDailyUptime());
        assertEquals(0f, stats.calculateFractionalUptime());
    }

    public void testNonZeroUptimeAndDowntime() {
        ApplicationSettings.UPTIME_HISTORY.set(new String[] { "123", "0" });
        ApplicationSettings.DOWNTIME_HISTORY.set(new String[] { "123", "123" });
        // TODO: seconds per day may not be divisible by 3 on all planets
        assertEquals(Statistics.SECONDS_PER_DAY / 3, stats.calculateDailyUptime());
        assertEquals(1/3f, stats.calculateFractionalUptime());        
    }
}
