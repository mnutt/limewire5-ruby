package org.limewire.setting;

import java.io.File;

import junit.framework.Test;

import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.setting.evt.SettingEvent.EventType;
import org.limewire.util.BaseTestCase;

public class SettingListenerTest extends BaseTestCase {
    
    public SettingListenerTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        return buildTestSuite(SettingListenerTest.class);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testValueChanged() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.addSettingListener(new SettingListener() {
            public void settingChanged(SettingEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.TEST.setValue(1);
            lock.wait(100L);
        }
        
        assertEquals(EventType.VALUE_CHANGED, type[0]);
        
        // Changing the value to the current value shouldn't
        // trigger an Event!
        type[0] = null;
        synchronized (lock) {
            TestSettings.TEST.setValue(1);
            lock.wait(100L);
        }
        assertNull(type[0]);
    }
    
    public void testReload() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.addSettingListener(new SettingListener() {
            public void settingChanged(SettingEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.TEST.reload();
            lock.wait(100L);
        }
        
        assertEquals(EventType.RELOAD, type[0]);
    }
    
    public void testRevertToDefault() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.setValue(123);
        TestSettings.TEST.addSettingListener(new SettingListener() {
            public void settingChanged(SettingEvent evt) {
                synchronized (lock) {
                    if (evt.getEventType() == EventType.REVERT_TO_DEFAULT) {
                        type[0] = evt.getEventType();
                        lock.notifyAll();
                    }
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.TEST.revertToDefault();
            lock.wait(100L);
        }
        
        assertEquals(EventType.REVERT_TO_DEFAULT, type[0]);
    }
    
    public void testPrivacyChanged() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.addSettingListener(new SettingListener() {
            public void settingChanged(SettingEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.TEST.setPrivate(!TestSettings.TEST.isPrivate());
            lock.wait(100L);
        }
        assertEquals(EventType.PRIVACY_CHANGED, type[0]);
        
        type[0] = null;
        synchronized (lock) {
            TestSettings.TEST.setPrivate(TestSettings.TEST.isPrivate());
            lock.wait(100L);
        }
        assertNull(type[0]);
    }
    
    public void testAlwaysSaveChanged() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.addSettingListener(new SettingListener() {
            public void settingChanged(SettingEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            TestSettings.TEST.setAlwaysSave(!TestSettings.TEST.shouldAlwaysSave());
            lock.wait(100L);
        }
        assertEquals(EventType.ALWAYS_SAVE_CHANGED, type[0]);
        
        type[0] = null;
        synchronized (lock) {
            TestSettings.TEST.setAlwaysSave(TestSettings.TEST.shouldAlwaysSave());
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
