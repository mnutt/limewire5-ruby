package org.limewire.ui.swing.filter;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Range filter format for file size.
 */
class FileSizeFilterFormat<E extends FilterableItem> implements RangeFilterFormat<E> {
    /** Default size options in bytes. */
    private static final long[] DEFAULT_SIZES = {
        0,
        1024 * 10,   // 10 KB
        1024 * 50,   // 50 KB
        1024 * 100,  // 100 KB
        1024 * 500,  // 500 KB
        1024 * 1024, // 1 MB
        1024 * 1024 * 5,    // 5 MB
        1024 * 1024 * 10,   // 10 MB
        1024 * 1024 * 50,   // 50 MB
        1024 * 1024 * 100,  // 100 MB
        1024 * 1024 * 500,  // 500 MB
        1024 * 1024 * 1024, // 1 GB
        (long) 1024 * 1024 * 1024 * 5,  // 5 GB  
        (long) 1024 * 1024 * 1024 * 10, // 10 GB  
        (long) 1024 * 1024 * 1024 * 50, // 50 GB  
        (long) 1024 * 1024 * 1024 * 100 // 100 GB
    };
    
    /** Size options for audio files. */
    private static final long[] AUDIO_SIZES = {
        0,
        1024 * 100,  // 100 KB
        1024 * 250,  // 250 KB
        1024 * 500,  // 500 KB
        1024 * 1024, // 1 MB
        1024 * 1024 * 2,    // 2 MB
        1024 * 1024 * 5,    // 5 MB
        1024 * 1024 * 10,   // 10 MB
        1024 * 1024 * 25,   // 25 MB
        1024 * 1024 * 50,   // 50 MB
        1024 * 1024 * 100,  // 100 MB
        1024 * 1024 * 250,  // 250 MB
        1024 * 1024 * 500,  // 500 MB
        1024 * 1024 * 1024, // 1 GB
    };
    
    /** Size options for video files. */
    private static final long[] VIDEO_SIZES = {
        0,
        1024 * 1024 * 10,   // 10 MB
        1024 * 1024 * 25,   // 25 MB
        1024 * 1024 * 50,   // 50 MB
        1024 * 1024 * 100,  // 100 MB
        1024 * 1024 * 250,  // 250 MB
        1024 * 1024 * 500,  // 500 MB
        1024 * 1024 * 1024, // 1 GB
        (long) 1024 * 1024 * 1024 * 2,   // 2 GB  
        (long) 1024 * 1024 * 1024 * 5,   // 5 GB  
        (long) 1024 * 1024 * 1024 * 10,  // 10 GB  
        (long) 1024 * 1024 * 1024 * 25,  // 25 GB  
        (long) 1024 * 1024 * 1024 * 50,  // 50 GB  
        (long) 1024 * 1024 * 1024 * 100, // 100 GB  
    };
    
    /** Size options for image files. */
    private static final long[] IMAGE_SIZES = {
        0,
        1024 * 10,   // 10 KB
        1024 * 25,   // 25 KB
        1024 * 50,   // 50 KB
        1024 * 100,  // 100 KB
        1024 * 250,  // 250 KB
        1024 * 500,  // 500 KB
        1024 * 1024, // 1 MB
        1024 * 1024 * 2,   // 2 MB
        1024 * 1024 * 5,   // 5 MB
        1024 * 1024 * 10,  // 10 MB
        1024 * 1024 * 25,  // 25 MB
        1024 * 1024 * 50,  // 50 MB
        1024 * 1024 * 100, // 100 MB
    };
    
    /** Array of size options in bytes. */
    private final long[] sizes;
    
    /**
     * Constructs a FileSizeFilterFormat for the specified filter category.
     */
    public FileSizeFilterFormat(SearchCategory filterCategory) {
        switch (filterCategory) {
        case AUDIO:
            sizes = AUDIO_SIZES;
            break;
            
        case VIDEO:
            sizes = VIDEO_SIZES;
            break;
            
        case IMAGE:
        case DOCUMENT:
            sizes = IMAGE_SIZES;
            break;
            
        default:
            sizes = DEFAULT_SIZES;
            break;
        }
    }

    @Override
    public String getHeaderText() {
        return I18n.tr("Size");
    }

    @Override
    public Matcher<E> getMatcher(long minValue, long maxValue) {
        return new FileSizeMatcher(minValue, maxValue);
    }

    @Override
    public long[] getValues() {
        return sizes;
    }

    @Override
    public String getValueText(int valueIndex) {
        return GuiUtils.toUnitbytes(sizes[valueIndex]);
    }
    
    @Override
    public boolean isMaximumAbsolute() {
        return false;
    }
    
    @Override
    public boolean isUpperLimitEnabled() {
        return true;
    }

    /**
     * A matcher used to filter an item by file size.
     */
    private class FileSizeMatcher implements Matcher<E> {
        private final long minSize;
        private final long maxSize;
        
        /**
         * Constructs a FileSizeMatcher for the specified file size range.
         */
        public FileSizeMatcher(long minSize, long maxSize) {
            this.minSize = minSize;
            this.maxSize = maxSize;
        }

        /**
         * Returns true if the specified item is within the file size range.
         */
        @Override
        public boolean matches(E item) {
            long size = item.getSize();
            boolean minValid = (minSize == sizes[0]) || (size >= minSize);
            boolean maxValid = (maxSize == sizes[sizes.length - 1]) || (size <= maxSize);
            return (minValid && maxValid); 
        }
    }
}
