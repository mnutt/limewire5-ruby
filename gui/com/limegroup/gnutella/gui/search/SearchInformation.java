package com.limegroup.gnutella.gui.search;


import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.limewire.io.IpPort;
import org.limewire.io.IpPortImpl;
import org.limewire.util.MediaType;

import com.limegroup.gnutella.gui.I18n;

/**
 * Simple struct-like class containing information about a search.
 */
public class SearchInformation {
    /** Constants used to serialize the search information to Map. */
    /** Key in map which holds property {@link #type}. */
    private static final String MAP_TYPE  = "type";
    /** Key in map which holds property {@link #query}. */
    private static final String MAP_QUERY = "query";
    /** Key in map which holds property {@link #xml}. */
    private static final String MAP_XML   = "xml";
    /** Key in map which holds property {@link #media}. */
    private static final String MAP_MEDIA = "media";
    /** Key in map which holds property {@link #title}. */
    private static final String MAP_TITLE = "title";
    
    /**
     * A keyword search.
     */
    public static final int KEYWORD = 0;
    
    /**
     * A what is new search.
     */
    public static final int WHATS_NEW = 1;
    
    /**
     * A browse host search.
     */
    public static final int BROWSE_HOST = 2;
    
    /**
     * The string to use to describe a what's new search.
     */
    public static final String WHATS_NEW_DESC =
        I18n.tr("New");
    
    /**
     * The kind of search this is.
     */
    private final int type;
    
    /**
     * The simple query string.
     */
    private final String query;
    
    /**
     * The XML string.
     */
    private final String xml;
    
    /**
     * The MediaType of the search.
     */
    private final MediaType media;

	/**
	 * The title of this search as it is displayed to the user. 
	 */
	private final String title;
    
    /**
     * Private constructor -- use factory methods instead.
     * @param title can be <code>null</code>, then the query is used.
     */
    private SearchInformation(int type, String query, String xml,
                              MediaType media, String title) {
        if(media == null)
            throw new NullPointerException("null media");
        if(query == null)
            throw new NullPointerException("null query");
        this.type = type;
        this.query = query.trim();
        this.xml = xml;
        this.media = media;
		this.title = title != null ? title : query;
    }
	
	private SearchInformation(int type, String query, String xml,
			MediaType media) {
		this(type, query, xml, media, null);
	}
    
    /**
     * Creates a new keyword, but state is loaded from Map generated
     * by {@link toMap()}.
     * @param map The map with storred state.
     * @see toMap()
     * @see fromMap(Map)
     */
    private SearchInformation(Map map) {
        Integer type = (Integer) map.get(MAP_TYPE);
        if ( type == null )
            throw new NullPointerException("null type");
        this.type = type.intValue();

        query = (String) map.get(MAP_QUERY);
        xml   = (String) map.get(MAP_XML);
        media = (MediaType) map.get(MAP_MEDIA);
        title = (String) map.get(MAP_TITLE);

        if(media == null)
            throw new NullPointerException("null media");
        if(query == null)
            throw new NullPointerException("null query");
    }
    
    
    /**
     * Creates a keyword search.
     */
    public static SearchInformation createKeywordSearch(String query,
                                                 String xml,
                                                 MediaType media) {
        return new SearchInformation(KEYWORD, query, xml, media);
    }
	
	/**
	 * Creates a keyword search with a title different from the query string.
	 * @param query
	 * @param xml
	 * @param media
	 * @param title
	 * @return
	 */
	public static SearchInformation createTitledKeywordSearch(String query,
			String xml, MediaType media, String title) {
		return new SearchInformation(KEYWORD, query, xml, media, title);
	}
    
    /**
     * Creates a what's new search.
     */
    public static SearchInformation createWhatsNewSearch(String name, MediaType type){
        return new SearchInformation(WHATS_NEW, 
            WHATS_NEW_DESC + " - " + name, null, type);
    }
    
    /**
     * Create's a browse host search.
     */
    public static SearchInformation createBrowseHostSearch(String desc) {
        return new SearchInformation(BROWSE_HOST, desc, null, 
            MediaType.getAnyTypeMediaType());
    }
    
    /**
     * Retrieves the basic query of the search.
     */
    public String getQuery() {
        return query;
    }
    
    /**
     * Retrieves the XML portion of the search.
     */
    public String getXML() {
        return xml;
    }
    
    /**
     * Retrieves the MediaType of the search.
     */
    public MediaType getMediaType() {
        return media;
    }
    
	public String getTitle() {
		return title;
	}
	
    /**
     * Gets the IP/Port if this is a browse-host.
     */
    IpPort getIpPort() {
        if(!isBrowseHostSearch())
            throw new IllegalStateException();

        StringTokenizer st = new StringTokenizer(getQuery(), ":");
        String host = null;
        int port = 6346;
        if (st.hasMoreTokens())
            host = st.nextToken();
        if (st.hasMoreTokens()) {
            try {
                port = Integer.parseInt(st.nextToken());
            } catch(NumberFormatException ignored) {}
        }
        
        return new IpPortImpl(InetSocketAddress.createUnresolved(host, port), host);
    }
    
    /**
     * Determines whether or not this is an XML search.
     */
    public boolean isXMLSearch() {
        return xml != null && xml.length() > 0;
    }
    
    /**
     * Determines if this is a keyword search.
     */
    public boolean isKeywordSearch() {
        return type == KEYWORD;
    }
    
    /**
     * Determines if this is a what's new search.
     */
    public boolean isWhatsNewSearch() {
        return type == WHATS_NEW;
    }
    
    /**
     * Determines if this is a browse host search.
     */
    public boolean isBrowseHostSearch() {
        return type == BROWSE_HOST;
    }
    
    /** Returns a string representation of the SearchInfo. */
    @Override
    public String toString() {
        return toMap().toString();
    }
    
    /**
     * Converts state storred in the object into map. In this way, state
     * can be storred in classes unawre of this class existance.
     * @return A map which holds parameters of the class.
     * @see fromMap()
     * @see SearchInformation(Map)
     */
    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<String, Serializable>(5);
        map.put(MAP_TYPE, new Integer(type));
        map.put(MAP_QUERY, query);
        map.put(MAP_XML, xml);
        map.put(MAP_MEDIA, media);
        map.put(MAP_TITLE, title);
        return map;
    }

    /**
     * Creates a new keyword, but state is loaded from map generated
     * by {@link toMap()}.
     * @param map The map with storred state.
     * @see toMap()
     * @see SearchInformation(Map)
     */
    public static SearchInformation createFromMap(Map map) {
        return new SearchInformation(map);
    }
}
