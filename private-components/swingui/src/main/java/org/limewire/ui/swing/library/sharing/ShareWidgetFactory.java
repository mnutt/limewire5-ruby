package org.limewire.ui.swing.library.sharing;

import java.io.File;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LocalFileItem;

public interface ShareWidgetFactory {
    ShareWidget<File> createFileShareWidget();

    ShareWidget<LocalFileItem[]> createMultiFileShareWidget();
    
    ShareWidget<LocalFileItem[]> createMultiFileFriendOnlyShareWidget();

    ShareWidget<LocalFileItem[]> createMultiFileUnshareWidget();
    
    ShareWidget<Category> createCategoryShareWidget();
}
