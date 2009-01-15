package org.limewire.ui.swing.properties;

import java.awt.Color;
import java.awt.Font;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.MagnetLinkFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;

public class DialogParam {
    @Inject private IconManager iconManager;
    @Inject private PropertiableHeadings propertiableHeadings;
    @Inject private LibraryNavigator libraryNavigator;
    @Inject private MagnetLinkFactory magnetLinkFactory;
    @Inject private CategoryIconManager categoryIconManager;
    @Inject private FilterList filterList;
    @Inject private PropertyDictionary propertyDictionary;
    @Resource private Color backgroundColor;
    @Resource private Font smallFont;
    @Resource private Font mediumFont;
    @Resource private Font largeFont;
    
    public DialogParam() {
        GuiUtils.assignResources(this);
    }

    public IconManager getIconManager() {
        return iconManager;
    }

    public PropertiableHeadings getPropertiableHeadings() {
        return propertiableHeadings;
    }
    
    public LibraryNavigator getLibraryNavigator() {
        return libraryNavigator;
    }

    public MagnetLinkFactory getMagnetLinkFactory() {
        return magnetLinkFactory;
    }

    public CategoryIconManager getCategoryIconManager() {
        return categoryIconManager;
    }


    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Font getSmallFont() {
        return smallFont;
    }

    public Font getMediumFont() {
        return mediumFont;
    }

    public Font getLargeFont() {
        return largeFont;
    }

    public FilterList getFilterList() {
        return filterList;
    }
    
    public PropertyDictionary getPropertyDictionary() {
        return propertyDictionary;
    }
}
