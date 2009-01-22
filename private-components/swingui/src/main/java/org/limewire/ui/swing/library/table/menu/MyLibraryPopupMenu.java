package org.limewire.ui.swing.library.table.menu;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

import javax.swing.JPopupMenu;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.table.menu.actions.DeleteAction;
import org.limewire.ui.swing.library.table.menu.actions.LaunchFileAction;
import org.limewire.ui.swing.library.table.menu.actions.LocateFileAction;
import org.limewire.ui.swing.library.table.menu.actions.PlayAction;
import org.limewire.ui.swing.library.table.menu.actions.RemoveAction;
import org.limewire.ui.swing.library.table.menu.actions.ShareAction;
import org.limewire.ui.swing.library.table.menu.actions.UnshareAction;
import org.limewire.ui.swing.library.table.menu.actions.ViewFileInfoAction;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * Popup menu implementation for MyLibrary
 */
public class MyLibraryPopupMenu extends JPopupMenu {

    private List<LocalFileItem> fileItems;

    private final LibraryManager libraryManager;

    private final Category category;

    private final PropertiesFactory<LocalFileItem> propertiesFactory;

    private final ShareWidgetFactory shareFactory;

    public MyLibraryPopupMenu(Category category, LibraryManager libraryManager,
            ShareWidgetFactory shareFactory, PropertiesFactory<LocalFileItem> propertiesFactory) {
        this.libraryManager = libraryManager;
        this.shareFactory = shareFactory;
        this.category = category;
        this.propertiesFactory = propertiesFactory;
    }

    public void setFileItems(List<LocalFileItem> items) {
        this.fileItems = items;
        initialize();
    }

    private void initialize() {
        boolean singleFile = fileItems.size() == 1;
        boolean multiFile = fileItems.size() > 1;

        LocalFileItem firstItem = fileItems.get(0);

        boolean playActionEnabled = singleFile;
        boolean launchActionEnabled = singleFile;
        boolean locateActionEnabled = singleFile && !firstItem.isIncomplete();
        boolean viewFileInfoEnabled = singleFile;
        boolean shareActionEnabled = false;
        boolean removeActionEnabled = false;
        boolean deleteActionEnabled = false;

        for (LocalFileItem localFileItem : fileItems) {
            if (localFileItem.isShareable()) {
                shareActionEnabled = true;
                break;
            }
        }
        
        for(LocalFileItem localFileItem : fileItems) {
            if(!localFileItem.isIncomplete()) {
                removeActionEnabled = true;
                deleteActionEnabled = true;
                break;
            }
        }

        removeAll();
        switch (category) {
        case AUDIO:
        case VIDEO:
            add(new PlayAction(firstItem)).setEnabled(playActionEnabled);
            break;
        case IMAGE:
        case DOCUMENT:
            add(new LaunchFileAction(I18n.tr("View"), firstItem)).setEnabled(launchActionEnabled);
            break;
        case PROGRAM:
        case OTHER:
            add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);
        }
        if (singleFile) {
            add(new SingleFileShareAction(firstItem, shareFactory)).setEnabled(shareActionEnabled);
        } else {
            add(new ShareAction(createFileItemArray(), shareFactory))
                    .setEnabled(shareActionEnabled);
        }

        if (multiFile) {
            add(new UnshareAction(createFileItemArray(), shareFactory)).setEnabled(
                    shareActionEnabled);
        }

        addSeparator();
        if (category != Category.PROGRAM && category != Category.OTHER) {
            add(new LocateFileAction(firstItem)).setEnabled(locateActionEnabled);
        }

        add(new RemoveAction(createFileItemArray(), libraryManager)).setEnabled(removeActionEnabled);
        
        add(new DeleteAction(createFileItemArray(), libraryManager)).setEnabled(deleteActionEnabled);

        addSeparator();
        add(new ViewFileInfoAction(firstItem, propertiesFactory)).setEnabled(viewFileInfoEnabled);
    }

    LocalFileItem[] createFileItemArray() {
        return fileItems.toArray(new LocalFileItem[fileItems.size()]);
    };

    private class SingleFileShareAction extends AbstractAction {
        private final ShareWidgetFactory shareWidgetFactory;
        private final LocalFileItem localFileItem;
        
        public SingleFileShareAction(LocalFileItem localFileItem, ShareWidgetFactory shareWidgetFactory) {
            super(I18n.tr("Share"));
            this.localFileItem = localFileItem;
            this.shareWidgetFactory = shareWidgetFactory;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            ShareWidget<File> shareWidget = shareWidgetFactory.createFileShareWidget();
            shareWidget.setShareable(localFileItem.getFile());
            shareWidget.show(GuiUtils.getMainFrame());
        }
    }
}
