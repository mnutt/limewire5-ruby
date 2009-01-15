package org.limewire.ui.swing.library.sharing.model;

import org.limewire.ui.swing.library.sharing.SharingTarget;

/**
 * Must be immutable
 */
public interface LibraryShareModel {
    //TODO: refactor LibraryShareModel implementations

    boolean isShared(SharingTarget sharingTarget);

    void shareFriend(SharingTarget sharingTarget);

    void unshareFriend(SharingTarget sharingTarget);

    boolean isGnutellaNetworkSharable();

}
