package org.limewire.core.impl.xmpp;

import java.util.ArrayList;
import java.util.Collection;

import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.FriendEvent;
import org.limewire.core.api.friend.FriendPresenceEvent;
import org.limewire.core.api.friend.feature.FeatureEvent;
import org.limewire.core.api.xmpp.RemoteFileItemFactory;
import org.limewire.core.api.xmpp.XMPPResourceFactory;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.xmpp.api.client.FileOfferEvent;
import org.limewire.xmpp.api.client.FriendRequestEvent;
import org.limewire.xmpp.api.client.PasswordManager;
import org.limewire.xmpp.api.client.RosterEvent;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.xmpp.api.client.XMPPService;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class MockXmppModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(XMPPService.class).to(MockXmppService.class);
        
        EventMulticaster<RosterEvent> rosterMulticaster = new EventMulticasterImpl<RosterEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<RosterEvent>>(){}).toInstance(rosterMulticaster);
        bind(new TypeLiteral<ListenerSupport<RosterEvent>>(){}).toInstance(rosterMulticaster);

        EventMulticaster<FileOfferEvent> fileOfferMulticaster = new EventMulticasterImpl<FileOfferEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);

        EventMulticaster<FriendRequestEvent> friendRequestMulticaster = new EventMulticasterImpl<FriendRequestEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);

        EventMulticaster<XMPPConnectionEvent> connectionMulticaster = new EventMulticasterImpl<XMPPConnectionEvent>();
        bind(new TypeLiteral<EventBroadcaster<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<XMPPConnectionEvent>>(){}).toInstance(connectionMulticaster);

        bind(PasswordManager.class).to(MockPasswordManager.class);
        bind(RemoteFileItemFactory.class).to(MockRemoteFileItemFactory.class);
        
        EventMulticaster<FriendEvent> knownMulticaster = new EventMulticasterImpl<FriendEvent>();
        EventMulticaster<FriendEvent> availMulticaster = new EventMulticasterImpl<FriendEvent>();
        EventMulticaster<FriendPresenceEvent> presenceMulticaster = new EventMulticasterImpl<FriendPresenceEvent>();
        EventMulticaster<FeatureEvent> featureMulticaster = new EventMulticasterImpl<FeatureEvent>();
        
        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("known")).toInstance(knownMulticaster);
        
        bind(new TypeLiteral<ListenerSupport<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendEvent>>(){}).annotatedWith(Names.named("available")).toInstance(availMulticaster);
        
        bind(new TypeLiteral<ListenerSupport<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);
        bind(new TypeLiteral<EventBroadcaster<FriendPresenceEvent>>(){}).toInstance(presenceMulticaster);
        
        bind(new TypeLiteral<ListenerSupport<FeatureEvent>>(){}).toInstance(featureMulticaster);
        bind(new TypeLiteral<EventMulticaster<FeatureEvent>>(){}).toInstance(featureMulticaster);
        
        bind(XMPPResourceFactory.class).to(MockXmppResourceFactory.class);
        
        bind(new TypeLiteral<Collection<Friend>>(){}).annotatedWith(Names.named("known")).toInstance(new ArrayList<Friend>());
    }

}
