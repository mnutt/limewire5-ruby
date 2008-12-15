package com.limegroup.gnutella.gui.search;

import java.util.HashMap;
import java.util.Map;

import org.limewire.core.settings.PromotionSettings;
import org.limewire.promotion.containers.PromotionMessageContainer;

import com.limegroup.gnutella.ApplicationServices;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

/**
 * Instances of this class will convert a {@link PromotionMessageContainer} into
 * a {@link SearchResult}. It uses an underlying
 * {@link PromotionSearchResultFactory}.
 */
final class PromotionMessageContainerToSearchResultConverter {

    /**
     * The factory we use to create a {@link LimeXMLDocument} when creating
     * search results.
     */
    private final PromotionSearchResultFactory creator;
    
    /** For the guid. */
    private final ApplicationServices applicationServices;

    PromotionMessageContainerToSearchResultConverter(LimeXMLDocumentFactory limeXMLDocumentFactory, ApplicationServices applicationServices) {
        this.creator = new PromotionSearchResultFactory(limeXMLDocumentFactory);
        this.applicationServices = applicationServices;
    }

    public SearchResult convert(PromotionMessageContainer container, String query) {
        Map<String, String> props = new HashMap<String, String>();
        props.put(PromotionSearchResultFactory.Attr.DISPLAY_URL.getValue(), container.getURL());
        props.put(PromotionSearchResultFactory.Attr.URL.getValue(), getURL(container));
        props.put(PromotionSearchResultFactory.Attr.NAME.getValue(), container.getDescription());
        Map<String, String> containerProps = container.getProperties();
        for (String key : containerProps.keySet()) {
            String val = containerProps.get(key);
            if (!isNormalProperty(key))
                key = "xml_" + key;
            props.put(key, val);
        }
        return creator.newSearchResult(props,query);
    }  
    
    /**
     * Returns <code>true</code> if <code>s</code> is a property in <b>all</b>
     * search results and <code>false</code> if it's not found in all.
     * 
     * @param s the String in question
     * @return <code>true</code> if <code>s</code> is a property in <b>all</b>
     *         search results and <code>false</code> if it's not found in all.
     */
    private boolean isNormalProperty(String s) {
        for (PromotionSearchResultFactory.Attr attr : PromotionSearchResultFactory.Attr.values()) {
            if (s.equals(attr.getValue()))
                return true;
        }
        return false;
    }      

    /**
     * Returns a version of the <code>container</code>'s URL that redirects.  We send along the following:
     * <ul>
     *  <li>the URL to which we redirect</li>     
     *  <li>the data we think it is</li>
     *  <li>the ID of the promo container</li>
     * </ul>
     * 
     * @param container container in question
     * @return a version of the <code>container</code>'s URL that redirects.
     */
    private String getURL(PromotionMessageContainer container) {
        String url = PromotionSettings.REDIRECT_URL.getValue();
        url += "?url=" + container.getURL();
        url += "&now=" + System.currentTimeMillis() / 1000;
        url += "&id=" + container.getUniqueID();
        return LimeWireUtils.addLWInfoToUrl(url, applicationServices.getMyGUID());
    }

}
