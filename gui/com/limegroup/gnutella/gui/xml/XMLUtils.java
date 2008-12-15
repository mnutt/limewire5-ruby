package com.limegroup.gnutella.gui.xml;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.util.CommonUtils;
import org.limewire.util.NameValue;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLUtils;
import com.limegroup.gnutella.xml.SchemaFieldInfo;
import com.limegroup.gnutella.xml.XMLStringUtils;

/**
 * Various GUI-related XML utilities.
 */
public class XMLUtils {
    
    private static final Log LOG = LogFactory.getLog(XMLUtils.class);
    
    /**
     * A mapping of Schema -> ResourceBundle for that schema.
     * These ResourceBundles are only used (and only created)
     * if someone attempts to retrieve a resource that does not exist
     * in the main ResourceBundle.
     */
    private static Map<String, ResourceBundle> bundles;
    
    private XMLUtils() {}
    
    /** Returns a List of "Name: Value" Strings from the given document, suitable for display. */
    public static List<String> getDisplayList(LimeXMLDocument doc) {
        Map<String, String> map = getDisplayProperties(doc);
        List<String> data = new LinkedList<String>();
        for (Map.Entry<String, String> entry : map.entrySet())
            data.add(entry.getKey() + ": " + entry.getValue());
        return data;
    }

    /** Returns a Map of the given document's names and values, suitable for display. */
    public static Map<String, String> getDisplayProperties(LimeXMLDocument doc) {
        Map<String, String> map = new LinkedHashMap<String, String>(); // Keep insertion order
        
        LimeXMLSchema schema = doc.getSchema();
        List<SchemaFieldInfo> fields = schema.getCanonicalizedFields();
        
        // For each name/value pair...
        for(SchemaFieldInfo sfi : fields) {
            String name = sfi.getCanonicalizedFieldName();
            String value = doc.getValue(name);
            if(value == null || sfi.isHidden())
                continue;
            
            NameValue<String> pair = getDisplayPair(sfi, value, schema);

            map.put(pair.getName(), pair.getValue());
        }
        
        return map;
    }

	/**
	 * Returns the NameValue pair of the display name for the field
	 * and a valid visual representation of the value.
	 */
	public static NameValue<String> getDisplayPair(SchemaFieldInfo field, String value, LimeXMLSchema schema) {
		String name = getResource(field.getCanonicalizedFieldName());
        value = getDisplay(field, value);
		return new NameValue<String>(name, value);
	}
    
    /**
     * Returns the String that should be used to display a field.
     */
    public static String getDisplay(SchemaFieldInfo field, String value) {
        if(field.getJavaType() == Date.class) {
            try {
                value = CommonUtils.seconds2time(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {}
        } else {
            // Try and convert any &XXX; to the right thing.
            value = LimeXMLUtils.unencodeXML(value);
        }
        return value;
    }
    
    /**
     * Returns the value as a Comparable.
     */
    public static Comparable<?> getComparable(SchemaFieldInfo field, String value) {
        if(field == null || value == null) {
            return null;
        } else if(field.getJavaType() == Integer.class || field.getJavaType() == Date.class) {
            try {
              return Integer.valueOf(value);  
            } catch(NumberFormatException nfe) {
                return null;
            }
        } else {
            return value;
        }
    }
	
	/**
	 * Gets the resource for the given string.
	 */
	public static String getResource(String field) {
	    String name = LimeXMLNames.getDisplayName(field);
	    if (name != null) {
	        return I18n.tr(name);
	    }
	        if(LOG.isWarnEnabled())
	            LOG.warn("Missing resource for '" + field + "', falling back to default");
	    return fallbackResource(field, getBundleForField(field));
    }
    
    /**
     * Gets the title of the schema based on the field.
     */
    public static String getTitleForSchemaFromField(String field) {
        // not an XML field?  ignore.
        if(!field.endsWith("__"))
            return null;
            
        // The canonicalKey is always going to be x__x__<other stuff here>
        int idx1 = field.indexOf(XMLStringUtils.DELIMITER) + 2;
        int idx2 = field.indexOf(XMLStringUtils.DELIMITER, idx1);
        return getResource(field.substring(0, idx2));
    }
        
    
    /**
     * Gets the correct display name for the given schemaURI.
     */
    public static String getTitleForSchemaURI(String schemaURI) {
        LimeXMLSchema schema = GuiCoreMediator.getLimeXMLSchemaRepository().getSchema(schemaURI);
        if(schema != null)
            return getTitleForSchema(schema);
        else
            return null;
    }
    
    /**
     * Gets the correct display name for the given schema.
     */
    public static String getTitleForSchema(LimeXMLSchema schema) {
        return getResource(schema.getRootXMLName() + XMLStringUtils.DELIMITER + schema.getInnerXMLName());
    }
    
    /**
     * Gets the resource bundle for the given field name.
     */
    private static ResourceBundle getBundleForField(String field) {
        if(bundles == null)
            loadBundles();
            
        return bundles.get(getDescriptionFromField(field));
    }
    
    /**
     * Gets the description of the schema from a field name.
     *
     * That is, where the field is called audios__audio__field,
     * the description this returns is "audio".
     */
    private static String getDescriptionFromField(String field) {
        // The canonicalKey is always going to be x__x__<other stuff here>
        int idx1 = field.indexOf(XMLStringUtils.DELIMITER) + 2;
        int idx2 = field.indexOf(XMLStringUtils.DELIMITER, idx1);
        if(idx2 == -1)
            idx2 = field.length();
        return field.substring(idx1, idx2);
    }
    
    /**
     * Populates the bundles map with the resource bundles we know about.
     */
    private static void loadBundles() {
        bundles = new HashMap<String, ResourceBundle>();
        Collection<LimeXMLSchema> schemas = GuiCoreMediator.getLimeXMLSchemaRepository().getAvailableSchemas();
        for(LimeXMLSchema schema : schemas) {
            String key = schema.getDescription();
            try {
                bundles.put(key, GUIMediator.getXMLResourceBundle(key));
            } catch(MissingResourceException mre) {
                if(LOG.isWarnEnabled())
                    LOG.warn("Missing resource bundle for schema: " + key, mre);
            }
        }
    }   
    
    /**
     * Gets the resource from the XML resource bundles for that field.
     */
    private static String fallbackResource(String field, ResourceBundle bundle) {
        if(bundle != null) {
            try {
                return bundle.getString(field);
            } catch(MissingResourceException mre) {
                if(LOG.isWarnEnabled())
                    LOG.warn("Missing fallback resource for: " + field + ", capitalizing field name.", mre);
            }
        }
        
        return processField(field);
    }
    
    /**
     * Capitalizes the first letter of the string and converts '_' to ' '.
     *
     * That is, where the name is "field_name", this will return "Field name".
     */
    private static String formatFieldName(String name) {
        return name.substring(0, 1).toUpperCase(Locale.US) + name.substring(1).replace('_', ' ').trim();
    }

    /**
     * Determines the field's name based on the field.
     *
     * That is, where the field is audios__audio__field,
     * this will return "Field".
     */
    private static String processField(String field) {
        int endIdx, startIdx;
        if(field.endsWith(XMLStringUtils.DELIMITER))
            endIdx = field.length() - 2; // 2 == XMLStringUtils.DELIMITER.length()
        else
            endIdx = field.length();
            
        startIdx = field.lastIndexOf(XMLStringUtils.DELIMITER, endIdx-1) + 2;
        return formatFieldName(field.substring(startIdx, endIdx));
    }
}