package org.limewire.ui.swing.search;

import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.filter.AdvancedFilterPanel;
import org.limewire.ui.swing.filter.AdvancedFilterPanelFactory;
import org.limewire.ui.swing.filter.FilterableSource;
import org.limewire.ui.swing.friends.login.FriendActions;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.IconManager;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Factory implementation for creating an advanced filter panel for search
 * results.
 */
public class SearchFilterPanelFactory implements AdvancedFilterPanelFactory<VisualSearchResult> {

    private final TextFieldDecorator textFieldDecorator;
    private final FriendActions friendManager;
    private final Provider<IconManager> iconManager;
    
    /**
     * Constructs a SearchFilterPanelFactory with the specified UI decorators
     * and service managers.
     */
    @Inject
    public SearchFilterPanelFactory(TextFieldDecorator textFieldDecorator,
            FriendActions friendManager,
            Provider<IconManager> iconManager) {
        this.textFieldDecorator = textFieldDecorator;
        this.friendManager = friendManager;
        this.iconManager = iconManager;
    }
    
    @Override
    public AdvancedFilterPanel<VisualSearchResult> create(
            FilterableSource<VisualSearchResult> filterableSource) {
        return new AdvancedFilterPanel<VisualSearchResult>(filterableSource,
                textFieldDecorator, friendManager, iconManager);
    }
}
