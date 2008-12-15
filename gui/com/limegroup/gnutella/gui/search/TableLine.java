package com.limegroup.gnutella.gui.search;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Icon;

import org.limewire.core.settings.SearchSettings;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.io.IpPortSet;
import org.limewire.security.SecureMessage.Status;
import org.limewire.util.NameValue;
import org.limewire.util.OSUtils;

import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.gui.search.Selector.PropertyType;
import com.limegroup.gnutella.gui.tables.AbstractDataLine;
import com.limegroup.gnutella.gui.tables.IconAndNameHolder;
import com.limegroup.gnutella.gui.tables.IconAndNameHolderImpl;
import com.limegroup.gnutella.gui.tables.LimeTableColumn;
import com.limegroup.gnutella.gui.tables.Linkable;
import com.limegroup.gnutella.gui.tables.SizeHolder;
import com.limegroup.gnutella.gui.xml.XMLUtils;
import com.limegroup.gnutella.gui.xml.XMLValue;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

/** 
 * A single line of a search result.
 */
public final class TableLine extends AbstractDataLine<SearchResult> implements Linkable {
    /**
     * The SearchTableColumns.
     */
    private final SearchTableColumns COLUMNS;
    
    /**
     * The SearchResult that created this particular line.
     */
    private SearchResult RESULT;
    
    /**
     * The list of other SearchResults that match this line.
     */
    private List<SearchResult> _otherResults;
    
    /**
     * The SHA1 of this line.
     */
    private URN _sha1;
    
    /**
     * The media type of this document.
     */
    private NamedMediaType _mediaType;
    
    /**
     * The set of other locations that have this result.
     */
    private Set<IpPort> _alts;

    /**
     * Whether or not this file is saved in the library.
     */
    private boolean _savedFile;
    
    /**
     * Whether or not this file is incomplete.
     */
    private boolean _incompleteFile;
    
    /**
     * Whether or not this file was downloading the last time we checked.
     */
    private boolean _downloading;
    
    /** Whether or not this result is a secure result. */
    private boolean _secure;

    /**
     * The speed of this line.
     */
    private ResultSpeed _speed = null;
    
    /**
     * The quality of this line.
     */
    private int _quality;
    
    /**
     * A chat enabled host if there is one.
     */
    private RemoteFileDesc _chatHost;
    
    /**
     * A browse enabled host if there is one.
     */
    private RemoteFileDesc _browseHost;
    
	/**
	 * A non firewalled host if there is one.
	 */
	private RemoteFileDesc _nonFirewalledHost;
	
    /**
     * The LimeXMLDocument for this line.
     */
    private LimeXMLDocument _doc;
    
    /**
     * The location of this line.
     */
    private EndpointHolder _location = null;
    
    /**
     * The date this was added to the network.
     */
    private long _addedOn;
    
    /**
     * The last spam rating this TableLine had
     */
    private float _lastRating = -1f;
    
    /** License info. */
    private int _licenseState = License.NO_LICENSE;
    private String _licenseName = null;
    
    public TableLine(SearchTableColumns stc) {
        COLUMNS = stc;
    }
    
    /**
     * Initializes this line with the specified search result.
     */
    @Override
    public void initialize(SearchResult sr) {
        super.initialize(sr);
        initilizeStart(sr);
        sr.initialize(this);
        initializeEnd();
    }
        
    private void initilizeStart(SearchResult sr) {
        RESULT = sr;
        _doc = sr.getXMLDocument();
        _sha1 = sr.getSHA1Urn();
        if(_doc != null)
            _mediaType = NamedMediaType.getFromDescription(_doc.getSchemaDescription());
        else
            _mediaType = NamedMediaType.getFromExtension(getExtension());
        _speed = new ResultSpeed(sr.getSpeed(), sr.isMeasuredSpeed());
        _quality = sr.getQuality();
        _secure = sr.getSecureStatus() == Status.SECURE;
    }

