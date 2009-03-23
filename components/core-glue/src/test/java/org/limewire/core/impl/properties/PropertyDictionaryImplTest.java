package org.limewire.core.impl.properties;

import java.util.List;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.limewire.util.BaseTestCase;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;

public class PropertyDictionaryImplTest extends BaseTestCase {

    private PropertyDictionaryImpl propertyDictionaryImpl;

    public PropertyDictionaryImplTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PropertyDictionaryImplTest.class);
    }
    
    @Override
    protected void setUp() throws Exception {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT);

        final LimeXMLSchemaRepository limeXMLSchemaRepository = injector
                .getInstance(LimeXMLSchemaRepository.class);
        propertyDictionaryImpl = new PropertyDictionaryImpl(limeXMLSchemaRepository);
    }

    @Override
    protected void tearDown() throws Exception {
        propertyDictionaryImpl = null;
    }

    public void testGetApplicationPlatforms() {
        List<String> propertyValues = propertyDictionaryImpl.getApplicationPlatforms();
        assertEquals(5, propertyValues.size());
        assertContains(propertyValues, "", "Windows", "OSX", "Linux/Unix", "Multi-platform");
        propertyValues = propertyDictionaryImpl.getApplicationPlatforms();
        assertEquals(5, propertyValues.size());
        assertContains(propertyValues, "", "Windows", "OSX", "Linux/Unix", "Multi-platform");
    }

    public void testGetAudioGenres() {
        List<String> propertyValues = propertyDictionaryImpl.getAudioGenres();
        assertEquals(127, propertyValues.size());
        assertContains(propertyValues, "", "Blues", "Classic Rock", "Country", "Funk");
        propertyValues = propertyDictionaryImpl.getAudioGenres();
        assertEquals(127, propertyValues.size());
        assertContains(propertyValues, "", "Blues", "Classic Rock", "Country", "Funk");
    }

    public void testGetVideoGenres() {
        List<String> propertyValues = propertyDictionaryImpl.getVideoGenres();
        assertEquals(10, propertyValues.size());
        assertContains(propertyValues, "", "Music Video", "Commercial", "Trailer", "Movie Clip",
                "Video Clip", "VHS Movie", "DVD Movie", "Adult", "Other");
        propertyValues = propertyDictionaryImpl.getVideoGenres();
        assertEquals(10, propertyValues.size());
        assertContains(propertyValues, "", "Music Video", "Commercial", "Trailer", "Movie Clip",
                "Video Clip", "VHS Movie", "DVD Movie", "Adult", "Other");
    }

    public void testGetVideoRatings() {
        List<String> propertyValues = propertyDictionaryImpl.getVideoRatings();
        assertContains(propertyValues, "", "G", "PG", "PG-13", "R", "NC-17", "NR");
        propertyValues = propertyDictionaryImpl.getVideoRatings();
        assertContains(propertyValues, "", "G", "PG", "PG-13", "R", "NC-17", "NR");
    }

    private <T> void assertContains(List<T> list, T firstObject, T... otherObjects) {
        assertContains(list, firstObject);
        for (T otherObject : otherObjects) {
            assertContains(list, otherObject);
        }
    }
}
