package org.limewire.ui.swing.options;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.ui.swing.search.SearchCategoryUtils;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SearchSettingListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * Search Option View
 */
@Singleton
public class SearchOptionPanel extends OptionPanel {

    private final AutoCompleteDictionary searchHistory;
    private SearchBarPanel searchBarPanel;
    private JCheckBox groupSimilarResults;

    @Inject
    public SearchOptionPanel(@Named("searchHistory") AutoCompleteDictionary searchHistory) {
        this.searchHistory = searchHistory;
        
        groupSimilarResults = new JCheckBox(I18n.tr("Group similar search results together"));
        groupSimilarResults.setContentAreaFilled(false);
        
        setLayout(new MigLayout("insets 15 15 15 15, fillx, wrap", "", ""));
        add(getSearchBarPanel(), "pushx, growx");
        
        add(groupSimilarResults, "gaptop 10, gapleft 15");
    }

    private OptionPanel getSearchBarPanel() {
        if (searchBarPanel == null) {
            searchBarPanel = new SearchBarPanel();
        }
        return searchBarPanel;
    }

    @Override
    boolean applyOptions() {
        SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.setValue(groupSimilarResults.isSelected());
        return getSearchBarPanel().applyOptions();
    }

    @Override
    boolean hasChanged() {
        return getSearchBarPanel().hasChanged() 
        || groupSimilarResults.isSelected() != SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue();
    }

    @Override
    public void initOptions() {
        getSearchBarPanel().initOptions();
        
        groupSimilarResults.setSelected(SwingUiSettings.GROUP_SIMILAR_RESULTS_ENABLED.getValue());
    }

    private class SearchBarPanel extends OptionPanel {

        private JComboBox defaultSearchSpinner;

        private JCheckBox searchTabNumberCheckBox;

        private JCheckBox suggestFriendFiles;

        private JButton clearNowButton;

        public SearchBarPanel() {
            super(I18n.tr("Search bar"));
            setLayout(new MigLayout("", "", "[][][]"));
            createComponents();

            add(new JLabel(I18n.tr("By default, search for")), "split");
            add(defaultSearchSpinner, "wrap");

            add(suggestFriendFiles, "split 4, wrap");

            add(searchTabNumberCheckBox, "split 4, push");

            add(clearNowButton, "wrap, alignx right");
        }

        private void createComponents() {
            defaultSearchSpinner = new JComboBox(SearchCategory.values());
            defaultSearchSpinner.setRenderer(new CategoryCellRenderer());
            defaultSearchSpinner.removeItem(SearchCategory.OTHER);

            LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SearchSettingListener(
                    LibrarySettings.ALLOW_PROGRAMS, SearchCategory.PROGRAM,
                    defaultSearchSpinner));

            suggestFriendFiles = new JCheckBox(I18n.tr("Suggest files from friends"));
            suggestFriendFiles.setContentAreaFilled(false);

            searchTabNumberCheckBox = new JCheckBox(I18n.tr("Remember my recent searches"));
            searchTabNumberCheckBox.setContentAreaFilled(false);
            
            clearNowButton = new JButton(I18n.tr("Clear Now"));
            clearNowButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    searchHistory.clear();
                }
            });
            clearNowButton.setBorderPainted(false);
        }

        @Override
        boolean applyOptions() {
            SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID
                    .setValue(((SearchCategory) defaultSearchSpinner.getSelectedItem()).getId());
            
            SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.setValue(suggestFriendFiles.isSelected());
            SwingUiSettings.KEEP_SEARCH_HISTORY.setValue(searchTabNumberCheckBox.isSelected());
            return false;
        }

        @Override
        boolean hasChanged() {
            return SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue() != ((SearchCategory) defaultSearchSpinner.getSelectedItem()).getId()
                    || SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.getValue() != suggestFriendFiles.isSelected()
                    || SwingUiSettings.KEEP_SEARCH_HISTORY.getValue() != searchTabNumberCheckBox.isSelected();
        }

        @Override
        public void initOptions() {
            defaultSearchSpinner.setSelectedItem(SearchCategory.forId(SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue()));
            suggestFriendFiles.setSelected(SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.getValue());
            searchTabNumberCheckBox.setSelected(SwingUiSettings.KEEP_SEARCH_HISTORY.getValue());
        }
    }
    
    private class CategoryCellRenderer extends DefaultListCellRenderer {
        public CategoryCellRenderer() {
            setOpaque(true);
        }
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            SearchCategory category = (SearchCategory)value;
            setText(SearchCategoryUtils.getOptionsName(category));
           
            return this;
        }
    }

}
