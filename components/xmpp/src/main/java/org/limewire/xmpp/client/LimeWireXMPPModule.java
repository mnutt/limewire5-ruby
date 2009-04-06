package org.limewire.xmpp.client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.friend.impl.LimeWireFriendXmppModule;
import org.limewire.listener.AsynchronousMulticaster;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.CachingEventMulticasterImpl;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.LogFactory;
import org.limewire.xmpp.activity.XmppActivityEvent;
import org.limewire.xmpp.api.client.ConnectBackRequestSender;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.FriendRequestEvent;
import org.limewire.xmpp.api.client.JabberSettings;
import org.limewire.xmpp.api.client.LibraryChangedEvent;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;
import org.limewire.xmpp.client.impl.ConnectionConfigurationFactory;
import org.limewire.xmpp.client.impl.DNSConnectionConfigurationFactory;
import org.limewire.xmpp.client.impl.FallbackConnectionConfigurationFactory;
import org.limewire.xmpp.client.impl.XMPPAddressRegistry;
import org.limewire.xmpp.client.impl.XMPPAddressResolver;
import org.limewire.xmpp.client.impl.XMPPAddressSerializer;
import org.limewire.xmpp.client.impl.XMPPAuthenticator;
import org.limewire.xmpp.client.impl.XMPPConnectionImplFactory;
import org.limewire.xmpp.client.impl.XMPPConnectionImpl;
import org.limewire.xmpp.client.impl.XMPPServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryProvider;

public class LimeWireXMPPModule extends AbstractModule {
    private final Class<? extends JabberSettings> jabberSettingsClass;
    
    public LimeWireXMPPModule(Class<? extends JabberSettings> jabberSettingsClass) {
        this.jabberSettingsClass = jabberSettingsClass;
    }
    
    @Override
    protected void configure() {
        install(new LimeWireFriendXmppModule());
        
        if(jabberSettingsClass != null) {
            bind(JabberSettings.class).to(jabberSettingsClass);
        }
        bind(XMPPService.class).to(XMPPServiceImpl.class);
        bind(ConnectBackRequestSender.class).to(XMPPServiceImpl.class);

        Executor executor = ExecutorsHelper.newProcessingQueue("XMPPEventThread");
        
        EventMulticaster<RosterEvent> rosterMulticaster = new AsynchronousMulticaster<RosterEvent>(executor); 
        bind(new TypeLiteral<EventBroadcaster<RosterEvent>>(){}).toInstance(rosterMulticaster);
        bind(new TypeLiteral<ListenerSupport<RosterEvent>>(){}).toInstance(rosterMulticaster);

        EventMulticaster<FileOfferEvent> fileOfferMulticaster = new EventMulticasterImpl<FileOfferEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);

        EventMulticaster<FriendRequestEvent> friendRequestMulticaster = new EventMulticasterImpl<FriendRequestEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);

        EventMulticaster<LibraryChangedEvent> libraryChangedMulticaster = new EventMulticasterImpl<LibraryChangedEvent>();
        bind(new TypeLiteral<EventBroadcaster<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);        
        
        AsynchronousMulticaster<XMPPConnectionEvent> asyncConnectionMulticaster =
            new AsynchronousMulticaster<XMPPConnectionEvent>(executor, LogFactory.getLog(XMPPConnectionEvent.class));
        CachingEventMulticasterImpl<XMPPConnectionEvent> connectionMulticaster =
            new CachingEventMulticasterImpl<XMPPConnectionEvent>(BroadcastPolicy.IF_NOT_EQUALS, asyncConnectionMulticaster, asyncConnectionMulticaster.getListenerContext());
        bind(new TypeLiteral<EventBean<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<EventMulticaster<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<EventBroadcaster<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        
        EventMulticaster<XmppActivityEvent> activityMulticaster = new CachingEventMulticasterImpl<XmppActivityEvent>(BroadcastPolicy.IF_NOT_EQUALS); 
        bind(new TypeLiteral<EventBroadcaster<XmppActivityEvent>>(){}).toInstance(activityMulticaster);
        bind(new TypeLiteral<ListenerSupport<XmppActivityEvent>>(){}).toInstance(activityMulticaster);

        List<ConnectionConfigurationFactory> connectionConfigurationFactories = new ArrayList<ConnectionConfigurationFactory>(2);
        connectionConfigurationFactories.add(new DNSConnectionConfigurationFactory());
        connectionConfigurationFactories.add(new FallbackConnectionConfigurationFactory());
        bind(new TypeLiteral<List<ConnectionConfigurationFactory>>(){}).toInstance(connectionConfigurationFactories);
        
        bind(XMPPConnectionImplFactory.class).toProvider(FactoryProvider.newFactory(XMPPConnectionImplFactory.class, XMPPConnectionImpl.class));
                
        // bind egearly, so it registers itself with SocketsManager
        bind(XMPPAddressResolver.class).asEagerSingleton();
        // dito
        bind(XMPPAddressSerializer.class).asEagerSingleton();
        
        bind(XMPPAuthenticator.class).asEagerSingleton();
        
        bind(XMPPAddressRegistry.class);
    }
}
