package com.limegroup.gnutella.spam;

import java.util.Locale;

import org.limewire.core.settings.SearchSettings;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

@Singleton
public class SpamManager {

    /**
     * Initial rating for a file that appears from its name to be incomplete.
     */
    private static final float INCOMPLETE_FILE_RATING = 0.8f;

    private final RatingTable ratingTable;

    @Inject
    SpamManager(RatingTable ratingTable) {
        this.ratingTable = ratingTable;
    }

    // For testing
    protected RatingTable getRatingTable() {
        return ratingTable;
    }

    /**
     * Clears bad ratings for the keywords in a query started by the user.
     * 
     * @param qr the QueryRequest started by the user
     */
    public void startedQuery(QueryRequest qr) {
        if (SearchSettings.ENABLE_SPAM_FILTER.getValue())
            ratingTable.clear(qr);
    }

    /**
     * Calculates, sets and returns the spam rating for a RemoteFileDesc.
     * 
     * @param rfd the RemoteFileDesc to rate
     * @return the spam rating of the RemoteFileDesc, between 0 (not spam) and 1
     *         (spam)
     */
    public float calculateSpamRating(RemoteFileDesc rfd) {
        if (!SearchSettings.ENABLE_SPAM_FILTER.getValue())
            return 0;

        float rating = 0;
        // TODO: these results should probably be ignored (possibly using the
        // filters package) rather than treated as spam
        if (isIncompleteFile(rfd.getFileName().toLowerCase(Locale.US))) {
            rating = 1 - (1 - rating) * (1 - INCOMPLETE_FILE_RATING);
        }

        // Apply the 'Bayesian' filter
        rating = 1 - (1 - rating) * (1 - ratingTable.getRating(rfd));
        rfd.setSpamRating(rating);
        return rating;
    }

    /**
     * Increases the spam ratings of tokens associated with a spam query reply.
     */
    public void handleSpamQueryReply(QueryReply qr) {
        if (SearchSettings.ENABLE_SPAM_FILTER.getValue())
            ratingTable.rate(qr, 1);
    }

    /**
     * Increases the spam ratings of RFDs marked by the user as being spam.
     * 
     * @param rfds an array of RemoteFileDescs that should be marked as spam
     */
    public void handleUserMarkedSpam(RemoteFileDesc[] rfds) {
        for (RemoteFileDesc rfd : rfds)
            rfd.setSpamRating(1);
        // Update the ratings of the tokens associated with the RFDs
        ratingTable.rate(rfds, 1);
    }

    /**
     * Reduces the spam ratings of RFDs marked by the user as being good.
     * 
     * @param rfds an array of RemoteFileDescs that should be marked as good
     */
    public void handleUserMarkedGood(RemoteFileDesc[] rfds) {
        for (RemoteFileDesc rfd : rfds)
            rfd.setSpamRating(0);
        // Update the ratings of the tokens associated with the RFDs
        ratingTable.rate(rfds, 0);
    }

    /**
     * Clears all collected filter data.
     */
    public void clearFilterData() {
        ratingTable.clear();
    }

    /**
     * Checks whether a filename appears to indicate an incomplete file.
     * 
     * @param name the name of the file (from a search result)
     * @return true if we think that this is an incomplete file
     */
    private boolean isIncompleteFile(String name) {
        if (name.startsWith("incomplete_"))
            return true;
        if (name.startsWith("incomplete~"))
            return true;
        if (name.startsWith("inacheve_"))
            return true;
        if (name.startsWith("in_"))
            return true;
        if (name.startsWith("__incomplete"))
            return true;
        if (name.startsWith("___incompleted"))
            return true;
        if (name.startsWith("___arestra"))
            return true;
        if (name.startsWith("preview-t-"))
            return true;
        if (name.startsWith("t-")) {
            for (int i = 2; i < name.length(); i++) {
                if (Character.isDigit(name.charAt(i)))
                    continue;
                else
                    return name.charAt(i) == '-';
            }
        }
        if (name.startsWith("corrupt-")) {
            for (int i = 8; i < name.length(); i++) {
                if (Character.isDigit(name.charAt(i)))
                    continue;
                else
                    return name.charAt(i) == '-';
            }
        }
        return false;
    }
}
