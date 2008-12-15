package com.limegroup.gnutella.gui.startup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.limewire.i18n.I18nMarker;

/**
 *  Creates the list of messages that can be displayed in the Go Pro dialog at startup. 
 *  This list can be remotely changed and given different percentages to display one
 *  ad more than the others
 */
public class StartupBanner {

    /** A default message if something should go wrong with the remote ones */
    private static final StartupBanner DEFAULT_BANNER = 
        new StartupBanner(I18nMarker.marktr("Upgrade to PRO") + "\t" +
        I18nMarker.marktr("For Turbo-Charged downloads, get LimeWire PRO. We guarantee that you will love the improved performance of PRO. Thank you for helping keep the Internet open by running LimeWire.") + "\t" +
        I18nMarker.marktr("Upgrade to LimeWire PRO?") + "\t" + 
        I18nMarker.marktr("Yes") + "\t" +
        I18nMarker.marktr("Get LimeWire PRO Now") + "\t" +
        I18nMarker.marktr("Why") + "\t" +
        I18nMarker.marktr("What does PRO give me?") + "\t" +
        I18nMarker.marktr("Later") + "\t" +
        I18nMarker.marktr("Get LimeWire PRO Later") + "\t" +
        "http://www.limewire.com/index.jsp/pro" + "\t" +
        "http://www.limewire.com/promote/whygopro" + "\t" +
        "\t" + 
        "http://clientpix.limewire.com/pix/defaultProAd.jpg" + "\t" +
        "1.0f");
       
    private final static int messageLength = 14;
    
    private final List<StartupAd> ads;
    
    public StartupBanner(String... source) {
        if( source == null )
            throw new IllegalArgumentException();
        if( source.length == 0)
            throw new IllegalArgumentException();
        
        ads = new ArrayList<StartupAd>();

        for(int i = 0; i < source.length; i++) {
            String[] subStrings = source[i].split("\t");
            if( subStrings.length != messageLength)
                continue;
            
            ads.add(new StartupAd(subStrings[0], subStrings[1],subStrings[2], subStrings[3], 
                    subStrings[4], subStrings[5], subStrings[6],subStrings[7],
                    subStrings[8], subStrings[9], subStrings[10], subStrings[11], subStrings[12],
                    Float.valueOf(subStrings[13])));
        }
        if( ads.size() == 0 )
            throw new IllegalArgumentException();
    }
    
    /**
     * @return the next message that should be displayed.
     */
    public StartupAd getRandomAd() {
        // if no ads were created return the default one
        if( ads.size() == 0 )
            return DEFAULT_BANNER.getRandomAd();
        float dice = (float)Math.random();
        float current = 0; 
        for (StartupAd ad : ads) {
            current += ad.getProbability();
            if (current >= dice)
                return ad;
        }
        return ads.get(ads.size() - 1);
    }
    
    public Collection<StartupAd> getAllAds() {
        return Collections.unmodifiableCollection(ads);
    }
    
    /**
     * @return a default message that is not related to the remotely changable messages
     */
    public static StartupBanner getDefaultBanner() {
        return DEFAULT_BANNER;
    }
}
