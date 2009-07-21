package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;

/**
 * A Dialog for displaying FileInfo about a file. This dialog changes 
 * depending on the state of the file and the location of the file. 
 */
public class FileInfoDialog extends LimeJDialog {
    
    /**
     * Type of FileInfoDialog that can be created.
     * 
     * LOCAL_FILE - File exists within the current LimeWire,
     * DOWNLOADNG_FILE - File is in the process of downloading or a portion
     *  of the file has been downloaded already,
     * REMOTE_FILE - File exists on one or more computers but not
     *  within this LimeWire
     */
    public static enum FileInfoType {
        LOCAL_FILE, DOWNLOADING_FILE, REMOTE_FILE
    }
    
    @Resource private Color backgroundColor;

    private final FileInfoPanel fileInfoPanel;
    private JButton okButton;
    
    @Inject
    public FileInfoDialog(@Assisted PropertiableFile propertiableFile, @Assisted FileInfoType type,
                        FileInfoPanelFactory factory) {
        super(GuiUtils.getMainFrame());
        GuiUtils.assignResources(this);
        
        this.fileInfoPanel = factory.createFileInfoPanel(propertiableFile, type);
        
        setTitle(I18n.tr("{0}   Properties", propertiableFile.getFileName()));
        
        setLayout(new MigLayout("gap 0, insets 0, fill, wrap"));
        getContentPane().setBackground(backgroundColor);

        add(fileInfoPanel, "north");
        createFooter();
    
        setPreferredSize(new Dimension(500,565));
        setModalityType(ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(FileInfoDialog.DISPOSE_ON_CLOSE);

        pack();

        setLocationRelativeTo(GuiUtils.getMainFrame());

        // listen to the visibility setting/ disposes of the dialog when it becomes invisible
        addComponentListener(new ComponentListener(){
            @Override
            public void componentHidden(ComponentEvent e) {
                //unregister any listeners used and dispose of dialog when made invisible
                FileInfoDialog.this.fileInfoPanel.unregisterListeners();    
                FileInfoDialog.this.dispose();
            }

            @Override
            public void componentMoved(ComponentEvent e) {}
            @Override
            public void componentResized(ComponentEvent e) {}
            @Override
            public void componentShown(ComponentEvent e) {
                if(okButton != null)
                    okButton.requestFocusInWindow();
            }
        });

        setVisible(true);
    }

    /**
     * Adds a footer with the cancel/ok button to close the dialog.
     */
    private void createFooter() {
        okButton = new JButton(new OKAction());
        JPanel footerPanel = new JPanel(new MigLayout("fill, insets 0 15 10 15"));
        footerPanel.add(okButton, "alignx right, aligny bottom, split, tag ok");
        footerPanel.add(new JButton(new CancelAction()), "aligny bottom, tag cancel");
        footerPanel.setBackground(backgroundColor);
        
        add(footerPanel, "grow, south");
    }

    /**
     * Closes the dialog and saves any data that may have changed.
     */
    private class OKAction extends AbstractAction {
        public OKAction() {
            super(tr("OK"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
            fileInfoPanel.commit();
        }
    }

    /**
     * Closes the data and does not save any data even if it
     * has changed.
     */
    private class CancelAction extends AbstractAction {
        public CancelAction() {
            super(tr("Cancel"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            setVisible(false);
        }
    }
}
