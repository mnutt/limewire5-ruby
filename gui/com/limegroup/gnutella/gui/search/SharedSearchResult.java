package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.limewire.core.settings.ConnectionSettings;
import org.limewire.io.GUID;
import org.limewire.io.NetworkUtils;
import org.limewire.security.SecureMessage.Status;

import com.limegroup.gnutella.NetworkManager;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.themes.ThemeFileHandler;
import com.limegroup.gnutella.library.CreationTimeCache;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.util.LimeWireUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;

/**
 *  A Single Shared Folder search result. This is generated when the user
 *  wishes to view all the files they are currently sharing. This behaves 
 *  similar to a GnutellaSearchResult except the FileDesc is passed along
 *  to make unsharing of a particular result easy. 
 */
public class SharedSearchResult extends AbstractSearchResult {

    private final FileDesc fileDesc;    
    private final CreationTimeCache creationTimeCache;
    private final NetworkManager networkManager;

    public SharedSearchResult(FileDesc fileDesc, CreationTimeCache creationTimeCache, NetworkManager networkManager){
        this.fileDesc = fileDesc;
        this.creationTimeCache = creationTimeCache;
        this.networkManager = networkManager;
    }
    
    public FileDesc getFileDesc(){
        return fileDesc;
    }

    public String getFileName() {
        return fileDesc.getFileName();
    }

    public long getSize() {
        return fileDesc.getFileSize();
    }

    public URN getSHA1Urn() {
        return fileDesc.getSHA1Urn();
    }

    public LimeXMLDocument getXMLDocument() {
        // TODO - use fileDesc.getLimeXMLDoc(); note that it has slightly different logic
        
        // legacy LW had multiple docs per file.
        // now only one is allowed.
        // could potentially pick the one that
        // corresponds to the files type.
        List<LimeXMLDocument> docs = fileDesc.getLimeXMLDocuments();
        if (docs.size() == 1) {
            return docs.get(0);
        } else {
            return null;
        }
    }

    public long getCreationTime() {
        return creationTimeCache.getCreationTime(getSHA1Urn());
    }

    public boolean isDownloading() {
        return false;
    }

    public String getVendor() {
        return LimeWireUtils.QHD_VENDOR_NAME;
    }

    public int getQuality() {
        return 0;
    }

    public Status getSecureStatus() {
        return Status.INSECURE; 
    }

    public int getSpeed() {
        return ConnectionSettings.CONNECTION_SPEED.getValue();
    }

    public boolean isMeasuredSpeed() {
        return false;
    }

    public float getSpamRating() {
        return 0.f;
    }

    public String getHost() {
        return NetworkUtils.ip2string(networkManager.getAddress());
    }

    public Color getEvenRowColor() {
        return ThemeFileHandler.TABLE_BACKGROUND_COLOR.getValue();
    }

    public Color getOddRowColor() {
        return ThemeFileHandler.TABLE_ALTERNATE_COLOR.getValue();
    }

    public void takeAction(TableLine line, GUID guid, File saveDir, String fileName, boolean saveAs, SearchInformation searchInfo) {
    }

    public JPopupMenu createMenu(JPopupMenu popupMenu, TableLine[] lines, boolean markAsSpam, boolean markAsNot, ResultPanel resultPanel) {
        JPopupMenu menu = new JPopupMenu();
        List<JLabel> labels = new ArrayList<JLabel>(5);
        for(TableLine line : lines) {
            SharedSearchResult result = (SharedSearchResult)line.getSearchResult();
            JLabel label = new JLabel(I18n.tr("Path") + ": " + result.getFileDesc().getPath());
            menu.insert(label, labels.size());
            labels.add(label);
            if(labels.size() == 5 && lines.length > labels.size()) {
                int numberOfMore = lines.length - labels.size();
                label = new JLabel(I18n.trn("... {0} more.", "... {0} more.", numberOfMore, numberOfMore));
                menu.insert(label, labels.size());
                labels.add(label);
                break;
            }
        }
        menu.addSeparator();
        
        // TODO: fix this hidden coupling, SharedSearchResult needs to be tied MyFilesResultPanel
        //        explicitly
        JMenuItem menuItem = new JMenuItem(((MySharedFilesResultPanel)resultPanel).getUnshareAction(lines.length));
        menu.add(menuItem);
        
        // configure label to have the same borders and margins as a menu item
        Insets margin = menuItem.getMargin();
        Insets border = menuItem.getInsets();
        for(JLabel label : labels) {
            label.setBorder(BorderFactory.createEmptyBorder(border.top, margin.left + border.left, border.bottom, margin.right + border.right));
            label.setIconTextGap(menuItem.getIconTextGap());
            // if icon is null, icon text gap value is ignored
            label.setIcon(new GUIUtils.EmptyIcon("empty", 0, 0));
        }
 
        return menu;
    }

    public void initialize(TableLine line) {
        line.createEndpointHolder("127.0.0.1", networkManager.getPort(), false);         
        line.setAddedOn(getCreationTime());
    }
}
