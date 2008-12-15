package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.upload.UploadItem;
import org.limewire.ui.swing.components.LimeProgressBarFactory;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.table.LimeSingleColumnTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.CategoryIconManager;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swing.EventTableModel;

public class UploadTable extends MouseableTable {
    private EventTableModel<UploadItem> model;

    public UploadTable(EventList<UploadItem> swingThreadSafeUploads, CategoryIconManager categoryIconManager, 
            LimeProgressBarFactory progressBarFactory, PropertiesFactory<UploadItem> propertiesFactory,
            LibraryNavigator libraryNavigator) {
        model = new EventTableModel<UploadItem>(swingThreadSafeUploads, new LimeSingleColumnTableFormat<UploadItem>());
        setModel(model);
        
        UploadActionHandler actionHandler = new UploadActionHandler(swingThreadSafeUploads, propertiesFactory, libraryNavigator);
        
        UploadTableRendererEditor editor = new UploadTableRendererEditor(categoryIconManager, progressBarFactory);
        editor.setActionHandler(actionHandler);
        getColumn(0).setCellEditor(editor);
        getColumn(0).setCellRenderer(new UploadTableRendererEditor(categoryIconManager, progressBarFactory));
        setRowHeight(editor.getPreferredSize().height);
        
        setPopupHandler(new UploadPopupHandler(this, actionHandler));
    }

    public UploadItem getUploadItem(int popupRow) {
        return model.getElementAt(popupRow);
    }

}
