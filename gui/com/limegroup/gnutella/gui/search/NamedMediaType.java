package com.limegroup.gnutella.gui.search;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.limewire.util.MediaType;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.tables.IconAndNameHolder;
import com.limegroup.gnutella.gui.xml.XMLUtils;
import com.limegroup.gnutella.xml.LimeXMLSchema;

/**
 * Associates a MediaType with a LimeXMLSchema.
 *
 * Also contains factory methods for retrieving all media types,
 * and retrieving the media type associated with a specific TableLine.
 */
public class NamedMediaType implements IconAndNameHolder, Comparable<NamedMediaType> {

    /**
     * The cached mapping of description -> media type,
     * for easy looking up from incoming results.
     */
    private static final Map<String, NamedMediaType> CACHED_TYPES =
        new HashMap<String, NamedMediaType>();
    
    /**
     * The MediaType this is describing.
     */
    private final MediaType _mediaType;
    
    /**
     * The name used to describe this MediaType/LimeXMLSchema.
     */
    private final String _name;
    
    /**
     * The icon used to display this mediaType/LimeXMLSchema.
     */
    private final Icon _icon;
    
    /**
     * The (possibly null) LimeXMLSchema.
     */
    private final LimeXMLSchema _schema;
    
    /**
     * Constructs a new NamedMediaType, associating the MediaType with the
     * LimeXMLSchema.
     */
    public NamedMediaType(MediaType mt, LimeXMLSchema schema) {
        if(mt == null)
            throw new NullPointerException("Null media type.");
        
        this._mediaType = mt;
        this._schema = schema;
        this._name = constructName(_mediaType, _schema);
        this._icon = getIcon(_mediaType, _schema);
    }
    
    /**
     * Compares this NamedMediaType to another.
     */
    public int compareTo(NamedMediaType other) {
        return _name.compareTo(other._name);
    }
    
    /**
     * Returns the name of this NamedMediaType.
     */
    public String getName() {
        return _name;
    }
    
    /**
     * Returns the icon representing this NamedMediaType.
     */
    public Icon getIcon() {
        return _icon;
    }
    
    /**
     * Returns the description of this NamedMediaType.
     */
    @Override
    public String toString() {
        return _name;
    }
    
    /**
     * Returns the media type this is wrapping.
     */
    public MediaType getMediaType() {
        return _mediaType;
    }
    
    /**
     * Returns the schema this is wrapping.
     */
    public LimeXMLSchema getSchema() {
        return _schema;
    }
    
    /**
     * Retrieves the named media type for the specified schema uri.
     *
     * This should only be used if you are positive that the media type
     * is already cached for this description OR it is not a default
     * type.
     */
    public static NamedMediaType getFromDescription(String description) {
        NamedMediaType type = CACHED_TYPES.get(description);
        if(type != null)
            return type;
            
        MediaType mt;
        // If it's not a default type, the MediaType is constructed.
        if(!MediaType.isDefaultType(description)) {
            mt = new MediaType(description);
        } else {
            // Otherwise, the default MediaType is used.
            mt = MediaType.getMediaTypeForSchema(description);
        }
        
        return getFromMediaType(mt);
    }
    
    /**
     * Retrieves the named media type from the specified extension.
     *
     * This should only be used if you are positive that the media type
     * is already cached for this extension.
     */
    public static NamedMediaType getFromExtension(String extension) {
        MediaType mt = MediaType.getMediaTypeForExtension(extension);
        if(mt == null)
            return null;
            
        String description = mt.getSchema();
        return getFromDescription(description);
    }
    
    /**
     * Retrieves all possible media types, wrapped in a NamedMediaType.
     */
    public static List<NamedMediaType> getAllNamedMediaTypes() {
        List<NamedMediaType> allSchemas = new LinkedList<NamedMediaType>();

        //Add all our schemas to the list.
        for(LimeXMLSchema schema : GuiCoreMediator.getLimeXMLSchemaRepository().getAvailableSchemas())
            allSchemas.add(getFromSchema(schema));

        //Add any default media types that haven't been added already.
        MediaType allTypes[] = MediaType.getDefaultMediaTypes();
        for(int i = 0; i < allTypes.length; i++) {
            if(!containsMediaType(allSchemas, allTypes[i]))
                allSchemas.add(getFromMediaType(allTypes[i]));
        }
        
        return allSchemas;
    }     
    
    /**
     * Retrieves the named media type for the specified schema.
     */
    private static NamedMediaType getFromSchema(LimeXMLSchema schema) {
        String description = schema.getDescription();
        NamedMediaType type = CACHED_TYPES.get(description);
        if(type != null)
            return type;
        
        MediaType mt;
        // If it's not a default type, the MediaType is constructed.
        if(!MediaType.isDefaultType(description)) {
            mt = new MediaType(description);
        } else {
            // Otherwise, the default MediaType is used.
            mt = MediaType.getMediaTypeForSchema(description);
        }
        
        type = new NamedMediaType(mt, schema);
        CACHED_TYPES.put(description, type);
        return type;
    }
    
    /**
     * Retrieves the named media type for the specified media type.
     */
    public static NamedMediaType getFromMediaType(MediaType media) {
        String description = media.getSchema();
        NamedMediaType type = CACHED_TYPES.get(description);
        if(type != null)
            return type;
            
        type = new NamedMediaType(media, null);
        CACHED_TYPES.put(description, type);
        return type;
    }
    
    /**
     * Determines whether or not the specified MediaType is in a list of
     * NamedMediaTypes.
     */
    private static boolean containsMediaType(List<? extends NamedMediaType> named, MediaType type) {
        for(NamedMediaType nmt : named) {
            if(nmt.getMediaType().equals(type))
                return true;
        }
        return false;
    }        
    
    /**
     * Retrieves the icon representing the MediaType/Schema.
     */
    private Icon getIcon(MediaType type, LimeXMLSchema schema) {
        final ImageIcon icon;
        
        if(type == MediaType.getAnyTypeMediaType())
            icon = GUIMediator.getThemeImage("lime");
        else {
            String location = GuiCoreMediator.getLimeXMLProperties().getXMLImagesResourcePath() + type.getSchema();
            icon = GUIMediator.getImageFromResourcePath(location);
            if(icon == null) {
                return new GUIUtils.EmptyIcon(getName(), 16, 16);
            }
        }
        
        icon.setDescription(getName());
        return icon;
    }
    
    /**
     * Returns the human-readable description of this MediaType/Schema.
     */
    private static String constructName(MediaType type, LimeXMLSchema schema) {
        // If we can act off the MediaType.
        String name = null;
        if(type.isDefault()) {
            String key = type.getDescriptionKey();
            try {
                if(key != null)
                    name = I18n.tr(key);
            } catch(MissingResourceException mre) {
                // oh well, will capitalize the mime-type
            }
            
            // If still no name, capitalize the mime-type.
            if(name == null) {
				name = type.getSchema();
                name = name.substring(0, 1).toUpperCase(Locale.US) + name.substring(1);
            }
        } else {
            name = XMLUtils.getTitleForSchema(schema);
        }
        
        return name;
    }
}
