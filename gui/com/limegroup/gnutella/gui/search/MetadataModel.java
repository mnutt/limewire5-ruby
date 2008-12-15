package com.limegroup.gnutella.gui.search;


import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.AbstractListModel;
import javax.swing.event.ListDataListener;

import org.limewire.collection.Comparators;

import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.search.Selector.PropertyType;
import com.limegroup.gnutella.gui.xml.XMLValue;
import com.limegroup.gnutella.util.DataUtils;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

/**
 * Maintains information about the metadata in a list of search results.
 *
 * ListModel views of specific fields may be retrieved in order to
 * 
 */
@SuppressWarnings("unchecked") // this class is insane
final class MetadataModel {
    
    // Important note about the below two mappings MODEL & PROPERTIES:
    // ..................................................................
    // The values of these MUST be either a ListModelMap or a Collection.
    // If the value is a ListModelMap, then recursively the value of that
    // map must either be another ListModelMap or a Collection.
    // ..................................................................
    
    /**
     * A mapping of:
     *   NamedMediaType (Schema) -> ListModelMap of:
     *              String (Field Name) -> ListModelMap of:
     *                      String (Value name) -> Collection (TableLine)
     *
     * This serves to easily look up what table lines match a given value
     * within a given field of a given URI.
     *
     * The Maps also double as ListModels, to return ListModel views of
     * sections.
     */
    private final ListModelMap MODEL;
    
    /**
     * A constant string to use as the field name when a document has no
     * fields.  This is so we can keep track of the elements in schemas
     * that have no schema.
     */
    private static final String UNKNOWN = "unknown";
    
    /**
     * A second mapping used for keeping track of specific properties, such as
     * extension, speed, etc...
     *
     * The mapping is of:
     *   String (Property) -> ListModelMap of:
     *          Object (Value name) -> Collection (TableLine)
     */
    private final ListModelMap PROPERTIES;
    
    /**
     * Constructs a new MetadataModel.
     */
    MetadataModel() {
        // Schemas use the natural ordering of the NamedMediaTypes
        MODEL = new Model();
        
        // Properties don't need to be case insensitive.
        PROPERTIES = new Model(Comparators.stringComparator());
        
        initialize();
    }
    
    /**
     * Clears this model.
     */
    void clear() {
       MODEL.clear();
       PROPERTIES.clear();
       initialize();
    } 
    
    /**
     * Adds a new TableLine, possibly also adding info in the LimeXMLDocument,
     * if one exists.
     */
    void addNew(TableLine line) {
        NamedMediaType mt = line.getNamedMediaType();
        
        // populate the properties map.
        addProperties(line);
        
        // no type at all, ignore.
        if(mt == null)
            return;

        Map fieldMap = getMap(MODEL, mt);
        LimeXMLDocument doc = line.getXMLDocument();
        if(doc != null)
            addDocument(fieldMap, mt.getSchema(), doc, line);
        else // keep track for the schema.
            getCollection(fieldMap, UNKNOWN).add(line);
    }
    
    /**
     * Removes any references to this table line.
     */
    void remove(TableLine line) {
        NamedMediaType mt = line.getNamedMediaType();
        
        removeProperties(line);
        if(mt == null)
            return;
        Map fieldMap = getMap(MODEL, mt);
        LimeXMLDocument doc = line.getXMLDocument();
        if(doc != null)
            removeDocument(fieldMap, mt.getSchema(), doc, line);
        else
            getCollection(fieldMap, UNKNOWN).remove(line);
    }
        
    
    /**
     * Adds LimeXMLDocument information to the map.
     *
     * It is assumed that the schema has already been added.
     */
    void addNewDocument(LimeXMLDocument doc, TableLine line) {
        NamedMediaType mt = line.getNamedMediaType();
        Map fieldMap = getMap(MODEL, mt);
        addDocument(fieldMap, mt.getSchema(), doc, line);
    }
    
    /**
     * Adds the associated the specified schema, field and value to
     * given TableLine.
     *
     * This should only be used when the full document has already been
     * added once before.
     */
    void addField(SchemaFieldInfo sfi, String field, String value, TableLine line) {
        NamedMediaType mt = line.getNamedMediaType();
        Map fieldMap = getMap(MODEL, mt);
        Map valueMap = getMapNatural(fieldMap, field);
        getCollection(valueMap, new XMLValue(value, sfi)).add(line);
    }
    
