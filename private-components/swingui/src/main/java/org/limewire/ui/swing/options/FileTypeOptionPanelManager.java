package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.limewire.collection.CollectionUtils;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.api.library.LibraryManager;
import org.limewire.core.settings.LibrarySettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.CheckBoxList;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListCheckChangeEvent;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListCheckChangeListener;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListSelectionEvent;
import org.limewire.ui.swing.components.CheckBoxList.CheckBoxListSelectionListener;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.CategoryUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;
import org.limewire.util.MediaType;

import com.google.inject.Inject;
import com.google.inject.Singleton;



/**
 * Constructs the file type sharing panel to be used in the options
 *  menu and setup manager.  Includes external interface for saving
 *  and reloading settings.  
 */

@Singleton
public final class FileTypeOptionPanelManager {

    private static final int MAX_EXT_LENGTH = 7;
    
    private JPanel mainContainer;
    private CardLayout mediaLayout;
    private JPanel currentPanel;
    
    private CheckBoxList<Category> sidePanel;
        
    private Map<Category,CheckBoxList<String>> panels;
        
    private final ExtensionProvider extensionProvider = new ExtensionProvider();
    private final ExtensionsExtrasProvider extensionExtrasProvider = new ExtensionsExtrasProvider();
    
    
    private Set<Category> mediaKeys;
    private Set<Category> mediaUnchecked;   
    private Category currentKey;
        
    private final Collection<String> originalExtensions;

    private final CategoryIconManager categoryIconManager;
    private final IconManager iconManager;
    private final LibraryData libraryData;
    private JButton addButton;
    private JTextField extTextField;
    private Set<String> removableExts;

    private Action addAction = new AbstractAction(I18n.tr("Add New Extension")) {
        @Override
        public void actionPerformed(ActionEvent arg0) {
            addNewExt();
        }
    };
    
    @Inject
    public FileTypeOptionPanelManager(CategoryIconManager categoryIconManager,
            IconManager iconManager, LibraryManager libraryManager) {
        
        this.categoryIconManager = categoryIconManager;
        this.iconManager = iconManager;
        this.libraryData = libraryManager.getLibraryData();
        this.originalExtensions = CollectionUtils.flatten(libraryData.getExtensionsPerCategory().values());
    }
    
    /**
     * Switches the active media panel to the one of the given key
     */
    private void switchPanel(Category mediaKey) {

        this.currentKey = mediaKey;
        this.mediaLayout.show(currentPanel, mediaKey.toString());
        this.sidePanel.setItemSelected(mediaKey);
    }
    
    
    /** 
     * Repaints the side panel (to update comment text)
     */
    private void refreshSidePanel() {
        this.sidePanel.update();
    }
    
    /**
     * Sets the enabled setting of a panel of a given key
     */
    private void setPanelEnabled(Category mediaKey, boolean enabled) {
       this.panels.get(mediaKey).setEnabled(enabled);
    }

    /**
     * Returns the panel that this class is used to produce
     */
    public Container getContainer() {
        return this.mainContainer;
    }
   
