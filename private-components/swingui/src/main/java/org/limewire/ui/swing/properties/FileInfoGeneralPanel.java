package org.limewire.ui.swing.properties;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.Category;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.core.api.library.MetaDataException;
import org.limewire.core.api.library.MetaDataManager;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.core.api.properties.PropertyDictionary;
import org.limewire.core.api.search.SearchResult;
import org.limewire.core.api.spam.SpamManager;
import org.limewire.friend.api.Friend;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.CollectionBackedComboBoxModel;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.HyperlinkButton;
import org.limewire.ui.swing.library.LibraryMediator;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

/**
 * Displays meta data information about the given PropertiableFile. If the file
 * is local, the meta data can be edited and saved as xml. 
 */
public class FileInfoGeneralPanel implements FileInfoPanel{

    @Resource private Font smallFont;
    @Resource private Font mediumFont;
    @Resource private Font headerFont;
    
    private final JPanel component;
    
    private final FileInfoType type;
    private final PropertiableFile propertiableFile;
    private final PropertyDictionary propertyDictionary;
    private final SpamManager spamManager;
    private final MetaDataManager metaDataManager;
    private final LibraryMediator libraryMediator;
    
    private final Map<FilePropertyKey, JComponent> changedProps = new HashMap<FilePropertyKey, JComponent>();
    
    public FileInfoGeneralPanel(FileInfoType type, PropertiableFile propertiableFile,
            PropertyDictionary propertyDictionary, SpamManager spamManager, MetaDataManager metaDataManager, LibraryMediator libraryMediator) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        this.propertyDictionary = propertyDictionary;
        this.spamManager = spamManager;
        this.metaDataManager = metaDataManager;
        this.libraryMediator = libraryMediator;
        
        component = new JPanel(new MigLayout("fillx"));
        
        GuiUtils.assignResources(this);
        
        init();
    }
    
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        return changedProps.size() != 0 && propertiableFile instanceof LocalFileItem;
    }

    @Override
    public void save() {
        switch(type) {
        case LOCAL_FILE: 
            if (hasChanged()) {
                LocalFileItem item = (LocalFileItem) propertiableFile;
                Map<FilePropertyKey, Object> newData = new HashMap<FilePropertyKey, Object>();
                for (FilePropertyKey key : changedProps.keySet()) {
                    JComponent component = changedProps.get(key);
                    if(component instanceof JTextComponent) {
                        newData.put(key, ((JTextComponent)component).getText().trim());
                    } else if(component instanceof JComboBox) {
                        newData.put(key, ((JComboBox)component).getSelectedItem());
                    }
                }
                try {
                    metaDataManager.save(item, newData);
                } catch (MetaDataException e) {
                    String message = I18n.tr("Unable to save metadata changes.");
                    FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(), message, I18n.tr("View File Info"), 
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }     
            break;
        }
    }
    
    @Override
    public void unregisterListeners() {
        //no listeners registered
    }
    
    private void init() {
        component.setOpaque(false);
        
        if(propertiableFile.getCategory() != Category.OTHER) {
            createEditableDetails();
        }
        createLocation();
    }
    
    private void createEditableDetails() {
        component.add(createHeaderLabel(I18n.tr("Details")), "span, wrap");
        
        JScrollPane descriptionScrollPane = new JScrollPane(createEditableTextArea(propertiableFile.getPropertyString(FilePropertyKey.DESCRIPTION), FilePropertyKey.DESCRIPTION), 
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        switch(propertiableFile.getCategory()) {
        case AUDIO:
            component.add(createPlainLabel(I18n.tr("Title")), "growx, pushx 100");
            component.add(createPlainLabel(I18n.tr("Artist")), "growx, span 3, pushx 50, wrap");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TITLE), FilePropertyKey.TITLE), "growx, pushx 100, gapright unrelated");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.AUTHOR), FilePropertyKey.AUTHOR), "growx, span 3, pushx 50, wrap");
            //second line
            component.add(createPlainLabel(I18n.tr("Album")), "pushx 100");
            component.add(createPlainLabel(I18n.tr("Genre")), "pushx 17");
            component.add(createPlainLabel(I18n.tr("Year")), "pushx 17");
            component.add(createPlainLabel(I18n.tr("Track")), "pushx 17, wrap");
            
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.ALBUM), FilePropertyKey.ALBUM), "growx, pushx 100, gapright unrelated");
            component.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.GENRE), getGenres(propertiableFile), FilePropertyKey.GENRE), "growx, pushx 17, gapright unrelated");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.YEAR), FilePropertyKey.YEAR), "growx, pushx 17, wmin 35, gapright unrelated");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TRACK_NUMBER), FilePropertyKey.TRACK_NUMBER), "growx, wmin 35, pushx 17, wrap");

            //TODO: temporarily taken out since we aren't reading/writing the description field in id3 tags
