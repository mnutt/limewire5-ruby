package org.limewire.core.impl.properties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.util.BaseTestCase;
import org.limewire.util.NameValue;

import com.limegroup.gnutella.xml.LimeXMLSchema;
import com.limegroup.gnutella.xml.LimeXMLSchemaRepository;
import com.limegroup.gnutella.xml.SchemaFieldInfo;

public class PropertyDictionaryImplTest extends BaseTestCase {

    public PropertyDictionaryImplTest(String name) {
        super(name);
    }

    public void testGetApplicationPlatforms() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final LimeXMLSchemaRepository limeXMLSchemaRepository = context
                .mock(LimeXMLSchemaRepository.class);
        PropertyDictionaryImpl propertyDictionaryImpl = new PropertyDictionaryImpl(
                limeXMLSchemaRepository);

        final LimeXMLSchema limeXMLSchema = context.mock(LimeXMLSchema.class);
        final SchemaFieldInfo schemaFieldInfo = context.mock(SchemaFieldInfo.class);

        final List<NameValue<String>> valueList = new ArrayList<NameValue<String>>();
        final String value1 = "test1";
        valueList.add(new NameValue<String>(value1));
        final String value2 = "test2";
        valueList.add(new NameValue<String>(value2));
        context.checking(new Expectations() {
            {
                one(limeXMLSchemaRepository).getAvailableSchemas();
                will(returnValue(Collections.singletonList(limeXMLSchema)));
                one(limeXMLSchema).getDescription();
                will(returnValue("application"));
                one(schemaFieldInfo).getCanonicalizedFieldName();
                will(returnValue("platform"));
                one(schemaFieldInfo).getEnumerationList();
                will(returnValue(valueList));
                one(limeXMLSchema).getEnumerationFields();
                will(returnValue(Collections.singletonList(schemaFieldInfo)));
            }
        });

        List<String> propertyValues = propertyDictionaryImpl.getApplicationPlatforms();
        assertContains(propertyValues, value1);
        assertContains(propertyValues, value2);

        context.assertIsSatisfied();
    }

    public void testGetAudioGenres() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final LimeXMLSchemaRepository limeXMLSchemaRepository = context
                .mock(LimeXMLSchemaRepository.class);
        PropertyDictionaryImpl propertyDictionaryImpl = new PropertyDictionaryImpl(
                limeXMLSchemaRepository);

        final LimeXMLSchema limeXMLSchema = context.mock(LimeXMLSchema.class);
        final SchemaFieldInfo schemaFieldInfo = context.mock(SchemaFieldInfo.class);

        final List<NameValue<String>> valueList = new ArrayList<NameValue<String>>();
        final String value1 = "test1";
        valueList.add(new NameValue<String>(value1));
        final String value2 = "test2";
        valueList.add(new NameValue<String>(value2));
        context.checking(new Expectations() {
            {
                one(limeXMLSchemaRepository).getAvailableSchemas();
                will(returnValue(Collections.singletonList(limeXMLSchema)));
                one(limeXMLSchema).getDescription();
                will(returnValue("audio"));
                one(schemaFieldInfo).getCanonicalizedFieldName();
                will(returnValue("genre"));
                one(schemaFieldInfo).getEnumerationList();
                will(returnValue(valueList));
                one(limeXMLSchema).getEnumerationFields();
                will(returnValue(Collections.singletonList(schemaFieldInfo)));
            }
        });

        List<String> propertyValues = propertyDictionaryImpl.getAudioGenres();
        assertContains(propertyValues, value1);
        assertContains(propertyValues, value2);

        context.assertIsSatisfied();
    }

    public void testGetVideoGenres() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final LimeXMLSchemaRepository limeXMLSchemaRepository = context
                .mock(LimeXMLSchemaRepository.class);
        PropertyDictionaryImpl propertyDictionaryImpl = new PropertyDictionaryImpl(
                limeXMLSchemaRepository);

        final LimeXMLSchema limeXMLSchema = context.mock(LimeXMLSchema.class);
        final SchemaFieldInfo schemaFieldInfo = context.mock(SchemaFieldInfo.class);

        final List<NameValue<String>> valueList = new ArrayList<NameValue<String>>();
        final String value1 = "test1";
        valueList.add(new NameValue<String>(value1));
        final String value2 = "test2";
        valueList.add(new NameValue<String>(value2));
        context.checking(new Expectations() {
            {
                one(limeXMLSchemaRepository).getAvailableSchemas();
                will(returnValue(Collections.singletonList(limeXMLSchema)));
                one(limeXMLSchema).getDescription();
                will(returnValue("video"));
                one(schemaFieldInfo).getCanonicalizedFieldName();
                will(returnValue("type"));
                one(schemaFieldInfo).getEnumerationList();
                will(returnValue(valueList));
                one(limeXMLSchema).getEnumerationFields();
                will(returnValue(Collections.singletonList(schemaFieldInfo)));
            }
        });

        List<String> propertyValues = propertyDictionaryImpl.getVideoGenres();
        assertContains(propertyValues, value1);
        assertContains(propertyValues, value2);

        context.assertIsSatisfied();
    }

    public void testGetVideoRatings() {
        Mockery context = new Mockery() {
            {
                setImposteriser(ClassImposteriser.INSTANCE);
            }
        };

        final LimeXMLSchemaRepository limeXMLSchemaRepository = context
                .mock(LimeXMLSchemaRepository.class);
        PropertyDictionaryImpl propertyDictionaryImpl = new PropertyDictionaryImpl(
                limeXMLSchemaRepository);

        final LimeXMLSchema limeXMLSchema = context.mock(LimeXMLSchema.class);
        final SchemaFieldInfo schemaFieldInfo = context.mock(SchemaFieldInfo.class);

        final List<NameValue<String>> valueList = new ArrayList<NameValue<String>>();
        final String value1 = "test1";
        valueList.add(new NameValue<String>(value1));
        final String value2 = "test2";
        valueList.add(new NameValue<String>(value2));
        context.checking(new Expectations() {
            {
                one(limeXMLSchemaRepository).getAvailableSchemas();
                will(returnValue(Collections.singletonList(limeXMLSchema)));
                one(limeXMLSchema).getDescription();
                will(returnValue("video"));
                one(schemaFieldInfo).getCanonicalizedFieldName();
                will(returnValue("rating"));
                one(schemaFieldInfo).getEnumerationList();
                will(returnValue(valueList));
                one(limeXMLSchema).getEnumerationFields();
                will(returnValue(Collections.singletonList(schemaFieldInfo)));
            }
        });

        List<String> propertyValues = propertyDictionaryImpl.getVideoRatings();
        assertContains(propertyValues, value1);
        assertContains(propertyValues, value2);

        context.assertIsSatisfied();
    }

}
