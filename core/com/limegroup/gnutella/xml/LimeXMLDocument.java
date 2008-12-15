package com.limegroup.gnutella.xml;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.NameValue;
import org.limewire.util.RPNParser.StringLookup;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.inject.Provider;
import com.limegroup.gnutella.licenses.CCConstants;
import com.limegroup.gnutella.licenses.License;
import com.limegroup.gnutella.licenses.LicenseFactory;
import com.limegroup.gnutella.licenses.LicenseType;

/**
 * A LimeXMLDocument is basically a hashmap that maps a
 * Names of fields to the values as per a XML document.
 * 
 * @author  Sumeet Thadani
 */
public class LimeXMLDocument implements StringLookup {
    
    private static final Log LOG = LogFactory.getLog(LimeXMLDocument.class);

    private static final String XML_ID_ATTRIBUTE = "identifier__";
    private static final String XML_ACTION_ATTRIBUTE = "action__";
    private static final String XML_ACTION_INFO = "addactiondetail__";
            static final String XML_INDEX_ATTRIBUTE = "index__";
    private static final String XML_LICENSE_ATTRIBUTE = "license__";
    private static final String XML_LICENSE_TYPE_ATTRIBUTE = "licensetype__";
    private static final String VERSION_STRING = "internal_version";
    private static final String XML_VERSION_ATTRIBUTE = VERSION_STRING + "__";
    
    public static final List<LimeXMLDocument> EMPTY_LIST = Collections.emptyList();
    
    /**
     * The current version of LimeXMLDocuments.
     *
     * Increment this number as features are added which require
     * reparsing documents on disk.
     */
    private static final int CURRENT_VERSION = 3;

	/**
	 * Cached hash code for this instance.
	 */
	private volatile int hashCode = 0;

	/**
     * Map of canonical attribute name -> value.
     */
    private Map<String, String> fieldToValue = new HashMap<String, String>();
    
    /**
     * The schema of this LimeXMLDocument.
     */
    private String schemaUri;
    
    /**
     * The cached string of attributes.
     */
    private String attributeString;

    /** 
     * The file this is related to.  Can be null if pure meta-data.
     */
    private File fileId;
    
    /**
     * The action that this doc has.
     */
    private String action;
    
    private String actionDetail;
    
    /**
     * The version of this LimeXMLDocument.
     */
    private volatile int version;
    
    /**
     * Cached list of keywords.  Because keywords are only filled up
     * upon construction, they can be cached upon retrieval.
     */
    private List<String> CACHED_KEYWORDS = null;
    
    /** The kind of license this has. */
    private volatile LicenseType licenseType = LicenseType.NO_LICENSE;

    private final LicenseFactory licenseFactory;
    private final Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository;
    
    /**
     * Constructs a LimeXMLDocument with the given string.
     */
    LimeXMLDocument(String xml, LicenseFactory licenseFactory,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) throws SAXException,
            SchemaNotFoundException, IOException {
        this.licenseFactory = licenseFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        if (xml == null || xml.equals(""))
            throw new SAXException("null or empty string");

        InputSource doc = new InputSource(new StringReader(xml));
        XMLParsingUtils.ParseResult result = XMLParsingUtils.parse(doc);
        if (result.isEmpty())
            throw new IOException("No element present");
        if (result.schemaURI == null)
            throw new SchemaNotFoundException("no schema");

        this.fieldToValue = result.get(0);
        this.schemaUri = result.schemaURI;
        setFields(result.canonicalKeyPrefix);
        
        if(!isValid())
            throw new IOException("Invalid XML: " + xml + ", fieldToValue: " + fieldToValue + ", attrString: " + getAttributeString() + ", schemaURI: " + schemaUri);
    }

    /**
     * Constructs a new LimeXMLDocument
     * @param map Map with keys in canonicalized
     * form and corresponding values that will be used to create the 
     * new instance
     * @param schemaURI The schema URI for the LimeXMLDocument to be
     * created
     */    
    LimeXMLDocument(Map<String, String> map, String schemaURI, String keyPrefix,
            LicenseFactory licenseFactory, Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository)
            throws IOException {
        this.licenseFactory = licenseFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        if(map.isEmpty())
            throw new IllegalArgumentException("empty map");

        this.schemaUri = schemaURI;
        this.fieldToValue = map;
        fieldToValue.remove(keyPrefix + XML_ID_ATTRIBUTE); // remove id.
        setFields(keyPrefix);
        
        if(!isValid())
            throw new IOException("invalid doc! "+map+" \nschema uri: "+schemaURI);
        
    }

