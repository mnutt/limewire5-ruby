package com.limegroup.gnutella.gui.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.NameValue;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLDocumentFactory;

public final class PromotionSearchResultFactory {
    
    private static final Log LOG = LogFactory.getLog(PromotionSearchResultFactory.class);    
    
    /**
     * The factory we use to create a {@link LimeXMLDocument} when creating search results.
     */
    private final LimeXMLDocumentFactory limeXMLDocumentFactory;  
    
    PromotionSearchResultFactory(LimeXMLDocumentFactory limeXMLDocumentFactory) {
        this.limeXMLDocumentFactory = limeXMLDocumentFactory;
    }
    
    public SearchResult newSearchResult(Map<String,String> nameValuePairs, String query) {
        String url            = nameValuePairs.get(Attr.URL.getValue());
        int size              = (int)getOrDefaultWithMax(nameValuePairs, Attr.SIZE.getValue(), -1, Integer.MAX_VALUE);
        long creationTime     = getOrDefaultWithMax(nameValuePairs, Attr.CREATION_TIME.getValue(), 0, Long.MAX_VALUE);
        String vendor         = nameValuePairs.get(Attr.VENDOR.getValue());
        String name           = nameValuePairs.get(Attr.NAME.getValue());
        String fileType       = nameValuePairs.get(Attr.FILE_TYPE.getValue());
        String xmlSchema      = nameValuePairs.get(Attr.XML_SCHEMA.getValue());
        String displayUrl     = nameValuePairs.get(Attr.DISPLAY_URL.getValue());
        
        if(xmlSchema == null) {
            xmlSchema = "audio";
        }
        Collection<NameValue<String>> xmlValues = xmlValuesIn(nameValuePairs, xmlSchema);

        SearchResult sr = newSearchResult(name, fileType, xmlSchema, url, size, creationTime,
                vendor, xmlValues, query, displayUrl);
        return name != null ? sr : null;
    }
    
    private Collection<NameValue<String>> xmlValuesIn(Map<String, String> allNameValues,
            String xmlSchema) {
        String plural = xmlSchema + "s";
        List<NameValue<String>> xmlValues = new ArrayList<NameValue<String>>();
        for (Map.Entry<String, String> entry : allNameValues.entrySet()) {
            if (entry.getKey().startsWith("xml_")) {
                xmlValues.add(new NameValue<String>(plural + "__" + xmlSchema + "__"
                        + entry.getKey().substring(4) + "__", entry.getValue()));
            }
        }
        return xmlValues;
    } 

    private SearchResult newSearchResult(String name, String fileType, String xmlSchema,
            String url, int size, long creationTime, String vendor,
            Collection<NameValue<String>> xmlValues, String keyword, String displayUrl) {
        LimeXMLDocument xmlDoc = null;
        if (xmlValues.size() > 0) {
            try {
                xmlDoc = limeXMLDocumentFactory.createLimeXMLDocument(xmlValues,
                        "http://www.limewire.com/schemas/" + xmlSchema + ".xsd");
            } catch (IllegalArgumentException iae) {
                LOG.error("error creating document", iae);
            }
        }
        
        return new PromotionSearchResult(name, fileType, url, size, creationTime, xmlDoc, vendor,
                keyword, displayUrl);
    }    
    
    /**
     * This class contains the attributes to use as keys for getting a
     * retrieving values in a query. It is <u>not</u> an <code>enum</code>
     * because sometimes we read from a file to create the DB and we aren't
     * assured that the values of an <code>enum</code> would be created with
     * declaring a variable of that type. And still, the compiler could choose
     * to optimize that away and the instances would not be created.
     */
    public enum Attr {
        
        DISPLAY_URL("displayUrl"),
        XML_SCHEMA("xmlSchema"),
        URL("url"),
        SIZE("size"),
        CREATION_TIME("creation_time"),
        VENDOR("vendor"),
        NAME("name"),
        FILE_TYPE("fileType");
        
        private final String value;
        private Attr(String value) { 
            this.value = value; 
        }
        
        public String getValue() {
            return value;
        }
        
        @Override
        public String toString() {
            return value;
        }

    };    
        
    /**
     * Returns the value found in <code>nameValuePairs</code> or
     * 'def' if it's not found.  If the value is over 'max', 'max' is returned.
     * 
     * @param nameValuePairs name value pair mapping
     * @param attr name of the value to find in <code>nameValuePairs</code>
     */
    private long getOrDefaultWithMax(Map<String, String> nameValuePairs, String attr, long def, long max) {
        String value = nameValuePairs.get(attr);
        if (value == null)
            return def;
        try {
            return Math.min(max, Long.parseLong(value));
        } catch (NumberFormatException ignored) {}
        return def;
    }

}
