package com.limegroup.gnutella.gui.options.panes;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.Action;
import javax.swing.JTextField;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LabeledComponent;
import com.limegroup.gnutella.gui.SaveDirectoryHandler;
import com.limegroup.gnutella.gui.SizedTextField;
import com.limegroup.gnutella.gui.GUIUtils.SizePolicy;
import com.limegroup.gnutella.gui.SaveDirectoryHandler.ValidationResult;
import com.limegroup.gnutella.gui.actions.AbstractAction;
import com.limegroup.gnutella.gui.options.OptionsMediator;

/**
 *  Creates a panel that displays the standard labels/buttons/text fields for
 *  allowing the user to change/revert to default a directory for saving files.
 */
public abstract class AbstractDirPaneItem extends AbstractPaneItem {

    /**
     * Constant for the key of the locale-specific <code>String</code> for the 
     * label on the component that allows to user to change the setting for this
     * <tt>PaneItem</tt>.
     */
    private final String OPTION_LABEL = I18nMarker.marktr("Folder:");
    
    /**
     * Handle to the <tt>JTextField</tt> that displays the save directory.
     */
    protected final JTextField saveField;
  
    /**
     * String for storing the initial save directory.
     */
    protected String saveDirectory;
  

    /**
     * The constructor constructs all of the elements of this 
     * <tt>AbstractPaneItem</tt>. This includes the row of buttons that allow
     * the user to select the save directory.
     * 
     * @param title - the title describing how the save directory is used 
     */
    public AbstractDirPaneItem(String title) {

        this(title, I18n.tr("You can choose the folder for saving files."));
    }
    
    public AbstractDirPaneItem(String title, String label) {
        super(title, label);

        saveField = new SizedTextField(25, SizePolicy.RESTRICT_HEIGHT);
        LabeledComponent comp = new LabeledComponent(OPTION_LABEL, saveField);

        ButtonRow br = new ButtonRow(getActions(), ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);
        
        add(comp.getComponent());
        add(getVerticalSeparator());
        add(br);
        add(getVerticalSeparator());
    }
    
    private Action[] getActions(){
        return new Action[]{
                new SelectSaveDirectoryListener(),
                new DefaultListener()
        };
    }

    /**
     * This listener responds to the selection of the default save directory
     * and sets the current save directory to be the default.
     */
    private class DefaultListener extends AbstractAction {
        
        public DefaultListener(){
            putValue(Action.NAME, I18n.tr("Use Default"));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Use the Default Folder"));
        }
        public void actionPerformed(ActionEvent e) {
            saveField.setText(getDefaultPath());
        }
    }
    
    /**
     * Defines the abstract method in <tt>AbstractPaneItem</tt>.
     * <p>
     *
     * Sets the options for the fields in this <tt>PaneItem</tt> when the 
     * window is shown.
     */
    @Override
    public abstract void initOptions(); 
    
    /**
     * Default directory to revert back on when setDefault is chosen
     * @return
     */
    public abstract String getDefaultPath();

    /**
     * This listener displays a <tt>JFileChooser</tt> to the user, allowing
     * the user to select the save directory.
     */
    private class SelectSaveDirectoryListener extends AbstractAction {
            
        public SelectSaveDirectoryListener(){
            putValue(Action.NAME, I18n.tr("Browse..."));
            putValue(Action.SHORT_DESCRIPTION, I18n.tr("Choose Another Folder"));
        }
        
        public void actionPerformed(ActionEvent e) { 
            File dir = FileChooserHandler.getInputDirectory(OptionsMediator.instance().getMainOptionsComponent(),
                    new File(saveDirectory));
            
            // If the user cancelled the file chooser, simply return.
            if (dir == null)
                return;
            
            ValidationResult result = SaveDirectoryHandler.isFolderValidForSaveDirectory(dir);
            switch(result) {
            case VALID:
                break;
            case BAD_BANNED:
            case BAD_VISTA:
            case BAD_SENSITIVE:
                return; // These already show a warning.
            case BAD_PERMS:
            default:
                // These need another message.
                GUIMediator.showError(I18n.tr("The selected save folder is invalid. You may not have permissions to write to the selected folder. LimeWire will revert to your previously selected folder."));
                return;
            }
            
            try {
                String newDir = dir.getCanonicalPath();
                if(!newDir.equals(saveDirectory)) {
                    saveField.setText(newDir);
                }
            } catch (IOException ioe) {}
        }
    }
    
    public void setDirectoryPath(String path){
        saveDirectory = path;
        saveField.setText(saveDirectory);
    }
 
    /**
     * Handles the logic for when save options is chosen
     */
    @Override
    public abstract boolean applyOptions() throws IOException;

    
    /**
     * @return true if the selected save directory is different than previous one.
     */
    public abstract boolean isDirty();
}
