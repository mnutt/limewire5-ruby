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
    
    private final RareFileStrategy rareFileStrategy;
    private final SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster;
    
    @Inject
    public FileDescFactoryImpl(RareFileStrategy rareFileStrategy,
            SourcedEventMulticaster<FileDescChangeEvent, FileDesc> multicaster) {
        this.rareFileStrategy = rareFileStrategy;
        this.multicaster = multicaster;
    }

    @Override
    public FileDesc createFileDesc(File file, Set<? extends URN> urns, int index) {
        return new FileDescImpl(rareFileStrategy, multicaster, file, urns, index);
    }
    
    @Override
    public IncompleteFileDesc createIncompleteFileDesc(File file, Set<? extends URN> urns,
            int index, String completedName, long completedSize, VerifyingFile vf) {
        return new IncompleteFileDescImpl(rareFileStrategy, multicaster, file, urns,
                index, completedName, completedSize, vf);
    }

}
