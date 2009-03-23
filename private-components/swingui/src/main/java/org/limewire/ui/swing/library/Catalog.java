package org.limewire.ui.swing.library;

import org.limewire.core.api.Category;
import org.limewire.core.api.playlist.Playlist;
import org.limewire.util.Objects;

/**
 * Catalog defines an organization of items in a library.  There are two 
 * catalog types: a media category, and a playlist.
 */
public class Catalog {
    /** Types of catalogs. */
    public enum Type {
        CATEGORY, PLAYLIST
    };

    /** Catalog type. */
    private final Type type;
    /** Name of catalog. */
    private final String name;
    
    /** Category represented by this catalog; may be null. */
    private Category category;
    /** Playlist represented by this catalog; may be null. */
    private Playlist playlist;

    /**
     * Constructs a Catalog with the specified type and name.
     */
    public Catalog(Type type, String name) {
        this.type = type;
        this.name = name;
    }
    
    /**
     * Constructs a Catalog for the specified media category.
     */
    public Catalog(Category category) {
        this(Type.CATEGORY, category.toString());
        this.category = category;
    }
    
    /**
     * Constructs a Catalog for the specified playlist.
     */
    public Catalog(Playlist playlist) {
        this(Type.PLAYLIST, playlist.getName());
        this.playlist = playlist;
    }
    
    /**
     * Returns the media category associated with this catalog.  May be null.
     */
    public Category getCategory() {
        return category;
    }
    
    /**
     * Returns the playlist associated with this catalog.  May be null.
     */
    public Playlist getPlaylist() {
        return playlist;
    }
    
    /**
     * Returns a unique identifier for this catalog by combining the type and
     * name values.
     */
    public String getId() {
        return (type + "." + name);
    }

    /**
     * Returns the catalog name.  
     */
    public String getName() {
        return name;
    }
    
    /**
     * Returns the catalog type.
     */
    public Type getType() {
        return type;
    }
    
    public boolean equals(Object obj) {
        if (obj instanceof Catalog) {
            Catalog item = (Catalog) obj;
            return (Objects.equalOrNull(type, item.type) &&
                    Objects.equalOrNull(name, item.name));
        }
        return false;
    }
    
    public int hashCode() {
        int result = 17;
        result = 31 * result + ((type == null) ? 0 : type.hashCode());
        result = 31 * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }
}
