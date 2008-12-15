package org.limewire.ui.swing.dock;

import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.util.OSUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Creates a DockIcon instance for the target
 * platform.
 *
 */
@Singleton
public class DockIconFactoryImpl implements DockIconFactory
{
    private final ServiceRegistry registry;
    
    @Inject
    public DockIconFactoryImpl (ServiceRegistry registry) {
        this.registry = registry;
    }
    
    public DockIcon createDockIcon () {        
        if (OSUtils.isMacOSX()) {
            DockIconMacOSXImpl icon = new DockIconMacOSXImpl();
            icon.register(registry);
            registry.start("UIHack");
            return icon;
        } else
            return new DockIconNoOpImpl();
    }
}
