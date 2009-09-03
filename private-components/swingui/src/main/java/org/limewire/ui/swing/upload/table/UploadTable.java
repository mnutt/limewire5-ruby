package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.table.LimeSingleColumnTableFormat;
import org.limewire.ui.swing.table.MouseableTable;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;

public class UploadTable extends MouseableTable {
    private DefaultEventTableModel<UploadItem> model;

    @Inject
    public UploadTable(UploadListManager uploadListManager, LibraryMediator libraryMediator, 
            LibraryManager libraryManager, FileInfoDialogFactory fileInfoFactory,
            UploadTableRendererEditor editor, UploadTableRendererEditor renderer) {
        model = new DefaultEventTableModel<UploadItem>(uploadListManager.getSwingThreadSafeUploads(), new LimeSingleColumnTableFormat<UploadItem>(UploadItem.class));
        setModel(model);
        
        setStripeHighlighterEnabled(false);
        setEmptyRowsPainted(false);
        setFillsViewportHeight(false);
        
        UploadActionHandler actionHandler = new UploadActionHandler(uploadListManager, libraryMediator, fileInfoFactory);
        
        editor.setActionHandler(actionHandler);
        getColumn(0).setCellEditor(editor);
        getColumn(0).setCellRenderer(renderer);
        setRowHeight(editor.getPreferredSize().height);
        
        setPopupHandler(new UploadPopupHandler(this, actionHandler, libraryManager));
    }

    public UploadItem getUploadItem(int popupRow) {
        return model.getElementAt(popupRow);
    }

}
