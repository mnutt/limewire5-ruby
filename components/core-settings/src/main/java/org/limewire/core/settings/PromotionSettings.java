package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.IntSetting;
import org.limewire.setting.StringSetting;

/**
 * Settings for The LimeWire Store&#8482; song search results. This is used by
 * {@link com.limegroup.gnutella.PromotionServicesImpl}.
 */
public final class PromotionSettings extends LimeProps {
    private PromotionSettings() {}

    /**
     * Enables the promotion system when <code>true</code>.
     */    
    public static final BooleanSetting PROMOTION_SYSTEM_IS_ENABLED =
        FACTORY.createRemoteBooleanSetting("PROMOTION_SYSTEM_IS_ENABLED", false, "Promo.promotionSystemIsEnabled");    
 
    /**
     * The url we use to search for {@link org.limewire.promotion.PromotionBinder}s.
     */    
    public static final StringSetting SEARCH_URL =
        FACTORY.createRemoteStringSetting("SEARCH_URL", "http://binders.links.limewire.com/lwp/getBuckets.php", "Promo.searchUrl");

    /**
     * The url we use to redirect for {@link org.limewire.promotion.PromotionBinder}s.
     */    
    public static final StringSetting REDIRECT_URL =
        FACTORY.createRemoteStringSetting("REDIRECT_URL", "http://binders.links.limewire.com/lwp/redirect.php", "Promo.redirectUrl");    
    
    /**
     * The mod we take with the bucket ID. 
     */    
    public static final IntSetting BUCKET_ID_MODULUS =
        FACTORY.createRemoteIntSetting("BUCKET_ID_MODULUS", 4000, "Promo.bucketIdModulous", 0, Integer.MAX_VALUE); 
    
    /**
     * The max number of search results.
     */    
    public static final IntSetting MAX_NUMBER_OF_SEARCH_RESULTS =
        FACTORY.createRemoteIntSetting("MAX_NUMBER_OF_SEARCH_RESULTS", 3, "Promo.maxNumberOfSearchResults", 0, 15);
}
