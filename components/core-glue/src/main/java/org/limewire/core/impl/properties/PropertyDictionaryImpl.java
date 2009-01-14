package org.limewire.core.impl.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.util.NameValue;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

@Singleton
class PropertyDictionaryImpl implements PropertyDictionary {
    @Inject private LimeXMLSchemaRepository schemaRepository;
    private List<String> audioGenres;
    private List<String> videoGenres;
    private List<String> videoRatings;
    private List<String> applicationPlatforms;
    
    @Override
    public List<String> getAudioGenres() {
        if (audioGenres == null) {
            audioGenres = Collections.unmodifiableList(getEnumeration("audio", "genre"));
        }
        return audioGenres;
    }
    
    @Override
    public List<String> getVideoRatings() {
        if (videoRatings == null) {
            videoRatings = Collections.unmodifiableList(getEnumeration("video", "rating"));
        }
        return videoRatings;
    }

    @Override
    public List<String> getVideoGenres() {
        if (videoGenres == null) {
            videoGenres = Collections.unmodifiableList(getEnumeration("video", "type"));
        }
        return videoGenres;
    }

    @Override
    public List<String> getApplicationPlatforms() {
        if (applicationPlatforms == null) {
            applicationPlatforms = Collections.unmodifiableList(getEnumeration("application", "platform"));
        }
        return applicationPlatforms;
    }

    private List<String> getEnumeration(String schemaDescription, String enumerationName) {
        List<String> enumeration = new ArrayList<String>();
        for (LimeXMLSchema schema : schemaRepository.getAvailableSchemas()) {
            if (schemaDescription.equals(schema.getDescription())) {
                for(SchemaFieldInfo info : schema.getEnumerationFields()) {
                    String canonicalizedFieldName = info.getCanonicalizedFieldName();
                    if (canonicalizedFieldName != null && canonicalizedFieldName.contains(enumerationName)) {
                        for(NameValue<String> nameValue : info.getEnumerationList()) {
                            enumeration.add(nameValue.getName());
                        }
                    }
                }
            }
        }
        return enumeration;
    }
}
