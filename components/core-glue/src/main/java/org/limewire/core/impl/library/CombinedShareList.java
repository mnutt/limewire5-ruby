package org.limewire.core.impl.library;

import java.awt.EventQueue;
import java.util.Comparator;

import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.library.FileList;
import org.limewire.core.api.library.LocalFileItem;

import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransformedList;
import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.util.concurrent.ReadWriteLock;

/**
 * Allows creation of a composite sharing the same ListEventPublisher and
 * ReadWriteLock. Provides methods for creating member lists and adding and
 * removing them to the internal composite list.
 */
class CombinedShareList implements FileList<LocalFileItem> {
    private final CompositeList<LocalFileItem> compositeList;

    private final EventList<LocalFileItem> threadSafeUniqueList;

    private volatile TransformedList<LocalFileItem, LocalFileItem> swingList;

    /**
     * Creates a read-only composite list using the given listeEventPublisher
     * and readWriteLock. The list does not contain duplicates. Uniqueness in
     * the list of maintained by the file path of the LocalFileItem.
     */
    public CombinedShareList(ListEventPublisher listEventPublisher, ReadWriteLock readWriteLock) {
        compositeList = new CompositeList<LocalFileItem>(listEventPublisher, readWriteLock);
        threadSafeUniqueList = GlazedListsFactory.uniqueList(GlazedListsFactory
                .threadSafeList(GlazedListsFactory.readOnlyList(compositeList)),
                new Comparator<LocalFileItem>() {
                    @Override
                    public int compare(LocalFileItem o1, LocalFileItem o2) {
                        return o1.getFile().getPath().compareTo(o2.getFile().getPath());
                    }
                });
    }

    /**
     * @see ca.odell.glazedlists.CompositeList#removeMemberList(EventList)
     */
    public void removeMemberList(EventList<LocalFileItem> eventList) {
        compositeList.getReadWriteLock().writeLock().lock();
        try {
            compositeList.removeMemberList(eventList);
        } finally {
            compositeList.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * @see ca.odell.glazedlists.CompositeList#createMemberList()
     */
    public EventList<LocalFileItem> createMemberList() {
        return compositeList.createMemberList();
    }

    /**
     * @see ca.odell.glazedlists.CompositeList#addMemberList(EventList)
     */
    public void addMemberList(EventList<LocalFileItem> eventList) {
        compositeList.getReadWriteLock().writeLock().lock();
        try {
            compositeList.addMemberList(eventList);
        } finally {
            compositeList.getReadWriteLock().writeLock().unlock();
        }
    }

    @Override
    public EventList<LocalFileItem> getModel() {
        return threadSafeUniqueList;
    }

    @Override
    public EventList<LocalFileItem> getSwingModel() {
        assert EventQueue.isDispatchThread();
        if (swingList == null) {
            swingList = GlazedListsFactory.swingThreadProxyEventList(threadSafeUniqueList);
        }
        return swingList;
    }

    @Override
    public int size() {
        return threadSafeUniqueList.size();
    }
}