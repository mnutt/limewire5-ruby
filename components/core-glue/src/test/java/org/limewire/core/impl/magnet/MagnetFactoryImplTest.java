package org.limewire.core.impl.magnet;

import java.net.URI;

import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.util.BaseTestCase;

public class MagnetFactoryImplTest extends BaseTestCase {

    public MagnetFactoryImplTest(String name) {
        super(name);
    }

    public void testIsMagnetLink() throws Exception {
        MagnetFactoryImpl magnetFactoryImpl = new MagnetFactoryImpl();

        assertTrue(magnetFactoryImpl
                .isMagnetLink(new URI(
                        "magnet:?&xt=urn:sha1:544FOK7DMKY2KBEHPPICGK5NVRJMWBSI&dn=limewire.gif&xs=urn:guid:5D4EBE04169604336CE2E3F9A5BDC800&xl=610")));
        assertTrue(magnetFactoryImpl.isMagnetLink(new URI("magnet://this_is_a_magent_link")));
        assertFalse(magnetFactoryImpl.isMagnetLink(new URI("http://this_is_NOT_a_magent_link")));
        assertFalse(magnetFactoryImpl.isMagnetLink(null));
    }

    public void testParseMagnetLink() throws Exception {
        MagnetFactoryImpl magnetFactoryImpl = new MagnetFactoryImpl();

        MagnetLink[] magnetLinks = magnetFactoryImpl
                .parseMagnetLink(new URI(
                        "magnet:?&xt=urn:sha1:544FOK7DMKY2KBEHPPICGK5NVRJMWBSI&dn=limewire.gif&xs=urn:guid:5D4EBE04169604336CE2E3F9A5BDC800&xl=610"));
        assertEquals(1, magnetLinks.length);

        assertTrue(magnetLinks[0].isDownloadable());
        assertEquals("limewire.gif", magnetLinks[0].getQueryString());
        assertFalse(magnetLinks[0].isKeywordTopicOnly());
    }

}
