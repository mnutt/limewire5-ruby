package org.limewire.ui.swing.util;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

public class CategoryUtils {
    
    public static Category getCategory(MediaType mt) {
        
        if (mt == MediaType.getAnyTypeMediaType()) {
            return null;
        }

        if (mt == MediaType.getAudioMediaType()) {
            return Category.AUDIO;
        }

        if (mt == MediaType.getVideoMediaType()) {
            return Category.VIDEO;
        }

        if (mt == MediaType.getImageMediaType()) {
            return Category.IMAGE;
        }

        if (mt == MediaType.getDocumentMediaType()) {
            return Category.DOCUMENT;
        }

        if (mt == MediaType.getProgramMediaType()) {
            return Category.PROGRAM;
        }

        return Category.OTHER;
    }
    
    /** Returns the Category of the file. */
    public static Category getCategory(File f) {
        MediaType mt = MediaType.getMediaTypeForExtension(FileUtils.getFileExtension(f));
        return getCategory(mt);
    }
    
}
