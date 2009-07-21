package org.limewire.ui.swing.upload.table;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadListManager;
import org.limewire.ui.swing.components.RemoteHostWidgetFactory;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.properties.FileInfoDialogFactory;
import org.limewire.ui.swing.table.LimeSingleColumnTableFormat;
import org.limewire.ui.swing.table.MouseableTable;
import org.limewire.ui.swing.util.CategoryIconManager;

import ca.odell.glazedlists.swing.DefaultEventTableModel;

import com.google.inject.Inject;

public class UploadTable extends MouseableTable {
    private DefaultEventTableModel<UploadItem> model;

    @Inject
    public UploadTable(UploadListManager uploadListManager, CategoryIconManager categoryIconManager, 
            ProgressBarDecorator progressBarFactory, LibraryMediator libraryMediator, LibraryManager libraryManager,
            RemoteHostWidgetFactory remoteHostWidgetFactory, FileInfoDialogFactory fileInfoFactory) {
        model = new DefaultEventTableModel<UploadItem>(uploadListManager.getSwingThreadSafeUploads(), new LimeSingleColumnTableFormat<UploadItem>(UploadItem.class));
        setModel(model);
        
        setStripeHighlighterEnabled(false);
        setEmptyRowsPainted(false);
        setFillsViewportHeight(false);
        
        UploadActionHandler actionHandler = new UploadActionHandler(uploadListManager, libraryMediator, fileInfoFactory);
        
        UploadTableRendererEditor editor = new UploadTableRendererEditor(categoryIconManager, progressBarFactory, remoteHostWidgetFactory);
        editor.setActionHandler(actionHandler);
        getColumn(0).setCellEditor(editor);
        getColumn(0).setCellRenderer(new UploadTableRendererEditor(categoryIconManager, progressBarFactory, remoteHostWidgetFactory));
        setRowHeight(editor.getPreferredSize().height);
        
        setPopupHandler(new UploadPopupHandler(this, actionHandler, libraryManager));
    }

    public UploadItem getUploadItem(int popupRow) {
        return model.getElementAt(popupRow);
    }

}
