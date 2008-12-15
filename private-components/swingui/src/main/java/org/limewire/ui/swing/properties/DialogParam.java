package org.limewire.ui.swing.properties;

import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;

public class DialogParam {
    @Inject private IconManager iconManager;
    @Inject private PropertiableHeadings propertiableHeadings;

    public IconManager getIconManager() {
        return iconManager;
    }

    public PropertiableHeadings getPropertiableHeadings() {
        return propertiableHeadings;
    }
}
