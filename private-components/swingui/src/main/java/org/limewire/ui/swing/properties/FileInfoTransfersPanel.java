package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.annotation.Resource;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.io.Address;
import org.limewire.ui.swing.components.Line;
import org.limewire.ui.swing.properties.FileInfoDialog.FileInfoType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class FileInfoTransfersPanel implements FileInfoPanel {

    @Resource private Font smallFont;
    @Resource private Font headerFont;
    
    private final JPanel component;
    private final FileInfoType type;
    private final PropertiableFile propertiableFile;
    private DownloadStatus downloadStatus;
    
    public FileInfoTransfersPanel(FileInfoType type, PropertiableFile propertiableFile) {
        this.type = type;
        this.propertiableFile = propertiableFile;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx"));
        
        init();
    }
    
    public JComponent getComponent() {
        return component;
    }

    @Override
    public boolean hasChanged() {
        return false;
    }

    @Override
    public void save() {
        //component never changes state
    }
    
    @Override
    public void unregisterListeners() {
        if(downloadStatus != null && propertiableFile instanceof DownloadItem) {
            ((DownloadItem)propertiableFile).removePropertyChangeListener(downloadStatus);
        }
    }
    
    private void init() {
        switch(type) {
        case DOWNLOADING_FILE:
            if(propertiableFile instanceof DownloadItem) {
                component.add(createHeaderLabel(I18n.tr("Downloading from")),"push");
                
                final JLabel percentLabel = createPlainLabel("");            
                component.add(percentLabel, "alignx right, wrap");
                component.add(Line.createHorizontalLine(),"span, growx 100, gapbottom 4, wrap");
                
                final ReadOnlyTableModel model = new ReadOnlyTableModel();
                final JTable readOnlyInfo = new JTable(model);
                model.setColumnCount(2);
                model.setColumnIdentifiers(new Object[]{tr("Address"), tr("Filename")});
                
                for(Address source : ((DownloadItem)propertiableFile).getSources()) {
                    model.addRow(new Object[] {source.getAddressDescription(), 
                                               ((DownloadItem)propertiableFile).getDownloadingFile().getName() });
                }
                downloadStatus = new DownloadStatus(percentLabel);
                ((DownloadItem)propertiableFile).addPropertyChangeListener(downloadStatus);
                
                component.add(new JScrollPane(readOnlyInfo), "span, grow, wrap");
            }
            break;
        }
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(headerFont);
        return label;
    }
    
    private JLabel createPlainLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallFont);
        return label;
    }
    
    private static class ReadOnlyTableModel extends DefaultTableModel {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
    
    /**
     * Listens for changes to the download status and updates the dialog.
     */
    private class DownloadStatus implements PropertyChangeListener {
        private final JLabel label;
        
        public DownloadStatus(JLabel label) {
            this.label = label;
        }
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    label.setText(tr("{0}% complete", ((DownloadItem)propertiableFile).getPercentComplete()));
                } 
            });
        }
    }
}
