package org.limewire.ui.swing.library.sharing;

import java.awt.Component;
import java.util.Collection;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.library.sharing.model.CategoryShareModel;
import org.limewire.ui.swing.util.I18n;

public class CategoryShareWidget implements ShareWidget<Category> {
private LibrarySharePanel sharePanel;
    
    public CategoryShareWidget(ShareListManager shareListManager, Collection<Friend> allFriends, ShapeDialog shapeDialog){
        sharePanel = new LibrarySharePanel(allFriends, shapeDialog);
        sharePanel.setShareModel(new CategoryShareModel(shareListManager));
    }
    
    @Override
    public void show(Component c) {
        sharePanel.show(c);
    }
    

    @Override
    public void dispose() {
        sharePanel.dispose();
    }
    

    @Override
    public void setShareable(Category category) {
        ((CategoryShareModel)sharePanel.getShareModel()).setCategory(category);
        String catStr = category.toString();
        sharePanel.setBottomLabel(
                I18n.tr("Sharing your {0} collection shares new {1} files that automatically get added to your Library", catStr, catStr.toLowerCase()));
   
    }

}
