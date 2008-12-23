/**
 * 
 */
package org.limewire.ui.swing.library;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.EnumMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.URN;
import org.limewire.core.api.library.LibraryFileList;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HyperLinkButton;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.dnd.LocalFileListTransferHandler;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.sharing.ShareWidget;
import org.limewire.ui.swing.library.sharing.ShareWidgetFactory;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.player.PlayerPanel;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

import com.google.inject.Inject;

public class MyLibraryPanel extends LibraryPanel {
    
    @Resource(key="LibraryPanel.selectionPanelBackgroundOverride")
    private Color selectionPanelBackgroundOverride = null;
    
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final PlayerPanel playerPanel;
    private final LibraryManager libraryManager;
    private final Map<Category, LibraryOperable> selectableMap;
    private ShareWidgetFactory shareFactory;
    
    private ShareWidget<Category> categoryShareWidget = null;
    
    @Inject
    public MyLibraryPanel(LibraryManager libraryManager,
                          IconManager iconManager,
                          LibraryTableFactory tableFactory,
                          CategoryIconManager categoryIconManager,
                          ShareWidgetFactory shareFactory,
                          LimeHeaderBarFactory headerBarFactory,
                          PlayerPanel player) {
        
        super(headerBarFactory);
        
        GuiUtils.assignResources(this);
        
        this.libraryManager = libraryManager;
        this.tableFactory = tableFactory;
        this.categoryIconManager = categoryIconManager;    
        this.shareFactory = shareFactory;
        this.playerPanel = player;
        this.selectableMap = new EnumMap<Category, LibraryOperable>(Category.class);

        if (selectionPanelBackgroundOverride != null) { 
            selectionPanel.setBackground(selectionPanelBackgroundOverride);
        }

        getHeaderPanel().setText(I18n.tr("My Library"));
        
        categoryShareWidget = shareFactory.createCategoryShareWidget();
        createMyCategories(libraryManager.getLibraryManagedList());
        selectFirst();
        
        addHeaderComponent(playerPanel, "cell 0 0, grow");
        playerPanel.setMaximumSize(new Dimension(999,999));
        playerPanel.setPreferredSize(new Dimension(999,999));
        setTransferHandler(new LocalFileListTransferHandler(libraryManager.getLibraryManagedList()));
        
    }
    