    /**
     * Updates the metadata information for the specified property.
     */
    void updateProperty(String property, Object current,
                        Object old, TableLine line) {
        Map map = getMap(PROPERTIES, property);
        getCollection(map, old).remove(line);
        getCollection(map, current).add(line);
    }
    
    /**
     * Retrieves the ListModelMap for the specified Selector.
     */
    ListModelMap getListModelMap(Selector selector) {
        switch(selector.getSelectorType()) {
        case Selector.SCHEMA:
            return MODEL;
        case Selector.FIELD:
            NamedMediaType mt = NamedMediaType.getFromDescription(selector.getSchema());
            return getMapNatural(getMap(MODEL, mt), selector.getValue());
        case Selector.PROPERTY:
            return getMap(PROPERTIES, selector.getValue());
        }
        return null;
    }
    
    /**
     * Retrieves a list of potential selectors for this model.
     */
    List /* of Selector */ getSelectorOptions() {
        List list = new LinkedList();
        
        // Always add the 'Schema' option.
        list.add(Selector.createSchemaSelector());
        
        // Then add each Field
        // First iterate through our schemas
        for(Iterator i = MODEL.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            
            NamedMediaType nmt = (NamedMediaType)entry.getKey();
            String schema = nmt.getMediaType().getSchema();
            // Then add the fields of those schemas.
            Iterator fields = ((Map)entry.getValue()).keySet().iterator();
            for(; fields.hasNext();) {
                String next = (String)fields.next();
                if(!UNKNOWN.equals(next))
                    list.add(Selector.createFieldSelector(schema, next));
            }
        }
        
        // Then add the properties.
        for(Iterator i = PROPERTIES.keySet().iterator(); i.hasNext();) {
            list.add(Selector.createPropertySelector((String)i.next()));
        }

        return list;
    }
    
    /**
     * Gets the intersection of the two ListModelMaps.
     *
     * The intend of this method is to filter out any elements from
     * the child map that do not correspond with the parent map's selection.
     * If the selection is null, it assumes that everything in the parent
     * Map is valid.
     *
     * This works in the following manner...  We are provided with
     * two ListModelMaps, the parent & the child, as well as the
     * selection within the parent.  For instance, assume that parent
     * is a Map of all audios__audio__artists__, the selection is
     * 'Sammy B', and the child is a Map of all audios__audio__albums.
     * If child contains entries for 'Piano Hits' and 'Bass Hits',
     * but only 'Piano Hits' is by 'Sammy B', then this will return a
     * ListModelMap that only contains 'Piano Hits'.
     *
     * The following steps are used to do this:
     * 1) a) Retrieve the element associated with the selection.
     *    b) If the selection was null or 'All', all parent elements are valid.
     * 2) Iterate through the entries in the child map.
     * 3) If any of the children's entries exist in the parent's, then
     *    add that entry to a new map that will be returned.
     */
    ListModelMap getIntersection(ListModelMap parent, Object selection,
                                 ListModelMap child) {
        Collection elements;

        // STEP 1.
        if(selection != null && !isAll(selection)) {
            // 1a
            Object values = parent.get(selection);
            if(values == null)
                throw new IllegalArgumentException("invalid selection");
            elements = getAllValues(values);
        } else {
            // 1b
            elements = getAllValues(parent);
        }

        // STEP 2.
        // Elements now contains all the Objects that the parent contains.
        // We must now iterate through child's elements and retain only those
        // elements whose children have an element in elements.
        ListModelMap ret;
        if(child.comparator() == null)
            ret = new Model();
        else 
            ret = new Model(child.comparator());
        for(Iterator i = child.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry entry = (Map.Entry)i.next();
            // STEP 3.
            if(DataUtils.containsAny(elements, getAllValues(entry.getValue())))
                ret.put(entry.getKey(), entry.getValue());
        }
        
        return ret;
    }
    
    /**
     * Initializes the maps to the appropriate values.
     */
    private void initialize() {
        // Ensure that type & speed use natural ordering of the elements.
        PROPERTIES.put(PropertyType.TYPE.getKey(), new Model());
        PROPERTIES.put(PropertyType.SPEED.getKey(), new Model());
    }    
    
