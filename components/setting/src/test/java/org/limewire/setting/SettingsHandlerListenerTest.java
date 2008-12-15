package org.limewire.setting;

import java.io.File;

import junit.framework.Test;

import org.limewire.setting.evt.SettingsGroupManagerEvent;
import org.limewire.setting.evt.SettingsGroupManagerListener;
import org.limewire.setting.evt.SettingsGroupManagerEvent.EventType;
import org.limewire.util.BaseTestCase;

public class SettingsHandlerListenerTest extends BaseTestCase {

    public SettingsHandlerListenerTest(String testName) {
        super(testName);
    }
    
    public static Test suite() {
        return buildTestSuite(SettingsHandlerListenerTest.class);
    }
    
    public static void main(java.lang.String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testSettingsAdded() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        SettingsGroupManager.instance().addSettingsHandlerListener(new SettingsGroupManagerListener() {
            public void handleGroupManagerEvent(SettingsGroupManagerEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            SettingsGroup group = new BasicSettingsGroup(new File("LimeWire"), "LimeWire");
            SettingsGroupManager.instance().addSettingsGroup(group);
            lock.wait(100L);
        }
        
        assertEquals(EventType.SETTINGS_GROUP_ADDED, type[0]);
    }
    
    public void testSettingsRemoved() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.reload();
        SettingsGroupManager.instance().addSettingsHandlerListener(new SettingsGroupManagerListener() {
            public void handleGroupManagerEvent(SettingsGroupManagerEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            SettingsGroupManager.instance().removeSettingsGroup(TestSettings.INSTANCE);
            lock.wait(100L);
        }
        
        assertEquals(EventType.SETTINGS_GROUP_REMOVED, type[0]);
    }
    
    public void testSave() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.reload();
        SettingsGroupManager.instance().addSettingsHandlerListener(new SettingsGroupManagerListener() {
            public void handleGroupManagerEvent(SettingsGroupManagerEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            SettingsGroupManager.instance().save();
            lock.wait(100L);
        }
        
        assertEquals(EventType.SAVE, type[0]);
    }
    
    public void testReload() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.reload();
        SettingsGroupManager.instance().addSettingsHandlerListener(new SettingsGroupManagerListener() {
            public void handleGroupManagerEvent(SettingsGroupManagerEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            SettingsGroupManager.instance().reload();
            lock.wait(100L);
        }
        
        assertEquals(EventType.RELOAD, type[0]);
    }
    
    public void testRevertToDefault() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.reload();
        SettingsGroupManager.instance().addSettingsHandlerListener(new SettingsGroupManagerListener() {
            public void handleGroupManagerEvent(SettingsGroupManagerEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            SettingsGroupManager.instance().revertToDefault();
            lock.wait(100L);
        }
        
        assertEquals(EventType.REVERT_TO_DEFAULT, type[0]);
    }
    
    public void testShouldSave() throws Exception {
        final Object lock = new Object();
        final EventType[] type = new EventType[1];
        
        TestSettings.TEST.reload();
        SettingsGroupManager.instance().addSettingsHandlerListener(new SettingsGroupManagerListener() {
            public void handleGroupManagerEvent(SettingsGroupManagerEvent evt) {
                synchronized (lock) {
                    type[0] = evt.getEventType();
                    lock.notifyAll();
                }
            }
        });
        
        synchronized (lock) {
            SettingsGroupManager.instance().setShouldSave(!TestSettings.TEST.shouldAlwaysSave());
            lock.wait(100L);
        }
        
        assertEquals(EventType.SHOULD_SAVE, type[0]);
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
