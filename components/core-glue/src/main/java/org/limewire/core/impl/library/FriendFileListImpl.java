package org.limewire.core.impl.library;

import org.limewire.listener.EventListener;

import com.limegroup.gnutella.library.FileListChangedEvent;
import com.limegroup.gnutella.library.FileManager;

/**
 * Implementation of the FriendFileList interface, used to keep track of what
 * files are shared with a specific friend.
 */
class FriendFileListImpl extends AbstractFriendFileList {
    private final FileManager fileManager;

    private com.limegroup.gnutella.library.FriendFileList friendFileList;

    private final String name;

    private volatile boolean committed = false;

    private volatile EventListener<FileListChangedEvent> eventListener;

    private final CombinedShareList combinedShareList;

    FriendFileListImpl(CoreLocalFileItemFactory coreLocalFileItemFactory, FileManager fileManager,
            String name, CombinedShareList combinedShareList) {
        super(combinedShareList.createMemberList(), coreLocalFileItemFactory);
        this.fileManager = fileManager;
        this.name = name;
        this.combinedShareList = combinedShareList;
    }

    @Override
    protected com.limegroup.gnutella.library.FriendFileList getCoreFileList() {
        return friendFileList;
    }

    @Override
    void dispose() {
        super.dispose();
        if (committed) {
            combinedShareList.removeMemberList(baseList);
            if (friendFileList != null)
                friendFileList.removeFileListListener(eventListener);
        }
    }

    /**
     * Commits to using this list.
     */
    void commit() {
        committed = true;
        eventListener = newEventListener();
        friendFileList = fileManager.getOrCreateFriendFileList(name);
        friendFileList.addFileListListener(eventListener);
        combinedShareList.addMemberList(baseList);

        com.limegroup.gnutella.library.FileList fileList = friendFileList;

        fileList.getReadLock().lock();
        try {
            addAllFileDescs(fileList);
        } finally {
            fileList.getReadLock().unlock();
        }
    }
}