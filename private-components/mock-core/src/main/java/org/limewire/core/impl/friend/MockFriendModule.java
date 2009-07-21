package org.limewire.core.impl.friend;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.api.friend.FileMetaDataConverter;
import org.limewire.friend.api.FileOfferEvent;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendConnectionFactory;
import org.limewire.friend.api.FriendEvent;
import org.limewire.friend.api.FriendManager;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.FriendRequestEvent;
import org.limewire.friend.api.LibraryChangedEvent;
import org.limewire.friend.api.PasswordManager;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.listener.AsynchronousCachingEventMulticasterImpl;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.AsynchronousEventMulticaster;
import org.limewire.listener.BroadcastPolicy;
import org.limewire.listener.EventBean;
import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.logging.LogFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class MockFriendModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(FriendManager.class).to(MockFriendManager.class);
        bind(PasswordManager.class).to(MockPasswordManager.class);
        
        EventMulticaster<FriendEvent> knownMulticaster = new EventMulticasterImpl<FriendEvent>(FriendEvent.class);
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
        bind(new TypeLiteral<EventBroadcaster<FeatureEvent>>(){}).toInstance(featureMulticaster);

        Executor executor = ExecutorsHelper.newProcessingQueue("FriendConnectionEventThread");

        AsynchronousCachingEventMulticasterImpl<FriendConnectionEvent> asyncConnectionMulticaster =
            new AsynchronousCachingEventMulticasterImpl<FriendConnectionEvent>(executor, BroadcastPolicy.IF_NOT_EQUALS, LogFactory.getLog(FriendConnectionEvent.class));
        bind(new TypeLiteral<EventBean<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<EventMulticaster<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<AsynchronousEventMulticaster<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<AsynchronousEventBroadcaster<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendConnectionEvent>>(){}).toInstance(asyncConnectionMulticaster);

        EventMulticaster<LibraryChangedEvent> libraryChangedMulticaster = new EventMulticasterImpl<LibraryChangedEvent>();
        bind(new TypeLiteral<EventBroadcaster<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        bind(new TypeLiteral<ListenerSupport<LibraryChangedEvent>>(){}).toInstance(libraryChangedMulticaster);
        
        bind(FriendConnectionFactory.class).to(MockFriendConnectionFactory.class);
        
        EventMulticaster<FileOfferEvent> fileOfferMulticaster = new EventMulticasterImpl<FileOfferEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileOfferEvent>>(){}).toInstance(fileOfferMulticaster);

        EventMulticaster<FriendRequestEvent> friendRequestMulticaster = new EventMulticasterImpl<FriendRequestEvent>(); 
        bind(new TypeLiteral<EventBroadcaster<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);
        bind(new TypeLiteral<ListenerSupport<FriendRequestEvent>>(){}).toInstance(friendRequestMulticaster);
        
        bind(FileMetaDataConverter.class).to(MockFileMetaDataConverter.class);
    }
    


    
    @Provides @Named("known") Collection<Friend> knownFriendsList() {
        return Collections.emptyList();
    }
    
    @Provides @Named("available") Collection<Friend> availableFriendsList() {
        return Collections.emptyList();
    }
    
    @Provides @Named("availableFriendIds") Set<String> availableFriendIds() {
        return Collections.emptySet();
    }
    
    @Provides @Named("available") Map<String, Friend> availableFriends() {
        return Collections.emptyMap();
    }
    
    @Provides @Named("known") Map<String, Friend> knownFriends() {
        return Collections.emptyMap();
    }
}
