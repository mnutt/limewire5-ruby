package com.limegroup.gnutella.tigertree;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.library.FileManager;

/**
 * A cache of HashTrees that can be serialized between session.
 * 
 * Also maintains a mapping between SHA1 URNs and their associated tree.
 */
public interface HashTreeCache {

    /**
     * If HashTree wasn't found, schedule file for hashing
     * 
     * @param fd
     *            the <tt>FileDesc</tt> for which we want to obtain the
     *            HashTree
     * @return HashTree for File
     */
    public HashTree getHashTree(FileDesc fd);

    /**
     * Retrieves the cached HashTree for this URN.
     * 
     * @param sha1
     *            the <tt>URN</tt> for which we want to obtain the HashTree
     * @return HashTree for URN
     */
    public HashTree getHashTree(URN sha1);

    /**
     * @return a TTROOT urn matching the sha1 urn
     */
    public URN getHashTreeRootForSha1(URN sha1);

    /**
     * Purges the HashTree for this URN.
     */
    public void purgeTree(URN sha1);

    /**
     * add a hashtree to the internal list if the tree depth is sufficient
     * 
     * @param sha1
     *            the SHA1- <tt>URN</tt> of a file
     * @param tree
     *            the <tt>HashTree</tt>
     */
    public void addHashTree(URN sha1, HashTree tree);

    public void addRoot(URN sha1, URN ttroot);

    /**
     * Write cache so that we only have to calculate them once.
     */
    public void persistCache(FileManager fileManager, DownloadManager downloadManager);

}