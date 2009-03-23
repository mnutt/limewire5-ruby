package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Set;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.downloader.VerifyingFile;

public class IncompleteFileListStub extends AbstractFileListStub implements IncompleteFileList {

    @Override
    public void addIncompleteFile(File incompleteFile, Set<? extends URN> urns, String name,
            long size, VerifyingFile vf) {
        // No-op.
    }

}
