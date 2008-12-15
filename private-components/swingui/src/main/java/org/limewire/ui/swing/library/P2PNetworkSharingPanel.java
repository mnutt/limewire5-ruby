package org.limewire.ui.swing.library;

import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;

public class P2PNetworkSharingPanel extends SharingPanel {

    @Inject
    public P2PNetworkSharingPanel(
            LibraryManager libraryManager, 
            ShareListManager shareListManager,
            IconManager iconManager,
            CategoryIconManager categoryIconManager,
            LibraryTableFactory tableFactory,
            LimeHeaderBarFactory headerBarFactory,
            ButtonDecorator buttonDecorator) {
        super(libraryManager.getLibraryManagedList().getSwingModel(), 
                shareListManager.getGnutellaShareList(), categoryIconManager, 
                tableFactory, headerBarFactory);

        getHeaderPanel().setText(I18n.tr("Share with {0}", getFullPanelName()));

        createMyCategories(libraryManager.getLibraryManagedList().getSwingModel(),
                           shareListManager.getGnutellaShareList());
        selectFirst();
    }
    
    protected String getFullPanelName() {
        return I18n.tr("the P2P Network");
    }
    
    protected String getShortPanelName() {
        return I18n.tr("the P2P Network");
    } 
}
