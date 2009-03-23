package org.limewire.ui.swing.library;

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
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HeaderBar;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.components.PromptTextField;
import org.limewire.ui.swing.components.decorators.ButtonDecorator;
import org.limewire.ui.swing.components.decorators.HeaderBarDecorator;
import org.limewire.ui.swing.components.decorators.TextFieldDecorator;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
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
    
    private final Map<Catalog, ButtonItem> catalogTables = new HashMap<Catalog, ButtonItem>();
    private final List<Catalog> catalogOrder = new ArrayList<Catalog>();
    private final List<Disposable> disposableList = new ArrayList<Disposable>();
    
    private final JPanel cardPanel = new JPanel();
    private final CardLayout cardLayout = new CardLayout();
    
    private ButtonItem currentItem = null;

    private final LibrarySelectionPanel selectionPanel = new LibrarySelectionPanel();

    private final HeaderBar headerPanel;    
    
    private final Next next = new Next();
    private final Prev prev = new Prev();

    private PromptTextField filterField;        
    
    public AbstractFileListPanel(HeaderBarDecorator headerBarFactory, TextFieldDecorator textFieldDecorator) {        
        cardPanel.setLayout(cardLayout);              
        filterField = createFilterField(textFieldDecorator, I18n.tr("Find..."));
        headerPanel = createHeaderBar(headerBarFactory);
        headerPanel.setLayout(new MigLayout("insets 0, gap 0, fill, alignx right"));
        
        if (filterField != null) {
        headerPanel.add(filterField, "gapbefore push, cell 1 0, gapafter 10");
        }
        
        layoutComponent();
        
        addDisposable(selectionPanel);
    }
    
    protected void layoutComponent() {
        setLayout(new MigLayout("fill, gap 0, insets 0"));
        
        addHeaderPanel();
        addNavPanel();
        addMainPanels();
    }
    
    protected abstract HeaderBar createHeaderBar(HeaderBarDecorator headerBarFactory);
    protected abstract PromptTextField createFilterField(TextFieldDecorator decorator, String prompt);
    
    protected HeaderBar getHeaderPanel() {
        return headerPanel;
    }
    
    protected LibrarySelectionPanel getSelectionPanel() {
        return selectionPanel;
    }
    
    protected void addHeaderPanel() {
        add(headerPanel, "dock north, growx");       
    }
    
    protected void addNavPanel() {
        add(selectionPanel, "dock west");
        // TODO: move to properties -- funky because this class gets subclassed.
        add(Line.createVerticalLine(Color.decode("#696969")), "dock west, width 1!");        
    }
    
    protected void addMainPanels() {
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
     * Selects the specified catalog for display.  If <code>catalog</code> is
     * null, then no catalog is displayed.
     */
    protected void select(Catalog catalog) {
        // Clear filter text when catalog view changes.
        Catalog oldCatalog = (currentItem != null) ? currentItem.getCatalog() : null;
        if ((catalog == null) || !catalog.equals(oldCatalog)) {
            filterField.setText(null);
        }
        
        if (currentItem != null) {
            currentItem.fireSelected(false);
        }
        
        if (catalog != null) {
            currentItem = catalogTables.get(catalog);
            currentItem.fireSelected(true);
            cardLayout.show(cardPanel, catalog.getId());
        }    
        selectionPanel.showCard(catalog);
    }
    
    /**
	 * Selects the specified category to display in the info panel.  If 
	 * <code>category</code> is null, than no category should be shown.  This 
	 * is accomplished by hiding the entire card layout.
	 */
    protected void select(Category category) {
        Catalog catalog = (category != null) ? new Catalog(category) : null;
        select(catalog);
    }    
    
    protected Category getSelectedCategory() {
        return (currentItem != null) ? currentItem.getCatalog().getCategory() : null;
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
    
    /**
     * Adds the specified catalog to the available list in the container.
     */
    protected <T extends FileItem> void addCatalog(Icon icon, Catalog catalog,
            JComponent component, FilterList<T> filteredAllFileList, FilterList<T> filteredList,
            CatalogSelectionCallback callback) {
        cardPanel.add(component, catalog.getId());
        
        ButtonItemImpl item = new ButtonItemImpl(catalog);
        catalogTables.put(catalog, item);
        catalogOrder.add(catalog);
        
        Action action = new SelectionAction(icon, catalog, item, callback);
        JComponent button = createCatalogButton(action, catalog, filteredAllFileList);
        
        item.setAction(action);
        
        addCatalogSizeListener(catalog, action, filteredAllFileList, filteredList);
        selectionPanel.add(button, "growx");
    }
    
    /** Adds the given category to the list of categories in the left. */
    protected <T extends FileItem> void addCategory(Icon icon, Category category, 
            JComponent component, FilterList<T> filteredList, CatalogSelectionCallback callback) {
        addCategory(icon, category, component, null, filteredList, callback);
    }
    
    /** Adds the given category to the list of categories in the left. */
    protected <T extends FileItem> void addCategory(Icon icon, Category category, 
            JComponent component, FilterList<T> filteredAllFileList, FilterList<T> filteredList,
            CatalogSelectionCallback callback) {
        Catalog catalog = new Catalog(category); 
        cardPanel.add(component, catalog.getId());
        
        ButtonItemImpl item = new ButtonItemImpl(catalog);
        catalogTables.put(catalog, item);
        catalogOrder.add(catalog);
        
        Action action = new SelectionAction(icon, catalog, item, callback);
        JComponent button = createCategoryButton(action, category, filteredAllFileList);
        
        item.setAction(action);
        
        addCategorySizeListener(category, action, filteredAllFileList, filteredList);
        selectionPanel.add(button, "growx");
    }
    
    /**
     * Adds the specified heading component to the category selection panel on
     * the left-side of the display.  
     */
    protected <T extends FileItem> void addHeading(JComponent heading, Catalog.Type catalog) {
        selectionPanel.addHeading(heading, catalog);
    }
    
    /** Adds a catalog to the InnerNav Info bar for My Library views*/
    protected<T extends FileItem> void addLibraryInfoBar(Catalog catalog, EventList<T> fileList) {
        selectionPanel.addCard(catalog, fileList, false);
    }
    
    /** Adds a category to the InnerNav Info bar for My Library views*/
    protected<T extends FileItem> void addLibraryInfoBar(Category category, EventList<T> fileList) {
        selectionPanel.addCard(category, fileList, false);
    }
    
    /** Adds a category to the InnerNav Info bar for Friend views*/
    protected<T extends FileItem> void addFriendInfoBar(Category category, EventList<T> fileList) {
        selectionPanel.addCard(category, fileList, true);
    }

    /** Adds a listener to the catalog so things can bind to the action. */
    protected <T extends FileItem> void addCatalogSizeListener(Catalog catalog,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        // Do nothing.
    }
    
    /** Adds a listener to the category so things can bind to the action, if necessary. */
    protected abstract <T extends FileItem> void addCategorySizeListener(Category category, Action action,
            FilterList<T> filteredAllFileList, FilterList<T> filteredList);
    
    /** Creates a catalog selection button using the specified action. */
    protected <T extends FileItem> JComponent createCatalogButton(Action action,
            Catalog catalog, FilterList<T> filteredAllFileList) {
        SelectionPanel component = new SelectionPanel(action, this);
        addNavigation(component.getButton());
        return component;
    }    

    /** Creates the category button & adds navigation listeners to it. */
    protected <T extends FileItem> JComponent createCategoryButton(Action action,
            Category category, FilterList<T> filteredAllFileList) {
        return createCatalogButton(action, new Catalog(category), filteredAllFileList);
    }

    @Override
    public void dispose() {
        for(Disposable disposable : disposableList)
            disposable.dispose();
    }
    
    private void selectNext() {
        for(int i = 0; i < catalogOrder.size(); i++) {
            if(catalogOrder.get(i).equals(currentItem.getCatalog())) {
                if(i == catalogOrder.size() -1)
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
    private Catalog getNext(int selectedIndex) {
        for(int i = selectedIndex; i <= catalogOrder.size() + selectedIndex; i++) {
            int index = i % catalogOrder.size();
            ButtonItem current = catalogTables.get(catalogOrder.get(index));
            if(current.isEnabled())
                return catalogOrder.get(index);
        }
        // if no categories are visible, return null
        return null;
    }
    
    private void selectPrev() {
        for(int i = 0; i < catalogOrder.size(); i++) {
            if(catalogOrder.get(i).equals(currentItem.getCatalog())) {
                if(i == 0) {
                    select(getPrev(catalogOrder.size()-1));
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
    private Catalog getPrev(int selectedIndex) {
        for(int i = catalogOrder.size() + selectedIndex; i >= selectedIndex; i--) {
            int index = i % catalogOrder.size();
            ButtonItem current = catalogTables.get(catalogOrder.get(index));
            if(current.isEnabled())
                return catalogOrder.get(index);
        }
        // if no categories are visible, return null
        return null;
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
    
    protected void playListSelected(boolean value) {
    }
        
    private class SelectionAction extends AbstractAction {
        public SelectionAction(Icon icon, final Catalog catalog, ButtonItem buttonItem, final CatalogSelectionCallback callback) {
            super(catalog.getName(), icon);
            
            putValue("limewire.category", catalog);
            
            buttonItem.addButtonItemListener(new ButtonItemListener(){
                @Override
                public void itemSelect(boolean selected) {
                    putValue(SELECTED_KEY, selected);
                    
                    if(catalog.getType() == Catalog.Type.PLAYLIST) {
                        playListSelected(selected);
                    }
                    
                    if (callback != null) {
                        callback.catalogSelected((Catalog)getValue("limewire.category"), selected);
                    }
                }
            });
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            select((Catalog)getValue("limewire.category"));
        }
    }
    
    /**
     * Allows a catalog creator to define a method to receive notification
     * messages on any selection events on this catalog.
     */
    protected interface CatalogSelectionCallback {
        public void catalogSelected(Catalog catalog, boolean selected);
    }
    
    private interface ButtonItem {
        
        public void addButtonItemListener(ButtonItemListener listener);
        
        public void fireSelected(boolean selected);
        
        public void select();
        
        public Catalog getCatalog();
        
        public boolean isEnabled();
    }
    
    private interface ButtonItemListener {
        public void itemSelect(boolean selected);
    }
    
    private class ButtonItemImpl implements ButtonItem {
        
        private final List<ButtonItemListener> listeners = new CopyOnWriteArrayList<ButtonItemListener>();
        
        private Catalog catalog;
        private Action action;
        
        public ButtonItemImpl(Catalog catalog) {
            this.catalog = catalog;
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
            AbstractFileListPanel.this.select(getCatalog());
        }

        @Override
        public Catalog getCatalog() {
            return catalog;
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
        @Resource Color selectedTextColor;
        @Resource Font textFont;
        @Resource Color textColor;
        
        private JButton button;
        private AbstractFileListPanel libraryPanel;
        
        public SelectionPanel(Action action, AbstractFileListPanel library) {
            super(new MigLayout("insets 0, fill, hidemode 3"));

            this.libraryPanel = library;
            
            GuiUtils.assignResources(this);
            setOpaque(false);
            
            button = new JButton(action);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,8,2,8));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setOpaque(false);
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
            
            add(button, "growx, push");
        }        
        
        public JButton getButton() {
            return button;
        }
    }
}
