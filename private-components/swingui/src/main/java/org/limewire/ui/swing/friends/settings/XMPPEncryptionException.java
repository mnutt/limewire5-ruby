package org.limewire.ui.swing.friends.settings;

import org.limewire.xmpp.api.client.XMPPException;

/**
 * This exception is throw when an error occurs while encrypting/decrypting
 * passwords, such as when the key used to decrypt/encrypt is invalid.
 */
public class XMPPEncryptionException extends XMPPException {

    public XMPPEncryptionException(String msg) {
        super(msg);
    }

    public XMPPEncryptionException(Throwable cause) {
        super(cause);
    }

    public XMPPEncryptionException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
