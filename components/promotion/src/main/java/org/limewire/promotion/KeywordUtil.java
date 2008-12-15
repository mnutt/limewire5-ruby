package org.limewire.promotion;

import java.util.List;

/**
 * Provides methods for massaging keywords and keyword sets, hashing keywords
 * into buckets, etc.
 */
public interface KeywordUtil {
    /**
     * @return a normalized query, taking the following steps:
     *         <li>normalize accented Latin characters into all lowercase, all
     *         Latin, and leave non-Latin characters alone.
     *         <li>drop all punctuation (.,'":; etc) and replace it with spaces
     *         <li>drop stop words (currently just English), but only if the
     *         query would have at least two words remaining after dropping
     *         these words
     *         <li>sort words alphabetically
     */
    String normalizeQuery(String query);

    /**
     * @return a long value which is a strongly random 63-bit hash (always a
     *         positive #) of the first two words of a query, after it has
     *         normalized the query using {@link #normalizeQuery(String)}.
     */
    long getHashValue(String query);

    /**
     * Takes as an input the String returned from
     * {@link org.limewire.promotion.containers.PromotionMessageContainer#getKeywords()}
     * and splits it on the tab character, then normalizes each phrase and
     * returns the normalized list.
     */
    List<String> splitKeywords(String keywords);
}
