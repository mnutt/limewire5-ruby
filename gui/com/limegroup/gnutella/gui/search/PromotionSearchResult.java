package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.swing.JPopupMenu;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.SpeedConstants;
import org.limewire.io.GUID;
import org.limewire.security.SHA1;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.Base32;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tables.Linkable;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.util.PopupUtils;
import com.limegroup.gnutella.util.EncodingUtils;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A search result returned for our sponsored search results.
 * 
 * @see GnutellaSearchResult
 */
public class PromotionSearchResult extends AbstractSearchResult implements Linkable {
    
    private final static Log LOG = LogFactory.getLog(PromotionSearchResult.class);
    
    private final String name;
    private final String url;
    private final int size;
    private final long creationTime;
    private final LimeXMLDocument xmlDoc;
    private final String vendor;
    private final URN urn;
    private final String type;
    private final String keyword;
    private final String displayUrl;
    
    public PromotionSearchResult(String name, String type, String url, int size, long creationTime,
            LimeXMLDocument xmlDoc, String vendor, String keyword, String displayUrl) {
        this.name = name;
        this.url = url;
        this.size = size;
        this.creationTime = creationTime;
        this.xmlDoc = xmlDoc;
        this.vendor = vendor;
        this.type = type == null ? "" : type;
        this.keyword = keyword;
        this.displayUrl = displayUrl;

        URN tmpUrn = null;
        try {
            
            SHA1 md1 = new SHA1();
            //
            // These could be null
            //
            if (name != null)   md1.update(name.getBytes("UTF-8"));
            if (url != null)    md1.update(url.getBytes("UTF-8"));
            if (vendor != null) md1.update(vendor.getBytes("UTF-8"));
            String urnString = new String(Base32.encode(md1.digest()));
            if (urnString.length() > 20) urnString = urnString.substring(0,20);
            tmpUrn = URN.createSHA1UrnFromBytes(urnString.getBytes());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            LOG.error("exception", e);
        }
        this.urn = tmpUrn;
    }
    
    @Override
    public String getFilenameNoExtension() {
        return name;
    }
    
    @Override
    public String getExtension() {
        return type;
    }
        
    public boolean isLink() {
        return url != null && !"".equals(url);
    }
    
    public String getLinkUrl() {
        return url;
    }
    
    public String getLinkDisplayUrl() {
        return displayUrl;
    }

    public String getFileName() {
        return name;
    }

    public URN getSHA1Urn() {
        return urn;
    }

    public long getSize() {
        return size;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public LimeXMLDocument getXMLDocument() {
        return xmlDoc;
    }

    public boolean isDownloading() {
        //
        // This is returning false right now because we have no means
        // currently of downloading one of these
        //
        return false;
    }

    public String getVendor() {
        return vendor == null ? "" : vendor;
    }

    public int getSpeed() {
        return SpeedConstants.THIRD_PARTY_SPEED_INT;
    }

    public boolean isMeasuredSpeed() {
        return true;
    }

    public int getQuality() {
        return QualityRenderer.THIRD_PARTY_RESULT_QUALITY;
    }

    public Status getSecureStatus() {
        // TODO return 0;
        throw new UnsupportedOperationException("old UI is broken");
    }
    
    public float getSpamRating() {
        return 0; // not spam
    }

    public String getHost() {
        //
        // We don't have a host, see the javadoc
        // for this method describing possible return values
        //
        return null; 
    }

    String getURL() {
        return url;
    }
    
    public Color getEvenRowColor() {
        return ThemeFileHandler.TABLE_SPECIAL_BACKGROUND_COLOR.getValue();
    }

    public Color getOddRowColor() {
        return ThemeFileHandler.TABLE_SPECIAL_ALTERNATE_COLOR.getValue();
    }

    public void takeAction(TableLine line, GUID guid, File saveDir, String fileName, boolean saveAs, SearchInformation searchInfo) {
        GUIMediator.openURL(LimeWireUtils.addLWInfoToUrl(getURL(), GuiCoreMediator.getApplicationServices().getMyGUID()) + "&keyword=" + EncodingUtils.encode(keyword) + "&client=true");
    }

    public void initialize(TableLine line) {
        //
        // Nothing needs to be done
        //
    }

    public JPopupMenu createMenu(JPopupMenu popupMenu, final TableLine[] lines, boolean markAsSpam, boolean markAsNot, final ResultPanel resultPanel) {
        
        PopupUtils.addMenuItem(I18n.tr("View in Browser..."), 
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        SearchMediator.downloadFromPanel(resultPanel, lines);
                    }
        }, popupMenu, lines.length > 0, 0);
        
        popupMenu.add(new JPopupMenu.Separator(), 1);
        
        return popupMenu;
    }

}
