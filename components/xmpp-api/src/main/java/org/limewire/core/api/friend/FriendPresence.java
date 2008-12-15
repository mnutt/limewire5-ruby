package org.limewire.core.api.friend;

import java.net.URI;
import java.util.Collection;

import org.limewire.core.api.friend.feature.Feature;

/**
 * A presence for a friend. One friend can have multiple presences.
 */
public interface FriendPresence {

    /**
     * Returns the containing friend.
     */
    Friend getFriend();

    /**
     * The ID of this specific presence. For example, an XMPP Presence would be
     * in the form of <code>user@host/resource</code> whereas a Gnutella
     * Presence would be the clientGUID.
     */
    String getPresenceId();

    /**
     * @return a Collection of Features that this FriendPresence supports
     */
    Collection<Feature> getFeatures();

    /**
     * @param id
     * @return the Feature corresponding the given id
     */
    Feature getFeature(URI id);

    /**
     * @param id
     * @return whether this FriendPresence supports all of the input feature ids
     */
    boolean hasFeatures(URI... id);

    /**
     * Adds a new Feature to this FriendPrsence
     * @param feature the feature to add
     */
    void addFeature(Feature feature);

    /**
     * Removes a feature from this FriendPresence
     * @param id the feature to remove
     */
    void removeFeature(URI id);
}