    /**
     * Constructs a new LimeXMLDocument
     * @param nameValueList List (of Map.Entry) of fieldnames (in canonicalized
     * form) and corresponding values that will be used to create the 
     * new instance
     * @param schemaURI The schema URI for the LimeXMLDocument to be
     * created
     */
    LimeXMLDocument(Collection<? extends Map.Entry<String, String>> nameValueList,
            String schemaURI, LicenseFactory licenseFactory,
            Provider<LimeXMLSchemaRepository> limeXMLSchemaRepository) {
        this.licenseFactory = licenseFactory;
        this.limeXMLSchemaRepository = limeXMLSchemaRepository;
        if(nameValueList.isEmpty())
            throw new IllegalArgumentException("empty list");

        //set the schema URI
        this.schemaUri = schemaURI;
                
        //iterate over the passed list of fieldnames & values
        for(Map.Entry<String, String> next : nameValueList)
            fieldToValue.put(next.getKey().trim(), next.getValue());
        
        // scan for action/id/etc..
        scanFields();
        
        if(!isValid()) {
            throw new IllegalArgumentException("Invalid Doc!  nameValueList: " + nameValueList + ", schema: " + schemaURI + ", attributeStrings: " + getAttributeString() + ", schemaFields: " + ((getSchema() != null) ? Arrays.asList(getSchema().getCanonicalizedFieldNames()) : "n/a"));
        }
    }
    
    /**
     * Determines whether or not this LimeXMLDocument is valid.
     */
    boolean isValid() {
        // no schemaURI or the schemaURI doesn't map to a LimeXMLSchema
        if(schemaUri == null || getSchema() == null)
            return false;

        // no valid attributes.
        if(getAttributeString().length() == 0)
            return false;
            
        return true;
    }

    /**
     * Returns the number of fields this document has.
     */
    public int getNumFields() {
        return fieldToValue.size();
    }

    /**
     * Returns all the non-numeric fields in this.  These are
     * not necessarily QRP keywords.  For example, one of the
     * elements of the returned list may be "Some comment-blah".
     * QRP code may want to split this into the QRP keywords
     * "Some", "comment", and "blah".
     *
     * Indivisible keywords are not returned.  To retrieve those,
     * use getIndivisibleKeywords().  Indivisible keywords are
     * those which QRP will not split up.
     */
    public List<String> getKeyWords() {
        if( CACHED_KEYWORDS != null )
            return CACHED_KEYWORDS;

        List<String> retList = new ArrayList<String>();
        for(Map.Entry<String, String> entry : fieldToValue.entrySet()) {
            String currKey = entry.getKey();
            String val = entry.getValue();
            if(val != null && !val.equals("") && !isIndivisible(currKey)) {
                try {
                    Double.parseDouble(val); // will trigger NFE.
                } catch(NumberFormatException ignored) {
                    retList.add(val);
                }
            }
        }
        CACHED_KEYWORDS = retList;
        return retList;
    }

    /**
     * Returns all the indivisible keywords for entry into QRP tables.
     */
    public List<String> getKeyWordsIndivisible() {
        return licenseType.getIndivisibleKeywords();
    }

    /**
     * Determines if this keyword & value is indivisible
     * (thus making QRP not split it).
     */
    private boolean isIndivisible(String currKey) {
        //the license-type is always indivisible.
        //note that for weed licenses, this works because getKeyWordsIndivisible
        //is returning a list of only 'WeedInfo.LAINFO'.  the content-id & version-id
        //are essentially lost & ignored.
        return currKey.endsWith(XML_LICENSE_TYPE_ATTRIBUTE);
    }    

    /**
     * Returns the unique identifier which identifies the schema this XML
     * document conforms to
     */
    public String getSchemaURI() {
        return schemaUri;
    }
    
    /**
     * Returns the LimeXMLSchema associated with this XML document.
     */
    public LimeXMLSchema getSchema() {
        return limeXMLSchemaRepository.get().getSchema(schemaUri);
    }
    
