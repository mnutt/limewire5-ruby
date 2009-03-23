package org.limewire.core.impl.friend;

import java.net.URI;
import java.util.Collection;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.friend.feature.Feature;
import org.limewire.core.api.friend.feature.features.AddressFeature;
import org.limewire.io.Address;
import org.limewire.util.BaseTestCase;

public class GnutellaPresenceTest extends BaseTestCase {

    public GnutellaPresenceTest(String name) {
        super(name);
    }

    public void testGetters() throws Exception {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };
        final Address address1 = context.mock(Address.class);
        final String id1 = "id1";
        final String description1 = "description1";

        context.checking(new Expectations() {
            {
                one(address1).getAddressDescription();
                will(returnValue(description1));
            }
        });
        GnutellaPresence gnutellaPresence = new GnutellaPresence(address1, id1);

        Friend friend1 = gnutellaPresence.getFriend();
        assertEquals(id1, friend1.getId());

        assertEquals(id1, gnutellaPresence.getPresenceId());

        String feature1String = "feature1";
        URI feature1URI = new URI("http://www.limewire.com/feature1");
        Feature<String> feature1 = new Feature<String>(feature1String, feature1URI);
        gnutellaPresence.addFeature(feature1);

        String feature2String = "feature2";
        URI feature2URI = new URI("http://www.limewire.com/feature2");
        Feature<String> feature2 = new Feature<String>(feature2String, feature2URI);
        gnutellaPresence.addFeature(feature2);

        URI feature3URI = new URI("http://www.limewire.com/feature3");

        Collection<Feature> features = gnutellaPresence.getFeatures();
        assertEquals(3, features.size());
        assertContains(features, feature1);
        assertContains(features, feature2);
        assertTrue(gnutellaPresence.hasFeatures(feature1URI, feature2URI, AddressFeature.ID));

        assertFalse(gnutellaPresence.hasFeatures(feature1URI, feature2URI, feature3URI));

        assertEquals(feature1, gnutellaPresence.getFeature(feature1URI));
        assertEquals(feature2, gnutellaPresence.getFeature(feature2URI));

        assertNull(gnutellaPresence.getFeature(feature3URI));

        assertTrue(gnutellaPresence.hasFeatures(feature2URI));
        
        gnutellaPresence.removeFeature(feature2URI);
        
        assertFalse(gnutellaPresence.hasFeatures(feature2URI));
        assertNull(gnutellaPresence.getFeature(feature2URI));
    }
}