    private void initializeEnd() {
        updateLicense();        
        updateFileStatus();        
    }
    
    public boolean isLink() {
        if(RESULT instanceof Linkable)
            return ((Linkable)RESULT).isLink();
        else
            return _doc != null && !"".equals(_doc.getAction());
    }
    
    public String getLinkUrl() {
        if(RESULT instanceof Linkable)
            return ((Linkable)RESULT).getLinkUrl();
        else if (_doc != null)
            return _doc.getAction();
        else
            return null;
    }
    
    public String getLinkDisplayUrl() {
        if(RESULT instanceof Linkable)
            return ((Linkable)RESULT).getLinkDisplayUrl();
        else if(_doc != null)
            return _doc.getAction();
        else
            return null;
    }
    
    /**
     * Adds a new SearchResult to this TableLine.
     */
    void addNewResult(SearchResult sr, MetadataModel mm) {

        URN resultSHA1 = RESULT.getSHA1Urn();
        URN thisSHA1 = sr.getSHA1Urn();
        if(resultSHA1 == null)
            assert thisSHA1 == null;
        else
            assert resultSHA1.equals(thisSHA1);

        if(_otherResults == null)
            _otherResults = new LinkedList<SearchResult>();
        _otherResults.add(sr);
        
        // mark that we need to recalculate the rating
        _lastRating = -1f;
        
        if (sr instanceof GnutellaSearchResult) {
            GnutellaSearchResult gsr = (GnutellaSearchResult)sr;
//            RemoteFileDesc rfd = gsr.getRemoteFileDesc();
            Set<? extends IpPort> alts = gsr.getAlts();
            if(alts != null && !alts.isEmpty()) {
                if(_alts == null)
                    _alts = new IpPortSet();
                _alts.addAll(alts);
                gsr.clearAlts();
                _location.addHosts(alts);
            }
            // TODO _location.addHost(rfd.getAddress(), rfd.getPort());
            throw new UnsupportedOperationException("old UI is broken");
        }
        
        
        // Set the speed correctly.
        ResultSpeed newSpeed = new ResultSpeed(sr.getSpeed(), sr.isMeasuredSpeed());
        // if we're changing a property, update the metadata model.
        if(_speed.compareTo(newSpeed) < 0) {
            if(mm != null)
                mm.updateProperty(PropertyType.SPEED.getKey(), _speed, newSpeed, this);
            _speed = newSpeed;
        }
        
        // Set the quality correctly.
        _quality = Math.max(sr.getQuality(), _quality);
        _secure |= sr.getSecureStatus() == Status.SECURE;        
        
        if (sr instanceof GnutellaSearchResult) {
            GnutellaSearchResult gsr = (GnutellaSearchResult)sr;
            RemoteFileDesc rfd = gsr.getRemoteFileDesc();
            if(rfd.getCreationTime() > 0)
                _addedOn = Math.min(_addedOn, rfd.getCreationTime());
                                      
            // Set chat host correctly.
            if (_chatHost == null && rfd.isChatEnabled()) {
    			_chatHost = rfd;
            }
            // Set browse host correctly.
    		if (_browseHost == null && rfd.isBrowseHostEnabled()) {
    			_browseHost = rfd;
    		}
//  TODO  		if (_nonFirewalledHost == null && !rfd.isFirewalled()) {
//  TODO  			_nonFirewalledHost = rfd;
//  TODO  		}
            throw new UnsupportedOperationException("old UI is broken");
        }
        
        
        if(sr.getCreationTime() > 0)
            _addedOn = Math.min(_addedOn, sr.getCreationTime());        
        
        updateXMLDocument(sr.getXMLDocument(), mm);        
    }
    
    final boolean isThirdPartyResult() {
        return RESULT instanceof PromotionSearchResult;
    }
    
