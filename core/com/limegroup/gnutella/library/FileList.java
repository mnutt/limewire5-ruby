package com.limegroup.gnutella.library;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Lock;

import org.limewire.concurrent.ListeningFuture;
import org.limewire.listener.EventListener;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A collection of FileDescs.
 */
public interface FileList extends Iterable<FileDesc> {

    /**
     * Returns the <tt>FileDesc</tt> that is wrapping this <tt>File</tt> or
     * null if the file is not shared or not a store file.
     */
    FileDesc getFileDesc(File f);

    /**
     * Returns all FileDescs that match this URN.
     */
    List<FileDesc> getFileDescsMatching(URN urn);

    /**
     * Returns the FileDesc at the given index. This returns the FileDesc for
     * which FileDesc.getIndex == index. This is supported as an optimization so
     * that classes can efficiently locate matches.
     */
    FileDesc getFileDescForIndex(int index);

    /**
     * Adds a folder to the FileList. Depending on the kind of FileList, this
     * may either add all current contents of the folder, or the folder itself
     * (allowing for future items in the folder to be added).
     * 
     * Returns a Future from which the list of all FDs that are going to be
     * added is returned.
     */
    ListeningFuture<List<ListeningFuture<FileDesc>>> addFolder(File folder);

    /**
     * Asynchronously adds a file to the list. The returned Future can be used
     * to retrieve the resulting FileDesc. If there is an error adding the file,
     * Future.get will throw an {@link ExecutionException}. If the FileDesc is
     * created but cannot be added to this list, Future.get will return null.
     */
    ListeningFuture<FileDesc> add(File file);

    /**
     * Asynchronously adds the specific file, using the given LimeXMLDocuments
     * as the default documents for that file. The returned Future can be used
     * to retrieve the resulting FileDesc. If there is an error adding the file,
     * Future.get will throw an {@link ExecutionException}. If the FileDesc is
     * created but cannot be added to this list, Future.get will also throw an
     * exception.
     */
    ListeningFuture<FileDesc> add(File file, List<? extends LimeXMLDocument> documents);

    /**
     * Removes the File from this list if there exists a FileDesc wrapper for
     * that file. If the value existed returns true, false if it did not exist
     * and nothing was removed. This method simply removes this FileDesc from
     * this list. If FileDesc is null, throws an IllegalArguementException.
     */
    boolean remove(File file);

    /**
     * Removes the FileDesc from the list if it exists. If the value existed
     * returns true, false if it did not exist and nothing was removed. This
     * method simply removes this FileDesc from this list. If FileDesc is null,
     * throws an IllegalArguementException.
     */
    boolean remove(FileDesc fileDesc);

    /** Returns true if this list contains a FileDesc for the given file. */
    boolean contains(File file);

    /**
     * Return true if this list contains this FileDesc, false otherwise.
     */
    boolean contains(FileDesc fileDesc);

    /**
     * Returns an iterator over all FileDescs. The returned iterator is *NOT*
     * thread safe. You must lock on FileList while using it.
     */
    Iterator<FileDesc> iterator();

    /**
     * Returns an iterable that is thread-safe and can be used over a period of
     * time (iterating through it piecemeal, with time lapses). The returned
     * iterable is much slower and more inefficient than the default iterator,
     * though, so only use it if absolutely necessary.
     */
    Iterable<FileDesc> pausableIterable();

    /**
     * Returns the number of files in this list.
     */
    int size();

    /**
     * Resets all values within this list.
     */
    void clear();

    /**
     * Adds a listener to this list
     */
    void addFileListListener(EventListener<FileListChangedEvent> listener);

    /**
     * Removes a listener from this list
     */
    void removeFileListListener(EventListener<FileListChangedEvent> listener);

    /** Returns a lock to use when iterating over this FileList. */
    Lock getReadLock();
}
