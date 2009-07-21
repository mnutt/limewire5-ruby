package org.limewire.ui.swing.search.advanced;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.FriendAutoCompleterFactory;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchCategory;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.search.KeywordAssistedSearchBuilder;
import org.limewire.ui.swing.search.SearchInfo;
import org.limewire.ui.swing.search.UiSearchListener;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;

/** The container for advanced searching. */
public class AdvancedSearchPanel extends JXPanel implements Disposable {
        
    private final FriendAutoCompleterFactory friendAutoCompleterFactory;
    private final KeywordAssistedSearchBuilder advancedSearchBuilder;
    private final PropertyDictionary propertyDictionary;
    private final ButtonDecorator buttonDecorator;
    
    private final Action searchAction = new SearchAction();
    
    /* CoW so that listeners can modify the list of listeners. */ 
    private final List<UiSearchListener> uiSearchListeners = new CopyOnWriteArrayList<UiSearchListener>();
    private Map<Category, AdvancedPanel> advancedPanels = new HashMap<Category, AdvancedPanel>();
    
    private AdvancedPanel visibleComponent = null;
    private final JPanel selectorPanel;
    private final JPanel inputPanel;
   
    @Resource private Font headingFont;

    private final JXButton searchButton = new JXButton(searchAction);
    
    @Inject
    public AdvancedSearchPanel(PropertyDictionary propertyDictionary, FriendAutoCompleterFactory friendAutoCompleterFactory,
            HeaderBarDecorator headerDecorator, ButtonDecorator buttonDecorator, KeywordAssistedSearchBuilder advancedSearchBuilder) {
        
        this.propertyDictionary = propertyDictionary;
        this.friendAutoCompleterFactory = friendAutoCompleterFactory;
        this.buttonDecorator = buttonDecorator;
        this.advancedSearchBuilder = advancedSearchBuilder;
        
        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, gap 0, hidemode 3"));
        
        HeaderBar heading = new HeaderBar(I18n.tr("Advanced Search"));
        headerDecorator.decorateBasic(heading);
        JLabel description = new JLabel(I18n.tr("Search for the following category:"));
        description.setFont(headingFont);
        add(heading, "dock north");
        add(description, "gapleft 15, gaptop 15, wrap");
        
        selectorPanel = new JPanel(new FlowLayout());
        selectorPanel.setOpaque(false);
        add(selectorPanel, "gapleft 30, gaptop 5, wrap");

        inputPanel = new JPanel(new MigLayout("insets 0, gap 0, hidemode 3, wrap"));
        
        addCategory(SearchCategory.AUDIO);
        addCategory(SearchCategory.VIDEO);
        addCategory(SearchCategory.IMAGE);
        addCategory(SearchCategory.DOCUMENT);
        addCategory(SearchCategory.PROGRAM);
        
        buttonDecorator.decorateGreenFullButton(searchButton);
        searchButton.setFont(headingFont);
        searchButton.setVisible(false);
        JPanel searchButtonPanel = new JPanel(new MigLayout("insets 0, gap 0, fill"));
        searchButtonPanel.add(searchButton, "dock east");
        inputPanel.add(searchButtonPanel, "dock south, gapbefore push, gapright 5, gaptop 5");
        add(inputPanel, "gapleft 45, gaptop 4");
    }

    
    /**
	 * Creates the correct AdvancedPanel based on the category 
	 * selected.
	 */
    private AdvancedPanel createAdvancedPanel(Category category) {
        Action enterKeyAction = new AbstractAction("pressed") { 
            public void actionPerformed(ActionEvent e) {
                searchButton.doClick();
            }
        };
        AdvancedPanel panel = null;
        if(category == Category.AUDIO) {
            panel =  new AdvancedAudioPanel(propertyDictionary, friendAutoCompleterFactory, enterKeyAction);
        } else if(category == Category.VIDEO) {
            panel = new AdvancedVideoPanel(propertyDictionary, friendAutoCompleterFactory, enterKeyAction);
        } else if(category == Category.DOCUMENT) {
            panel = new AdvancedDocumentPanel(friendAutoCompleterFactory, enterKeyAction);
        } else if(category == Category.IMAGE) {
            panel = new AdvancedImagePanel(friendAutoCompleterFactory, enterKeyAction);
        } else if(category == Category.PROGRAM) {
            panel = new AdvancedProgramPanel(friendAutoCompleterFactory, enterKeyAction);
        }
        
        return panel;
    }

    private void addCategory(final SearchCategory category) {
        final JXButton button = new JXButton(I18n.tr(category.getCategory().getPluralName()));
                
        button.setModel(new JToggleButton.ToggleButtonModel());
        buttonDecorator.decorateLinkButton(button);
        button.setFont(headingFont);
        FontUtils.underline(button);
        
        selectorPanel.add(button);
        
        button.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    
                    AdvancedPanel component = advancedPanels.get(category.getCategory());
                    if(component == null) {
                        component = createAdvancedPanel(category.getCategory());
                        advancedPanels.put(category.getCategory(), component);
                        
                        ResizeUtils.forceWidth(component, 300);
                        inputPanel.add(component);
                    }
                    
                    // Update visibility so invisible panels don't effect size
                    if (visibleComponent != null) {
                        visibleComponent.setVisible(false);
                    }
                    component.setVisible(true);
                    visibleComponent = component;
                    
                    updateSelection(button);
                    searchButton.setVisible(true);
                }
            }
        });
    }

    private void updateSelection(JButton button) {
        button.setSelected(true);
        for ( Component comp : selectorPanel.getComponents() ) {
            if (comp instanceof JButton && button != comp) {
                ((JButton)comp).setSelected(false);
            }
        }
    }
    
    /** Adds a listener that will be notified when an advanced search is triggered. */
    public void addSearchListener(UiSearchListener uiSearchListener) {
        uiSearchListeners.add(uiSearchListener);
    }

    /**
     * Action that creates a search and notifies all listeners.
     */
    private class SearchAction extends AbstractAction {
        
        public SearchAction() {
            super(I18n.tr("Search"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if(visibleComponent != null) {
                
                Map<FilePropertyKey,String> searchData = visibleComponent.getSearchData();
                
                if(searchData != null) {
                    
                    SearchInfo info = advancedSearchBuilder.createAdvancedSearch(searchData,
                            visibleComponent.getSearchCategory());
                    
                    for(UiSearchListener uiSearchListener : uiSearchListeners) {
                        uiSearchListener.searchTriggered(info);
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        uiSearchListeners.clear();
    }
}