    /**
     * Returns the description of the schema URI.
     */
    public String getSchemaDescription() {
        LimeXMLSchema schema = getSchema();
        if(schema != null)
            return schema.getDescription();
        else
            return LimeXMLSchema.getDisplayString(schemaUri);
    }
    
    /**
     * Returns the name of the file that the data in this XML document 
     * corresponds to. If the meta-data does not correspond to any file
     * in the file system, this method will rerurn a null.
     */
    public File getIdentifier() {
        return fileId;
    }
    
    /**
     * Sets the identifier.
     */
    public void initIdentifier(File id) {
    	//assert fileId == null;
        fileId = id;
    }

    /**
     * Returns the action corresponding with this LimeXMLDocument.
     */
    public String getAction() {
        if(action == null)
            return "";
        else
            return action;
    }
    
    public boolean actionDetailRequested() {
        return "true".equalsIgnoreCase(actionDetail);
    }

    /**
     * Returns a Set of Map.Entry, where each key-value corresponds to a
     * Canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     * <p>
     * Canonicalization:
     * <p>
     * So as to preserve the structure, Structure.Field will be represented as
     * Structure__Field (Double Underscore is being used as a delimiter to
     * represent the structure).
     *<p>
     * In case of multiple structured values with same name,
     * as might occur while using + or * in the regular expressions in schema,
     * those should be represented as using the array index using the __
     * notation (withouth the square brackets)
     * for e.g. myarray[0].name ==> myarray__0__name
     *
     * attribute names for an element in the XML schema should be postfixed 
     * with __ (double underscore).
     * So element.attribute ==> element__attribute__
     *
     * @return a Set of Map.Entry, where each key-value corresponds to a
     * canonicalized field name (placeholder), and its corresponding value in
     * the XML Document.
     */
    public Set<Map.Entry<String, String>> getNameValueSet() {
        return fieldToValue.entrySet();
    }
    
    /**
     * Returns a set of the names within this LimeXMLDocument.
     */
    public Set<String> getNameSet() {
        return fieldToValue.keySet();
    }
    
    /**
     * Returns a collection of the values of this LimeXMLDocument.
     */
    public Collection<String> getValueList() {
        return fieldToValue.values();
    }
    
    /**
     * Determines if a license exists that this LimeXMLDocument knows about.
     */
    public boolean isLicenseAvailable() {
        return licenseType != LicenseType.NO_LICENSE;
    }
    
    /**
     * Returns a string that can be used to verify if this license is valid.
     */
    public String getLicenseString() {
        if(isLicenseAvailable()) {
            String licenseStringSuffix = getVerifiableLicenseElement(licenseType);
            if (licenseStringSuffix == null)
                return null;
            for(Map.Entry<String, String> next : fieldToValue.entrySet()) {
                String key = next.getKey();
                if (key.endsWith(licenseStringSuffix))
                    return next.getValue();
            }
        }
        return null;
    }
    
    private static String getVerifiableLicenseElement(LicenseType type) {
        if (type == LicenseType.CC_LICENSE)
            return LimeXMLDocument.XML_LICENSE_ATTRIBUTE;
        if (type.isDRMLicense())
            return LimeXMLDocument.XML_LICENSE_TYPE_ATTRIBUTE;
        return null;
    }
    
    /**
     * Returns the license.
     */
    public License getLicense() {
        String license = getLicenseString();
        if(license != null)
            return licenseFactory.create(license);
        else
            return null;
    }

    /**
     * Returns a list of attributes and their values in the same order
     * as is in the schema.
     */
    public List<NameValue<String>> getOrderedNameValueList() {
        if(getSchema() == null)
            return Collections.emptyList();
        
        String[] fNames = getSchema().getCanonicalizedFieldNames();
        List<NameValue<String>> retList = new ArrayList<NameValue<String>>(fNames.length);
        for (int i = 0; i < fNames.length; i++) {
            String name = fNames[i].trim();
            String value = fieldToValue.get(name);
            if (value != null)
                retList.add(new NameValue<String>(name, value));
        }
            
        return retList;
    }
    
    /**
     * Returns the value associated with this canonicalized fieldname.
     */
    public String getValue(String fieldName) {
        return fieldToValue.get(fieldName);
    }
    
