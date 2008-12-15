package org.limewire.ui.swing.library;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.Map;

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
import org.limewire.core.api.library.FileItem;
import org.limewire.core.api.library.FriendFileList;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.LocalFileList;
import org.limewire.ui.swing.components.LimeHeaderBar;
import org.limewire.ui.swing.components.LimeHeaderBarFactory;
import org.limewire.ui.swing.components.LimePromptTextField;
import org.limewire.ui.swing.library.image.LibraryImagePanel;
import org.limewire.ui.swing.library.table.LibraryTable;
import org.limewire.ui.swing.library.table.LibraryTableFactory;
import org.limewire.ui.swing.library.table.LibraryTableModel;
import org.limewire.ui.swing.lists.CategoryFilter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableDoubleClickHandler;
import org.limewire.ui.swing.table.MouseableTable.TableColors;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.FontUtils;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.swing.TextComponentMatcherEditor;

abstract class SharingPanel extends AbstractFileListPanel implements PropertyChangeListener {
    private final LibraryTableFactory tableFactory;
    private final CategoryIconManager categoryIconManager;
    private final FriendFileList friendFileList;
    
    private final Map<Category, LockableUI> locked = new EnumMap<Category, LockableUI>(Category.class);
    private final Map<Category, SharingSelectionPanel> listeners = new EnumMap<Category, SharingSelectionPanel>(Category.class);
    
    SharingPanel(EventList<LocalFileItem> wholeLibraryList,
                 FriendFileList friendFileList,
                 CategoryIconManager categoryIconManager,
                 LibraryTableFactory tableFactory,
                 LimeHeaderBarFactory headerBarFactory) {
        super(headerBarFactory);
        
        this.categoryIconManager = categoryIconManager;
        this.tableFactory = tableFactory;
        this.friendFileList = friendFileList;        
        this.friendFileList.addPropertyChangeListener(this);

        //TODO: fix this. Turns text to Black for the time being till we get some sort of color spec
        getHeaderPanel().setForeground(Color.BLACK);
    }
    
    /** Returns the full name of the panel, which may be very long. */
    abstract String getFullPanelName();
    /** Returns a shorter more concise version of the panel name. */
    abstract String getShortPanelName();
    
    @Override
    protected LimeHeaderBar createHeaderBar(LimeHeaderBarFactory headerBarFactory) {
        return headerBarFactory.createSpecial();
    }
    
    @Override
    protected LimePromptTextField createFilterField(String prompt) {
        return new LimePromptTextField(prompt, AccentType.NONE);
    }
    
    protected void createMyCategories(EventList<LocalFileItem> wholeLibraryList, LocalFileList friendFileList) {
        for(Category category : Category.getCategoriesInOrder()) {
            FilterList<LocalFileItem> filteredAll = GlazedListsFactory.filterList(wholeLibraryList, new CategoryFilter(category));
            FilterList<LocalFileItem> filteredShared = GlazedListsFactory.filterList(friendFileList.getSwingModel(), new CategoryFilter(category));
            addCategory(categoryIconManager.getIcon(category), category,
                        createMyCategoryAction(category, filteredAll, friendFileList), filteredAll, filteredShared, null);
            addDisposable(filteredAll);
            addDisposable(filteredShared);
        }
    }
    
