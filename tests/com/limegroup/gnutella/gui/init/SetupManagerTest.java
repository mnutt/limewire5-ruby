package com.limegroup.gnutella.gui.init;

import org.limewire.net.FirewallService;
import org.limewire.net.LimeWireNetModule;
import org.limewire.util.OSUtils;

import com.google.inject.Injector;
import com.limegroup.gnutella.LimeTestUtils;
import com.limegroup.gnutella.gui.GUIBaseTestCase;

import junit.framework.Test;

public class SetupManagerTest extends GUIBaseTestCase {

    private SetupManager setupManager;
        
    public static Test suite() {
        return buildTestSuite(SetupManagerTest.class);
    }
    
    public SetupManagerTest(String name) {
        super(name);
    }
    
    @Override
    public void setUp() {
        Injector injector = LimeTestUtils.createInjectorAndStart(new LimeWireNetModule());
        FirewallService firewallService = injector.getInstance(FirewallService.class);
        setupManager = new SetupManager(firewallService);
    }
    
    /**
     * Tests whether the firewall window warning window should be displayed
     * 
     * NOTE: Only the non-Windows case is tested here
     * 
     */
    public void testShouldShowFirewallWindow() {        
        if (!OSUtils.isWindows()) {
            assertFalse(setupManager.shouldShowFirewallWindow());
        }
    }
    
    
}