//            //third line
//            component.add(createPlainLabel(I18n.tr("Description")), "span, wrap");
//
//            component.add(descriptionScrollPane, "span, growx, hmin 42");
            break;
        case VIDEO:
            component.add(createPlainLabel(I18n.tr("Title")), "wrap");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TITLE), FilePropertyKey.TITLE), "span, growx, wrap");
            component.add(createPlainLabel(I18n.tr("Genre")), "pushx 35");
            component.add(createPlainLabel(I18n.tr("Rating")), "pushx 35");
            component.add(createPlainLabel(I18n.tr("Year")), "pushx 30, wrap");
            component.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.GENRE), getGenres(propertiableFile), FilePropertyKey.GENRE), "growx, pushx 35, gapright unrelated");
            component.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.RATING), getRatings(propertiableFile), FilePropertyKey.RATING), "growx, pushx 35, gapright unrelated");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.YEAR), FilePropertyKey.YEAR), "growx, wmin 35, pushx 30, wrap");
            component.add(createPlainLabel(I18n.tr("Description")), "wrap");
            component.add(descriptionScrollPane, "span, growx, hmin 42, wrap");
            break;
        case IMAGE:
            component.add(createPlainLabel(I18n.tr("Title")), "wrap");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TITLE), FilePropertyKey.TITLE), "growx, wrap");
            component.add(createPlainLabel(I18n.tr("Description")), "wrap");
            component.add(descriptionScrollPane, "growx, hmin 42, wrap");
            break;
        case DOCUMENT:
            component.add(createPlainLabel(I18n.tr("Author")), "wrap");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.AUTHOR), FilePropertyKey.AUTHOR), "growx, wrap");
            component.add(createPlainLabel(I18n.tr("Description")), "wrap");
            component.add(descriptionScrollPane, "growx, hmin 42, wrap");
            break;
        case PROGRAM:
            component.add(createPlainLabel(I18n.tr("Title")), "wrap");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.TITLE), FilePropertyKey.TITLE), "growx, span, wrap");
            component.add(createPlainLabel(I18n.tr("Platform")), "growx 30");
            component.add(createPlainLabel(I18n.tr("Company")), "growx 70, wrap");
            component.add(createEditableComboBox(propertiableFile.getPropertyString(FilePropertyKey.PLATFORM), getPlatforms(propertiableFile), FilePropertyKey.PLATFORM), "growx 30, gapright 5");
            component.add(createEditableTextField(propertiableFile.getPropertyString(FilePropertyKey.COMPANY), FilePropertyKey.COMPANY), "growx 70, wrap");
            break;
        case OTHER:
            break;
        }
    }
    
    private void createLocation() {
        component.add(createHeaderLabel(I18n.tr("Location")), "span, gaptop 15, wrap");
        
        switch(type) {
        case LOCAL_FILE:
            if(propertiableFile instanceof LocalFileItem) {
                component.add(createLabelField(((LocalFileItem)propertiableFile).getFile().getAbsolutePath()), "span, growx, wrap");
                
                HyperlinkButton locateOnDisk = new HyperlinkButton(
                    new AbstractAction(I18n.tr("Locate on Disk")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            NativeLaunchUtils.launchExplorer(((LocalFileItem)propertiableFile).getFile());
                        }
                    });
                
                HyperlinkButton locateInLibrary = new HyperlinkButton( 
                    new AbstractAction(I18n.tr("Locate in Library")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            component.getRootPane().getParent().setVisible(false);
                            libraryMediator.selectInLibrary(((LocalFileItem)propertiableFile).getFile());
                        }
                    });
                
                component.add(locateOnDisk, "span, alignx right, split");
                component.add(locateInLibrary, "gapleft 15, wrap");
            }
            break;
        case REMOTE_FILE:
            if(propertiableFile instanceof VisualSearchResult) {
                final ReadOnlyTableModel model = new ReadOnlyTableModel();
                final JTable table = new JTable(model);
                
                model.setColumnIdentifiers(new Object[] { I18n.tr("Name"), I18n.tr("Address"), I18n.tr("Filename") });
    
                for (SearchResult result : ((VisualSearchResult)propertiableFile).getCoreSearchResults()) {
                    for (RemoteHost host : result.getSources()) {
                        Friend f = host.getFriendPresence().getFriend();
                        model.addRow(new Object[] {
                                f.getRenderName(),
                                f.getName(),
                                result.getFileName()
                        });
                    }
                }
                component.add(new JScrollPane(table), "span, grow, wrap");
                
                table.addMouseListener(new MousePopupListener() {
                    @Override
                    public void handlePopupMouseEvent(final MouseEvent e) {
                        JPopupMenu blockingMenu = new JPopupMenu();
                        blockingMenu.add(new AbstractAction(I18n.tr("Block Address")) {
                            @Override
                            public void actionPerformed(ActionEvent actionEvent) {
                                int blockRow = table.rowAtPoint(e.getPoint());
                                table.getSelectionModel().setSelectionInterval(blockRow, blockRow);
                                Object value = model.getValueAt(blockRow, 1);
                                if (value != null) {
                                    addToFilterList(value.toString());
                                }
                            }
                        });
                        blockingMenu.show(table, e.getX(), e.getY());
                    }
                });
            } else if(propertiableFile instanceof SearchResult) {
                String friend = ((SearchResult)propertiableFile).getSources().get(0).getFriendPresence().getFriend().getRenderName();
                component.add(createLabelField(friend), "span, growx, wrap");
            }
            break;
        case DOWNLOADING_FILE:
            if(propertiableFile instanceof DownloadItem) {
                File launchableFile = ((DownloadItem)propertiableFile).getDownloadingFile();
                if(launchableFile != null && launchableFile.getAbsoluteFile() != null)
                    component.add(createLabelField(launchableFile.getAbsolutePath()), "span, growx, wrap");
                else
                    component.add(createLabelField(propertiableFile.getFileName()), "span, growx, wrap");
                
                HyperlinkButton locateOnDisk2 = new HyperlinkButton(
                    new AbstractAction(I18n.tr("Locate on Disk")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if( ((DownloadItem)propertiableFile).getDownloadingFile() != null) {
                                NativeLaunchUtils.launchExplorer(((DownloadItem)propertiableFile).getDownloadingFile());
                            }
                        }
                    });
                
                HyperlinkButton locateInLibrary2 = new HyperlinkButton( 
                    new AbstractAction(I18n.tr("Locate in Library")) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            component.getRootPane().getParent().setVisible(false);
                            libraryMediator.selectInLibrary(((DownloadItem)propertiableFile).getUrn());
                        }
                    });
                
                component.add(locateOnDisk2, "span, alignx right, split");
                component.add(locateInLibrary2, "gapleft 15, wrap");
            }
            break;
        }
    }
    
    private JLabel createPlainLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallFont);
        return label;
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(headerFont);
        return label;
    }
    
    private JTextField createEditableTextField(String text, FilePropertyKey key) {
        JTextField field = new JTextField(text);
        field.setCaretPosition(0);
        if(type != FileInfoType.LOCAL_FILE)
            field.setEditable(false);
        else
            changedProps.put(key, field);
        field.setFont(mediumFont);
        return field;
    }
    
    private JTextArea createEditableTextArea(String text, FilePropertyKey key) {
        JTextArea area = new JTextArea(text);
        if(type != FileInfoType.LOCAL_FILE) {
            area.setEditable(false);
            area.setBackground(UIManager.getLookAndFeel().getDefaults().getColor("TextField.disabledBackground"));
        } else {
            changedProps.put(key, area);
        }
        area.setFont(mediumFont);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }
    
    /**
     * Returns a List of values to populate the genre combobox with.
     */
    private List<String> getGenres(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case AUDIO:
            return propertyDictionary.getAudioGenres();
        case VIDEO:
            return propertyDictionary.getVideoGenres();
        default:
            return Collections.emptyList();
        }
    }
    
    /**
     * If this combo box is editable, sets the model and selects the box, otherwise
     * returns a non-editable JTextField.
     */
    private JComponent createEditableComboBox(String selection, List<String> model, FilePropertyKey key) {
        if(type != FileInfoType.LOCAL_FILE) {
            return createEditableTextField(selection, null);
        } else {
            JComboBox comboBox = new JComboBox();
            changedProps.put(key, comboBox);
            setupComboBox(comboBox, selection, model);
            return comboBox;
        }
    }
    
    /**
     * Loads a combo box and selects the currently selected item.
     */
    private void setupComboBox(JComboBox comboBox, String current, List<String> possibles) {
        if(current == null) {
            current = "";
        }
        
        // If any are listed, current is non-empty, and possibles doesn't contain, add it in.
        if(!possibles.contains(current) && !current.equals("") && possibles.size() > 0) {
            possibles = new ArrayList<String>(possibles);            
            possibles.add(0, current);
            possibles = Collections.unmodifiableList(possibles);
        }
        
        ComboBoxModel model = new CollectionBackedComboBoxModel(possibles);
        comboBox.setModel(model);
        comboBox.setSelectedItem(current);
    }
    
    /**
     * Returns a list of values for the rating comboBox
     */
    private List<String> getRatings(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case VIDEO:
            return propertyDictionary.getVideoRatings();
        default:
            return Collections.emptyList();
        }
    }
    
    /**
     * Returns a list of values for the OS combobox.
     */
    private List<String> getPlatforms(final PropertiableFile propertiableSeed) {
        switch (propertiableSeed.getCategory()) {
        case PROGRAM:
            return propertyDictionary.getApplicationPlatforms();
        default:
            return Collections.emptyList();
        }
    }
    
    /**
     * Blacklists the given ipAddress.
     */
    private void addToFilterList(String ipAddress) {
        spamManager.addToBlackList(ipAddress);
    }
    
    private static class ReadOnlyTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
    
    private JTextField createLabelField(String text) {
        JTextField field = new JTextField(text);
        field.setCaretPosition(0);
        field.setEditable(false);
        field.setOpaque(false);
        field.setFont(smallFont);
        field.setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
        return field;
    }
}