    void initCore() {        
        Collection<String> selectedExts = CollectionUtils.flatten(libraryData.getExtensionsPerCategory().values());
        Collection<String> allExts = new TreeSet<String>();
        removableExts = new HashSet<String>();
        
        allExts.addAll(libraryData.getDefaultExtensions());
        allExts.addAll(selectedExts);
                        
        Map<Category, List<String>> extensionsByType = createExtensionsMap(allExts);

        this.mediaKeys = new TreeSet<Category>();
        Set<Category> s; 
        if ((s = extensionsByType.keySet()) != null) {        
            this.mediaKeys.addAll(s);
        }

        this.panels = new LinkedHashMap<Category,CheckBoxList<String>>();
        this.mediaUnchecked = new HashSet<Category>();
        this.currentKey = null;
        
        final PanelsCheckChangeListener refreshListener = new PanelsCheckChangeListener(this);
        
        for (Category key : mediaKeys) {
            if (this.currentKey == null) {
                this.currentKey = key;
            }
            
            Collection<String> extensionsInCategory = extensionsByType.get(key);
            Collection<String> notSelectedInCategory = new HashSet<String>(extensionsInCategory);
            notSelectedInCategory.removeAll(selectedExts);
            
            CheckBoxList<String> newPanel = new CheckBoxList<String>(extensionsInCategory, notSelectedInCategory, 
                    extensionProvider, extensionExtrasProvider,
                    CheckBoxList.SELECT_FIRST_OFF);
            
            newPanel.setCheckChangeListener(refreshListener);
            
            if (extensionsInCategory.equals(notSelectedInCategory)) {
                newPanel.setEnabled(false);
                this.mediaUnchecked.add(key);
            }
            
            this.panels.put(key, newPanel);
        }
        
        CheckBoxList<String> otherPanel = panels.get(Category.OTHER);
        for ( String item : removableExts ) {
            otherPanel.setRemovable(item, true);
        }
    }

    
    public void buildUI() {
        
        if (this.panels.isEmpty()) return;
        
        this.mainContainer = new JPanel(new BorderLayout());        
        this.mediaLayout   = new CardLayout();
        this.currentPanel  = new JPanel(mediaLayout);
        
        for (Category key : mediaKeys) {
            this.currentPanel.add(this.panels.get(key), key.toString());
        }
        
        this.sidePanel = new CheckBoxList<Category>(this.mediaKeys, this.mediaUnchecked,
                new MediaProvider(), new MediaExtrasProvider(),
                CheckBoxList.SELECT_FIRST_ON);
        
        if (!LibrarySettings.ALLOW_PROGRAMS.getValue()) {
            sidePanel.removeItem(Category.PROGRAM);
        }
        
        LibrarySettings.ALLOW_PROGRAMS.addSettingListener(new SettingListener() {            
            @Override
            public void settingChanged(SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        if (LibrarySettings.ALLOW_PROGRAMS.getValue()) {
                            sidePanel.removeItem(Category.PROGRAM);
                        } 
                        else {
                            sidePanel.addItem(Category.PROGRAM);
                        }
                        
                    }
                });
            }            
        });
        
        this.sidePanel.setPreferredSize(new Dimension(150, 0));
        this.sidePanel.setSelectionListener(new SideSelectListener(this));
        this.sidePanel.setCheckChangeListener(new CheckChangeListener(this));
        this.sidePanel.setItemSelected(this.currentKey);
        this.sidePanel.setCheckBoxesVisible(false);
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                this.sidePanel, this.currentPanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(180);
        splitPane.setEnabled(false);
        
        this.mainContainer.add(splitPane, BorderLayout.CENTER);
        
        JPanel addExtPanel = new JPanel(new MigLayout("insets 5, gap 10, alignx right"));
        addExtPanel.setOpaque(false);
        addExtPanel.add(new JLabel(I18n.tr("File Extension:")));
        extTextField = new JTextField(6);
        addExtPanel.add(extTextField);
        addButton = new JButton(addAction);
        addExtPanel.add(addButton);
        
        mainContainer.add(addExtPanel, BorderLayout.SOUTH);      
        
        this.mainContainer.validate();
    }
    
    private void addNewExt() {
        String text = extTextField.getText();
        
        if (text == null || text.isEmpty()) {
            return;
        }
        
        extTextField.setText("");
        
        if (!checkExt(text)) {
            return;
        }
        
        CheckBoxList<String> otherPanel = panels.get(Category.OTHER);
        otherPanel.addItem(text);
        otherPanel.setItemChecked(text);
        otherPanel.setRemovable(text, true);
        otherPanel.setItemSelected(text);
        switchPanel(Category.OTHER);
    }
    
    
    private boolean checkExt(String text) {
        if (text == null) {
            return false;
        }
        
        CheckBoxList<String> otherPanel = panels.get(Category.OTHER);
        
        String malformedMsg = I18n.tr("The extension name was not valid, could not add.");
        
        if (   text.length() == 0 
            || text.length() > MAX_EXT_LENGTH || text.indexOf('.') > -1
           ) {                            
            JOptionPane.showMessageDialog(null, malformedMsg);
            return false;
        }
        
        if (contains(libraryData.getDefaultExtensions(), text)) {
            Category type = CategoryUtils.getCategory(MediaType.getMediaTypeForExtension(text));
            
            if (type == null) {
                type = Category.OTHER;
            }
            
            CheckBoxList<String> panel = this.panels.get(type); 
            
            if (panel == null) {
                JOptionPane.showMessageDialog(null, malformedMsg);
                return false;    
            }
         
            this.switchPanel(type);
            panel.setItemChecked(text);
            this.sidePanel.setItemSelected(type);
            
            return false;
        }

        if (otherPanel.getElements().contains(text)) {
            
            this.switchPanel(Category.OTHER);
            otherPanel.setItemChecked(text);
            this.sidePanel.setItemSelected(Category.OTHER);
            
            return false;
        }
        
        
        return true;
    }
    
    private static boolean contains(Collection<String> list, String value) {
        for ( Object item : list ) {
            if (item.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    private Map<Category, List<String>> createExtensionsMap(Collection<String> extensions) {
        Map<Category, List<String>> extensionsByType = new LinkedHashMap<Category, List<String>>();
        for (String extension : extensions) { 
            Category category = CategoryUtils.getCategory(MediaType.getMediaTypeForExtension(extension));
            if (category == null) {
                category = CategoryUtils.getCategory(MediaType.getOtherMediaType());
            }
            if (!extensionsByType.containsKey(category)) {
                extensionsByType.put(category, new ArrayList<String>(8));
            }
            List<String> typeExtension = extensionsByType.get(category);
            if (!typeExtension.contains(extension)) {
                typeExtension.add(extension);
                
                if (category == Category.OTHER) {
                    if (!contains(libraryData.getDefaultExtensions(), extension)) {
                        removableExts.add(extension);
                    }
                }
            }
        }
        
        return extensionsByType;
    }
    
    private Collection<String> getExtensions() {
        Set<String> elements = new HashSet<String>();
        
        for ( CheckBoxList<String> panel : this.panels.values() ) {
             elements.addAll(panel.getCheckedElements());
        }
        return elements;
    }
    
    
    // Methods to integrate with PaneItem
    
    public void initOptions() {
        this.initCore();
    }

    public boolean applyOptions() {
        libraryData.setManagedExtensions(getExtensions());        
        return false;
    }

    public boolean hasChanged() {
        return !this.originalExtensions.equals(this.getExtensions());
    }
    
    
    // Listener Classes
    
    private static class SideSelectListener implements
            CheckBoxListSelectionListener {

        private FileTypeOptionPanelManager parent;

        public SideSelectListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListSelectionEvent e) {
            parent.switchPanel((Category)e.getSelected());
        }
    }

    private static class CheckChangeListener implements
            CheckBoxListCheckChangeListener<Category> {

        private FileTypeOptionPanelManager parent;

        public CheckChangeListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListCheckChangeEvent<Category> e) {
            this.parent.setPanelEnabled(e.getSelected(), e.getChecked());
            this.parent.refreshSidePanel();
        }
    }  
    
    private static class PanelsCheckChangeListener implements
    CheckBoxListCheckChangeListener<MediaType> {

        private FileTypeOptionPanelManager parent;

        public PanelsCheckChangeListener(FileTypeOptionPanelManager parent) {
            this.parent = parent;
        }

        public void valueChanged(CheckBoxListCheckChangeEvent<MediaType> e) {
            this.parent.refreshSidePanel();
        }
    }  

    // Providers   
    
    private class ExtensionProvider implements CheckBoxList.TextProvider<String> {
        
        public String getText(String obj) {
            if (obj == null) {
                new NullPointerException("Null object passed to text lookup.");
            }
            
            return obj;
        }
        
        public String getToolTipText(String obj) {
            return iconManager.getMIMEDescription(obj);
        }

        public Icon getIcon(String obj) {
            if (obj == null) {
                throw new NullPointerException("Null object passed to icon lookup.");
            }
            
            Icon icon = iconManager.getIconForExtension(obj);
            
            return icon != null ? icon : iconManager.getBlankIcon();
        }
    }
    
    
    private class MediaProvider 
        implements CheckBoxList.TextProvider<Category> {
        
        public String getText(Category obj) {
                    
            return obj.toString();
        }
        
        public String getToolTipText(Category obj) {
            return null;
        }

        public Icon getIcon(Category obj) {
            return categoryIconManager.getIcon(obj);
        }
    }
        
    private class MediaExtrasProvider
        implements CheckBoxList.ExtrasProvider<Category> {
        
        public MediaExtrasProvider() {

        }        
        
        @Override
        public boolean isSeparated(Category obj) {
            return false;
        }

        @Override
        public String getComment(Category obj) {
            CheckBoxList<String> panel = panels.get(obj);
            
            if (panel == null) {
                return "(0)";
            }
            
            return "(" + panel.getCheckedElements().size() + ")";
        }

        @Override
        public int getCommentFieldSize() {
            return 0;
        }
    }    
    
    private class ExtensionsExtrasProvider
        implements CheckBoxList.ExtrasProvider<String> {
    
        public ExtensionsExtrasProvider() {
        }        
    
        @Override
        public boolean isSeparated(String obj) {
            return false;
        }

        @Override
        public String getComment(String obj) {
            return iconManager.getMIMEDescription(obj);
        }

        @Override
        public int getCommentFieldSize() {
            return 70;
        }
    }    

}