    /**
     * Updates the XMLDocument and the MetadataModel.
     */
    private void updateXMLDocument(LimeXMLDocument newDoc, MetadataModel mm) {
        // If nothing new, nothing to do.
        if(newDoc == null)
            return;
        
        // If no document exists, just set it to be the new doc
        if(_doc == null) {
            _doc = newDoc;
            updateLicense();
            if(mm != null) {
                _mediaType = NamedMediaType.getFromDescription(
                                _doc.getSchemaDescription());
                mm.addNewDocument(_doc, this);
            }
            return;
        }
        
        // Otherwise, if a document does exist in the group, see if the line
        // has extra fields that can be added to the group.
        
        // Must have the same schema...
        if(!_doc.getSchemaURI().equals(newDoc.getSchemaURI()))
            return;
        
        Set<String> oldKeys = _doc.getNameSet();
        Set<String> newKeys = newDoc.getNameSet();
        // if the we already have everything in new, do nothing
        if(oldKeys.containsAll(newKeys))
            return;

        // Now we want to add the values of newKeys that weren't
        // already in oldKeys.
        newKeys = new HashSet<String>(newKeys);
        newKeys.removeAll(oldKeys);
        // newKeys now only has brand new elements.
        Map<String, String> newMap = new HashMap<String, String>(oldKeys.size() + newKeys.size());
        for(Map.Entry<String, String> entry : _doc.getNameValueSet())
            newMap.put(entry.getKey(), entry.getValue());
        
        LimeXMLSchema schema = _doc.getSchema();
        for(SchemaFieldInfo sfi : schema.getCanonicalizedFields()) {
            String key = sfi.getCanonicalizedFieldName();
            if(newKeys.contains(key)) {
                String value = newDoc.getValue(key);
                if(mm != null)
                    mm.addField(sfi, key, value, this);
            }
        }

        _doc = GuiCoreMediator.getLimeXMLDocumentFactory().createLimeXMLDocument(newMap.entrySet(), _doc.getSchemaURI());
        updateLicense();
    }

    /**
     * Updates the file status of this line.
     */
    private void updateFileStatus() {
        // hack for LWC-1099 -- can't check incomplete or else deadlock
        if(RESULT instanceof SharedSearchResult) {
            SharedSearchResult ssr = (SharedSearchResult)RESULT;
            if(GuiCoreMediator.getFileManager().getIncompleteFileList().contains(ssr.getFileDesc()))
                _incompleteFile = true;
            else
                _savedFile = true;
        } else {
            if(_sha1 != null) {
                _savedFile = GuiCoreMediator.getFileManager().getGnutellaFileList().getFileDesc(_sha1) != null;
                _incompleteFile = GuiCoreMediator.getDownloadManager().isIncomplete(_sha1);
            } else {
                _savedFile = false;
                _incompleteFile = false;
            }
//            if(!_savedFile) {
//                _savedFile =
//                    GuiCoreMediator.getSavedFileManager().isSaved(_sha1, getFilename());
//            }
        }
    }
    
    /**
     * Updates cached data about this line.
     */
    @Override
    public void update() {
        updateLicense();
        _lastRating = -1f;
    }
    
    /**
     * Updates the license status.
     */
    private void updateLicense() {
        if(_doc != null && _sha1 != null) {
            String licenseString = _doc.getLicenseString();
            LicenseFactory factory = _doc.getLicenseFactory();
            if(licenseString != null) {
                if(factory.isVerifiedAndValid(_sha1, licenseString))
                    _licenseState = License.VERIFIED;
                else
                    _licenseState = License.UNVERIFIED;
                _licenseName = factory.getLicenseName(licenseString);
            }
        }
    }
    
    /** Determines if this TableLine is secure. */
    boolean isSecure() { 
        return _secure;
    }
    
    /**
     * Determines if a license is available.
     */
    boolean isLicenseAvailable() {
        return _licenseState != License.NO_LICENSE;
    }
    
    /**
     * Gets the license associated with this line.
     */
    License getLicense() {
        if(_doc != null && _sha1 != null)
            return _doc.getLicense();
        else
        return null;
    }
    
