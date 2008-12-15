/**
 * 
 */
package org.limewire.xmpp.client.impl;

import org.limewire.xmpp.api.client.JabberSettings;

public class EmptyJabberSettings implements JabberSettings {
    @Override
    public boolean isDoNotDisturbSet() {
        return false;
    }
}