    /**
     * Constructs an XML string from this document.
     */
    public String getXMLString() {
        StringBuilder fullXML = new StringBuilder();
        LimeXMLDocumentHelper.buildXML(fullXML, getSchema(), getAttributeString() + "/>");
        return fullXML.toString();
    }
    
    /**
     * Returns the attribute string with the given index.
     *
     * For example, this will return:
     *   <thing att1="value1" att2="value2" att3="value3" index="4"/>
     */
    public String getAttributeStringWithIndex(int i) {
        String attributes = getAttributeString();
        return attributes + " index=\"" + i + "\"/>";
    }
    
    /**
     * Returns the attribute string. THIS IS NOT A FULL XML ELEMENT.
     * It is purposely left unclosed so an index can easily be inserted.
     */
    private String getAttributeString() {
        if(attributeString == null)
            attributeString = constructAttributeString();
        return attributeString;
    }

    /**
     * Retrieves the XML of this, with the version embedded. This is useful for
     * serializing the XML.
     */
    String getXmlWithVersion() {
        StringBuilder fullXML = new StringBuilder();
        LimeXMLDocumentHelper.buildXML(fullXML, getSchema(), getAttributeString() + " " + VERSION_STRING + "=\"" + version + "\"/>");
        return fullXML.toString();
    }
    
    /** Determines if this XML was built with the current version. */
    boolean isCurrent() { return version == CURRENT_VERSION; }
    
    /** Sets this XML to be current. */
    void setCurrent() { version = CURRENT_VERSION; }
    
    /**
     * Constructs the open-ended XML that contains the attributes.
     * This is purposely open-ended so that an index can easily be
     * inserted.
     * If no attributes exist, this returns an empty string,
     * to easily be marked as invalid.
     */
    private String constructAttributeString() {
        List<NameValue<String>> attributes = getOrderedNameValueList();
        if(attributes.isEmpty())
            return ""; // invalid.
            
        StringBuilder tag = new StringBuilder();
        String root = getSchema().getRootXMLName();
        String type = getSchema().getInnerXMLName();
        String canonicalKey = root + "__" + type + "__";
        tag.append("<");
        tag.append(type);

        for(NameValue<String> nv : attributes) {
            String name = XMLStringUtils.getLastField(canonicalKey, nv.getName());
            if(name == null)
                continue;
            // Construct: ' attribute="value"'
            tag.append(" ");
            tag.append(name);
            tag.append("=\"");
            tag.append(LimeXMLUtils.encodeXML(nv.getValue()));
            tag.append("\"");
        }
        return tag.toString();
    }

	/**
	 * Overrides equals to check for equality of all xml document fields.
	 *
	 * @param o the object to compare
	 * @return <tt>true</tt> if the objects are equal, <tt>false</tt>
	 *  otherwise
	 */
	@Override
    public boolean equals(Object o) {
		if(o == this)
		    return true;
		if(o == null)
		    return false;
		if(!(o instanceof LimeXMLDocument))
		    return false;

		LimeXMLDocument xmlDoc = (LimeXMLDocument)o;
		return ((schemaUri == null ? xmlDoc.schemaUri == null :
				 schemaUri.equals(xmlDoc.schemaUri)) &&
				(action == null ? xmlDoc.action == null :
				 action.equals(xmlDoc.action)) &&
                 (actionDetail == null ? xmlDoc.actionDetail == null :
                     actionDetail.equals(xmlDoc.actionDetail)) &&
				(fieldToValue == null ? xmlDoc.fieldToValue == null : 
				 fieldToValue.equals(xmlDoc.fieldToValue)) &&
				 version == xmlDoc.version);
	}

	/**
	 * Overrides <tt>Object.hashCode</tt> to satisfy the contract for
	 * hashCode, given that we're overriding equals.
	 *
	 * @return a hashcode for this object for use in hash-based collections
	 */
	@Override
    public int hashCode() {
		if(hashCode == 0) {
			int result = 17;
			if(fieldToValue != null)
				result = 37*result + fieldToValue.hashCode();
			if(schemaUri != null)
				result = 37*result + schemaUri.hashCode();
			if(action != null)
				result = 37*result + action.hashCode();
            if (actionDetail != null)
                result = 37*result + actionDetail.hashCode();
			hashCode = result;
		} 
		return hashCode;
	}
	
