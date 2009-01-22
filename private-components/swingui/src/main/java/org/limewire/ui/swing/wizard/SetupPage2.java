package org.limewire.ui.swing.wizard;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.Category;
import org.limewire.core.api.library.LibraryData;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.HorizonalCheckBoxListPanel;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryTreeTableContainer;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.IconManager;

public class SetupPage2 extends WizardPage {

    /**
     * A switch to control whether this page overrides or updates any existing lw settings.
     * 
     * (When doing an update this will be true and the new directories will be appended to the list.)
     */
    private final boolean isUpgrade;
    
    private final String titleLine = I18n.tr("My Library is where you view, share and unshare your files.");
    private final String titleLineUpgrade = I18n.tr("My Library is where you view, share and unshare your files.  Don't worry, we will still share files from the older version.");
    private final String autoText = I18n.tr("Automatically add files to My Library, but don't share any files");
    private final String autoTextUpgrade = I18n.tr("Automatically add files to My Library, but don't share any new files");
    private final String autoExplanation = I18n.tr("Have LimeWire automatically add files from My Documents and the Desktop to My Library.");
    private final String manualText = I18n.tr("Manually add files to My Library, but don't share any files");
    private final String manualTextUpgrade = I18n.tr("Manually add files to My Library, but don't share any new files");
    private final String manualExplanation = I18n.tr("Select the folders and categories LimeWire automatically adds to My Library.");
    private final String manualExplanationOpen = I18n.tr("Add the following categories from the folders below to My Library:");
    private final String bottomText = I18n.tr("You can change this later from Tools > Options");
    
    private final LibraryData libraryData;
    
    private final ButtonGroup buttonGroup;
    private final JRadioButton autoButton;
    private final JRadioButton manualButton;
    
    private final JLabel manualLabel;
    
    private final LibraryTreeTableContainer treeTableContainer;
    private final JXButton addFolderButton;
    
    private final HorizonalCheckBoxListPanel<Category> checkBoxes;
        
    public SetupPage2(SetupComponentDecorator decorator, IconManager iconManager, LibraryData libraryData) {
        this(decorator, iconManager, libraryData, false);
    }
    
    public SetupPage2(SetupComponentDecorator decorator, IconManager iconManager, LibraryData libraryData,
            boolean isUpgrade) {
        
        this.libraryData = libraryData;
        this.isUpgrade = isUpgrade;
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, nogrid"));
        
        ButtonSelectionListener buttonSelectionListener = new ButtonSelectionListener();
        
        autoButton = new JRadioButton();
        decorator.decorateLargeRadioButton(autoButton);
        autoButton.setSelected(true);
        autoButton.addActionListener(buttonSelectionListener);
        
        manualLabel = new MultiLineLabel(manualExplanation);
        decorator.decorateNormalText(manualLabel);
        
        manualButton = new JRadioButton();
        decorator.decorateLargeRadioButton(manualButton);
        manualButton.addActionListener(buttonSelectionListener);
        
        addFolderButton = new JXButton(new AddDirectoryAction(SetupPage2.this));
        decorator.decoratePlainButton(addFolderButton);
        
        treeTableContainer = new LibraryTreeTableContainer(iconManager, libraryData);
        initManualPanel();
        setTreeTableVisiable(false);
                
        Collection<Category> selectedCategories = getDefaultManagedCategories();
        checkBoxes = new HorizonalCheckBoxListPanel<Category>(Category.getCategoriesInOrder(), selectedCategories);
        checkBoxes.getCheckBox(Category.PROGRAM).setEnabled(false);
        checkBoxes.setVisible(false);
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(autoButton);
        buttonGroup.add(manualButton);
        
        JLabel label;

        add(autoButton, "gaptop 15, gapleft 40");
        if (isUpgrade) {
            label = new JLabel(autoTextUpgrade);
        } 
        else { 
            label = new JLabel(autoText);
        }
        label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(autoButton));
        decorator.decorateHeadingText(label);
        add(label, "gaptop 15, gapleft 10, wrap");
        
