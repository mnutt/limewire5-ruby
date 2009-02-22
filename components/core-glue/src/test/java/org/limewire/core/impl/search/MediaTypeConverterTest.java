package org.limewire.core.impl.search;

import org.limewire.core.api.Category;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MediaType;

public class MediaTypeConverterTest extends BaseTestCase {

    public MediaTypeConverterTest(String name) {
        super(name);
    }

    public void testToMediaTypeFromCategory() {
        assertEquals(MediaType.getAudioMediaType(), MediaTypeConverter.toMediaType(Category.AUDIO));
        assertEquals(MediaType.getDocumentMediaType(), MediaTypeConverter.toMediaType(Category.DOCUMENT));
        assertEquals(MediaType.getImageMediaType(), MediaTypeConverter.toMediaType(Category.IMAGE));
        assertEquals(MediaType.getVideoMediaType(), MediaTypeConverter.toMediaType(Category.VIDEO));
        assertEquals(MediaType.getOtherMediaType(), MediaTypeConverter.toMediaType(Category.OTHER));
        assertEquals(MediaType.getProgramMediaType(), MediaTypeConverter.toMediaType(Category.PROGRAM));
    }
    
    public void testToMediaTypeFromSearchCategory() {
        assertEquals(MediaType.getAnyTypeMediaType(), MediaTypeConverter.toMediaType(SearchCategory.ALL));
        assertEquals(MediaType.getAudioMediaType(), MediaTypeConverter.toMediaType(SearchCategory.AUDIO));
        assertEquals(MediaType.getDocumentMediaType(), MediaTypeConverter.toMediaType(SearchCategory.DOCUMENT));
        assertEquals(MediaType.getImageMediaType(), MediaTypeConverter.toMediaType(SearchCategory.IMAGE));
        assertEquals(MediaType.getVideoMediaType(), MediaTypeConverter.toMediaType(SearchCategory.VIDEO));
        assertEquals(MediaType.getOtherMediaType(), MediaTypeConverter.toMediaType(SearchCategory.OTHER));
        assertEquals(MediaType.getProgramMediaType(), MediaTypeConverter.toMediaType(SearchCategory.PROGRAM));
    }
}
