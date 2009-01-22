package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;
import java.util.TooManyListenersException;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.jxlayer.JXLayer;
import org.jdesktop.jxlayer.plaf.effect.LayerEffect;
import org.jdesktop.jxlayer.plaf.ext.LockableUI;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.decorator.ColorHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.decorator.HighlightPredicate;
import org.limewire.collection.glazedlists.GlazedListsFactory;
import org.limewire.core.api.Category;
import org.limewire.core.api.friend.Friend;
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.components.MessageComponent;
import org.limewire.ui.swing.dnd.GhostDragGlassPane;
import org.limewire.ui.swing.dnd.GhostDropTargetListener;
import org.limewire.ui.swing.dnd.SharingLibraryTransferHandler;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.painter.FilterPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableColors;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

abstract class SharingPanel extends AbstractFileListPanel implements PropertyChangeListener {
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final FriendFileList friendFileList;
    private final Friend friend;
    
    private final Map<Category, LockableUI> locked = new EnumMap<Category, LockableUI>(Category.class);
    private final Map<Category, SharingSelectionPanel> listeners = new EnumMap<Category, SharingSelectionPanel>(Category.class);
    
    private JPanel backButtonSlot;
    
    SharingPanel(EventList<LocalFileItem> wholeLibraryList,
                 FriendFileList friendFileList,
                 CategoryIconManager categoryIconManager,
                 LibraryTableFactory tableFactory,
                 LimeHeaderBarFactory headerBarFactory,
                 GhostDragGlassPane ghostPane, Friend friend) {
        super(headerBarFactory);
        
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.friendFileList = friendFileList;        
        this.friend = friend;
        this.friendFileList.addPropertyChangeListener(this);
        setTransferHandler(new SharingLibraryTransferHandler(null, friendFileList));
        
        try {
            getDropTarget().addDropTargetListener(new GhostDropTargetListener(this,ghostPane, friend));
        } catch (TooManyListenersException ignoreException) {            
        } 
    }

    /** Returns the full name of the panel, which may be very long. */
    abstract String getFullPanelName();
    /** Returns a shorter more concise version of the panel name. */
    abstract String getShortPanelName();
        
    @Override
    protected LimeHeaderBar createHeaderBar(LimeHeaderBarFactory headerBarFactory) {
        JPanel headerTitlePanel = new JPanel(new MigLayout("insets 0, gap 0, fill, aligny center"));
        headerTitlePanel.setOpaque(false);
        
        JLabel titleTextLabel = new JLabel();
        
        backButtonSlot = new JPanel(new MigLayout("insets 0, gap 0, fill, aligny center"));
        backButtonSlot.setOpaque(false);
        
        backButtonSlot.setVisible(false);
        
        headerTitlePanel.add(backButtonSlot);
        headerTitlePanel.add(titleTextLabel, "gapbottom 2");
        
        LimeHeaderBar bar = headerBarFactory.createSpecial(headerTitlePanel, titleTextLabel);
                
        return bar;
    }
    
    @Override
    protected LimePromptTextField createFilterField(String prompt) {
        // Create filter field and install painter.
        return FilterPainter.decorate(new LimePromptTextField(prompt, AccentType.GREEN_SHADOW));
    }
    
    protected void addBackButton(JButton button) {
        backButtonSlot.add(button, "gapafter 6");
        backButtonSlot.setVisible(true);
    }
    
