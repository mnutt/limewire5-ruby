package org.limewire.core.impl.library;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.limewire.collection.CharSequenceKeyAnalyzer;
import org.limewire.collection.PatriciaTrie;
import org.limewire.collection.glazedlists.AbstractListEventListener;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendLibrary;
import org.limewire.core.api.library.PresenceLibrary;
import org.limewire.core.api.library.RemoteLibraryManager;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.SearchDetails;
import org.limewire.core.api.search.SearchResult;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FriendLibraries {
    private static final Log LOG = LogFactory.getLog(FriendLibraries.class);

    private final Map<String, Library> libraries;

    FriendLibraries() {
        this.libraries = new ConcurrentHashMap<String, Library>();
    }

    private static class Library {
        private final String presenceId;

        private final SearchResultTrie suggestionsIndex;

        private final SearchResultTrie fileNameIndex;

        private final Map<FilePropertyKey, SearchResultTrie> propertiesIndexes;

        private final Map<FilePropertyKey, SearchResultTrie> suggestionPropertiesIndexes;

        public Library(String presenceId) {
            this.presenceId = presenceId;
            suggestionsIndex = new SearchResultTrie();
            fileNameIndex = new SearchResultTrie();
            propertiesIndexes = new ConcurrentHashMap<FilePropertyKey, SearchResultTrie>();
            suggestionPropertiesIndexes = new ConcurrentHashMap<FilePropertyKey, SearchResultTrie>();
        }
        
        public void clear() {
            suggestionsIndex.lock.writeLock().lock();
            try {
                suggestionsIndex.clear();
            } finally {
                suggestionsIndex.lock.writeLock().unlock();
            }
            fileNameIndex.lock.writeLock().lock();
            try {
                fileNameIndex.clear();
            } finally {
                fileNameIndex.lock.writeLock().unlock();
            }

            synchronized(propertiesIndexes) {
                propertiesIndexes.clear();
            }
            synchronized(suggestionPropertiesIndexes) {
                suggestionPropertiesIndexes.clear();
            }
            
        }

        public SearchResultTrie getOrCreateFilePropertyIndex(FilePropertyKey filePropertyKey) {
            SearchResultTrie propertiesIndex = propertiesIndexes.get(filePropertyKey);
            if (propertiesIndex == null) {
                synchronized (propertiesIndexes) {
                    propertiesIndex = propertiesIndexes.get(filePropertyKey);
                    if (propertiesIndex == null) {
                        propertiesIndex = new SearchResultTrie();
                        propertiesIndexes.put(filePropertyKey, propertiesIndex);
                    }
                }
            }
            return propertiesIndex;
        }

        public SearchResultTrie getFilePropertyIndex(FilePropertyKey filePropertyKey) {
            return propertiesIndexes.get(filePropertyKey);
        }

        public SearchResultTrie getOrCreateSuggestionPropertyIndex(FilePropertyKey filePropertyKey) {
            SearchResultTrie propertiesIndex = suggestionPropertiesIndexes.get(filePropertyKey);
            if (propertiesIndex == null) {
                synchronized (suggestionPropertiesIndexes) {
                    propertiesIndex = suggestionPropertiesIndexes.get(filePropertyKey);
                    if (propertiesIndex == null) {
                        propertiesIndex = new SearchResultTrie();
                        suggestionPropertiesIndexes.put(filePropertyKey, propertiesIndex);
                    }
                }
            }
            return propertiesIndex;
        }

        public SearchResultTrie getSuggestionPropertyIndex(FilePropertyKey filePropertyKey) {
            return suggestionPropertiesIndexes.get(filePropertyKey);
        }

        public SearchResultTrie getFileNameIndex() {
            return fileNameIndex;
        }

        public SearchResultTrie getSuggestionsIndex() {
            return suggestionsIndex;
        }

        /**
         * Indexes the file name in both the suggestions and fileName indexes.
         * The suggestions index only indexes the phrase as a whole. While the
         * filename indexes the phrase by breaking it apart into all the words
         * within.
         */
        private void indexFileName(SearchResult newFile) {
            String fileName = newFile.getFileNameWithoutExtension();
            if (fileName != null) {
                LOG.debugf("adding file {0} for {1}, indexing under:", fileName, presenceId);

                getSuggestionsIndex().lock.writeLock().lock();
                try {
                    // indexes the whole file name so suggestions return the
                    // whole
                    // name back
                    getSuggestionsIndex().addWordToIndex(newFile, fileName);
                } finally {
                    getSuggestionsIndex().lock.writeLock().unlock();
                }

                getFileNameIndex().lock.writeLock().lock();
                try {
                    getFileNameIndex().addPhraseToIndex(newFile, fileName);
                } finally {
                    getFileNameIndex().lock.writeLock().unlock();
                }
            }
        }

        /**
         * Indexes properties in both the suggestions and properties indexes.
         * <p>
         * The suggestions index only indexes the phrase as a whole. While the
         * filename indexes the phrase by breaking it apart into all the words
         * within.
         */
        private void indexProperty(SearchResult newFile, FilePropertyKey filePropertyKey, String phrase) {

            SearchResultTrie filePropertyIndex = getOrCreateFilePropertyIndex(filePropertyKey);

            filePropertyIndex.lock.writeLock().lock();
            try {
                filePropertyIndex.addWordToIndex(newFile, phrase);
                filePropertyIndex.addPhraseToIndex(newFile, phrase);
            } finally {
                filePropertyIndex.lock.writeLock().unlock();
            }

            getSuggestionsIndex().lock.writeLock().lock();
            try {
                // indexes the whole string so suggestions return the whole name back
                getSuggestionsIndex().addWordToIndex(newFile, phrase);
            } finally {
                getSuggestionsIndex().lock.writeLock().unlock();
            }

            SearchResultTrie suggestionsFilePropertyIndex = getOrCreateSuggestionPropertyIndex(filePropertyKey);
            suggestionsFilePropertyIndex.lock.writeLock().lock();
            try {
                suggestionsFilePropertyIndex.addWordToIndex(newFile, phrase);
            } finally {
                suggestionsFilePropertyIndex.lock.writeLock().unlock();
            }
        }
    }

    @Inject
    void register(RemoteLibraryManager remoteLibraryManager) {
        remoteLibraryManager.getFriendLibraryList().addListEventListener(
                new ListEventListener<FriendLibrary>() {
                    @Override
                    public void listChanged(ListEvent<FriendLibrary> listChanges) {
                        while (listChanges.next()) {
                            int type = listChanges.getType();
                            if (type == ListEvent.INSERT) {
                                FriendLibrary friendLibrary = listChanges.getSourceList().get(
                                        listChanges.getIndex());
                                new AbstractListEventListener<PresenceLibrary>() {
                                    private final Map<String, LibraryListener> listeners = new HashMap<String, LibraryListener>();

                                    @Override
                                    protected void itemAdded(PresenceLibrary item, int idx,
                                            EventList<PresenceLibrary> source) {
                                        String presenceId = item.getPresence().getPresenceId();
                                        Library library = new Library(presenceId);
                                        LOG.debugf("adding library for presence {0} to index", presenceId);
                                        libraries.put(item.getPresence().getPresenceId(), library);
                                        LibraryListener listener = new LibraryListener(library);
                                        listeners.put(item.getPresence().getPresenceId(), listener);
                                        item.getModel().addListEventListener(listener);
                                    }

                                    @Override
                                    protected void itemRemoved(PresenceLibrary item, int idx,
                                            EventList<PresenceLibrary> source) {
                                        LOG.debugf("removing library for presence {0} from index",
                                                item.getPresence().getPresenceId());
                                        libraries.remove(item.getPresence().getPresenceId());
                                        LibraryListener listener = listeners.remove(item
                                                .getPresence().getPresenceId());
                                        item.getModel().removeListEventListener(listener);
                                    }

                                    @Override
                                    protected void itemUpdated(PresenceLibrary item,
                                            PresenceLibrary priorItem, int idx,
                                            EventList<PresenceLibrary> source) {
                                    }
                                }.install(friendLibrary.getPresenceLibraryList());
                            }
                        }
                    }
                });
    }

    /** Returns all suggestions for search terms based on the given prefix. */
    public Collection<String> getSuggestions(String prefix, SearchCategory category) {
        Set<String> matches = new HashSet<String>();
        for (Library library : libraries.values()) {
            library.getSuggestionsIndex().lock.readLock().lock();
            try {
                insertMatchingKeysInto(library.getSuggestionsIndex().getPrefixedBy(prefix),
                        category, matches);
            } finally {
                library.getSuggestionsIndex().lock.readLock().unlock();
            }
        }
        return matches;
    }

    public Collection<String> getSuggestions(String prefix, SearchCategory category,
            FilePropertyKey filePropertyKey) {
        Set<String> matches = new HashSet<String>();
        for (Library library : libraries.values()) {
            SearchResultTrie propertyStringTree = library
                    .getSuggestionPropertyIndex(filePropertyKey);

            if (propertyStringTree != null) {
                propertyStringTree.lock.readLock().lock();
                try {
                    insertMatchingKeysInto(propertyStringTree.getPrefixedBy(prefix), category,
                            matches);
                } finally {
                    propertyStringTree.lock.readLock().unlock();
                }
            }
        }
        return matches;
    }

    private void insertMatchingKeysInto(Map<String, Collection<SearchResult>> prefixedBy,
            SearchCategory category, Collection<String> results) {
        if (category == SearchCategory.ALL) {
            results.addAll(prefixedBy.keySet());
        } else {
            for (Map.Entry<String, Collection<SearchResult>> item : prefixedBy.entrySet()) {
                if (containsCategory(category, item.getValue())) {
                    results.add(item.getKey());
                }
            }
        }
    }

    private boolean containsCategory(SearchCategory category,
            Collection<SearchResult> searchResults) {
        for (SearchResult item : searchResults) {
            if (category == SearchCategory.forCategory(item.getCategory())) {
                return true;
            }
        }
        return false;
    }

    /** Returns all results that match the query. */
    public Collection<SearchResult> getMatchingItems(SearchDetails searchDetails) {

        Set<SearchResult> matches = standardSearch(searchDetails);
        matches = advancedSearch(searchDetails, matches);

        if (matches != null) {
            return matches;
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * Returns Set of remote file items matching the advanced SearchDetails. If
     * it is a valid search but no results match, then an empty set will be
     * returned. If the search is not valid, i.e. there is advanced search data.
     * then a null set will be returned.
     */
    private Set<SearchResult> advancedSearch(SearchDetails searchDetails,
            Set<SearchResult> matches) {
        SearchCategory category = searchDetails.getSearchCategory();
        Map<FilePropertyKey, String> advancedDetails = searchDetails.getAdvancedDetails();

        if (advancedDetails != null && advancedDetails.size() > 0) {
            for (FilePropertyKey filePropertyKey : advancedDetails.keySet()) {
                String phrase = advancedDetails.get(filePropertyKey);
                StringTokenizer st = new StringTokenizer(phrase);
                while (st.hasMoreElements()) {
                    Set<SearchResult> keywordMatches = new HashSet<SearchResult>();
                    String keyword = st.nextToken();
                    for (Library library : libraries.values()) {
                        SearchResultTrie propertyStringTrie = library
                                .getFilePropertyIndex(filePropertyKey);
                        if (propertyStringTrie != null) {
                            propertyStringTrie.lock.readLock().lock();
                            try {
                                insertMatchingItemsInto(propertyStringTrie.getPrefixedBy(keyword)
                                        .values(), category, keywordMatches, matches);
                            } finally {
                                propertyStringTrie.lock.readLock().unlock();
                            }
                        }
                    }

                    if (matches == null) {
                        matches = keywordMatches;
                    } else {
                        // Otherwise, we're looking for additional keywords
                        // -- retain only matched ones.
                        matches.retainAll(keywordMatches);
                    }

                    // Optimization: If nothing matched this keyword,
                    // nothing can be added.
                    if (matches.isEmpty()) {
                        return Collections.emptySet();
                    }
                }
            }
        }
        return matches;
    }

    /**
     * Returns Set of remote file items matching the stand SearchDetails. If it
     * is a valid search but no results match, then an empty set will be
     * returned. If the search is not valid, i.e. there is no query string. then
     * a null set will be returned.
     */
    private Set<SearchResult> standardSearch(SearchDetails searchDetails) {
        String query = searchDetails.getSearchQuery();
        SearchCategory category = searchDetails.getSearchCategory();
        Set<SearchResult> matches = null;
        StringTokenizer st = new StringTokenizer(query);
        while (st.hasMoreElements()) {
            Set<SearchResult> keywordMatches = new HashSet<SearchResult>();
            String keyword = st.nextToken();
            for (Library library : libraries.values()) {
                library.fileNameIndex.lock.readLock().lock();
                try {
                    insertMatchingItemsInto(library.fileNameIndex.getPrefixedBy(keyword).values(),
                            category, keywordMatches, matches);
                } finally {
                    library.fileNameIndex.lock.readLock().unlock();
                }

                for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                    SearchResultTrie propertyStringTrie = library
                            .getFilePropertyIndex(filePropertyKey);
                    if (propertyStringTrie != null) {
                        propertyStringTrie.lock.readLock().lock();
                        try {
                            insertMatchingItemsInto(propertyStringTrie.getPrefixedBy(keyword)
                                    .values(), category, keywordMatches, matches);
                        } finally {
                            propertyStringTrie.lock.readLock().unlock();
                        }
                    }
                }
            }

            // If this is the first keyword, just assign matches to it.
            if (matches == null) {
                matches = keywordMatches;
            } else {
                // Otherwise, we're looking for additional keywords -- retain
                // only matched ones.
                matches.retainAll(keywordMatches);
            }

            // Optimization: If nothing matched this keyword, nothing can be
            // added.
            if (matches.isEmpty()) {
                return Collections.emptySet();
            }
        }
        return matches;
    }

    private void insertMatchingItemsInto(Collection<Collection<SearchResult>> prefixedBy,
            SearchCategory category, Set<SearchResult> storage, Set<SearchResult> allowedItems) {
        for (Collection<SearchResult> searchResults : prefixedBy) {
            for (SearchResult item : searchResults) {
                Category testCategory = item.getCategory();
                boolean allowCategory = category == SearchCategory.ALL
                        || category == SearchCategory.forCategory(testCategory);
                boolean allowItem = allowedItems == null || allowedItems.contains(item);
                if (allowCategory && allowItem) {
                    storage.add(item);
                }
            }
        }
    }

    private static class SearchResultTrie extends PatriciaTrie<String, Collection<SearchResult>> {
        private final ReadWriteLock lock;

        public SearchResultTrie() {
            super(new CharSequenceKeyAnalyzer());
            lock = new ReentrantReadWriteLock();
        }

        ReadWriteLock getLock() {
            return lock;
        }

        private String canonicalize(final String s) {
            return s.toUpperCase(Locale.US).toLowerCase(Locale.US);
        }

        @Override
        public boolean containsKey(Object k) {
            return super.containsKey(canonicalize((String) k));
        }

        @Override
        public Collection<SearchResult> get(Object k) {
            return super.get(canonicalize((String) k));
        }

        @Override
        public SortedMap<String, Collection<SearchResult>> getPrefixedBy(String key, int offset, int length) {
            return super.getPrefixedBy(canonicalize(key), offset, length);
        }

        @Override
        public SortedMap<String, Collection<SearchResult>> getPrefixedBy(String key, int length) {
            return super.getPrefixedBy(canonicalize(key), length);
        }

        @Override
        public SortedMap<String, Collection<SearchResult>> getPrefixedBy(String key) {
            return super.getPrefixedBy(canonicalize(key));
        }

        @Override
        public SortedMap<String, Collection<SearchResult>> getPrefixedByBits(String key,
                int bitLength) {
            return super.getPrefixedByBits(canonicalize(key), bitLength);
        }

        @Override
        public SortedMap<String, Collection<SearchResult>> headMap(String toKey) {
            return super.headMap(canonicalize(toKey));
        }

        @Override
        public Collection<SearchResult> put(String key, Collection<SearchResult> value) {
            return super.put(canonicalize(key), value);
        }

        @Override
        public Collection<SearchResult> remove(Object k) {
            return super.remove(canonicalize((String) k));
        }

        @Override
        public Entry<String, Collection<SearchResult>> select(String key,
                Cursor<? super String, ? super Collection<SearchResult>> cursor) {
            return super.select(canonicalize(key), cursor);
        }

        @Override
        public Collection<SearchResult> select(String key) {
            return super.select(canonicalize(key));
        }

        @Override
        public SortedMap<String, Collection<SearchResult>> subMap(String fromKey, String toKey) {
            return super.subMap(canonicalize(fromKey), canonicalize(toKey));
        }

        @Override
        public SortedMap<String, Collection<SearchResult>> tailMap(String fromKey) {
            return super.tailMap(canonicalize(fromKey));
        }

        /**
         * Adds the given word to the index as a whole.
         */
        public void addWordToIndex(SearchResult newFile, String word) {
            LOG.debugf("\t {0}", word);
            Collection<SearchResult> filesForWord;
            filesForWord = get(word);
            if (filesForWord == null) {
                filesForWord = new ArrayList<SearchResult>(1);
                put(word, filesForWord);
            }
            filesForWord.add(newFile);
        }

        /**
         * Takes the given phrase and tokenizes it by spaces. Each individual
         * token gets added to the index.
         */
        public void addPhraseToIndex(SearchResult newFile, String phrase) {
            StringTokenizer st = new StringTokenizer(phrase);
            while (st.hasMoreElements()) {
                String word = st.nextToken();
                addWordToIndex(newFile, word);
            }
        }
    }

    /**
     * Listens to events on a specific presence and updates the library index
     * based on these events.
     */
    private static class LibraryListener implements ListEventListener<SearchResult> {
        private final Library library;

        LibraryListener(Library library) {
            this.library = library;
        }

        public void listChanged(ListEvent<SearchResult> listChanges) {
            // optimization:  if we know the ultimate list is size 0, clear & exit
            if(listChanges.getSourceList().size() == 0) {
                library.clear();
            } else {
                while (listChanges.next()) {
                    switch(listChanges.getType()) {
                    case ListEvent.INSERT:
                        SearchResult newFile = listChanges.getSourceList().get(listChanges.getIndex());
                        index(newFile);                    
                        break;
                    case ListEvent.DELETE:
                    case ListEvent.UPDATE:
                        // TODO: if glazedlists supported retrieving the removed items,
                        //       we could just update the one element -- instead,
                        //       we need to rebuild the whole thing.
                        rebuild(listChanges.getSourceList());
                        return;
                    }
                }
            }
        }
        
        private void rebuild(List<SearchResult> files) {
            library.clear();
            for(SearchResult item : files) {
                index(item);
            }
        }

        /**
         * Indexes the files properties and name.
         */
        private void index(SearchResult newFile) {
            library.indexFileName(newFile);

            for (FilePropertyKey filePropertyKey : FilePropertyKey.getIndexableKeys()) {
                Object property = newFile.getProperty(filePropertyKey);
                if (property != null) {
                    String sentence = property.toString();
                    library.indexProperty(newFile, filePropertyKey, sentence);
                }
            }
        }
    }
}
