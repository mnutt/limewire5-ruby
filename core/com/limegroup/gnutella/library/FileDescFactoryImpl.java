package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import org.limewire.listener.SourcedEventMulticaster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;

@Singleton
class FileDescFactoryImpl implements FileDescFactory {
    
    private final RareFileDefinition rareFileDefinition;
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster;
    
    @Inject
    public FileDescFactoryImpl(RareFileDefinition rareFileDefinition,
            SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster) {
        this.rareFileDefinition = rareFileDefinition;
        this.multicaster = multicaster;
    }

    @Override
    public FileDesc createFileDesc(File file, Set<? extends URN> urns, int index) {
        return new FileDescImpl(rareFileDefinition, multicaster, file, urns, index);
    }
    
    @Override
    public IncompleteFileDesc createIncompleteFileDesc(File file, Set<? extends URN> urns,
            int index, String completedName, long completedSize, VerifyingFile vf) {
        return new IncompleteFileDescImpl(rareFileDefinition, multicaster, file, urns,
                index, completedName, completedSize, vf);
    }

}
