package org.limewire.promotion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.limewire.util.ByteUtils;
import org.limewire.util.I18NConvert;
import org.limewire.util.StringUtils;

import com.google.inject.Singleton;

@Singleton
public final class KeywordUtilImpl implements KeywordUtil {
    
    private final Set<String> stopwords = new HashSet<String>();

    public KeywordUtilImpl() {
        initEnglishStopwords();
    }

    public List<String> splitKeywords(String keywords) {
        List<String> list = new ArrayList<String>();
        for (String word : keywords.split("\t"))
            list.add(normalizeQuery(word));
        return list;
    }

    private void initEnglishStopwords() {
        for (String word : new String[] { "i", "a", "s", "about", "an", "are", "as", "at", "be",
                "by", "com", "de", "en", "for", "from", "how", "in", "is", "it", "la", "of", "on",
                "or", "that", "the", "this", "to", "was", "what", "when", "where", "who", "will",
                "with", "und", "www" })
            addEnglishStopword(word);
    }

    /** Adds a word to the instance's set of stop words. */
    public void addEnglishStopword(String word) {
        stopwords.add(word);
    }

    String stripPunctuation(String query) {
        return query.replaceAll("[,.!?<>:;\\*'\"\\$\\s]", " ");
    }

    public String normalizeQuery(String query) {
        if (query == null)
            return null;
        query = stripPunctuation(query);
        query = I18NConvert.instance().getNorm(query);
        String[] queryArray = sortAlphabetically(query.split(" "));
        queryArray = stripEnglishStopWords(queryArray);
        return unsplitString(queryArray);
    }

    String unsplitString(String[] words) {
        StringBuilder builder = new StringBuilder();
        for (String word : words)
            builder.append(word).append(' ');
        return builder.substring(0, builder.length() - 1);
    }

    /**
     * @return an alphabetically sorted array of words.
     */
    String[] sortAlphabetically(String[] words) {
        class AlphaComparator implements Comparator<String> {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        }
        String[] sorted = new String[words.length];
        System.arraycopy(words, 0, sorted, 0, words.length);
        Arrays.sort(sorted, new AlphaComparator());
        return sorted;
    }

    /**
     * @return a new array with all English stop words removed, otherwise
     *         ordered the same as the original array. If the resulting array is
     *         less than 2 words long, the original array is returned (to
     *         prevent us from dropping queries like "The Who")
     */
    String[] stripEnglishStopWords(String[] words) {
        List<String> strippedWords = new ArrayList<String>();
        for (String word : words)
            if (!stopwords.contains(word))
                strippedWords.add(word);

        if (strippedWords.size() >= 2)
            return strippedWords.toArray(new String[strippedWords.size()]);
        return words;
    }

    /**
     * @return an sorted array of words, longest first, same length words sorted
     *         by alpha
     */
    String[] sortByLength(String[] words) {
        class LengthComparator implements Comparator<String> {
            public int compare(String o1, String o2) {
                if (o1.length() == o2.length())
                    return o1.compareToIgnoreCase(o2);
                return (o2.length() - o1.length());
            }
        }
        String[] sorted = new String[words.length];
        System.arraycopy(words, 0, sorted, 0, words.length);
        Arrays.sort(sorted, new LengthComparator());
        return sorted;
    }

    public long getHashValue(String query) {
        query = normalizeQuery(query);
        final String[] words = sortByLength(query.split(" "));
        query = "";
        if (words.length > 0)
            query = words[0];
        if (words.length > 1)
            query += " " + words[1];

        final byte[] sha1 = computeSHA1(query);
        final byte[] hashArray = new byte[8];
        System.arraycopy(sha1, 0, hashArray, 0, 8);
        // Make sure it's not negative (no leading bit set)
        hashArray[0] &= 127;
        return ByteUtils.beb2long(hashArray, 0, 8);
    }
  
    private byte[] computeSHA1(String input) {
        try {
            final MessageDigest outputSHA1 = MessageDigest.getInstance("SHA-1");
            final byte[] data = new byte[64 * 1024]; // 64k Chunks
            InputStream in = new ByteArrayInputStream(StringUtils.toUTF8Bytes(normalizeQuery(input)));

            while (true) {
                final int bytesRead = in.read(data);
                if (bytesRead < 0)
                    break;
                outputSHA1.update(data, 0, bytesRead);
            }
            // Done, let's compute the hash
            return outputSHA1.digest();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException("NoSuchAlgorithmException during computation: ", ex);
        } catch (IOException ex) {
            throw new RuntimeException("Impossible IOException during computation: ", ex);
        }

    }
}
