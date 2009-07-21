package org.limewire.core.impl.search;

import org.limewire.core.settings.PromotionSettings;
import org.limewire.promotion.containers.PromotionMessageContainer;

/**
 * Class for creating and modifying URLs for search functionality.
 */
class SearchUrlUtils {

    /**
     * Creates the promotion URL and appends relevant GET params.
     * 
     * @return the generated URL.
     */
    public static String createPromotionUrl(PromotionMessageContainer container, long time) {
        StringBuffer url = new StringBuffer(PromotionSettings.REDIRECT_URL.get());
        url.append("?url=");
        url.append(container.getURL());
        url.append("&now=");
        url.append(time);
        url.append("&id=");
        url.append(container.getUniqueID());
        return url.toString();
    }

    /**
     * Strips "http://" and anything after ".com" (or .whatever) from the url
     * 
     * @return the stripped URL.
     */
    public static String stripUrl(String url){
        int dotIndex = url.indexOf('.');
        int endIndex = url.indexOf('/', dotIndex);
        endIndex = endIndex == -1 ? url.length() :  endIndex;
        int startIndex = url.indexOf("//");
        // this will either be 0 or the first character after "//"
        startIndex = startIndex == -1 ? 0 :  startIndex + 2;
        return url.substring(startIndex, endIndex);
    }

}