    /**
     * Gets the SHA1 urn of this line.
     */
    URN getSHA1Urn() { 
        return _sha1;
    }
    
    /**
     * Gets the speed of this line.
     */
    ResultSpeed getSpeed() {
        return _speed;
    }
    
    /**
     * Gets the creation time.
     */
    Date getAddedOn() {
        if(_addedOn > 0)
            return new Date(_addedOn);
        else
            return null;
    }
    
    /**
     * Gets the quality of this line.
     */
    int getQuality() {
        boolean downloading = RESULT.isDownloading();
        if(downloading != _downloading)
            updateFileStatus();
        _downloading = downloading;
        
        if(_savedFile)
            return QualityRenderer.SAVED_FILE_QUALITY;
        else if(downloading)
            return QualityRenderer.DOWNLOADING_FILE_QUALITY;
        else if(_incompleteFile)
            return QualityRenderer.INCOMPLETE_FILE_QUALITY;
        else if (_secure)
            return QualityRenderer.SECURE_QUALITY;
        else if (!isThirdPartyResult() && SearchSettings.ENABLE_SPAM_FILTER.getValue() && SpamFilter.isAboveSpamThreshold(this))
            return QualityRenderer.SPAM_FILE_QUALITY;
        else
            return _quality;
    }
    
    /**
     * Returns the NamedMediaType.
     */
    public NamedMediaType getNamedMediaType() {
        return _mediaType;
    }
    
    /**
     * Gets the LimeXMLDocument for this line.
     */
    public LimeXMLDocument getXMLDocument() {
        return _doc;
    }
    
    /**
     * Gets the EndpointHolder holding locations.
     */
    EndpointHolder getLocation() {
        return _location;
    }
    
    /**
     * Gets the other results for this line.
     */
    List<SearchResult> getOtherResults() {
    	if (_otherResults == null) {
    		return Collections.emptyList();
    	}
    	else {
    		return _otherResults;
    	}
    }
    
    /**
     * Gets the alternate locations for this line.
     */
    Set<? extends IpPort> getAlts() {
        if(_alts == null)
            return Collections.emptySet();
        else
            return _alts;
    }
    
    /**
     * Gets the number of locations this line holds.
     */
    int getLocationCount() {
        // The location will be null for store song or special lines.
        //
        if (_location == null)
            return 1;
        
        // the query result handler should know the number of
        // distinct locations in addition to the number of 
        // times the file is accessible based on the set of
        // all partial search results - that is, all hosts
        // that report having a subset of the file data.
        //
        return _location.getNumLocations();
    }
    
    /**
     * Returns the license name of null if File(s) have no license
     */
    String getLicenseName() {
        return _licenseName;
    }
    
    /**
     * Returns the SHA1 hash of this line.
     */
    public URN getSHA1() {
        return _sha1;
    }
    
    /**
     * Determines whether or not chat is enabled.
     */
    boolean isChatEnabled() {
        return _chatHost != null;
    }
    
    /**
     * Determines whether or not browse host is enabled.
     */
    boolean isBrowseHostEnabled() {
        return _browseHost != null;
    }
	
	/**
	 * Determines whether there is a non firewalled host for this result.
	 */
	boolean hasNonFirewalledRFD() {
		return _nonFirewalledHost != null;
	}
    
    /**
     * Determines if this line is launchable.
     */
    boolean isLaunchable() {
        return _doc != null && _doc.getAction() != null &&
                               !"".equals(_doc.getAction());
    }
    
    /**
     * Gets the filename without the extension.
     */
    String getFilenameNoExtension() {
        return RESULT.getFilenameNoExtension();
    }
    
    /**
     * Returns the icon & extension.
     */
    IconAndNameHolder getIconAndExtension() {
        String ext = getExtension();
        return new IconAndNameHolderImpl(getIcon(), ext);
    }
    
