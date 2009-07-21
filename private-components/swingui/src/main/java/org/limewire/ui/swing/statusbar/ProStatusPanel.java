package org.limewire.ui.swing.statusbar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.Timer;

import org.limewire.core.api.Application;
import org.limewire.i18n.I18nMarker;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.LanguageUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProStatusPanel extends HyperlinkButton implements SettingListener, ActionListener {

    private final Set<InvisibilityCondition> conditions = new HashSet<InvisibilityCondition>();
    
    /** Default banner to use on non-English systems */
    private static final Banner DEFAULT_BANNER = new Banner(new String[] {
        I18nMarker.marktr("For Turbo-Charged searches get LimeWire PRO."),
        "http://www.limewire.com/index.jsp/pro&21",
        "0.111111",
        
        I18nMarker.marktr("Want faster downloads?  Get LimeWire PRO."),
        "http://www.limewire.com/index.jsp/pro&22",
        "0.111111",
        
        I18nMarker.marktr("Purchase LimeWire PRO to help us make downloads faster."),
        "http://www.limewire.com/index.jsp/pro&23",
        "0.111111",
        
        I18nMarker.marktr("For Turbo-Charged downloads get LimeWire PRO."),
        "http://www.limewire.com/index.jsp/pro&24",
        "0.111111",
        
        I18nMarker.marktr("For the best BitTorrent downloads, get LimeWire PRO."),
        "http://www.limewire.com/index.jsp/pro&25",
        "0.111111",
        
        I18nMarker.marktr("LimeWire PRO comes with FREE tech support. "),
        "http://www.limewire.com/index.jsp/pro&26",
        "0.111111",
        
        I18nMarker.marktr("For Turbo-Charged performance get LimeWire PRO."),
        "http://www.limewire.com/index.jsp/pro&27",
        "0.111111",
        
        I18nMarker.marktr("Keep the Internet open. Get LimeWire PRO."),
        "http://www.limewire.com/index.jsp/pro&28",
        "0.111111",
        
        I18nMarker.marktr("FREE updates and support - LimeWire PRO."),
        "http://www.limewire.com/index.jsp/pro&29",
        "0.111111"
    });
    
    /**
     * banner of pro ads.
     * LOCKING: labels
     */
    private Banner ads;
    
    /**
     * Map from pro ads to a label.
     */
    private final Map<Ad,LabelURLPair> labels = Collections.synchronizedMap(new HashMap<Ad, LabelURLPair>());
    
    /**
     * The currently displayed <tt>LabelURLPair</tt>.
     */
    private LabelURLPair currentLabel;

    private boolean isInitialised = false;
    
    private final Application application;

    @Inject
    public ProStatusPanel(Application application) {
        this.application = application;
        setName("ProStatusPanel");        
        if (application.isProVersion()) {
            addCondition(InvisibilityCondition.IS_PRO);
        }
    }
    
    /**
     * Add a new visibility condition, will probably result
     *  in the panel being hidden.
     */
    public void addCondition(InvisibilityCondition condition) {
        conditions.add(condition);
        updateVisibility();
    }
    
    /**
     * Remove a visibility condition.  If there are none left the 
     *  panel will be shown.
     */
    public void removeCondition(InvisibilityCondition condition) {
        conditions.remove(condition);
        updateVisibility();
    }
    
    private void updateVisibility() {
        if (!isVisible() && !isInitialised && conditions.isEmpty()) {
            init();
            isInitialised  = true;
        }
        
        setVisible(conditions.isEmpty());
    }
    
    private void init() {        
        if (LanguageUtils.isEnglishLocale(LanguageUtils.getCurrentLocale())) {
            loadLabels();
            SwingUiSettings.PRO_ADS.addSettingListener(this);
        }
        
        // if not English or loading from props failed, load default
        synchronized(labels) {
            if (labels.isEmpty()) {
                updateLabels(DEFAULT_BANNER);
            }
            assert !labels.isEmpty() : "couldn't load any pro banner!";
        }
        
        addActionListener(this);
        
        // only build and start timer if there are labels to cycle through
        if (labels.size() > 1) {
            new Timer(30 * 1000, new LabelTimerListener()).start();
            
            // Load first label
            handleLinkChange();
        }
    }
        
    /**
     * Conditions that cause this panel to be hidden.
     *  If any of these conditions are added to the panel
     *  it will not be visible.
     */
    public static enum InvisibilityCondition {
        NOT_FULLY_CONNECTED, PRO_ADD_SHOWN, IS_PRO ;
    }
    
    public void settingChanged(SettingEvent evt) {
        if (evt.getEventType() != SettingEvent.EventType.VALUE_CHANGED)
            return;
        loadLabels();
    }

    private void loadLabels() {
        try {
            Banner b = new Banner(SwingUiSettings.PRO_ADS.get());
            updateLabels(b);
        } catch (IllegalArgumentException bad) {
            return;
        }
    }
    
    private void updateLabels(Banner b) {
        synchronized(labels) {
            ads = b;
            labels.clear();
            for (Ad ad : ads.getAllAds()) {
                String text = ad.getText();
                text = I18n.tr(text);
                labels.put(ad,new LabelURLPair(text, ad.getURI()));
            }
        }
    }
    
    /**
     * @return the next <tt>LabelURLPair</tt> in the list
     */
    private LabelURLPair getNextLabelURLPair() {
        synchronized(labels) {
            currentLabel = labels.get(ads.getAd());
        }
        return currentLabel;
    }
    
    /**
     * Handle a change in the current <tt>LabelURLPair</tt> pair.
     */
    private void handleLinkChange() {
        String label = getNextLabelURLPair().getLabel();
        this.setText(label);
    }
    
    /**
     * Handles a click on the current link by opening the appropriate web 
     * page.
     */
    private void handleLinkClick() {
        NativeLaunchUtils.openURL(application.addClientInfoToUrl(currentLabel.getURL()));
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        handleLinkClick();
    }
        
    /**
     * Private class for handling a change in the link/label pair.
     */
    private class LabelTimerListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            ProStatusPanel.this.handleLinkChange();
        }
    }
    
    /**
     * This class wraps a label for the link and the url to link to.
     */
    private static final class LabelURLPair {
         
        /**
         * Constant for the label string for this pair.
         */
        private final String LABEL_STRING;

        /**
         * Constant for the url for this pair.
         */
        private final String URL;

        /**
         * Creates a new <tt>LabelURLPair</tt> instance with the 
         * specified label and url.
         *
         * @param label the label for the link
         * @param url to url to link to
         */
        private LabelURLPair(final String label, final String url) {
            LABEL_STRING = label;
            URL = url;
        }

        /**
         * Returns the label's text
         *  previous implementation returned the text in html format
         *  to simulate a hyperlink look; the new label provides the
         *  framework to simulate this work at a substantial reduction
         *  in memory and processor cost; the jvm treats any html as a
         *  possible full blown document and sets up the structures to
         *  process hyper text formatting for just these couple of words
         *  every 30 seconds (no reuse either). Also, the old html link
         *  wasn't truly clickable, we use a mouse listener to provide
         *  mouse clicking action support; <font color=blue>LABEL_STRING</font>
         *  would have provided the same effect (or affect :)
         *  previous: return "<html><a href=\"\">"+LABEL_STRING+"</a></html>";
         *
         * @return the label
         */
        private String getLabel() {
            return LABEL_STRING;
        }

        /**
         *
         * @return the url to link to
         */
        private String getURL() {
            return URL;
        }
    }
}
