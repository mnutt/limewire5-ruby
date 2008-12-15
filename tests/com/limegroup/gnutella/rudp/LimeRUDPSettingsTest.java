package com.limegroup.gnutella.rudp;

import org.limewire.core.settings.DownloadSettings;

import junit.framework.Test;

import com.limegroup.gnutella.util.LimeTestCase;

public class LimeRUDPSettingsTest extends LimeTestCase {

    public LimeRUDPSettingsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LimeRUDPSettingsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSettings() {
        LimeRUDPSettings settings = new LimeRUDPSettings();
        assertEquals(5, settings.getMaxSkipAcks()); // tests the default setting.
        DownloadSettings.MAX_SKIP_ACKS.setValue(3);
        assertEquals(3, settings.getMaxSkipAcks());
        
        assertEquals(1.3f, settings.getMaxSkipDeviation()); // tests the default setting.
        DownloadSettings.DEVIATION.setValue(1.7f);
        assertEquals(1.7f, settings.getMaxSkipDeviation());
        
        assertEquals(10, settings.getSkipAckHistorySize()); // tests the default setting.
        DownloadSettings.HISTORY_SIZE.setValue(8);
        assertEquals(8, settings.getSkipAckHistorySize());
        
        assertEquals(500, settings.getSkipAckPeriodLength()); // tests the default setting.
        DownloadSettings.PERIOD_LENGTH.setValue(250);
        assertEquals(250, settings.getSkipAckPeriodLength());
        
        assertEquals(true, settings.isSkipAcksEnabled()); // tests the default setting.
        DownloadSettings.SKIP_ACKS.setValue(false);
        assertEquals(false, settings.isSkipAcksEnabled());
    }
}
