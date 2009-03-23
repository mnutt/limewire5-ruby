package org.limewire.ui.swing.search.model;

import java.util.List;
import java.util.Map;

import org.limewire.core.api.FilePropertyKey;

import ca.odell.glazedlists.TextFilterator;

/**
 * Implementation of TextFilterator for a VisualSearchResult.  Property
 * values for all indexable keys, along with the file extension, are added
 * to the list of strings checked by the filter.  
 */
public class VisualSearchResultTextFilterator implements TextFilterator<VisualSearchResult> {
    
    @Override
    public void getFilterStrings(List<String> list, VisualSearchResult vsr) {
        // Add file extension to list.
        list.add(vsr.getFileExtension());
        
        // Add non-null values for all indexable keys.
        Map<FilePropertyKey, Object> props = vsr.getProperties();
        for (FilePropertyKey key : props.keySet()) {
            
            if (!FilePropertyKey.getIndexableKeys().contains(key)) {  
                continue;
            }
         
            String value = vsr.getPropertyString(key);
            if (value != null) {
                list.add(value);
            }
        }
    }
}
