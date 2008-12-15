package com.limegroup.gnutella.gui.dock;

import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.gui.GuiCoreMediator;

/**
 * Creates a DockIcon instance for the target
 * platform.
 *
 */
@Singleton
public class DockIconFactoryImpl implements DockIconFactory
{
    @Inject
    public DockIconFactoryImpl () {
        
    }
    
    public DockIcon createDockIcon () {        
        if (OSUtils.isMacOSX()) {
            ServiceRegistry registry = GuiCoreMediator.getCore().getServiceRegistry();
            DockIconMacOSXImpl icon = new DockIconMacOSXImpl();
            icon.register(registry);
            registry.start("UIHack");
            return icon;
        } else
            return new DockIconNoOpImpl();
    }
}
