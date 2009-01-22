package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.ButtonDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.TransformedList;

/**
 * This is the framework for a Library Panel. It contains a Header
 * displaying the category being displayed/filter box. It contains
 * a left hand navigation bar where category buttons can be 
 * added to change the view of the library category on the right/main
 * panel
 */
abstract class AbstractFileListPanel extends JPanel implements Disposable {
    
    private final Map<Category, ButtonItem> categoryTables = new HashMap<Category, ButtonItem>();
    private final List<Category> categoryOrder = new ArrayList<Category>();
    private final List<Disposable> disposableList = new ArrayList<Disposable>();
    
    private final JPanel cardPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    
    private ButtonItem currentItem = null;

    private final LibrarySelectionPanel selectionPanel = new LibrarySelectionPanel();

    private final LimeHeaderBar headerPanel;    
    
    private final Next next = new Next();
    private final Prev prev = new Prev();

    private final LimePromptTextField filterField;        
    
    public AbstractFileListPanel(LimeHeaderBarFactory headerBarFactory) {        
        setLayout(new MigLayout("fill, gap 0, insets 0", "[][][grow]", "[][grow]"));

        cardPanel.setLayout(cardLayout);              
        filterField = createFilterField(I18n.tr("Search Library..."));
        headerPanel = createHeaderBar(headerBarFactory);
        headerPanel.setLayout(new MigLayout("insets 0, gap 0, fill, alignx right"));
        headerPanel.add(filterField, "gapbefore push, cell 1 0, gapafter 10");
        
        add(headerPanel, "span, grow, wrap");
        addMainPanels();
        
        addDisposable(selectionPanel);
    }
    
    protected void enableFilterBox(boolean value) {
        filterField.setEnabled(value);
    }
    
    protected abstract LimeHeaderBar createHeaderBar(LimeHeaderBarFactory headerBarFactory);
    protected abstract LimePromptTextField createFilterField(String prompt);
    
    protected LimeHeaderBar getHeaderPanel() {
        return headerPanel;
    }
    
    protected LibrarySelectionPanel getSelectionPanel() {
        return selectionPanel;
    }
    
    protected void addMainPanels() {
        add(selectionPanel, "growy");
        // TODO: move to properties -- funky because this class gets subclassed.
        add(Line.createVerticalLine(Color.decode("#696969")), "growy, width 1!");
        add(cardPanel, "grow");
    }
    
    protected void addButtonToHeader(Action action, ButtonDecorator buttonDecorator) {        
        JXButton shareButton = new JXButton(action);
        buttonDecorator.decorateDarkFullButton(shareButton);
        headerPanel.add(shareButton, "cell 0 0, push");
    }
    
    protected void addButtonToHeader(Action action, ButtonDecorator buttonDecorator, 
            AccentType accentType) {        
        JXButton shareButton = new JXButton(action);
        buttonDecorator.decorateDarkFullButton(shareButton, accentType);
        headerPanel.add(shareButton, "cell 0 0, push");
    }
    
    protected void addHeaderComponent(JComponent player, Object constraints) {
        headerPanel.add(player, constraints);
    }
    
    protected void setInnerNavLayout(LayoutManager layout) {
        selectionPanel.updateLayout(layout);
    }
    
    /**
	 * Selects which category to show in the info panel. If Id
	 * is null, than no category should be shown. This is accomplished
     * be hiding the entire card layout.
	 */
    protected void select(Category id) {
        // Clear filter text when category view changes.
        Category oldCategory = (currentItem != null) ? currentItem.getCategory() : null;
        if (id != oldCategory) {
            filterField.setText(null);
        }
        
        if(currentItem != null)
            currentItem.fireSelected(false);
        
        if(id != null) {
            currentItem = categoryTables.get(id);
            currentItem.fireSelected(true);
            cardLayout.show(cardPanel, id.name());
        }    
        selectionPanel.showCard(id);
    }
    
    protected void selectFirstVisible() {
        select(getNext(0));
    }
    
    protected JTextComponent getFilterTextField() {
        return filterField;
    }
    
    protected void addDisposable(Disposable disposable) {
        disposableList.add(disposable);
    }
    
