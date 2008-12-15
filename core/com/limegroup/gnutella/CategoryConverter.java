package com.limegroup.gnutella;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

public class CategoryConverter {
    
    private CategoryConverter() {}
    
    public static Category categoryForExtension(String ext) {
        return toCategory(MediaType.getMediaTypeForExtension(ext));
    }
    
    public static Category categoryForFileName(String fileName) {
        return categoryForExtension(FileUtils.getFileExtension(fileName));
    }
    
    public static Category categoryForFile(File file) {
        return categoryForExtension(FileUtils.getFileExtension(file));
    }
    
    public static Category toCategory(MediaType type) {
        if (type == MediaType.getAudioMediaType()) {
            return Category.AUDIO;
        } else if (type == MediaType.getVideoMediaType()) {
            return Category.VIDEO;
        } else if (type == MediaType.getImageMediaType()) {
            return Category.IMAGE;
        } else if (type == MediaType.getDocumentMediaType()) {
            return Category.DOCUMENT;
        } else if (type == MediaType.getProgramMediaType()) {
            return Category.PROGRAM;
        }
        return Category.OTHER;
    }
    
}