    /**
     * Returns the icon.
     */
    Icon getIcon() {
        // Return the yellow icon for the special results
        if (isThirdPartyResult()) {
            return new Icon() {
                private final Icon icon = GUIMediator.getThemeImage("external_link");
                
                public int getIconHeight() {
                    return icon.getIconHeight();
                }
                public int getIconWidth() {
                    return icon.getIconWidth();
                }
                public void paintIcon(Component c, Graphics g, int x, int y) {
                    icon.paintIcon(c, g, x, y);
                }
                @Override
                public String toString() {
                    return null;
                }
            };
        } else {
            String ext = getExtension();
            return IconManager.instance().getIconForExtension(ext);
        }
    }

    /**
     * Returns the extension of this result.
     */
    String getExtension() {
        return RESULT.getExtension();
    }

    /**
     * Returns this filename, as passed to the constructor.  Limitation:
     * if the original filename was "a.", the returned value will be
     * "a".
     */
    public String getFilename() {
        return RESULT.getFileName();
    }
    
    /**
     * Gets the size of this TableLine.
     */
    public long getSize() {
        return RESULT.getSize();
    }

    /**
     * Returns the vendor code of the result.
     */
    String getVendor() {
        return RESULT.getVendor();
    }
    
    /**
     * Gets the LimeTableColumn for this column.
     */
    public LimeTableColumn getColumn(int idx) {
        return COLUMNS.getColumn(idx);
    }
    
    /**
     * Returns the number of columns.
     */
    public int getColumnCount() {
        return SearchTableColumns.COLUMN_COUNT;
    }    
    
    /**
     * Determines if the column is dynamic.
     */
    public boolean isDynamic(int idx) {
        return false;
    }
    
    /**
     * Determines if the column is clippable.
     */
    public boolean isClippable(int idx) {
        switch(idx) {
        case SearchTableColumns.QUALITY_IDX: 
        case SearchTableColumns.COUNT_IDX:
        case SearchTableColumns.ICON_IDX: 
        case SearchTableColumns.CHAT_IDX:
        case SearchTableColumns.LICENSE_IDX:
            return false;
        default:
            return true;
        }
    }
    
    public int getTypeAheadColumn() {
        return SearchTableColumns.NAME_IDX;
    }

    /**
     * Gets the value for the specified idx.
     */
    public Object getValueAt(int index){
        switch (index) {
        case SearchTableColumns.QUALITY_IDX: return new Integer(getQuality());
        case SearchTableColumns.COUNT_IDX:
            int count = getLocationCount();
            if(count <= 1)
                return null;
            else
                return new Integer(count);
        case SearchTableColumns.ICON_IDX: return getIcon();
        case SearchTableColumns.NAME_IDX: return new ResultNameHolder(this);
        case SearchTableColumns.TYPE_IDX: return getExtension();
        case SearchTableColumns.SIZE_IDX: return new SizeHolder(getSize());
        case SearchTableColumns.SPEED_IDX: return getSpeed();
        case SearchTableColumns.CHAT_IDX: return isChatEnabled() ? Boolean.TRUE : Boolean.FALSE;
        case SearchTableColumns.LOCATION_IDX: return getLocation();
        case SearchTableColumns.VENDOR_IDX: return RESULT.getVendor();
        case SearchTableColumns.ADDED_IDX: return getAddedOn();
        case SearchTableColumns.LICENSE_IDX: return new NameValue.ComparableByName<Integer>(_licenseName, new Integer(_licenseState));
		case SearchTableColumns.SPAM_IDX: return new Float(getSpamRating());
        default:
            if(_doc == null || index == -1) // no column, no value.
                return null;
            XMLSearchColumn ltc = (XMLSearchColumn)getColumn(index);
            return new XMLValue(_doc.getValue(ltc.getId()), ltc.getSchemaFieldInfo());
        }
    }
    
