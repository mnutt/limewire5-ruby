package org.limewire.ui.swing.lists;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * Filter based on a category. Only displays FileItems of 
 * one category type.
 */
public class CategoryFilter implements Matcher<FileItem>{
    private final Category category;
    
    public CategoryFilter(Category category) {
        this.category = category;
    }

    @Override
    public boolean matches(FileItem item) {
        if (item == null) {
            return false;
        }
        
        if (category == null) {
            return true;
        }

        return item.getCategory().equals(category);
    }
}