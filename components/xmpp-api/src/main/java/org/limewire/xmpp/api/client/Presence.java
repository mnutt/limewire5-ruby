package org.limewire.xmpp.api.client;

import org.limewire.core.api.friend.FriendPresence;
import org.limewire.i18n.I18nMarker;

/**
 * Represents xmpp <code><presence></code> messages.
 */
public interface Presence extends FriendPresence {

    enum EventType {
        /** Indicates that this is the first time we're seeing this presence. */
        PRESENCE_NEW,

        /**
         * Indicates that this is an update to the presence. For the exact kind
         * of update, see the {@link Type}.
         */
        PRESENCE_UPDATE
    }

    enum Type {
        available, unavailable, subscribe, subscribed, unsubscribe, unsubscribed, error
    }
    
    /**
     * The actual presence status. 
     */
    enum Mode {
        // lower case enum values to allow direct mapping from to the Mode enum
        // defined in smack
        chat(I18nMarker.marktr("Free to chat"), 0),
        available(I18nMarker.marktr("Available"), 1),
        away(I18nMarker.marktr("Away"), 2),//away and extended away given the same order for now, since they are rendered in the ui the same otherwise it would be confusing
        xa(I18nMarker.marktr("Away for a while"), 2),
        dnd(I18nMarker.marktr("Do not disturb"), 3);

        private final String name;
        private final int order;
        
        Mode(String name, int order) {
            this.name = name;
            this.order = order;
        }
        
        /**
         * @return the Order that this Mode should be sorted against other modes.
         */
        public int getOrder() {
            return order;
        }

        public String toString() {
            return name;
        }
    }
    
    public static final int MIN_PRIORITY = -127;
    
    public static final int MAX_PRIORITY = 127;

    public User getUser();

    /**
     * the jid of the user.  jid's have the form <code>user@host.com/client</code>
     * @return the jid of the user.
     */
    public String getJID();

    /**
     * @return the presence type
     */
    public Type getType();

    /**
     * @return the presence status message; can be <code>null</code>
     */
    public String getStatus();

    /**
     * @return the priority of this presence in relation to other presence's of the same user
     */
    public int getPriority();

    /**
     * @return the presence mode
     */
    public Mode getMode();
}