    /**
     * Returns the XMLDocument as a tool tip.
     */
    @Override
    public String[] getToolTipArray(int col) {
        // only works on windows, which gives good toString descriptions
        // of its native file icons.
        if(col == SearchTableColumns.ICON_IDX && OSUtils.isWindows()) {
            Icon icon = getIcon();
            if(icon != null) {
                String str = icon.toString();
                if(str != null)
                    return new String[] { icon.toString() };
            }
            return null;
        }
        // if we're on the location column and we've got multiple results,
        // list them all out.
        if(col == SearchTableColumns.LOCATION_IDX && getLocationCount() > 1) {
            StringBuilder sb = new StringBuilder(3 * 23);
            List<String> retList = new LinkedList<String>();
            Iterator<String> iter = _location.getHosts().iterator();
            for(int i = 0; iter.hasNext(); i++) {
                if(i == 3) {
                    i = 0;
                    retList.add(sb.toString());
                    sb = new StringBuilder(3 * 23);
                } 
                sb.append(iter.next());
                if(iter.hasNext())
                    sb.append(", ");
                else
                    retList.add(sb.toString());
            }
            return retList.toArray(new String[retList.size()]);
        }
        
        List<String> tips = new LinkedList<String>();
        if(isSecure()) {
            tips.add(I18n.tr("This is a secure result. No information in this result has been tampered."));
            tips.add("");
        }
        
        if(isLink()) {
            tips.add("");
            tips.add(getLinkDisplayUrl());
            tips.add("");
        }
        
        if(_doc != null)
            tips.addAll(XMLUtils.getDisplayList(_doc));

        if (!tips.isEmpty() ) {
            // if it had data, display the filename in the tooltip also.
            tips.add(0, getFilenameNoExtension());
            return tips.toArray(new String[tips.size()]);
        } else {
            return null;
        }
    }
    
    /**
     * Returns <code>true</code> if <code>this</code> {@link SearchResult}
     * is the same kind as <code>line</code>'s, e.g. one from gnutella and
     * one from gnutella. Currently we compare classes.
     * 
     * @param line line to which we compare
     * @return <code>true</code> if <code>this</code> {@link SearchResult}
     *         is the same kind as <code>line</code>'s, e.g. one from
     *         gnutella and one from gnutella
     */
    public final boolean isSameKindAs(TableLine line) {
        return getSearchResult().getClass().equals(line.getSearchResult().getClass());
    }
    
    /**
     * Gets the main result's host.
     */
    String getHostname() {
        return RESULT.getHost();
    }
    
    /**
     * Gets all RemoteFileDescs for this line.
     */
    RemoteFileDesc[] getAllRemoteFileDescs() {
        if (isThirdPartyResult()) {
            return new RemoteFileDesc[0];
        }
        GnutellaSearchResult sr = (GnutellaSearchResult)RESULT;
        int size = getOtherResults().size() + 1;
        RemoteFileDesc[] rfds = new RemoteFileDesc[size];
        rfds[0] = sr.getRemoteFileDesc();
        int j = 1;
        for(Iterator i = getOtherResults().iterator(); i.hasNext(); j++)
            rfds[j] = ((GnutellaSearchResult)i.next()).getRemoteFileDesc();
        return rfds;
    }
    
    /**
     * Does a chat.
     */
    void doChat() {
// TODO		if (_chatHost != null && _chatHost.getAddress() != null 
// TODO				&& _chatHost.getPort() != -1) {
// TODO			GUIMediator.createChat(_chatHost.getAddress(), _chatHost.getPort());
// TODO	}
        throw new UnsupportedOperationException("old UI is broken");
    }
    
	/**
	 * Returns the rfd of the search result for which this download was enabled
	 * @return
	 */
	RemoteFileDesc getRemoteFileDesc() {
        return RESULT instanceof GnutellaSearchResult 
                ? ((GnutellaSearchResult)RESULT).getRemoteFileDesc() 
                : null;
	}
    