	/**
	 * Returns the XML identifier for the string.
	 */
	@Override
    public String toString() {
	    return getXMLString();
    }
    
    /**
     * Looks in the fields for the ACTION, IDENTIFIER, and INDEX, and a license.
     * Action is stored, index & identifier are removed.
     */
    private void scanFields() {
        String canonicalKey = getCanonicalKey(getNameValueSet());
        if(canonicalKey == null)
            return;

        setFields(canonicalKey);
        fieldToValue.remove(canonicalKey + XML_INDEX_ATTRIBUTE);
        fieldToValue.remove(canonicalKey + XML_ID_ATTRIBUTE);
    }
    
    /**
     * Stores whether or not an action or CC license are in this LimeXMLDocument.
     */
    private void setFields(String prefix) {
        // store action.
        action = fieldToValue.get(prefix + XML_ACTION_ATTRIBUTE);
        actionDetail = fieldToValue.get(prefix + XML_ACTION_INFO);

        // deal with updating license_type based on the license
        String license = fieldToValue.get(prefix + XML_LICENSE_ATTRIBUTE);
        String type = fieldToValue.get(prefix + XML_LICENSE_TYPE_ATTRIBUTE);
        
        if(LOG.isDebugEnabled())
            LOG.debug("type: " + type);
        
        // Do specific stuff on licenseType for various licenses.
        // CC licenses require that the 'license' field has the CC_URI_PREFIX & CC_URL_INDICATOR
        // somewhere.  Weed licenses require that the 'license type' field has WeedInfo.LINFO,
        // a content id & a version id.
        licenseType = LicenseType.determineLicenseType(license, type);        
        if (licenseType == LicenseType.CC_LICENSE) {
            fieldToValue.put(prefix + XML_LICENSE_TYPE_ATTRIBUTE, CCConstants.CC_URI_PREFIX);
        }        
        if (licenseType == LicenseType.LIMEWIRE_STORE_PURCHASE) {
            fieldToValue.put(prefix + XML_LICENSE_TYPE_ATTRIBUTE,
                    LicenseType.LIMEWIRE_STORE_PURCHASE.toString());
        }
        
        // Grab the version, if it exists.
        String versionString = fieldToValue.get(prefix + XML_VERSION_ATTRIBUTE);
        if(versionString != null) {
            try {
                version = Integer.parseInt(versionString);
                if(LOG.isDebugEnabled())
                    LOG.debug("Set version to: " + version);
            } catch(NumberFormatException nfe) {
                LOG.warn("Unable to set version", nfe);
                version = CURRENT_VERSION;
            }
        } else {
            version = CURRENT_VERSION;
        }
        fieldToValue.remove(prefix + XML_VERSION_ATTRIBUTE);
        
        if(LOG.isDebugEnabled())
            LOG.debug("Fields after setting: " + fieldToValue);
    }
    
    /**
     * Derives a canonicalKey from a collection of Map.Entry's.
     */
    private String getCanonicalKey(Collection<? extends Map.Entry<String, String>> entries) {
        if(entries.isEmpty())
            return null;
        Map.Entry<String, String> firstEntry = entries.iterator().next();
        String firstKey = firstEntry.getKey();
        
        // The canonicalKey is always going to be x__x__<other stuff here>
        int idx = firstKey.indexOf(XMLStringUtils.DELIMITER);
        idx = firstKey.indexOf(XMLStringUtils.DELIMITER, idx+1);
        // not two delimiters? can't find the canonicalKey
        if(idx == -1)
            return null;
            
        // 2 == XMLStringUtils.DELIMITER.length()
        return firstKey.substring(0, idx + 2);
    }
    
    public String lookup(String key) {
        if (key == null)
            return null;
        if ("schema".equals(key))
            return getSchemaDescription();
        if ("numKWords".equals(key))
            return String.valueOf(getKeyWords().size());
        if ("numKWordsI".equals(key))
            return String.valueOf(getKeyWordsIndivisible().size());
        if ("numFields".equals(key))
            return String.valueOf(getNumFields());
        if ("ver".equals(key))
            return String.valueOf(version);
        if (key.startsWith("field_")) {
            key = key.substring(6,key.length());
            return getValue(key);
        }
        return null;
    }
    
    public LicenseFactory getLicenseFactory() {
        return licenseFactory;
    }
}