    /**
     * Adds the contents of the LimeXMLDocument to the internal maps.
     */
    private void addDocument(Map fieldMap, LimeXMLSchema schema, LimeXMLDocument doc, TableLine line) {
        boolean added = false;
        for(Iterator i = schema.getCanonicalizedFields().iterator(); i.hasNext(); ) {
            SchemaFieldInfo sfi = (SchemaFieldInfo)i.next();
            String field = sfi.getCanonicalizedFieldName();
            String value = doc.getValue(field);
            if(value != null) {
                added = true;
                // Retrieve the map of values -> list
                Map valueMap = getMapNatural(fieldMap, field);
                // Add this value to the ones for this value.
                getCollection(valueMap, new XMLValue(value, sfi)).add(line);
            }
        }
        // if it had no fields, make sure its still counted in the schema.
        if(!added)
            getCollection(fieldMap, UNKNOWN).add(line);
    }
    
    /**
     * Removes a references to this line.
     */
    private void removeDocument(Map fieldMap, LimeXMLSchema schema, LimeXMLDocument doc, TableLine line) {
        boolean removed = false;
        for(Iterator i = schema.getCanonicalizedFields().iterator(); i.hasNext(); ) {
            SchemaFieldInfo sfi = (SchemaFieldInfo)i.next();
            String field = sfi.getCanonicalizedFieldName();
            String value = doc.getValue(field);
            if(value != null) {
                removed = true;
                // Retrieve the map of values -> list
                Map valueMap = getMapNatural(fieldMap, field);
                // Add this value to the ones for this value.
                getCollection(valueMap, new XMLValue(value, sfi)).remove(line);
            }
        }
        // if it had no fields, make sure its still counted in the schema.
        if(!removed)
            getCollection(fieldMap, UNKNOWN).remove(line);
    }    
    
    /**
     * Adds various properties of the TableLine as metadata.
     *
     * This currently supports:
     *   extension (RESULT_PANEL_TYPE)
     *   speed     (RESULT_PANEL_SPEED)
     *   vendor    (RESULT_PANEL_VENDOR)
     */
    private void addProperties(TableLine line) {
        Map extMap = getMap(PROPERTIES, PropertyType.TYPE.getKey());
        getCollection(extMap, line.getIconAndExtension()).add(line);
            
        Map speedMap = getMap(PROPERTIES, PropertyType.SPEED.getKey());
        getCollection(speedMap, line.getSpeed()).add(line);
        
        Map vendorMap = getMap(PROPERTIES, PropertyType.VENDOR.getKey());
        getCollection(vendorMap, line.getVendor()).add(line);
    }
    
    /**
     * Removes this line from its properties.
     */
    private void removeProperties(TableLine line) {
        Map extMap = getMap(PROPERTIES, PropertyType.TYPE.getKey());
        getCollection(extMap, line.getIconAndExtension()).remove(line);
            
        Map speedMap = getMap(PROPERTIES, PropertyType.SPEED.getKey());
        getCollection(speedMap, line.getSpeed()).remove(line);
        
        Map vendorMap = getMap(PROPERTIES, PropertyType.VENDOR.getKey());
        getCollection(vendorMap, line.getVendor()).remove(line);
    }        
    
    /**
     * Returns all possible child elements of the Object.
     *
     * If the object is a Map, it iterates through the values looking
     * for either a Map (in which case it recursively calls itself), or a 
     * Collection (in which case it adds all the contents of the Collection
     * to the return value).
     */
    private Collection getAllValues(Object parent) {
        // already a Collection, return it.
        if(parent instanceof Collection)
            return (Collection)parent;
            
        if(parent instanceof Map) {
            Collection values = new HashSet();
            for(Iterator i = ((Map)parent).values().iterator(); i.hasNext();) {
                values.addAll(getAllValues(i.next()));
            }
            return values;
        }
        
        // Otherwise we can't handle it.
        throw new IllegalArgumentException("parent: " + parent);
    }
    
    /**
     * Retrieves a map from another map, adding a new one if it didn't exist.
     *
     * If the the inner map doesn't exist, creates a new one using a case
     * insensitive string comparator.
     */
    private ListModelMap getMap(Map parent, Object key) {
        ListModelMap m = (ListModelMap)parent.get(key);
        if(m == null) {
            m = new Model(Comparators.caseInsensitiveStringComparator());
            parent.put(key, m);
        }
        return m;
    }
    