	/**
	 * Gets the spam rating
	 */
	float getSpamRating() {
	    if(_lastRating == -1f) {
	        _lastRating = RESULT.getSpamRating();
            // If there's more than one result, return the max spam rating
            if(_otherResults != null) {
                for(SearchResult r : _otherResults) {
                    if(r instanceof GnutellaSearchResult) {
                        GnutellaSearchResult g = (GnutellaSearchResult)r; 
                        float rating = g.getRemoteFileDesc().getSpamRating();
                        if(rating > _lastRating)
                            _lastRating = rating;
                    }
                }
            }
	    } 
	    return _lastRating;
	}
    
    /**
     * Gets the first browse-host enabled RFD or <code>null</code>.
     */
    RemoteFileDesc getBrowseHostEnabledRFD() {
        return _browseHost;
    }
	
	/**
	 * Returns the first non-firewalled rfd for this result or <code>null</code>.
	 */
	RemoteFileDesc getNonFirewalledRFD() {
		return _nonFirewalledHost;
	}
	
	/**
	 * Returns the first chat enabled rfd for this result or <code>null</code>.
	 */
	RemoteFileDesc getChatEnabledRFD() {
		return _chatHost;
	}
    
    /**
     * Returns the underlying search result.  This is needed by {@link StoreResultPanel}.
     * 
     * @return the underlying search result
     */
    public final SearchResult getSearchResult() {
        return RESULT;
    }
    
    /**
     * Returns the color for painting an even row. Delegates to the member
     * {@link SearchResult}.
     * 
     * @return the color for painting an even row. Delegates to the member
     *         {@link SearchResult}
     */
    public final Color getEvenRowColor() {
        return RESULT.getEvenRowColor();
    }
    
    /**
     * Returns the color for painting an odd row. Delegates to the member
     * {@link SearchResult}.
     * 
     * @return the color for painting an odd row. Delegates to the member
     *         {@link SearchResult}
     */
    public final Color getOddRowColor() {
        return RESULT.getOddRowColor();
    }

    /**
     * Delegate to the {@link #RESULT} to take some action, such as download or
     * display in browser, etc.
     * 
     * @see SearchResult#takeAction(TableLine, GUID, File, String, boolean,
     *      SearchInformation)
     */
    public final void takeAction(TableLine line, GUID guid, File saveDir, String fileName, boolean saveAs, SearchInformation searchInfo) {
        RESULT.takeAction(line, guid, saveDir, fileName, saveAs, searchInfo);
    }
    
    /* -----------------------------------------------------------------------------
     * These were exposed to give a {@link SearchResult} access to this
     * TableLine in initialization.
     * ----------------------------------------------------------------------------- 
     */
    
    /**
     * Sets the value for the chat host {@link RemoteFileDesc}.
     * 
     * @param rfd new chat host
     */
    final void setChatHost(RemoteFileDesc rfd) {
        _chatHost = rfd;
    }

    /**
     * Sets the new 'added on' date.
     * 
     * @param creationTime new 'added on' data
     */
    final void setAddedOn(long creationTime) {
        _addedOn = creationTime;
    }

    /**
     * Sets the value for the non-firewalled host {@link RemoteFileDesc}.
     * 
     * @param rfd new non-firewalled host
     */
    final void setNonFirewalledHost(RemoteFileDesc rfd) {
        _nonFirewalledHost = rfd;
    }

    /**
     * Sets the value for the browse host {@link RemoteFileDesc}.
     * 
     * @param rfd new browse host
     */
    final void setBrowseHost(RemoteFileDesc rfd) {
        _browseHost = rfd;
    }

    final void createEndpointHolder(String host, int port, boolean isReplyToMulticast) {
        _location = new EndpointHolder(host, port, isReplyToMulticast);
    }       
    

    final Set<IpPort> getAltIpPortSet() {
        if(_alts == null)
            _alts = new IpPortSet();
        return _alts;
    }

}
