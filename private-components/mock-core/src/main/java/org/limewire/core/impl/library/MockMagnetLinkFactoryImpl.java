package org.limewire.core.impl.library;

import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.MagnetLinkFactory;

public class MockMagnetLinkFactoryImpl implements MagnetLinkFactory{


    @Override
    public String createMagnetLink(FileItem fileItem) {
        return "I am a magnet link";
    }

}