    /**
     * Retrieves a map from another map, adding a new one if it didn't exist.
     *
     * If the the inner map doesn't exist, creates a new one using natural ordering.
     */
    private ListModelMap getMapNatural(Map parent, Object key) {
        ListModelMap m = (ListModelMap)parent.get(key);
        if(m == null) {
            m = new Model();
            parent.put(key, m);
        }
        return m;
    }    
    
    /**
     * Retrieves a collection from a map, adding one if it didn't exist.
     *
     * The collection added is a HashSet, although this can be changed.
     */
    private Collection getCollection(Map parent, Object key) {
        if(key instanceof String) {
            // make sure spaces get chopped off.
            key = ((String)key).trim();
        }
        Collection l = (Collection)parent.get(key);
        if(l == null) {
            l = new HashSet();
            parent.put(key, l);
        }
        return l;
    }
    
    /**
     * A Model that implements both Map & ListModel.
     */
    private static class Model extends TreeMap implements ListModelMap {
        /**
         * The delegate ListModel for propogating ListModel events.
         */
        private final SimpleListModel DELEGATE = new SimpleListModel();
        
        /**
         * Constructs the map with a natural ordering of the elements.
         */
        Model() {
            super();
        }
        
        /**
         * Constructs the Map with the specified comparator.
         */
        Model(Comparator comp) {
            super(comp);
        }
        
        @Override
        public Object put(Object a, Object b) {
            Object o = super.put(a, b);
            DELEGATE.fireContentsChanged(this, 0, size());
            return o;
        }
        
        public void fireContentsChanged() {
            DELEGATE.fireContentsChanged(this, 0, size());
        }
        
        /**
         * Adds a ListDataListener to the values.
         */
        public void addListDataListener(ListDataListener l) {
            DELEGATE.addListDataListener(l);
        }
        
        /**
         * Removes a ListDataListener from the values.
         */
        public void removeListDataListener(ListDataListener l) {
            DELEGATE.removeListDataListener(l);
        }
        
        /**
         * Returns the length of the list.
         */
        public int getSize() {
            return size() + 1; // +1 because of the 'All' element.
        }
        
        /**
         * Retrieves the element at the specified index.
         */
        public Object getElementAt(int idx) {
            // The first element to display is always 'All'.
            if(idx == 0)
                return new All(size());

            if(idx > size())
                throw new IndexOutOfBoundsException("index: " + idx + 
                                                    ", size: " + getSize());
            
            // TODO: Don't iterate this way.
            Iterator i = keySet().iterator();
            // Start at 1 because they think we have one more than we do.
            for(int j = 1; j < idx; j++)
                i.next();
            return i.next();
        }
        
        /**
         * Determines if the ListModel contains the specified object.
         */
        public boolean contains(Object o) {
            if(isAll(o))
                return true;
            else
                return containsKey(o);
        }
        
        /**
         * Returns the index of the given value.
         */
        public int indexOf(Object o) {
            if(isAll(o))
                return 0;
            else {
                Iterator iter = keySet().iterator();
                for(int i = 1; iter.hasNext(); i++)
                    if(compare(o, iter.next()) == 0)
                        return i;
                return -1;
            }
        }
        
        /**
         * Returns the iterator of this map.
         */
        public Iterator iterator() {
            return keySet().iterator();
        }

        /**
         * Compares two keys using the correct comparison method for this Map.
         */
        private int compare(Object k1, Object k2) {
            return (comparator()==null ? ((Comparable)k1).compareTo(k2)
                                     : comparator().compare(k1, k2));
        }
            
    }
    
    /**
     * A simple ListModel, useful for delegating to for action calls.
     */
    private static class SimpleListModel extends AbstractListModel {
        public int getSize() { throw new IllegalStateException(); }
        public Object getElementAt(int idx) { throw new IllegalStateException(); }
        @Override
        public void fireContentsChanged(Object src, int a, int b) {
            super.fireContentsChanged(src, a, b);
        }
    }
    
    /**
     * Determines whether or not the specified value is the 'All' selection.
     */
    static boolean isAll(Object value) {
        return (value instanceof All);
    }
    
    /**
     * The 'All' selection.
     */
    private static class All {
        private static final String ALL =
            I18n.tr("All") + " (";
        
        final int number;
        
        private All(int number) {
            this.number = number;
        }
        
        @Override
        public String toString() {
            return ALL + number + ")";
        }
    }
}
