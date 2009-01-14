package org.limewire.core.impl.properties;

import java.util.Collections;
import java.util.List;

import org.limewire.core.api.properties.PropertyDictionary;

public class MockPropertyDictionary implements PropertyDictionary {

    @Override
    public List<String> getApplicationPlatforms() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getAudioGenres() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getVideoGenres() {
        return Collections.emptyList();
    }

    @Override
    public List<String> getVideoRatings() {
        return Collections.emptyList();
    }
}