    private JComponent createMyCategoryAction(Category category, EventList<LocalFileItem> filtered, final LocalFileList friendFileList) {
        EventList<LocalFileItem> filterList = GlazedListsFactory.filterList(filtered, 
                new TextComponentMatcherEditor<LocalFileItem>(getFilterTextField(), new LibraryTextFilterator<LocalFileItem>()));

        Comparator<LocalFileItem> c = new java.util.Comparator<LocalFileItem>() {
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
        };
        
        SortedList<LocalFileItem> sortedList = new SortedList<LocalFileItem>(filterList, c);

        JScrollPane scrollPane;
        
        if (category != Category.IMAGE) {
            LibraryTable table = tableFactory.createSharingTable(category, sortedList, friendFileList);
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
            table.addHighlighter(new ColorHighlighter(new UnsharedHighlightPredicate(getTableModel(table), friendFileList), null, tableColors.getDisabledForegroundColor(), null, tableColors.getDisabledForegroundColor()));
        } else {//Category.IMAGE
            scrollPane = new JScrollPane();
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            LibraryImagePanel imagePanel = tableFactory.createSharingImagePanel(sortedList, scrollPane, friendFileList);
            addDisposable(imagePanel);
            
            scrollPane.setViewportView(imagePanel);
            scrollPane.setBorder(BorderFactory.createEmptyBorder());
        }
        
        if(category == Category.AUDIO || category == Category.VIDEO || category == Category.IMAGE) {
            LockableUI blurUI = new LockedUI(category.toString());
            JXLayer<JComponent> jxlayer = new JXLayer<JComponent>(scrollPane, blurUI);
            
            if(category == Category.AUDIO && this.friendFileList.isAddNewAudioAlways()) {
                blurUI.setLocked(true);
            } else if(category == Category.VIDEO && this.friendFileList.isAddNewVideoAlways()) {
                blurUI.setLocked(true);
            } if(category == Category.IMAGE && this.friendFileList.isAddNewImageAlways()) {
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
    protected JComponent createCategoryButton(Action action, Category category) {
        SharingSelectionPanel panel = new SharingSelectionPanel(action, category, this);
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
        ButtonSizeListener<T> listener = new ButtonSizeListener<T>(category, action, filteredAllFileList, filteredList);
        filteredAllFileList.addListEventListener(listener);
        filteredList.addListEventListener(listener);
        addDisposable(listener);
    }
    
    private static class ButtonSizeListener<T> implements Disposable, ListEventListener<T> {
        private final Category category;
        private final Action action;
        private final FilterList<T> allFileList;
        private final FilterList<T> list;
        
        private ButtonSizeListener(Category category, Action action, FilterList<T> allFileList, FilterList<T> list) {
            this.category = category;
            this.action = action;
            this.allFileList = allFileList;
            this.list = list;
            setText();
        }

        private void setText() {
            action.putValue(Action.NAME, I18n.tr(category.toString()) + " (" + list.size() + "/" + allFileList.size() + ")");
            if(category == Category.OTHER) {
                action.setEnabled(allFileList.size() > 0);
            }
        }
        
        @Override
        public void dispose() {
            list.removeListEventListener(this);
            allFileList.removeListEventListener(this);
        }

        @Override
        public void listChanged(ListEvent<T> listChanges) {
            setText();
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
    
    private static class UnsharedHighlightPredicate implements HighlightPredicate {
        LibraryTableModel<LocalFileItem> libraryTableModel;
        private LocalFileList friendFileList;
        public UnsharedHighlightPredicate (LibraryTableModel<LocalFileItem> libraryTableModel, LocalFileList friendFileList) {
            this.libraryTableModel = libraryTableModel;
            this.friendFileList = friendFileList;
        }
        @Override
        public boolean isHighlighted(Component renderer, ComponentAdapter adapter) {
            LocalFileItem fileItem = libraryTableModel.getFileItem(adapter.row);
            //TODO cache values?
            return !(friendFileList.contains(fileItem.getUrn()));
        }       
    }
    
    private class SharingSelectionPanel extends JPanel {
        @Resource Color selectedBackground;
        @Resource Color selectedTextColor;
        @Resource Color textColor;
        
        private JCheckBox checkBox;
        private JButton button;
        private AbstractFileListPanel libraryPanel;
        
        public SharingSelectionPanel(Action action, final Category category, AbstractFileListPanel library) {
            super(new MigLayout("insets 0, fill"));
            
            this.libraryPanel = library;
            
            GuiUtils.assignResources(this);     

            checkBox = new JCheckBox();                
            checkBox.setContentAreaFilled(false);
            checkBox.setBorderPainted(false);
            checkBox.setFocusPainted(false);
            checkBox.setBorder(BorderFactory.createEmptyBorder(2,2,2,0));
            checkBox.setOpaque(false);
            
            add(checkBox);
            
            setOpaque(false);
            
            if(category != Category.AUDIO && category != Category.VIDEO && category != Category.IMAGE) {
                checkBox.setVisible(false);
            } else {
                if(category == Category.AUDIO) {
                    checkBox.setSelected(friendFileList.isAddNewAudioAlways());
                } else if(category == Category.VIDEO) {
                    checkBox.setSelected(friendFileList.isAddNewVideoAlways());
                } else if(category == Category.IMAGE) {
                    checkBox.setSelected(friendFileList.isAddNewImageAlways());
                }
                
                checkBox.addItemListener(new ItemListener(){
                    @Override
                    public void itemStateChanged(ItemEvent e) {
                        select(category);
                        if(category == Category.AUDIO) {
                            friendFileList.setAddNewAudioAlways(checkBox.isSelected());
                        } else if(category == Category.VIDEO) {
                            friendFileList.setAddNewVideoAlways(checkBox.isSelected());
                        } else if(category == Category.IMAGE) {
                            friendFileList.setAddNewImageAlways(checkBox.isSelected());
                        }
                    }
                });
            }

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
                            libraryPanel.selectFirst();
                        }
                    }
                }
            });
            add(button, "growx");
        }
        
        public void setSelect(boolean value) {
            checkBox.setSelected(value);
        }
        
        public JButton getButton() {
            return button;
        }
    }    
    
    /**
     * Creates a locked layer over a table. This layer prevents the user from
     * interacting with the contents underneath it.
     */
    private class LockedUI extends LockableUI {
        private JXPanel panel;
        private JPanel messagePanel;
        private JLabel label;
        private JLabel minLabel;
        
        public LockedUI(String category, LayerEffect... lockedEffects) {
            super(lockedEffects);
            
            messagePanel = new JPanel(new MigLayout("insets 5, gapy 10, wrap, alignx 50%"));
            messagePanel.setBackground(new Color(209,247,144));
            
            label = new JLabel(I18n.tr("Sharing entire {0} Collection with {1}", category, getFullPanelName()));
            FontUtils.setSize(label, 12);
            FontUtils.bold(label);
            
            minLabel = new JLabel(I18n.tr("Sharing your {0} collection shares new {1} files that automatically get added to your Library", category, category.toLowerCase()));
            FontUtils.setSize(minLabel, 10);
            
            panel = new JXPanel(new MigLayout("aligny 50%, alignx 50%"));
            panel.setBackground(new Color(147,170,209,80));
            panel.setVisible(false);
            
            messagePanel.add(label, "alignx 50%");
            messagePanel.add(minLabel, "alignx 50%");
            
            panel.add(messagePanel);
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
}
