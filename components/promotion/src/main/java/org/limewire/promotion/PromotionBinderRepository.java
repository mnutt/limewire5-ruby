package org.limewire.promotion;

/**
 * Provides a mechanism to retrieve {@link PromotionBinder} instances, which may
 * be retrieved from the network, cached on disk, or distributed in some other
 * manner.
 */
public interface PromotionBinderRepository {

    /**
     * This is the main way to retrieve a bucket-based binder. This method may
     * hit the network, or may pull the content from cache.
     * 
     * @param bucketNumber a 63-bit number, that this factory will take a
     *        modulus of to determine the real bucket to retrieve.
     * @return A promo binder that corresponds to the given bucket number, or
     *         null if there is no matching bucket (which should be rare).
     */
    PromotionBinder getBinderForBucket(long bucketNumber);

    /**
     * Sets a remote URL to use for search.
     * 
     * @param url the new URL
     * @param mod the modulous with which to send the bucket ID
     */
    void init(String url, int mod);

}
