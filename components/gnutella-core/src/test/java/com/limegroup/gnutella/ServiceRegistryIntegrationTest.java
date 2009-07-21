package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.lifecycle.Service;
import org.limewire.lifecycle.ServiceRegistry;
import org.limewire.lifecycle.ServiceRegistryListener;
import org.limewire.lifecycle.ServiceStage;
import org.limewire.lifecycle.StagedRegisterBuilder;

import com.google.inject.AbstractModule;
import com.google.inject.Stage;

public class ServiceRegistryIntegrationTest extends LimeTestCase {
    
    public ServiceRegistryIntegrationTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ServiceRegistryIntegrationTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testServicesAdded() {
        final FakeRegistry registry = new FakeRegistry();
        LimeTestUtils.createInjector(Stage.PRODUCTION, new AbstractModule() {
            @Override
            protected void configure() {
                bind(ServiceRegistry.class).toInstance(registry);
            }
        });
        
        
        // Add/remove as services change
        String[] names = new String[] { "Network Management", "NetworkManagerImpl", 
        "Update Checks", "UpdateHandlerImpl", 
        "Download Management", "DownloadManagerImpl", 
        "Old Downloads", "", 
        "Browse Host Handler", "BrowseHostHandlerManagerImpl", 
        "Statistic Management", "StatisticsAccumulatorImpl", 
        "Upload Management", "HTTPUploadManager", 
        "Promotion System", "PromotionServicesImpl", 
        "Shared Files", "FileManagerImpl", 
        "Ultrapeer/DHT Management", "NodeAssignerImpl", 
        "Connection Listener", "AcceptorImpl", 
        "UPnP", "", 
        "Directed Querier", "QueryUnicaster", 
        "Connection Management", "ConnectionManagerImpl", 
        "Content Management", "ContentManager", 
        "Mojito DHT", "DHTManagerImpl", 
        "Peer Locator", "HostCatcher", 
        "Static Messages", "StaticMessages", 
        "Message Routing", "StandardMessageRouter", 
        "Core Glue", "LimeCoreGlue", 
        "RUDP Message Routing", "LimeRUDPMessageHandler", 
        "Peer Listener", "Pinger", 
        "HTTP Request Listening", "HTTPAcceptor", 
        "Magnet Processor", "LocalHTTPAcceptor", 
        "Connection Dispatching", "", 
        "OOB Throughput Measurer", "OutOfBandThroughputMeasurer", 
        "Stale Connection Management", "ConnectionWatchdog", 
        "Spam Management", "RatingTable",  
        "Download Upgrade Task", "", 
        "LimeWire Store Integration", "LWSIntegrationServicesImpl", 
        "Local Socket Listener", "LocalAcceptor", 
        "Various Core Services", "",
        "QRP Updater", "QRPUpdater",
        "Gnutella Connections", "",
        "Firewall Manager", "FirewallServiceImpl",
        "DAAP", "",
        "Metadata Loader", "",
        "What's New Manager", "",
        "P2P Network Keyword Library", "",
        "Uptime Statistics", "UptimeStatTimer", 
        "PushEndpointCacheImpl WeakHashMap Cleaner", "ScheduledService",
        "OnDemandUnicaster.Expirer", "ScheduledService",
        "OnDemandUnicaster.QueriedHostsExpirer", "ScheduledService",
        "ForMeReplyHandler.Clear Push Requests", "ScheduledService",
        "DiskContrller.CacheCleaner", "ScheduledService",
        "urncache persister", "ScheduledService",
        "TorrentManager", "LazyTorrentManager",
        "Settings Saver", "SettingsSaverService",
        
        };
        
        List<String> missing = new ArrayList<String>();
        
        boolean found;
        for(int i = 0; i < names.length; i+=2 ) {
            found = false;
            for(Iterator<Service> iterator = registry.services.iterator(); iterator.hasNext(); ) {
                Service service = iterator.next();
                if(service.getServiceName().equals(names[i]) && service.getClass().getSimpleName().equals(names[i+1])) {
                    found = true;
                    iterator.remove();
                    break;
                }
            }
            if(!found)
                missing.add("[" + names[i] + "/" + names[i+1] + "]");
        }
        
        if(!missing.isEmpty() || !registry.services.isEmpty()) {
            fail("couldn't find: " + missing + ", and had extra: " + toNames(registry.services));
        }
    }
    
    private String toNames(List<Service> services) {
        List<String> names = new ArrayList<String>();
        for(Service service : services) 
            names.add("[" + service.getServiceName() + "/" + service.getClass().getSimpleName() + "]");
        return names.toString();
    }
    
    private static class FakeRegistry implements ServiceRegistry {
        private final List<Service> services = new ArrayList<Service>();
        
        public void addListener(ServiceRegistryListener serviceRegistryListener) {
        }
        public void initialize() {
        }
        public StagedRegisterBuilder register(Service service) {
            services.add(service);
            return new StagedRegisterBuilder() {
                public void in(Object customStage) {
                }
                public void in(ServiceStage stage) {
                }
            };
        }
        public void start() {
        }
        public void start(Object stage) {
        }
        public void stop() {
        }
    }
    
}
