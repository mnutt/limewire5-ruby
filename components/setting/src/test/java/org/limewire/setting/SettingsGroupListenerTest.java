package org.limewire.setting;

import java.io.File;

import junit.framework.Test;

import org.limewire.setting.evt.SettingsGroupEvent;
import org.limewire.setting.evt.SettingsGroupListener;
import org.limewire.setting.evt.SettingsGroupEvent.EventType;
import org.limewire.util.BaseTestCase;

public class SettingsGroupListenerTest extends BaseTestCase {
    
    public SettingsGroupListenerTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        return buildTestSuite(SettingsGroupListenerTest.class);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSave() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.INSTANCE.addSettingsGroupListener(new SettingsGroupListener() {
            public void settingsGroupChanged(SettingsGroupEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.INSTANCE.save();
            lock.wait(100L);
        }
        
        assertEquals(EventType.SAVE, type[0]);
    }
    
    public void testReload() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.INSTANCE.addSettingsGroupListener(new SettingsGroupListener() {
            public void settingsGroupChanged(SettingsGroupEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.INSTANCE.reload();
            lock.wait(100L);
        }
        
        assertEquals(EventType.RELOAD, type[0]);
    }
    
    public void testRevertToDefault() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.INSTANCE.addSettingsGroupListener(new SettingsGroupListener() {
            public void settingsGroupChanged(SettingsGroupEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.INSTANCE.revertToDefault();
            lock.wait(100L);
        }
        
        assertEquals(EventType.REVERT_TO_DEFAULT, type[0]);
    }
    
    public void testShouldSave() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.INSTANCE.addSettingsGroupListener(new SettingsGroupListener() {
            public void settingsGroupChanged(SettingsGroupEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.INSTANCE.setShouldSave(!TestSettings.INSTANCE.getShouldSave());
            lock.wait(100L);
        }
        
        assertEquals(EventType.SHOULD_SAVE, type[0]);
        
        type[0] = null;
        synchronized (lock) {
            TestSettings.INSTANCE.setShouldSave(TestSettings.INSTANCE.getShouldSave());
            lock.wait(100L);
        }
        assertNull(type[0]);
    }
    
    private static class TestSettings extends BasicSettingsGroup {
        
        private static final TestSettings INSTANCE = new TestSettings();
        
        public static final SettingsFactory FACTORY = INSTANCE.getFactory();
        
        public TestSettings() {
            super(new File("test.props"), "Test Settings");
        }
        
        public static final IntSetting TEST
            = FACTORY.createIntSetting("TEST", 0);
    }
}
