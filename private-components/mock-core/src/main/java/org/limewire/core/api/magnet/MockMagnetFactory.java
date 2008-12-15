package org.limewire.core.api.magnet;

import java.net.URI;

public class MockMagnetFactory implements MagnetFactory {

    @Override
    public boolean isMagnetLink(URI uri) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public MagnetLink[] parseMagnetLink(URI uri) {
        // TODO Auto-generated method stub
        return null;
    }

}