    protected void createMyCategories(EventList<LocalFileItem> wholeLibraryList, LocalFileList friendFileList) {
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<LocalFileItem> filteredAll = GlazedListsFactory.filterList(wholeLibraryList, new CategoryFilter(category));
            FilterList<LocalFileItem> filteredShared = GlazedListsFactory.filterList(friendFileList.getSwingModel(), new CategoryFilter(category));
            addCategory(categoryIconManager.getIcon(category), category,
                        createMyCategoryAction(category, filteredAll, friendFileList), filteredAll, filteredShared, null);
            addDisposable(filteredAll);
            addDisposable(filteredShared);
            addSharingInfoBar(category, filteredAll, this.friendFileList, filteredShared);
        }
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> filtered, final LocalFileList friendFileList) {
        FilterList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));
        addDisposable(filterList);

        
        FilterList<LocalFileItem> storeFileFilteredList = GlazedListsFactory.filterList(filterList, new Matcher<LocalFileItem>() {
                @Override
                public boolean matches(LocalFileItem item) {
                    return item.isShareable();
                }
        });
        addDisposable(storeFileFilteredList);
        
        Comparator<LocalFileItem> c = new LocalFileItemComparator(friendFileList);
        
        SortedList<LocalFileItem> sortedList = new SortedList<LocalFileItem>(storeFileFilteredList, c);
        addDisposable(sortedList);

        JScrollPane scrollPane;
        
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createSharingTable(category, sortedList, friendFileList, friend);
            table.setDoubleClickHandler(new MyLibraryDoubleClickHandler(getTableModel(table)));
            table.enableSharing();
            addDisposable(table);
            
            scrollPane = new JScrollPane(table);
            scrollPane.setColumnHeaderView(table.getTableHeader());
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
            
            if (table.isColumnControlVisible()) {
                scrollPane.setCorner(JScrollPane.UPPER_TRAILING_CORNER, table.getColumnControl());
                scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            }
			TableColors tableColors = new TableColors();
            table.addHighlighter(new ColorHighlighter(new IncompleteHighlightPredicate(getTableModel(table)), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
        } else {//Category.IMAGE
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createSharingImagePanel(sortedList, scrollPane, friendFileList);
            addDisposable(imagePanel);
            
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
        
        if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
            LockableUI blurUI = new LockedUI(category);
            JXLayer<JComponent> jxlayer = new JXLayer<JComponent>(scrollPane, blurUI);
            if(this.friendFileList.isCategoryAutomaticallyAdded(category)) {
                blurUI.setLocked(true);
            }
            locked.put(category, blurUI);
            return jxlayer;
        }
        return scrollPane;
    }
    
    @SuppressWarnings("unchecked")
    private LibraryTableModel<LocalFileItem> getTableModel(LibraryTable table){
        return (LibraryTableModel<LocalFileItem>)table.getModel();
    }   
    
    @Override
    protected <T extends FileItem> JComponent createCategoryButton(Action action, Category category, FilterList<T> filteredAllFileList) {
        SharingSelectionPanel panel = new SharingSelectionPanel(action, category, this, new ShareAction(category), new UnshareAction(category));
        listeners.put(category, panel);
        addNavigation(panel.getButton());
        return panel;
    }
    
    @Override
    public void dispose() {
        super.dispose();
        friendFileList.removePropertyChangeListener(this);
    }
    
    @Override
    protected <T extends FileItem> void addCategorySizeListener(Category category,
            Action action, FilterList<T> filteredAllFileList, FilterList<T> filteredList) {
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredAllFileList, filteredList, friendFileList, friend);
        filteredAllFileList.addListEventListener(listener);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    private static class ButtonSizeListener<T> implements Disposable, ListEventListener<T>, SettingListener {
        private final Category category;
        private final Action action;
        private final FilterList<T> allFileList;
        private final FilterList<T> list;
        private final FriendFileList friendList;
        private final Friend friend;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> allFileList, FilterList<T> list, FriendFileList friendList, Friend friend) {
            this.category = category;
            this.action = action;
            this.allFileList = allFileList;
            this.list = list;
            this.friendList = friendList;
            this.friend = friend;
            
            setText();
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.addSettingListener(this);
            }
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                LibrarySettings.SNAPSHOT_SHARING_ENABLED.addSettingListener(this);
            }
            if(category == Category.DOCUMENT && friend.getId().equals(Friend.P2P_FRIEND_ID)) {
                LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.addSettingListener(this);
            }
        }

        private void setText() {
            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue()) {
                action.putValue(Action.NAME, I18n.tr(category.toString()) + " (" + list.size() + ")");
            } else {
                switch(category) {
                case AUDIO:
                case VIDEO:
                case IMAGE:
                    if(friendList.isCategoryAutomaticallyAdded(category)) {
                        action.putValue(Action.NAME, I18n.tr(category.toString()) + I18n.tr(" ({0})", "all"));
                        break;
                    }
                default:
                    action.putValue(Action.NAME, I18n.tr(category.toString()) + " (" + list.size() + ")");
                }
            }
            if(category == Category.OTHER) {
                action.setEnabled(allFileList.size() > 0);
            } else if(category == Category.PROGRAM) {// hide program category is not enabled
                action.setEnabled(LibrarySettings.ALLOW_PROGRAMS.getValue());
            } else if(category == Category.DOCUMENT && friend.getId().equals(Friend.P2P_FRIEND_ID)) {
                action.setEnabled(LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.getValue());
            }
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            allFileList.removeListEventListener(this);
            if(category == Category.PROGRAM) {
                LibrarySettings.ALLOW_PROGRAMS.removeSettingListener(this);
            }
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                LibrarySettings.SNAPSHOT_SHARING_ENABLED.removeSettingListener(this);
            }
            if(category == Category.DOCUMENT && friend.getId().equals(Friend.P2P_FRIEND_ID)) {
                LibrarySettings.ALLOW_DOCUMENT_GNUTELLA_SHARING.removeSettingListener(this);
            }
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    setText();                    
                }
            });
        }
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
    
    private static class IncompleteHighlightPredicate implements HighlightPredicate {
        LibraryTableModel<LocalFileItem> libraryTableModel;
        public IncompleteHighlightPredicate (LibraryTableModel<LocalFileItem> libraryTableModel) {
            this.libraryTableModel = libraryTableModel;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LocalFileItem fileItem = libraryTableModel.getFileItem(adapter.row);
            return !fileItem.isShareable();
        }       
    }
    
    private class SharingSelectionPanel extends JPanel implements Disposable, SettingListener {
        @Resource Color selectedBackground;
        @Resource Color selectedTextColor;
        @Resource Color textColor;
        @Resource Font  shareButtonFont;
        @Resource Font  shareLabelFont;
        
        private JLabel emptyCheckBox;
        private JCheckBox checkBox;
        private JButton button;
        
        private JLabel shareLabel;
        private HyperlinkButton shareButton;
        private HyperlinkButton unshareButton;
        
        private AbstractFileListPanel libraryPanel;
        private Category category;
        
        public SharingSelectionPanel(Action action, Category category, AbstractFileListPanel library, 
                ShareAction shareAction, UnshareAction unshareAction) {
            super(new MigLayout("gap 0, insets 0 0 2 0, fill, hidemode 3"));
            
            this.libraryPanel = library;
            this.category = category;
            
            GuiUtils.assignResources(this);     
            
            setOpaque(false);
            LibrarySettings.SNAPSHOT_SHARING_ENABLED.addSettingListener(this);            
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                createCheckBox(); 
                add(checkBox);
                createSelectionButton(action);
                add(button, "gapleft 2, growx, span, wrap");
            } else {
                createSelectionButton(action);
                emptyCheckBox = new JLabel();
                emptyCheckBox.setPreferredSize(new Dimension(16,16));
                emptyCheckBox.setVisible(!LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue());
                add(emptyCheckBox);
                add(button, "gapleft 2, growx, span, wrap");
            }
            
            if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
                createShareButtons(shareAction, unshareAction);
                add(shareLabel, "gapleft 22, span 2, split");
                add(shareButton, "split");
                add(unshareButton);
            }
            

          libraryPanel.selectFirstVisible();
        }
        
        private void createSelectionButton(Action action) {
            button = new JButton(action);           
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setBorder(BorderFactory.createEmptyBorder(2,0,2,0));
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.getAction().addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if(evt.getPropertyName().equals(Action.SELECTED_KEY)) {
                        if(Boolean.TRUE.equals(evt.getNewValue())) {
                            setOpaque(true);
                            setBackground(selectedBackground);
                            button.setForeground(selectedTextColor);
                            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue())
                                setShareButtonVisible(true);
                        } else {
                            setOpaque(false);
                            button.setForeground(textColor);
                            if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue())
                                setShareButtonVisible(false);
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
        
        private void setShareButtonVisible(boolean value) {
            if(shareLabel != null) {
                shareLabel.setVisible(value);
                shareButton.setVisible(value);
                unshareButton.setVisible(value);
            }
        }
        
        private void createShareButtons(Action shareAction, Action unshareAction) {
            shareLabel = new JLabel(I18n.tr("Share:"));
            shareButton = new HyperlinkButton(shareAction);
            unshareButton = new HyperlinkButton(unshareAction);

            shareLabel.setForeground(textColor);
            shareLabel.setFont(shareLabelFont);
            shareButton.setFont(shareButtonFont);        
            unshareButton.setFont(shareButtonFont);
            
            boolean visible = LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue() && category == Category.AUDIO;
            shareLabel.setVisible(visible);
            shareButton.setVisible(visible);
            unshareButton.setVisible(visible);
        }
        
        private void createCheckBox() {
            checkBox = new JCheckBox();                
            checkBox.setContentAreaFilled(false);
            checkBox.setBorderPainted(false);
            checkBox.setFocusPainted(false);
            checkBox.setBorder(BorderFactory.createEmptyBorder(2,2,2,0));
            checkBox.setOpaque(false);
            checkBox.setVisible(!LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue());
            checkBox.setSelected(friendFileList.isCategoryAutomaticallyAdded(category));     
            
            // This is explicitly an ActionListener and not an ItemListener
            // because we only want to perform events if we CLICKED here..
            // Since the box is synced up to the setting, the state
            // will change if other settings changed, and we don't
            // want to select, change, or clear if other areas change.
            checkBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    select(category);
                    friendFileList.setCategoryAutomaticallyAdded(category, checkBox.isSelected());
                    if(!checkBox.isSelected()) {
                        friendFileList.clearCategory(category);
                    }
                }
            });
        }
        
        public void setSelect(boolean value) {
            checkBox.setSelected(value);
        }
        
        public JButton getButton() {
            return button;
        }

        @Override
        public void dispose() {
            LibrarySettings.SNAPSHOT_SHARING_ENABLED.removeSettingListener(this);
        }

        @Override
        public void settingChanged(SettingEvent evt) {
            SwingUtilities.invokeLater(new Runnable(){
                public void run() {
                    if(LibrarySettings.SNAPSHOT_SHARING_ENABLED.getValue()) {
                        if(checkBox != null) {
                            checkBox.setVisible(false);
                            checkBox.setSelected(false);
                        }
                        if(emptyCheckBox != null) {
                            emptyCheckBox.setVisible(false);
                        }

                        if(button != null && 
                           button.getAction() != null && 
                           button.getAction().getValue(Action.SELECTED_KEY) != null && 
                           button.getAction().getValue(Action.SELECTED_KEY).equals(Boolean.TRUE)) {
                            setShareButtonVisible(true);
                        }
                    } else {
                        if(checkBox != null) {
                            checkBox.setVisible(true);
                        }
                        if(emptyCheckBox != null)
                            emptyCheckBox.setVisible(true);
                        setShareButtonVisible(false);
                    }
                    revalidate();
                }
            });
        }
    }    
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        private MessageComponent messageComponent;
        private JLabel label;
        private JLabel minLabel;
        
        public LockedUI(Category category, LayerEffect... lockedEffects) {
            super(lockedEffects);
            
            messageComponent = new MessageComponent();
            
            label = new JLabel(I18n.tr("Sharing entire {0} Collection with {1}", category.getSingularName(), getFullPanelName()));
            messageComponent.decorateHeaderLabel(label);
            
            minLabel = new JLabel(I18n.tr("New {0} files that are added to your Library will be automatically shared with this person", category.getSingularName()));
            messageComponent.decorateSubLabel(minLabel);

            messageComponent.addComponent(label, "wrap");
            messageComponent.addComponent(minLabel, "");
            
            panel = new JXPanel(new MigLayout("fill"));
            panel.setBackground(new Color(147,170,209,80));
            panel.setVisible(false);           
            panel.add(messageComponent, "align 50% 40%");
        }
        
        @SuppressWarnings("unchecked")
        public void installUI(JComponent c) {
            super.installUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new BorderLayout());
            l.getGlassPane().add(panel, BorderLayout.CENTER);
        }
        
        @SuppressWarnings("unchecked")
        public void uninstall(JComponent c) {
            super.uninstallUI(c);
            JXLayer<JComponent> l = (JXLayer<JComponent>) c;
            l.getGlassPane().setLayout(new FlowLayout());
            l.getGlassPane().remove(panel);
        }
        
        public void setLocked(boolean isLocked) {
            super.setLocked(isLocked);
            panel.setVisible(isLocked);
        }
        
        @Override
        public Cursor getLockedCursor() {
            return Cursor.getDefaultCursor();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals("audioCollection")) {
            locked.get(Category.AUDIO).setLocked((Boolean)evt.getNewValue());
            listeners.get(Category.AUDIO).setSelect((Boolean)evt.getNewValue());
        } else if(evt.getPropertyName().equals("videoCollection")) {
            locked.get(Category.VIDEO).setLocked((Boolean)evt.getNewValue());
            listeners.get(Category.VIDEO).setSelect((Boolean)evt.getNewValue());
        } else if(evt.getPropertyName().equals("imageCollection")) {
            locked.get(Category.IMAGE).setLocked((Boolean)evt.getNewValue());
            listeners.get(Category.IMAGE).setSelect((Boolean)evt.getNewValue());
        }
    }

    private static class LocalFileItemComparator implements Comparator<LocalFileItem> {
        private final LocalFileList friendFileList;

        public LocalFileItemComparator(LocalFileList friendFileList) {
            this.friendFileList = friendFileList;
        }

        @Override
            public int compare(LocalFileItem fileItem1, LocalFileItem fileItem2) {
            boolean containsF1 = friendFileList.contains(fileItem1.getFile());
            boolean containsF2 = friendFileList.contains(fileItem2.getFile());
            if(containsF1 && containsF2)
                return 0;
            else if(containsF1 && !containsF2)
                return -1;
            else
                return 1;
        }
    }
    
    /**
     * Shares all the files currently in a category, with a friend
     */
    private class ShareAction<T extends FileItem> extends AbstractAction {
        private Category category;
        
        public ShareAction(Category category) {
            this.category = category;
            
            putValue(Action.NAME, I18n.tr("all"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Share all the files in this category."));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            friendFileList.addSnapshotCategory(category);           
            SharingPanel.this.repaint();
        }
    }
    
    /**
     * Unshares all the files currently in a category, with a friend
     */
    private class UnshareAction<T extends FileItem> extends AbstractAction {
        private Category category;

        public UnshareAction(Category category) {
            this.category = category;
            
            putValue(Action.NAME, I18n.tr("none"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Unshare all the files in this category."));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {   
            friendFileList.clearCategory(category);

            SharingPanel.this.repaint();
        }
    }
}
