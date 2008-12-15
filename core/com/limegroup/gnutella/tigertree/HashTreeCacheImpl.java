package com.limegroup.gnutella.tigertree;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.collection.Tuple;
import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;
import com.limegroup.gnutella.library.IncompleteFileDesc;

/**
 * @author Gregorio Roper
 * 
 * This class maps SHA1_URNs to TigerTreeCache
 */
/* This is public for tests, but only the interface should be used. */
@Singleton
public final class HashTreeCacheImpl implements HashTreeCache {
    
    private static final Log LOG = LogFactory.getLog(HashTreeCacheImpl.class);
    
    /**
     * The ProcessingQueue to do the hashing.
     */
    private final ExecutorService QUEUE = ExecutorsHelper.newProcessingQueue("TreeHashTread");

    /**
     * A root tree that is not fully constructed yet
     */
    private final URN BUSH = URN.INVALID;
    
    /**
     * SHA1 -> TTROOT mapping
     */
    private final Map<URN, URN> ROOT_MAP;
    
    /**
     * TigerTreeCache container.
     */
    private final Map<URN, HashTree> TREE_MAP;
    
    /**
     * File where the Mapping SHA1->TTROOT is stored
     */
    private final File ROOT_CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "ttroot.cache");
    
    /**
     * File where the Mapping TTROOT->TIGERTREE is stored
     */
    private final File TREE_CACHE_FILE =
        new File(CommonUtils.getUserSettingsDir(), "ttrees.cache");
        
    /**
     * Whether or not data dirtied since the last time we saved.
     */
    private volatile boolean dirty = false;
    
    private final HashTreeFactory tigerTreeFactory;
    
    @Inject
    HashTreeCacheImpl(HashTreeFactory tigerTreeFactory) {
        this.tigerTreeFactory = tigerTreeFactory;
        Tuple<Map<URN, URN>, Map<URN, HashTree>> tuple = loadCaches();
        ROOT_MAP = tuple.getFirst();
        TREE_MAP = tuple.getSecond();
    }

    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#getHashTreeAndWait(com.limegroup.gnutella.FileDesc, long)
     */
    public HashTree getHashTreeAndWait(FileDesc fd, long timeout) throws InterruptedException, TimeoutException {
        if (fd instanceof IncompleteFileDesc) {
            throw new IllegalArgumentException("fd must not inherit from IncompleFileDesc");
        }
        
        synchronized (this) {
            URN root = ROOT_MAP.get(fd.getSHA1Urn());
            if (root != null && root != BUSH)
                return TREE_MAP.get(root);

            ROOT_MAP.put(fd.getSHA1Urn(), BUSH);
        }
        
        Future<?> future = QUEUE.submit(new HashRunner(fd));
        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        
        synchronized (this) {
            URN root = ROOT_MAP.get(fd.getSHA1Urn());
            if (root != null && root != BUSH)
                return TREE_MAP.get(root);
        }
        
        throw new RuntimeException("hash tree was not calculated");
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#getHashTree(com.limegroup.gnutella.FileDesc)
     */
    public synchronized HashTree getHashTree(FileDesc fd) {
        
        URN root = ROOT_MAP.get(fd.getSHA1Urn());
        if (root == null || root == BUSH)
            return null;
        HashTree tree = TREE_MAP.get(root);
        if (tree == null) {
            ROOT_MAP.put(fd.getSHA1Urn(), BUSH);
            QUEUE.execute(new HashRunner(fd));
        }
        return tree;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#getHashTree(com.limegroup.gnutella.URN)
     */
    public synchronized HashTree getHashTree(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        URN root = ROOT_MAP.get(sha1);
        if (root == null || root == BUSH)
            return null;
        
        return TREE_MAP.get(root);
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#getTigerTreeRootForSha1(com.limegroup.gnutella.URN)
     */
    public synchronized URN getHashTreeRootForSha1(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        URN root = ROOT_MAP.get(sha1);
        if (root == BUSH)
            root = null;
        return root;
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#purgeTree(com.limegroup.gnutella.URN)
     */
    public synchronized void purgeTree(URN sha1) {
        if (!sha1.isSHA1())
            throw new IllegalArgumentException();
        URN root = ROOT_MAP.remove(sha1);
        if (root == null)
            return;
        if(TREE_MAP.remove(root) != null)
            dirty = true;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#addHashTree(com.limegroup.gnutella.URN, com.limegroup.gnutella.tigertree.TigerTree)
     */
    public synchronized void addHashTree(URN sha1, HashTree tree) {
        URN root = tree.getTreeRootUrn();
        addRoot(sha1, root);
        if (tree.isGoodDepth()) {
            ROOT_MAP.put(sha1, root);
            TREE_MAP.put(root, tree);
            dirty = true;
            if (LOG.isDebugEnabled())
                LOG.debug("added hashtree for urn " +
                          sha1 + ";" + tree.getRootHash());
        } else if (LOG.isDebugEnabled())
            LOG.debug("hashtree for urn " + sha1 + " had bad depth");
    }
    
    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#addRoot(com.limegroup.gnutella.URN, com.limegroup.gnutella.URN)
     */
    public synchronized void addRoot(URN sha1, URN ttroot) {
        if (!sha1.isSHA1() || !ttroot.isTTRoot())
            throw new IllegalArgumentException();
        dirty |= !ttroot.equals(ROOT_MAP.put(sha1,ttroot));
    }
    
    /**
     * Loads values from the root and tree caches
     */
    private Tuple<Map<URN, URN>, Map<URN, HashTree>> loadCaches() {
        Object roots;
        Object trees;
        try {
            roots = ROOT_CACHE_FILE.exists() ? FileUtils.readObject(ROOT_CACHE_FILE) : new HashMap();
            trees = TREE_CACHE_FILE.exists() ? FileUtils.readObject(TREE_CACHE_FILE) : new HashMap();
        } catch (Throwable t) {
            LOG.debug("Error reading from disk.", t);
            roots = new HashMap();
            trees = new HashMap();
        }

        Map<URN,URN> rootsMap = GenericsUtils.scanForMap(roots, URN.class, URN.class, GenericsUtils.ScanMode.REMOVE);
        Map<URN,HashTree> treesMap = GenericsUtils.scanForMap(trees, URN.class, HashTree.class, GenericsUtils.ScanMode.REMOVE);
        
        // remove roots that don't have a sha1 mapping for them
        // cause we can't use them.
        treesMap.keySet().retainAll(rootsMap.values());
        
        // and make sure urns are the correct type
        for (Iterator<Map.Entry<URN, URN>> iter = rootsMap.entrySet().iterator();iter.hasNext();) {
            Map.Entry<URN, URN> e = iter.next();
            if (!e.getKey().isSHA1() || !e.getValue().isTTRoot())
                iter.remove();
        }
        
        for (Iterator<URN> iter = treesMap.keySet().iterator(); iter.hasNext();) {
            URN urn = iter.next();
            if (!urn.isTTRoot())
                iter.remove();
        }
        
        // Note: its ok to have roots without trees.
        return new Tuple<Map<URN,URN>, Map<URN,HashTree>>(rootsMap, treesMap);
    }

    /**
     * Removes any stale entries from the map so that they will automatically
     * be replaced.
     * 
     * @param map
     *            the <tt>Map</tt> to check
     */
    private void removeOldEntries(Map<URN,URN> roots, Map <URN, HashTree> map, FileManager fileManager, DownloadManager downloadManager) {
        // discard outdated info
        Iterator<URN> iter = roots.keySet().iterator();
        while (iter.hasNext()) {
            URN sha1 = iter.next();
            if (roots.get(sha1) != BUSH) {
                if (!fileManager.getManagedFileList().getFileDescsMatching(sha1).isEmpty())
                    continue;
                else if (downloadManager.getIncompleteFileManager().getFileForUrn(sha1) != null)
                    continue;
                else if (Math.random() > map.size() / 200)
                    // lazily removing entries if we don't have
                    // that many anyway. Maybe some of the files are
                    // just temporarily unshared.
                    continue;
            }
            iter.remove();
            map.remove(roots.get(sha1));
            dirty = true;
        }
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.tigertree.HashTreeCache#persistCache(com.limegroup.gnutella.FileManager, com.limegroup.gnutella.DownloadManager)
     */
    public void persistCache(FileManager fileManager, DownloadManager downloadManager) {
        if(!dirty)
            return;
        
        Map<URN,URN> roots;
        Map<URN, HashTree> trees;
        synchronized(this) {
            trees = new HashMap<URN,HashTree>(TREE_MAP);
            roots = new HashMap<URN,URN>(ROOT_MAP);
        }
        
        removeOldEntries(roots, trees, fileManager, downloadManager);
        
        synchronized(this) {
            TREE_MAP.clear();
            TREE_MAP.putAll(trees);
            ROOT_MAP.clear();
            ROOT_MAP.putAll(roots);
        }
        
        try {
            FileUtils.writeObject(ROOT_CACHE_FILE, roots);
            FileUtils.writeObject(TREE_CACHE_FILE, trees);
            dirty = false;
        } catch (IOException e) {} 
        // this may any roots added while writing to get lost
    }

    /**
     * Simple runnable that processes the hash of a FileDesc.
     */
    private class HashRunner implements Runnable {
        
        private final FileDesc FD;

        HashRunner(FileDesc fd) {
            FD = fd;
        }

        public void run() {
            try {
                URN sha1 = FD.getSHA1Urn();
                // if it was scheduled multiple times, ignore latter times.
                if (HashTreeCacheImpl.this.getHashTree(sha1) == null) {
                    HashTree tree = tigerTreeFactory.createHashTree(FD);
                    addHashTree(sha1, tree);
                }
            } catch(IOException ignored) {}
        }
    }
}
