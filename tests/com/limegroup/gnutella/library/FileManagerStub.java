package com.limegroup.gnutella.library;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileManagerStub implements FileManager {

    private ManagedFileListStub managedStub;
    private IncompleteFileListStub incompleteStub;
    private GnutellaFileListStub gnutellaStub;    
    
    @Inject
    public FileManagerStub() {    
        gnutellaStub = new GnutellaFileListStub();
        incompleteStub = new IncompleteFileListStub();
        managedStub = new ManagedFileListStub();
    }
    
    @Override
    public GnutellaFileListStub getGnutellaFileList() {
        return gnutellaStub;
    }
    
    @Override
    public IncompleteFileListStub getIncompleteFileList() {
        return incompleteStub;
    }
    
    @Override
    public ManagedFileListStub getManagedFileList() {
        return managedStub;
    }   

    
    @Override
    public FriendFileList getFriendFileList(String name) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public FriendFileList getOrCreateFriendFileList(String name) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public void removeFriendFileList(String name) {
        throw new UnsupportedOperationException("not supported");
    }
    
    @Override
    public void start() {}
    
    @Override
    public void stop() {}

    public void unloadFilesForFriend(String name) { }


}

