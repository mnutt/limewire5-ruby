package org.limewire.ui.swing.wizard;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.library.LibraryData;
import org.limewire.core.settings.InstallSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.MultiLineLabel;
import org.limewire.ui.swing.library.manager.LibraryManagerItemImpl;
import org.limewire.ui.swing.library.manager.LibraryManagerModel;
import org.limewire.ui.swing.library.manager.LibraryManagerTreeTable;
import org.limewire.ui.swing.library.manager.RootLibraryManagerItem;
import org.limewire.ui.swing.util.FileChooser;
import org.limewire.ui.swing.util.I18n;

public class SetupPage2 extends WizardPage {

    private final String line1 = I18n.tr("LimeWire is ready to fill your Library");
    private final String line2 = I18n.tr("Your Library is a central location to view, share and unshare your files with the P2P Network and, or your friends.");
    private final String autoText = I18n.tr("Automatically manage My Library");
    private final String autoExplanation = I18n.tr("Choose this option to have LimeWire automatically scan files into your Library from My Documents and the Desktop.");
    private final String manualText = I18n.tr("Manually manage My Library");
    private final String manualExplanation = I18n.tr("Choose this option if you want to select which folders LimeWire scans into your Library.");
    private final String bottomText1 = I18n.tr("Scanning these folders into your Library will not automatically share your files.");
    private final String bottomText2 = I18n.tr("You can change these options later from Tools > Options");
    
    private final LibraryData libraryData;
    
    private final ButtonGroup buttonGroup;
    private final JRadioButton autoButton;
    private final JRadioButton manualButton;
    
    private final LibraryManagerTreeTable treeTable;
    private final JScrollPane treeTableScrollPane;
    private final JXButton addFolderButton;
        
    public SetupPage2(SetupComponentDecorator decorator, LibraryData libraryData) {
        this.libraryData = libraryData;
        
        setOpaque(false);
        setLayout(new MigLayout("insets 0, gap 0, nogrid"));
        
        ButtonSelectionListener buttonSelectionListener = new ButtonSelectionListener();
        
        autoButton = new JRadioButton();
        decorator.decorateLargeRadioButton(autoButton);
        autoButton.setSelected(true);
        autoButton.addActionListener(buttonSelectionListener);
        
        manualButton = new JRadioButton();
        decorator.decorateLargeRadioButton(manualButton);
        manualButton.addActionListener(buttonSelectionListener);
        
        addFolderButton = new JXButton(new AddDirectoryAction(SetupPage2.this));
        decorator.decoratePlainButton(addFolderButton);
        
        treeTable = new LibraryManagerTreeTable(libraryData);
        initManualPanel();
        treeTableScrollPane = new JScrollPane(treeTable);
        
        setTreeTableVisiable(false);
        
        buttonGroup = new ButtonGroup();
        buttonGroup.add(autoButton);
        buttonGroup.add(manualButton);
        
        JLabel label;

        add(autoButton, "gaptop 10, gapleft 40");
        label = new JLabel(autoText);
        decorator.decorateHeadingText(label);
        add(label, "gaptop 10, gapleft 10, wrap");
        
        label = new MultiLineLabel(autoExplanation, 500);
        decorator.decorateNormalText(label);
        add(label, "gapleft 76, wrap");
        
        add(manualButton, "gaptop 10, gapleft 40");
        label = new JLabel(manualText);
        decorator.decorateHeadingText(label);
        add(label, "gaptop 10, gapleft 10, wrap");
        
        label = new MultiLineLabel(manualExplanation, 500);
        decorator.decorateNormalText(label);
        add(label, "gapleft 76, wrap");
        
        add(treeTableScrollPane, "gaptop 10, gapleft 40, growx");
        add(addFolderButton, "gaptop 10, gapright 30, wrap");

        label = new MultiLineLabel(bottomText1,630);
        decorator.decorateHeadingText(label);
        add(label, "gaptop 10, gapleft 14, pushy, aligny bottom, wrap");
        
        label = new JLabel(bottomText2);
        decorator.decorateNormalText(label);
        add(label, "gapleft 14, aligny bottom");        
    }

    private void initManualPanel() {
        RootLibraryManagerItem root = new RootLibraryManagerItem();
        for(File file : libraryData.getDirectoriesToManageRecursively()) {
            root.addChild(new LibraryManagerItemImpl(root, libraryData, file, true, true));
        }

        treeTable.setTreeTableModel(new LibraryManagerModel(root));
    }

    @Override
    public String getLine1() {
        return line1;
    }
    
    @Override
    public String getLine2() {
        return line2;
    }

    @Override
    public String getFooter() {
        return "";
    }
    
    @Override
    public void applySettings() {
        InstallSettings.SCAN_FILES.setValue(true);
        LibraryManagerModel model = treeTable.getLibraryModel();
        Collection<File> manage = model.getManagedDirectories();
        Collection<File> exclude = model.getExcludedDirectories();
        libraryData.setManagedOptions(manage, exclude, libraryData.getManagedCategories());
    }
    
    private void setTreeTableVisiable(boolean visible){
        treeTableScrollPane.setVisible(visible);
        addFolderButton.setVisible(visible);
    }
    
    private class ButtonSelectionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            setTreeTableVisiable(buttonGroup.getSelection() == manualButton.getModel());
        }        
    }
    
    private class AddDirectoryAction extends AbstractAction {

        private Container parent;
        
        public AddDirectoryAction(Container parent) {
            this.parent = parent;
            
            putValue(Action.NAME, I18n.tr("Add New Folder"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose a folder to automatically scan for changes"));
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            File folder = FileChooser.getInputDirectory(parent);            
            if(folder == null) {
                return;
            } else {
                if(libraryData.isDirectoryAllowed(folder)) {
                    treeTable.addDirectory(folder);
                } else {
                    // TODO: Display message?
                }
            }
        }
    }
}
