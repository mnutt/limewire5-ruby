package org.limewire.ui.swing.library.sharing.model;

import java.beans.PropertyChangeListener;

import org.limewire.ui.swing.library.sharing.SharingTarget;

public interface LibraryShareModel {
    //TODO: refactor LibraryShareModel implementations

    boolean isShared(SharingTarget sharingTarget);

    void shareFriend(SharingTarget sharingTarget);

    void unshareFriend(SharingTarget sharingTarget);

    boolean isGnutellaNetworkSharable();

    void addPropertyChangeListener(PropertyChangeListener listener);

    void removePropertyChangeListener(PropertyChangeListener listener);
}