        label = new MultiLineLabel(autoExplanation, 500);
        decorator.decorateNormalText(label);
        add(label, "gapleft 76, wrap");
        
        manualButton.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (manualButton.isSelected()) {
                    manualLabel.setText(manualExplanationOpen);
                }
                else {
                    manualLabel.setText(manualExplanation);
                }
            }
        });
        add(manualButton, "gaptop 25, gapleft 40");
        
        if (isUpgrade) {
            label = new JLabel(manualTextUpgrade);
        } 
        else { 
            label = new JLabel(manualText);
        }

        decorator.decorateHeadingText(label);
        label.addMouseListener(new SetupComponentDecorator.ToggleExtenderListener(manualButton));
        add(label, "gaptop 25, gapleft 10, wrap");

        add(manualLabel, "gapleft 76, wrap");
        add(checkBoxes, "gaptop 5, gapleft 76, growx, wrap");
        
        add(treeTableContainer, "gaptop 5, gapleft 76, growx");
        add(addFolderButton, "aligny 0%, gaptop 5, gapright 30, wrap");


    }

    private void initManualPanel() {
        RootLibraryManagerItem root = new RootLibraryManagerItem(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
       
        for ( File file : AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData) ) {
            root.addChild(new LibraryManagerItemImpl(root, libraryData, file, false));
        }
        
        treeTableContainer.getTable().setTreeTableModel(new LibraryManagerModel(root));
    }

    @Override
    public String getLine1() {
        if (isUpgrade) {
            return titleLineUpgrade;
        }
        else {
            return titleLine;
        }
    }
    
    @Override
    public String getLine2() {
        return null;
    }

    @Override
    public String getFooter() {
        return bottomText;
    }
    
    @Override
    public void applySettings() {
        InstallSettings.SCAN_FILES.setValue(true);
        
        Collection<File> manage = new HashSet<File>();
        Collection<File> exclude = new HashSet<File>();
        Collection<Category> managedCategories = new HashSet<Category>();
        
        if (manualButton.isSelected()) {
            LibraryManagerModel model = treeTableContainer.getTable().getLibraryModel();
            manage.addAll(model.getRootChildrenAsFiles());
            exclude.addAll(model.getAllExcludedSubfolders());
            managedCategories.addAll(checkBoxes.getSelected());
        } else {
            manage.addAll(AutoDirectoryManageConfig.getDefaultManagedDirectories(libraryData));
            managedCategories.addAll(getDefaultManagedCategories());            
        }

        // Always add existing settings
        manage.addAll(libraryData.getDirectoriesToManageRecursively());
        exclude.addAll(libraryData.getDirectoriesToExcludeFromManaging());
        
        libraryData.setManagedOptions(manage, exclude, managedCategories);
    }
    
    private void setTreeTableVisiable(boolean visible){
        treeTableContainer.setVisible(visible);
        addFolderButton.setVisible(visible);
    }
    
    private class ButtonSelectionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean visible = buttonGroup.getSelection() == manualButton.getModel();
            setTreeTableVisiable(visible);
            checkBoxes.setVisible(visible);
        }        
    }
    
    private class AddDirectoryAction extends AbstractAction {

        private Container parent;
        
        public AddDirectoryAction(Container parent) {
            this.parent = parent;
            
            putValue(Action.NAME, I18n.tr("Add Folder"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a folder to automatically add to My Library"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            File folder = FileChooser.getInputDirectory(parent);            
            if(folder == null) {
                return;
            } else {
                if(libraryData.isDirectoryAllowed(folder)) {
                    treeTableContainer.getTable().addDirectory(folder);
                } else {
                    // TODO: Display message?
                }
            }
        }
    }
    
    private Collection<Category> getDefaultManagedCategories() {
        Collection<Category> categories = new HashSet<Category>();
        categories.addAll(Category.getCategoriesInOrder());
        categories.remove(Category.OTHER);
        categories.remove(Category.PROGRAM);
        return categories;
    }
}
