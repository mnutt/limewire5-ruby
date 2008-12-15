package com.limegroup.gnutella.gui.options.panes;

import java.io.IOException;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import org.limewire.core.settings.QuestionsHandler;
import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.download.DownloaderDialog;

/**
 * This class defines the panel in the options
 * window that allows the user to select the
 * default file already exists behavior.
 */
public class DefaultActionPaneItem extends AbstractPaneItem { 
    
    public final static String TITLE = I18n.tr("File Already Exists Behavior");
    
    public final static String LABEL = I18n.tr("You can choose the default action that LimeWire will perform when you try to download a file that already exists on disk.");

    /** RadioButton for selecting Save as
     */    
    private JRadioButton saveAs;
    
    /** RadioButton for selecting Append (#) to filename
     */    
    private JRadioButton append;
    
    /** RadioButton for selecting Ask everytime
     */        
    private JRadioButton ask;

    /** Creates new ShutdownOptionsPaneItem
     *
     * @param key the key for this <tt>AbstractPaneItem</tt> that 
     *      the superclass uses to generate locale-specific keys
     */
    public DefaultActionPaneItem() {
        super(TITLE, LABEL);
        
        BoxPanel buttonPanel = new BoxPanel();
        
        String saveAsLabel = I18nMarker.marktr("Always Show the \'Save As\' Dialog");
        String appendLabel  = I18nMarker.marktr("Always Append (#) to the Filename");
        String askLabel  = I18nMarker.marktr("Always Ask What to do");
        saveAs = new JRadioButton(I18n.tr(saveAsLabel));
        append = new JRadioButton(I18n.tr(appendLabel));
        ask = new JRadioButton(I18n.tr(askLabel));
        
        ButtonGroup bg = new ButtonGroup();
        buttonPanel.add(ask);
        buttonPanel.add(saveAs);
        buttonPanel.add(append);
        bg.add(ask);
        bg.add(saveAs);
        bg.add(append);
        
        BoxPanel mainPanel = new BoxPanel(BoxPanel.X_AXIS);
        mainPanel.add(buttonPanel);
        mainPanel.add(Box.createHorizontalGlue());
        
        add(mainPanel);
    }

    /**
     * Applies the options currently set in this <tt>PaneItem</tt>.
     * 
     * @throws IOException if the options could not be fully applied
     */
    @Override
    public boolean applyOptions() throws IOException {
        if (ask.isSelected())
            QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.setValue(0);
        else if (append.isSelected())
            QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.setValue(1);
        else if (saveAs.isSelected())
            QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.setValue(2);

        return false;
    }
    
    /**
     * Sets the options for the fields in this <tt>PaneItem</tt> when the
     * window is shown.
     */
    @Override
    public void initOptions() {
        int index = QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.getValue();
        if (index == DownloaderDialog.DEFAULT_ASK)
            ask.setSelected(true);
        else if (index == DownloaderDialog.DEFAULT_APPEND)
            append.setSelected(true);
        else if (index == DownloaderDialog.DEFAULT_SAVE_AS)
            saveAs.setSelected(true);
    }
    
    public boolean isDirty() {
        int index = -1;
        if (ask.isSelected())
            index = DownloaderDialog.DEFAULT_ASK;
        else if (append.isSelected())
            index = DownloaderDialog.DEFAULT_APPEND;
        else if (saveAs.isSelected())
            index = DownloaderDialog.DEFAULT_SAVE_AS;

        return QuestionsHandler.DEFAULT_ACTION_FILE_EXISTS.getValue() != index;
    }
}
