package org.limewire.core.api.friend.feature.features;

import java.net.URI;
import java.net.URISyntaxException;

import org.limewire.core.api.friend.feature.Feature;

public class LibraryChangedNotifierFeature extends Feature<LibraryChangedNotifier> {

    public static final URI ID;

    static {
        try {
            ID = new URI("http://www.limewire.org/library-changed-notifier/2008-12-01");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public LibraryChangedNotifierFeature(LibraryChangedNotifier notifier) {
        super(notifier, ID);
    }
}