    protected void addDisposable(final TransformedList<?, ?> list) {
        disposableList.add(new Disposable() {
            @Override
            public void dispose() {
                list.dispose();
            }
        });
    }
    
    /** Adds the given category to the list of categories in the left. */
    protected <T extends FileItem> void addCategory(Icon icon, Category category, 
            JComponent component, FilterList<T> filteredList, CategorySelectionCallback callback) {
        addCategory(icon, category, component, null, filteredList, callback);
    }
    
    /** Adds the given category to the list of categories in the left. */
    protected <T extends FileItem> void addCategory(Icon icon, Category category, 
            JComponent component, FilterList<T> filteredAllFileList, FilterList<T> filteredList,
            CategorySelectionCallback callback) {
        cardPanel.add(component, category.name());
        
        ButtonItem item = new ButtonItemImpl(category);
        categoryTables.put(category, item);
        categoryOrder.add(category);
        
        Action action = new SelectionAction(icon, category, item, callback);
        JComponent button = createCategoryButton(action, category, filteredAllFileList);
        
        ((ButtonItemImpl) item).setAction(action);
        
        addCategorySizeListener(category, action, filteredAllFileList, filteredList);
        selectionPanel.add(button, "growx");
    }
    
    /** Adds a category to the InnerNav Info bar for My Library views*/
    protected<T extends FileItem> void addLibraryInfoBar(Category category, EventList<T> fileList) {
        selectionPanel.addCard(category, fileList, null, null, false);
    }
    
    /** Adds a category to the InnerNav Info bar for Sharing views*/
    protected<T extends FileItem> void addSharingInfoBar(Category category, EventList<T> fileList, FriendFileList friendList, FilterList<T> sharedList) {
        selectionPanel.addCard(category, fileList, friendList, sharedList, false);
    }
    
    /** Adds a category to the InnerNav Info bar for Friend views*/
    protected<T extends FileItem> void addFriendInfoBar(Category category, EventList<T> fileList) {
        selectionPanel.addCard(category, fileList, null, null, true);
    }
    
    /** Adds a listener to the category so things can bind to the action, if necessary. */
    protected abstract <T extends FileItem> void addCategorySizeListener(Category category, Action action,
            FilterList<T> filteredAllFileList, FilterList<T> filteredList);
    
    
    /** Creates the category button & adds navigation listeners to it. */
    protected <T extends FileItem> JComponent createCategoryButton(Action action, Category category, FilterList<T> filteredAllFileList) {
        SelectionPanel component = new SelectionPanel(action, this);
        addNavigation(component.getButton());
        return component;
    }    

    @Override
    public void dispose() {
        for(Disposable disposable : disposableList)
            disposable.dispose();
    }
    
    private void selectNext() {
        for(int i = 0; i < categoryOrder.size(); i++) {
            if(categoryOrder.get(i).equals(currentItem.getCategory())) {
                if(i == categoryOrder.size() -1)
                    select(getNext(0));
                else
                    select(getNext(i+1));
                break;
            }
        }
    }
    
    /**
     * Returns the next visible category. If only the currently selected category
     * is visible, returns that category.
     */
    private Category getNext(int selectedIndex) {
        for(int i = selectedIndex; i < categoryOrder.size() + selectedIndex; i++) {
            int index = i % categoryOrder.size();
            ButtonItem current = categoryTables.get(categoryOrder.get(index));
            if(current.isEnabled())
                return categoryOrder.get(index);
        }
        // if no categories are visible, return null
        return null;
    }
    
    private void selectPrev() {
        for(int i = 0; i < categoryOrder.size(); i++) {
            if(categoryOrder.get(i).equals(currentItem.getCategory())) {
                if(i == 0) {
                    select(getPrev(categoryOrder.size()-1));
                } else
                    select(getPrev(i-1));
                break;
            }
        }
    }
    
    /**
     * Returns the previous visible category. If only the currently selected category
     * is visible, returns that category.
     */
    private Category getPrev(int selectedIndex) {
        for(int i = categoryOrder.size() + selectedIndex; i > categoryOrder.size(); i--) {
            int index = i % categoryOrder.size();
            ButtonItem current = categoryTables.get(categoryOrder.get(index));
            if(current.isEnabled())
                return categoryOrder.get(index);
        }
        return categoryOrder.get(selectedIndex);
    }
    
