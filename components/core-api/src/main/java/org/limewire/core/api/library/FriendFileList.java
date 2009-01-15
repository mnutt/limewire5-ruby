package org.limewire.core.api.library;

import java.beans.PropertyChangeListener;

import org.limewire.core.api.Category;

/** An extension to {@link LocalFileList} that contains methods specific for friends. */
public interface FriendFileList extends LocalFileList {
    
    /** Clears the FileList of all items in the category. */
    void clearCategory(Category category);
    
    /** Adds all the Managed Files from this category into this FileList*/
    void addSnapshotCategory(Category category);
    
    /** Returns true if files in this category will automatically be added to this list. */
    boolean isCategoryAutomaticallyAdded(Category category);
    
    /** Sets whether or not files in this category should automatically be added to this list. */
    void setCategoryAutomaticallyAdded(Category category, boolean added);
    
    void addPropertyChangeListener(PropertyChangeListener listener);
    
    void removePropertyChangeListener(PropertyChangeListener listener);
}
