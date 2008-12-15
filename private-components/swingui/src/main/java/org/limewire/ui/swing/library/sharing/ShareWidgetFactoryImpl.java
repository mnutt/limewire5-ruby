package org.limewire.ui.swing.library.sharing;

import java.io.File;
import java.util.Collection;

import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.ShareListManager;
import org.limewire.ui.swing.components.ShapeDialog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class ShareWidgetFactoryImpl implements ShareWidgetFactory {
    
    private ShareWidget<File> fileShareWidget;
    
    private ShareWidget<Category> categoryShareWidget;

    private ShareWidget<LocalFileItem[]> multiFileShareWidget;
    
    private ShareWidget<LocalFileItem[]> multiFileUnshareWidget;

    private ShareListManager shareListManager;

    private Collection<Friend> allFriends;

    private ShapeDialog shapeDialog;
    
    @Inject
    public ShareWidgetFactoryImpl(ShareListManager shareListManager,
            @Named("known") Collection<Friend> allFriends, ShapeDialog shapeDialog){
        this.shareListManager = shareListManager;
        this.allFriends = allFriends;
        this.shapeDialog = shapeDialog;
    }

    @Override
    public ShareWidget<File> createFileShareWidget() {
        if(fileShareWidget == null){
            fileShareWidget = new FileShareWidget(shareListManager, allFriends, shapeDialog);
        }
        return fileShareWidget;
    }

    @Override
    public ShareWidget<LocalFileItem[]> createMultiFileShareWidget() {
        if(multiFileShareWidget == null){
            multiFileShareWidget = new MultiFileShareWidget(shareListManager, allFriends, shapeDialog);
        }
        return multiFileShareWidget;
    }

    @Override
    public ShareWidget<LocalFileItem[]> createMultiFileUnshareWidget() {
        if(multiFileUnshareWidget == null){
            multiFileUnshareWidget = new MultiFileUnshareWidget(shareListManager, allFriends, shapeDialog);
        }
        return multiFileUnshareWidget;
    }
    
    @Override
    public ShareWidget<Category> createCategoryShareWidget() {
        if(categoryShareWidget == null){
            categoryShareWidget = new CategoryShareWidget(shareListManager, allFriends, shapeDialog);
        }
        return categoryShareWidget;
    }

}
