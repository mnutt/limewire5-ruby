package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.io.File;

import javax.swing.JPopupMenu;

import org.limewire.collection.ApproximateMatcher;
import org.limewire.io.GUID;
import org.limewire.security.SecureMessage.Status;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A single SearchResult. These are returned in the {@link SearchInputPanel} and
 * are used to create {@link TableLine}s to show search results. *
 */
interface SearchResult {

    /**
     * @return the file name
     */
    String getFileName();
    
    /**
     * Gets the size of this SearchResult.
     */
    long getSize();
    
    /**
     * @return the SHA1 URN for this artifact
     */
    URN getSHA1Urn();
    
    /**
     * @return the XML document representing the search result
     */
    LimeXMLDocument getXMLDocument();
    
    /**
     * @return milliseconds since January 01, 1970 the artifact of t
     */
    long getCreationTime();
    
    /**
     * @return <code>true</code> if this result is currently being downloaded
     */
    boolean isDownloading();
    
    /**
     * @return the name of vendor who created this artifact
     */
    String getVendor();
    
    /**
     * @return the connection speed of this result or
     *         {@link SpeedConstants#THIRD_PARTY_SPEED_INT} for a
     *         {@link PromotionSearchResult}
     */
    int getSpeed();
    
    /**
     * @return <code>true</code> if this speed is messaured.
     */
    boolean isMeasuredSpeed();
    
    /**
     * @return the quality of the search result as one of
     * <ul>
     *  <li>{@link QualityRenderer#SPAM_FILE_QUALITY}</li>   
     *  <li>{@link QualityRenderer#SAVED_FILE_QUALITY}</li>
     *  <li>{@link QualityRenderer#DOWNLOADING_FILE_QUALITY}</li>
     *  <li>{@link QualityRenderer#INCOMPLETE_FILE_QUALITY}</li>   
     *  <li>{@link QualityRenderer#SECURE_QUALITY}</li>   
     *  <li>{@link QualityRenderer#THIRD_PARTY_RESULT_QUALITY}</li>   
     *  <li>{@link QualityRenderer#MULTICAST_QUALITY}</li>   
     *  <li>{@link QualityRenderer#EXCELLENT_QUALITY}</li>   
     *  <li>{@link QualityRenderer#GOOD_QUALITY}</li>   
     *  <li>{@link QualityRenderer#FAIR_QUALITY}</li>   
     *  <li>{@link QualityRenderer#POOR_QUALITY}</li>
     *  <li>{@link QualityRenderer#THIRD_PARTY_RESULT_QUALITY}</li>
     * </ul>
     */
    int getQuality();
    
    /**
     * @return the secure status of the search result as one of
     * <ul>
     *  <li>{@link SecureMessage#FAILED</li>
     *  <li>{@link SecureMessage#SECURE</li>
     *  <li>{@link SecureMessage#INSECURE</li>
     * </ul>
     */
    Status getSecureStatus();
    
    /** 
     * @return <code>0</code> for not spam or a higher value for spam
     */
    float getSpamRating();

    /**
     * Returns host or <code>null</code> for no host.
     * 
     * @return host or <code>null</code> for no host.
     */
    String getHost();

    /**
     * Returns the color for painting an even row.
     * 
     * @return the color for painting an even row
     */
    Color getEvenRowColor();

    /**
     * Returns the color for painting an odd row.
     * 
     * @return the color for painting an odd row
     */
    Color getOddRowColor();

    /**
     * Gets the filename without the extension.
     */
    String getFilenameNoExtension();

    /**
     * Returns the extension of this result.
     */
    String getExtension();

    /** 
     * Compares <code>this</code> against <code>o</code> approximately:
     * <ul>
     *  <li> Returns <code>0</code> if <code>o</code> is similar to this. 
     *  <li> Returns <code>1</code> if they have non-similar extensions.
     *  <li> Returns <code>2</code> if they have non-similar sizes.
     *  <li> Returns <code>3</code> if they have non-similar names.
     * <ul>
     *
     * Design note: this takes an ApproximateMatcher as an argument so that many
     * comparisons may be done with the same matcher, greatly reducing the
     * number of allocations.<b>
     *
     * <b>This method is not thread-safe.</b>
     */
    int match(SearchResult o, ApproximateMatcher matcher);

    /**
     * This method is called when a {@link TableLine} is clicked on in the
     * {@link ResultPanel}, and the user wants to take some action, such as
     * downloading or displaying the result in a browser.
     * 
     * @param line the line on which was clicked; this is needed for the
     *        {@link GnutellaSearchResult} so it can pass the line back to
     *        {@link SearchMediator} to do the download
     * @param guid the GUID of the result; used for the reason above
     * @param saveDir if we were to download the result, it would go here; used
     *        for the reason above
     * @param fileName name of the file in which we would save the result on a
     *        download; used for the reason above
     * @param saveAs used for the reason above
     * @param searchInfo the info used for the original search
     */
    void takeAction(TableLine line, GUID guid, File saveDir, String fileName,
            boolean saveAs, SearchInformation searchInfo);

    /**
     * Initializes <code>line</code> to hold <code>this</code>.
     * 
     * @param line {@link TableLine} that will hold <code>this</code>
     */
    void initialize(TableLine line);
            
            
    
    JPopupMenu createMenu(JPopupMenu popupMenu, TableLine[] lines, boolean markAsSpam, boolean markAsNot, ResultPanel rp);

}