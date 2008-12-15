package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collections;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.settings.UISettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.security.SecureMessage.Status;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.BitziLookupAction;
import com.limegroup.gnutella.gui.actions.CopyMagnetLinkToClipboardAction;
import com.limegroup.gnutella.gui.properties.ResultProperties;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.gui.util.PopupUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 * A single SearchResult.
 *
 * (A collection of RemoteFileDesc, HostData, and Set of alternate locations.)
 */
final class GnutellaSearchResult extends AbstractSearchResult {
    private final RemoteFileDesc RFD;
    private Set<? extends IpPort> _alts;
    
    /**
     * Constructs a new SearchResult with the given data.
     */
    GnutellaSearchResult(RemoteFileDesc rfd, Set<? extends IpPort> alts) {
        RFD = rfd;
        if(UISettings.UI_ADD_REPLY_ALT_LOCS.getValue())
            _alts = alts;
        else
            _alts = Collections.emptySet();
    }
    
    /** Gets the RemoteFileDesc */
    RemoteFileDesc getRemoteFileDesc() { return RFD; }
    
    /** Gets the Alternate Locations */
    Set<? extends IpPort> getAlts() { return _alts; }
    
    /**
     * Clears the alternate locations for this SearchResult.
     */
    void clearAlts() {
        _alts = null;
    }
    
    /**
     * Sets the alternate locations for this SearchResult.
     */
    void setAlts(Set<? extends IpPort> alts) {
        _alts = alts;
    }
    
    public String getFileName() {
        return RFD.getFileName();
    }

    public long getSize() {
        return RFD.getSize();
    }

    public URN getSHA1Urn() {
        return RFD.getSHA1Urn();
    }

    public LimeXMLDocument getXMLDocument() {
        return RFD.getXMLDocument();
    }

    public long getCreationTime() {
        return RFD.getCreationTime();
    }

    public boolean isDownloading() {
        // TODO return RFD.isDownloading();
        throw new UnsupportedOperationException("old UI is broken");
    }

    public String getVendor() {
        return RFD.getVendor();
    }

    public int getQuality() {
        return RFD.getQuality();
    }

    public Status getSecureStatus() {
        return RFD.getSecureStatus();
    }

    public int getSpeed() {
        return RFD.getSpeed();
    }

    public boolean isMeasuredSpeed() {
        return false;
    }

    public float getSpamRating() {
        return RFD.getSpamRating();
    }

    public String getHost() {
        // TODO return RFD.getAddress();
        throw new UnsupportedOperationException("old UI is broken");
    }

    public Color getEvenRowColor() {
        return ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue();
    }

    public Color getOddRowColor() {
        return ThemeFileHandler.TABLE_ALTERNATE_COLOR.getValue();
    }

    public void takeAction(TableLine line, GUID guid, File saveDir, String fileName, boolean saveAs, SearchInformation searchInfo) {
        SearchMediator.downloadGnutellaLine(line, guid, saveDir, fileName, saveAs, searchInfo);
    }

    public void initialize(TableLine line) {
        RemoteFileDesc rfd = getRemoteFileDesc();
//        Set<? extends IpPort> alts = getAlts();

        if (rfd.isChatEnabled()) {
            line.setChatHost(rfd);
        }
        if (rfd.isBrowseHostEnabled()) {
            line.setBrowseHost(rfd);
        }
// TODO       if (!rfd.isFirewalled()) {
// TODO           line.setNonFirewalledHost(rfd);
// TODO       }
// TODO       line.createEndpointHolder(
// TODO           rfd.getAddress(), rfd.getPort(),
// TODO           rfd.isReplyToMulticast());
// TODO
// TODO       line.setAddedOn(rfd.getCreationTime());
// TODO
// TODO       if(alts != null && !alts.isEmpty()) {
// TODO           Set<IpPort> as = line.getAltIpPortSet();
// TODO           as.addAll(alts);
// TODO           clearAlts();
// TODO           line.getLocation().addHosts(alts);
// TODO       }
        throw new UnsupportedOperationException("old UI is broken");
    }