    private void createMyCategories(LibraryFileList libraryFileList) {
        for(Category category : Category.getCategoriesInOrder()) {        
            CategorySelectionCallback callback = null;
            if (category == Category.AUDIO) {
                callback = new CategorySelectionCallback() {
                    @Override
                    public void categorySelected(Category category, boolean state) {
                        playerPanel.setVisible(state);
                    }
                };
            }
            
            FilterList<LocalFileItem> filtered = GlazedListsFactory.filterList(libraryFileList.getSwingModel(), new CategoryFilter(category));
            addCategory(categoryIconManager.getIcon(category), category, 
                    createMyCategoryAction(category, filtered), filtered, callback);
            addDisposable(filtered);
            addLibraryInfoBar(category, filtered);
        }
    }

    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> filtered) {        
        //TODO: can this be a singleton??? 
        final ShareWidget<File> fileShareWidget = shareFactory.createFileShareWidget();
        addDisposable(fileShareWidget);             
        JScrollPane scrollPane;        
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        if (category != Category.IMAGE) {
            LibraryTable<LocalFileItem> table = tableFactory.createMyTable(category, filterList);
            table.enableMyLibrarySharing(fileShareWidget);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            selectableMap.put(category, table);
            scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());    
            addDisposable(table);
        } else { //Category.IMAGE 
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createImagePanel(filterList, scrollPane, fileShareWidget);
            selectableMap.put(category, imagePanel);
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());            
            addDisposable(imagePanel);
        }                      
        return scrollPane;
    }
    
    @Override
    protected JComponent createCategoryButton(Action action, Category category) {
        MySelectionPanel component = new MySelectionPanel(action, new ShareAllAction(category), category, this);
        addNavigation(component.getButton());
        return component;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    @Override
    public void dispose() {
        super.dispose();
        
        categoryShareWidget.dispose();
    }
    
    private static class MyLibraryDoubleClickHandler implements TableDoubleClickHandler{
        private LibraryTableModel<LocalFileItem> model;

        public MyLibraryDoubleClickHandler(LibraryTableModel<LocalFileItem> model){
            this.model = model;
        }

        @Override
        public void handleDoubleClick(int row) {
            File file = model.getFileItem(row).getFile();
            switch (model.getFileItem(row).getCategory()){
            case AUDIO:
                PlayerUtils.playOrLaunch(file);
                break;
            case OTHER:
            case PROGRAM:
                NativeLaunchUtils.launchExplorer(file);
                break;
            case IMAGE:
                //TODO: image double click
            case VIDEO:
            case DOCUMENT:
                NativeLaunchUtils.safeLaunchFile(file);
            }
        }
    }
    
    /**
     * Display the Share Collection widget when pressed
     */
    private class ShareAllAction extends AbstractAction {

        private Category category;
        
        public ShareAllAction(Category category) {
            this.category = category;
            
            putValue(Action.NAME, I18n.tr("share"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share collection"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
          categoryShareWidget.setShareable(category);
          categoryShareWidget.show((JComponent)e.getSource());
        }
    }
    
    //TODO: use a button painter and JXButton
    private static class MySelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Color selectedTextColor;
        @Resource Color textColor;
        @Resource Font shareButtonFont;
        @Resource Color shareForegroundColor;
        @Resource Color shareMouseOverColor;
        
        private JButton button;
        private HyperLinkButton shareButton;
        private LibraryPanel libraryPanel;
        
        public MySelectionPanel(Action action, Action shareAction, Category category, LibraryPanel panel) {
            super(new MigLayout("insets 0, fill, hidemode 3"));

            this.libraryPanel = panel;
            
            GuiUtils.assignResources(this);
            setOpaque(false);

            button = new JButton(action);           
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,8,2,0));
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
                            if(shareButton != null) {
                                shareButton.setVisible(true);
                            }
                        } else {
                            setOpaque(false);
                            button.setForeground(textColor);
                            if(shareButton != null) {
                                shareButton.setVisible(false);
                            }
                        }
                        repaint();
                    } else if(evt.getPropertyName().equals("enabled")) {
                        boolean value = (Boolean)evt.getNewValue();
                        setVisible(value);
                        //select first category if this category is hidden
                        if(value == false && button.getAction().getValue(Action.SELECTED_KEY) != null && 
                                button.getAction().getValue(Action.SELECTED_KEY).equals(Boolean.TRUE)) {
                            libraryPanel.selectFirst();
                        }
                    }
                }
                    
            });
            
            add(button, "growx, push");
            
            // only add a share category button if its an audio/video/image category
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                shareButton = new HyperLinkButton(null, shareAction);
                shareButton.setContentAreaFilled(false);
                shareButton.setBorderPainted(false);
                shareButton.setFocusPainted(false);
                shareButton.setBorder(BorderFactory.createEmptyBorder(2,0,2,4));
                shareButton.setOpaque(false);
                shareButton.setVisible(false);
                FontUtils.underline(shareButton);
                shareButton.setFont(shareButtonFont);
                shareButton.setForeground(shareForegroundColor);
                shareButton.setMouseOverColor(shareMouseOverColor);
                add(shareButton, "wrap");
            }
        }
        
        public JButton getButton() {
            return button;
        }
    }

    public LibraryFileList getLibrary() {
        return libraryManager.getLibraryManagedList();
    }

    public void selectItem(File file, Category category) {
        select(category);
        selectableMap.get(category).selectAndScrollTo(file);
    }

    public File getNextItem(File file, Category category) {
        return selectableMap.get(category).getNextItem(file);
    }

    public File getPreviousItem(File file, Category category) {
        return selectableMap.get(category).getPreviousItem(file);
    }

    public void selectItem(URN urn, Category category) {
        select(category);
        selectableMap.get(category).selectAndScrollTo(urn);        
    }    
}
