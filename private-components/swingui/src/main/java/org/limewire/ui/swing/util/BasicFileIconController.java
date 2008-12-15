package org.limewire.ui.swing.util;

import java.io.File;

import javax.swing.Icon;

import org.limewire.core.api.Category;
import org.limewire.util.FileUtils;
import org.limewire.util.MediaType;

import com.google.inject.Inject;

/**
 * A FileIconController that uses default icons.
 */
public class BasicFileIconController extends NonBlockFileIconController {
    
    private final CategoryIconManager categoryIconManager;
    
    @Inject
    public BasicFileIconController(CategoryIconManager categoryIconManager) {
        this.categoryIconManager = categoryIconManager;
    }
    
    /** Returns the icon associated with the extension of the file. */
    public Icon getIconForFile(File f) {
        if(f == null)
            return null;
        
        String extension = FileUtils.getFileExtension(f);
        if(!extension.isEmpty())
            return getIconForExtension(extension);
        else
            return null;
    }
    
    /** Returns the icon assocated with the extension. */
    public Icon getIconForExtension(String ext) {
        MediaType mt = null;
        if (ext != null)
            mt = MediaType.getMediaTypeForExtension(ext);
        
        Category category = CategoryUtils.getCategory(mt);        
        return this.categoryIconManager.getIcon(category);
    }

    /** Icons are always available immediately. */
    public boolean isIconForFileAvailable(File f) {
        return true;
    }
}