    private class Next extends AbstractAction {
        final static String KEY = "MOVE_DOWN";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectNext();
        }
    }
    
    private class Prev extends AbstractAction {
        final static String KEY = "MOVE_UP";
        
        @Override
        public void actionPerformed(ActionEvent e) {
            selectPrev();
        }
    }
    
    private class SelectionAction extends AbstractAction {
        
        public SelectionAction(Icon icon, Category category, ButtonItem buttonItem, final CategorySelectionCallback callback) {
            super(I18n.tr(category.toString()), icon);
            
            putValue("limewire.category", category);
            
            buttonItem.addButtonItemListener(new ButtonItemListener(){
                @Override
                public void itemSelect(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                    
                    if (callback != null) {
                        callback.categorySelected((Category)getValue("limewire.category"), selected);
                    }
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            select((Category)getValue("limewire.category"));
        }
    }
    
    /**
     * Allows a category creator to define a method to notify back any selection
     *  events on this category 
     */
    protected interface CategorySelectionCallback {
        public void categorySelected(Category category, boolean selected);
    }
    
    private interface ButtonItem {
        
        public void addButtonItemListener(ButtonItemListener listener);
        
        public void fireSelected(boolean selected);
        
        public void select();
        
        public Category getCategory();
        
        public boolean isEnabled();
    }
    
    private interface ButtonItemListener {
        public void itemSelect(boolean selected);
    }
    
    private class ButtonItemImpl implements ButtonItem {
        
        private final List<ButtonItemListener> listeners = new CopyOnWriteArrayList<ButtonItemListener>();
        
        private Category category;
        private Action action;
        
        public ButtonItemImpl(Category category) {
            this.category = category;
        }
        
        public void setAction(Action action) {
            this.action = action;
        }
        
        @Override
        public boolean isEnabled() {
            if(action == null)
                return false;
            else
                return action.isEnabled();
        }

        @Override
        public void select() {
            AbstractFileListPanel.this.select(getCategory());
        }

        @Override
        public Category getCategory() {
            return category;
        }
        
        @Override
        public void addButtonItemListener(ButtonItemListener listener) {
            listeners.add(listener);
        }

        @Override
        public void fireSelected(boolean selected) {
            for(ButtonItemListener listener : listeners)
                listener.itemSelect(selected);
        }
    }
    
    protected void addNavigation(JComponent component) {
        component.getActionMap().put(Next.KEY, next);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), Next.KEY);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), Next.KEY);
        
        component.getActionMap().put(Prev.KEY, prev);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), Prev.KEY);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), Prev.KEY);
    }
    
    private static class SelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Font selectedTextFont;
        @Resource Color selectedTextColor;
        @Resource Font textFont;
        @Resource Color textColor;
        
        private JButton button;
        private AbstractFileListPanel libraryPanel;
        
        public SelectionPanel(Action action, AbstractFileListPanel library) {
            super(new BorderLayout());

            this.libraryPanel = library;
            
            GuiUtils.assignResources(this);
            
            button = new JButton(action);
            
            add(button, BorderLayout.CENTER);
            
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            
            setOpaque(false);

            button.getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(Boolean.TRUE.equals(evt.getNewValue())) {
                            setOpaque(true);
                            setBackground(selectedBackground);
                            button.setForeground(selectedTextColor);
                        } else {
                            setOpaque(false);
                            button.setForeground(textColor);
                        }
                        repaint();
                    } else if(evt.getPropertyName().equals("enabled")) {
                        boolean value = (Boolean)evt.getNewValue();
                        setVisible(value);
                        //select first category if this category is hidden
                        if(value == false && button.getAction().getValue(Action.SELECTED_KEY) != null && 
                                button.getAction().getValue(Action.SELECTED_KEY).equals(Boolean.TRUE)) {
                            libraryPanel.selectFirstVisible();
                        }
                    }
                }
            });
        }        
        
        public JButton getButton() {
            return button;
        }
    }
}
