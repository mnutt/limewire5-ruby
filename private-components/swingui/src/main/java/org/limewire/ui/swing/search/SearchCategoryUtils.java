package org.limewire.ui.swing.search;

import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.util.I18n;

public class SearchCategoryUtils {
    
    private SearchCategoryUtils() {}
        
    public static String getName(SearchCategory category) {
        switch(category) {
        case ALL:      return I18n.trc("All (categories)", "All");
        case AUDIO:    return I18n.tr("Audio"); 
        case DOCUMENT: return I18n.tr("Documents"); 
        case IMAGE:    return I18n.tr("Images"); 
        case PROGRAM:  return I18n.tr("Programs"); 
        case VIDEO:    return I18n.tr("Videos"); 
        case OTHER: 
        default:
            return I18n.tr("Other");
             
        }
    }
    
    public static String getWhatsNewMenuName(SearchCategory category) {
        switch(category) {
        case ALL:      return I18n.trc("&All (categories)", "&All");
        case AUDIO:    return I18n.tr("A&udio"); 
        case DOCUMENT: return I18n.tr("&Documents"); 
        case IMAGE:    return I18n.tr("&Images"); 
        case PROGRAM:  return I18n.tr("&Programs"); 
        case VIDEO:    return I18n.tr("&Videos"); 
        case OTHER: 
        default:
            return I18n.tr("&Other");
             
        }
    }
    
    public static String getOptionsName(SearchCategory category) {
        switch(category) {
        case ALL:      return I18n.tr("All files");
        case AUDIO:    return I18n.tr("Only audio"); 
        case DOCUMENT: return I18n.tr("Only documents"); 
        case IMAGE:    return I18n.tr("Only images"); 
        case PROGRAM:  return I18n.tr("Only programs"); 
        case VIDEO:    return I18n.tr("Only videos"); 
        case OTHER: 
        default:
            return I18n.tr("Other");
             
        }
    }

}