    public JPopupMenu createMenu(JPopupMenu popupMenu, TableLine[] lines, boolean markAsSpam, boolean markAsNot, ResultPanel resultPanel) {
        
        PopupUtils.addMenuItem(SearchMediator.DOWNLOAD_STRING, resultPanel.DOWNLOAD_LISTENER,
                popupMenu, lines.length > 0, 0);
        PopupUtils.addMenuItem(I18n.tr("Download As..."), resultPanel.DOWNLOAD_AS_LISTENER,
                popupMenu, lines.length == 1, 1);
        PopupUtils.addMenuItem(I18n.tr("View License"), new LicenseListener(resultPanel),
                popupMenu, lines.length > 0 && lines[0].isLicenseAvailable(), 2);
        PopupUtils.addMenuItem(SearchMediator.CHAT_STRING, resultPanel.CHAT_LISTENER, popupMenu,
                lines.length > 0 && lines[0].isChatEnabled(), 3);
        PopupUtils.addMenuItem(SearchMediator.BROWSE_HOST_STRING, resultPanel.BROWSE_HOST_LISTENER,
                popupMenu, lines.length > 0 && lines[0].isBrowseHostEnabled(), 4);
        PopupUtils.addMenuItem(SearchMediator.BLOCK_STRING, new BlockListener(resultPanel),
                popupMenu, lines.length > 0, 5);
        
        JMenu spamMenu = new JMenu(SearchMediator.MARK_AS_STRING);
        spamMenu.setEnabled(markAsSpam || markAsNot);
        PopupUtils.addMenuItem(SearchMediator.SPAM_STRING, resultPanel.MARK_AS_SPAM_LISTENER,
                spamMenu, markAsSpam);
        PopupUtils.addMenuItem(SearchMediator.NOT_SPAM_STRING,
                resultPanel.MARK_AS_NOT_SPAM_LISTENER, spamMenu, markAsNot);
        popupMenu.add(spamMenu, 6);
        
        popupMenu.add(new JPopupMenu.Separator(), 7);
        popupMenu.add(createAdvancedMenu(lines.length > 0 ? lines[0] : null, resultPanel), 8);
        
        popupMenu.add(new JPopupMenu.Separator());
        PopupUtils.addMenuItem(ResultProperties.title(), resultPanel.PROPERTIES_LISTENER,
                popupMenu, lines.length > 0);

        return popupMenu;
    }
    
    
    /**
     * This may return null for non-gnutella search results.
     * 
     * @param line
     * @return
     */
    private JMenu createAdvancedMenu(TableLine line, ResultPanel resultPanel) {
        JMenu menu = new JMenu(I18n.tr("Advanced"));
        
        if (line == null) {
            menu.setEnabled(false);
            return menu;
        }
        
        BitziLookupAction bitziAction = new BitziLookupAction(resultPanel);

        bitziAction.setEnabled(line.getRemoteFileDesc().getSHA1Urn() != null);
        menu.add(new JMenuItem(bitziAction));
        
        CopyMagnetLinkToClipboardAction magnet =
            new CopyMagnetLinkToClipboardAction(resultPanel);
        magnet.setEnabled(line.hasNonFirewalledRFD());
        menu.add(new JMenuItem(magnet));
        
        // launch action
        if(line.isLaunchable()) {
            menu.addSeparator();
            PopupUtils.addMenuItem(SearchMediator.LAUNCH_STRING, resultPanel.DOWNLOAD_LISTENER, 
                    menu.getPopupMenu(), true);
        }

        
        return menu;
    }    
    


    private static class LicenseListener implements ActionListener {
        private final ResultPanel p;
        LicenseListener(ResultPanel p) {
            this.p = p;
        }

        public void actionPerformed(ActionEvent e) {
            p.showLicense();
        }
    }

    private static class BlockListener implements ActionListener {
        private final ResultPanel p;
        BlockListener(ResultPanel p) {
            this.p = p;
        }

        public void actionPerformed(ActionEvent e) {
            p.blockHost();
        }
    }
   


}