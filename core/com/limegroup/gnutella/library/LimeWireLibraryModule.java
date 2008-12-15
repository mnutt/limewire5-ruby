package com.limegroup.gnutella.library;

import org.limewire.listener.EventBroadcaster;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SourcedEventMulticaster;
import org.limewire.listener.SourcedEventMulticasterImpl;
import org.limewire.listener.SourcedListenerSupport;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;


public class LimeWireLibraryModule extends AbstractModule {
    
    @Override
    protected void configure() {
        bind(LocalFileDetailsFactory.class).to(LocalFileDetailsFactoryImpl.class);
        bind(SharedFilesKeywordIndex.class).to(SharedFilesKeywordIndexImpl.class);
        bind(CreationTimeCache.class);        
        
        bind(FileManager.class).to(FileManagerImpl.class);
        bind(ManagedFileList.class).to(ManagedFileListImpl.class);
        
        EventMulticaster<ManagedListStatusEvent> managedListMulticaster =
            new EventMulticasterImpl<ManagedListStatusEvent>();
        bind(new TypeLiteral<EventBroadcaster<ManagedListStatusEvent>>(){}).toInstance(managedListMulticaster);
        bind(new TypeLiteral<ListenerSupport<ManagedListStatusEvent>>(){}).toInstance(managedListMulticaster);
        bind(new TypeLiteral<EventMulticaster<ManagedListStatusEvent>>(){}).toInstance(managedListMulticaster);
        
        SourcedEventMulticaster<FileDescChangeEvent, FileDesc> fileDescMulticaster =
            new SourcedEventMulticasterImpl<FileDescChangeEvent, FileDesc>();
        bind(new TypeLiteral<EventBroadcaster<FileDescChangeEvent>>(){}).toInstance(fileDescMulticaster);
        bind(new TypeLiteral<SourcedListenerSupport<FileDescChangeEvent, FileDesc>>(){}).toInstance(fileDescMulticaster);
        bind(new TypeLiteral<ListenerSupport<FileDescChangeEvent>>(){}).toInstance(fileDescMulticaster);
        bind(new TypeLiteral<SourcedEventMulticaster<FileDescChangeEvent, FileDesc>>(){}).toInstance(fileDescMulticaster);

        bind(FileDescFactory.class).to(FileDescFactoryImpl.class);
        
    }

}
