package org.limewire.ui.swing.library.table.menu.actions;

import java.awt.event.ActionEvent;

import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Opens the share widget for the given list of files. 
 */
public class ShareAction extends AbstractAction {
    
    private final ShareWidgetFactory shareFactory;
    private final LocalFileItem[] fileItemArray;
    public ShareAction(LocalFileItem[] fileItemArray, ShareWidgetFactory shareFactory) {
        super(I18n.tr("Share"));
        this.shareFactory = shareFactory;
        this.fileItemArray = fileItemArray;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ShareWidget<LocalFileItem[]> shareWidget = shareFactory.createMultiFileShareWidget();
        shareWidget.setShareable(fileItemArray);
        shareWidget.show(GuiUtils.getMainFrame());
    }
}