package org.limewire.collection;

import java.util.Map;

/**
 * Stores a property key and its corresponding value pair. <code>KeyValue</code>
 * implements <code>Map.Entry</code>, but there's no backing map.
 * 
 * <pre>
 * System.out.println(new KeyValue&lt;String, String&gt;(&quot;myKey&quot;, &quot;myValue&quot;)); 
 * 
 *     Output:
 *         myKey = myValue
 * </pre>
 */
public class KeyValue<K, V> implements Map.Entry<K, V> {
    /** key of the property */
    private K key = null;

    /** Value of the property */
    private V value = null;

    /**
     * Constructor.
     * 
     * @param key key of the property
     * @param value corresponding value of the property
     */
    public KeyValue(K key, V value) {
        this.key = key;
        this.value = value;
    }// end of constructor

    /**
     * Default Constructor.
     */
    public KeyValue() {
        this.key = null;
        this.value = null;
    }

    /**
     * Sets the key and value fields.
     * 
     * @param key key of the property
     * @param value corresponding value of the property
     */
    public void set(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Compares the instance of this class with another instance. Returns true,
     * if the key field is same, regardless of the value.
     * 
     * @param o Another instance of the KeyValue class to which it has to be
     *        compared.
     */
    @Override
    public boolean equals(Object o) {
        KeyValue keyValue = (KeyValue) o;

        return key.equals(keyValue.getKey());
    }

    /**
     * Converts the key Value pair into a string representation.
     */
    @Override
    public String toString() {
        return key + " = " + value;
    }

    /**
     * @return the key(key) in the key value pair
     */
    public K getKey() {
        return key;
    }

    /**
     * @return the value corresponding to this entry.
     */
    public V getValue() {
        return value;
    }

    /**
     * Replaces the value corresponding to this entry with the specified value.
     * 
     * @param value new value to be stored in this entry.
     * @return old value corresponding to the entry.
     */
    public V setValue(V value) {
        // get the old value
        V oldValue = this.value;

        // change the value
        this.value = value;

        // return the old value
        return oldValue;

    }

    /**
     * Returns the hash code value for this map entry. The hash code of a map
     * entry <tt>e</tt> is defined to be:
     * 
     * <pre>
     * (e.getKey() == null ? 0 : e.getKey().hashCode())
     *         &circ; (e.getValue() == null ? 0 : e.getValue().hashCode())
     * </pre>
     * 
     * This ensures that <tt>e1.equals(e2)</tt> implies that
     * <tt>e1.hashCode()==e2.hashCode()</tt> for any two Entries <tt>e1</tt> and
     * <tt>e2</tt>, as required by the general contract of
     * <tt>Object.hashCode</tt>.
     * 
     * @return the hash code value for this map entry.
     * @see Object#hashCode()
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    @Override
    public int hashCode() {
        return ((key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode()));
    }

}// end of class
