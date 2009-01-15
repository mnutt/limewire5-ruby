package org.limewire.ui.swing.library.sharing;

import java.awt.Component;

import org.limewire.core.api.Category;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.CategoryShareModel;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.impl.ThreadSafeList;

public class CategoryShareWidget implements ShareWidget<Category> {
    
    private LibrarySharePanel sharePanel;
    private Category category;
    private ShareListManager shareListManager;
    
    public CategoryShareWidget(ShareListManager shareListManager, ThreadSafeList<SharingTarget> allFriends, ShapeDialog shapeDialog){
        this.shareListManager = shareListManager;
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog);
    }
    
    @Override
    public void show(Component c) {
        sharePanel.show(null, new CategoryShareModel(shareListManager, category));
    }
    

    @Override
    public void dispose() {
        sharePanel.dispose();
    }
    

    @Override
    public void setShareable(Category category) {
        this.category = category;
        String catStr = category.getSingularName();
        sharePanel.setTitleLabel(I18n.tr("Share {0} collection", catStr));
        sharePanel.setTopLabel("Sharing collection with:");
        sharePanel.setBottomLabel(
                I18n.tr("Sharing your {0} collection shares new {1} files that automatically get added to your Library", catStr, catStr.toLowerCase()));
   
    }

}
