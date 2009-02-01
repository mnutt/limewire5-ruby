package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.I18n;

/**
 * Opens the file info view for the given file. 
 */
public class ViewFileInfoAction extends AbstractAction {
    
    private final PropertiesFactory<LocalFileItem> propertiesFactory;
    private final LocalFileItem localFileItem;
    
    public ViewFileInfoAction(LocalFileItem localFileItem, PropertiesFactory<LocalFileItem> propertiesFactory) {
        super(I18n.tr("View File Info..."));
        this.propertiesFactory = propertiesFactory;
        this.localFileItem = localFileItem;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        propertiesFactory.newProperties().showProperties(localFileItem);
    }
}