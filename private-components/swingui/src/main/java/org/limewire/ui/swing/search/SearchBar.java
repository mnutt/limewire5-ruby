package org.limewire.ui.swing.search;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.collection.AutoCompleteDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.core.api.search.friend.FriendAutoCompleters;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.core.settings.SearchSettings;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.DropDownListAutoCompleteControl;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeComboBox;
import org.limewire.ui.swing.components.LimeComboBoxFactory;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.SwingUtils;
import org.limewire.xmpp.api.client.XMPPConnectionEvent;
import org.limewire.util.I18NConvert;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

/**
 * The search bar component at the top of the UI main window.  This includes
 * the drop-down search category box, search input field, and search button.
 */
@Singleton
public class SearchBar extends JXPanel {

    @Resource private Color searchBorder;
    
    private final LimeComboBox comboBox;
    private final LimePromptTextField searchField;
    private final IconButton searchButton;
    
    private final HistoryAndFriendAutoCompleter autoCompleter;
    private final FriendAutoCompleters friendLibraries;
    private final AutoCompleteDictionary searchHistory;
    
    private final Action programAction;
    
    private SearchCategory categoryToSearch; 
    
    
    @Inject
    public SearchBar(LimeComboBoxFactory comboBoxFactory, 
            final FriendAutoCompleters friendLibraries,
            @Named("searchHistory") AutoCompleteDictionary searchHistory,
            CategoryIconManager categoryIconManager) {
        super(new MigLayout("ins 0, gapx 0, gapy 0"));
    
        GuiUtils.assignResources(this);

        this.searchHistory = searchHistory;
        this.friendLibraries = friendLibraries;
        this.autoCompleter = new HistoryAndFriendAutoCompleter();
        this.categoryToSearch = SearchCategory.forId(SwingUiSettings.DEFAULT_SEARCH_CATEGORY_ID.getValue());
        
        Action actionToSelect = null;
        Action progAction = null;
        
        List<Action> typeActions = new LinkedList<Action>();
        for (SearchCategory cat : SearchCategory.values()) {
            if (cat == SearchCategory.OTHER) {
                continue;
            }

            Icon icon = null;
            if(cat != SearchCategory.ALL) {
                icon = categoryIconManager.getIcon(cat.getCategory());                
            }
            Action action = new CatagoryAction(cat, icon);
            if (cat == SearchCategory.PROGRAM) {
                progAction = action;
                continue;
            }

            if (cat == this.categoryToSearch) {
                actionToSelect = action;
            }

            typeActions.add(action);
        }
        
        programAction = progAction;
        
        comboBox = comboBoxFactory.createLightFullComboBox(typeActions);
        comboBox.setName("SearchBar.comboBox");
                
        searchField = new LimePromptTextField(I18n.tr("Search"), AccentType.BUBBLE, searchBorder);
        searchField.setName("SearchBar.searchField");
        searchField.setDocument(new SearchFieldDocument());
        
        searchButton = new IconButton();
        searchButton.removeActionHandListener();
        searchButton.setName("SearchBar.searchButton");
        searchButton.setFocusPainted(false);
        searchButton.setToolTipText(I18n.tr("Search P2P Network"));
        
        configureProgramCategory();
        
        final DropDownListAutoCompleteControl autoCompleteControl = DropDownListAutoCompleteControl.install(this.searchField, autoCompleter);
        autoCompleteControl.setAutoComplete(true);
        autoCompleter.setHistoryDictionary(searchHistory);
        addSearchActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get search text, and add to history if non-empty.
                String searchText = getSearchText();
                if (!searchText.isEmpty() && SwingUiSettings.KEEP_SEARCH_HISTORY.getValue()) {
                    SearchBar.this.searchHistory.addEntry(searchText);
                }
            }
        });
        SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtils.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        autoCompleter.setSuggestionsShown(SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.getValue());
                    }
                });
            }
        });
        
        autoCompleter.setSuggestionsShown(SwingUiSettings.SHOW_FRIEND_SUGGESTIONS.getValue());
        if (actionToSelect != null) {
            autoCompleter.setSuggestionDictionary(friendLibraries.getDictionary(categoryToSearch));
            comboBox.setSelectedAction(actionToSelect);
        } else {
            autoCompleter.setSuggestionDictionary(friendLibraries.getDictionary(SearchCategory.ALL));
        }
        
        setOpaque(false);
        add(comboBox);
        add(searchField, "gap 5");
        add(searchButton, "gap 5");

        LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SettingListener() {            
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        configureProgramCategory();                        
                    }
                });
            }            
        });
    }
    
    /**
     * Performs additional Guice injection tasks.  This method adds a listener
     * to handle XMPP connection events.
     */
    @Inject
    void register(ListenerSupport<XMPPConnectionEvent> connectionSupport) {
        connectionSupport.addListener(new EventListener<XMPPConnectionEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(XMPPConnectionEvent event) {
                switch (event.getType()) {
                case CONNECTED:
                    searchButton.setToolTipText(I18n.tr("Search P2P Network and Friends"));
                    break;
                default:
                    searchButton.setToolTipText(I18n.tr("Search P2P Network"));
                    break;
                }
            }
        });
    }
    
    private void configureProgramCategory() {
        if (!LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            comboBox.removeAction(this.programAction);
        } else {
            comboBox.addAction(this.programAction);
        }
    }
    
    public boolean requestSearchFocus() {
        // Request w/ temporary==true to indicate we don't
        // want to select everything.
        return searchField.requestFocus(true);
    }
    
    public void selectAllSearchText() {
        searchField.setCaretPosition(searchField.getDocument().getLength());
        searchField.selectAll();
    }
    
    public void setText(String text) {
        searchField.setText(text);
    }
    
    public void addSearchActionListener(ActionListener actionListener) {
        searchField.addActionListener(actionListener);
        searchButton.addActionListener(actionListener);
    }
    
    /**
     * Returns the search text with leading and trailing whitespace removed.
     * The method returns an empty string if there are no non-whitespace
     * characters.
     */
    public String getSearchText() {
        // Get search text and trim whitespace.
        String searchText = searchField.getText();
        return searchText != null ? searchText.trim() : "";
    }
    
    public SearchCategory getCategory() {
        return categoryToSearch;
    }
    
    private class CatagoryAction extends AbstractAction {
        private final SearchCategory category;
        
        CatagoryAction(SearchCategory category, Icon icon) {
            super(SearchCategoryUtils.getName(category));
            
            putValue(SMALL_ICON, icon);
            this.category = category;
        }
        
        @Override
        public void actionPerformed(ActionEvent arg0) {
            categoryToSearch = category;            
            autoCompleter.setSuggestionDictionary(friendLibraries.getDictionary(category));
        }
    }

    /**
     * Helper class that filters out all characters that make the search longer
     * than the maximum allowed length.  If characters entered make the search
     * field too long (normalized or not normalized), the system should beep.
     */
    private static class SearchFieldDocument extends PlainDocument {

        private static final int MAX_QUERY_LENGTH =
                SearchSettings.MAX_QUERY_LENGTH.getValue();


        @Override
        public void insertString(int offs,
                                 String str,
                                 AttributeSet a) throws BadLocationException {

            if(str == null) {
                return;
            }

            if(offs >= MAX_QUERY_LENGTH) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }

            // Normalized String are maybe longer or shorter than MAX_QUERY_LENGTH
            String norm = I18NConvert.instance().getNorm(str);
            if (getMaxLength() + Math.max(str.length(), norm.length()) > MAX_QUERY_LENGTH) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            super.insertString(offs, str, a);
        }

        /**
         * Returns the maximum length of the existing text normalized or not
         * normalized.
         */
        private int getMaxLength() {
            try {
                String text = getText(0, getLength());
                return Math.max(text.length(), I18NConvert.instance().getNorm(text).length());
            } catch (BadLocationException e) {
                return 0;
            }
        }
    }
}
