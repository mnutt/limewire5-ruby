package org.limewire.ui.swing.downloads.table;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.io.Address;
import org.limewire.ui.swing.properties.AbstractPropertiableFileDialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.PropertiableHeadings;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DownloadItemPropertiesFactory implements PropertiesFactory<DownloadItem> {
    private final PropertiableHeadings propertiableHeadings;
    private final DialogParam dialogParam;
    
    @Inject
    public DownloadItemPropertiesFactory(PropertiableHeadings propertiableHeadings, DialogParam dialogParam) {
        this.propertiableHeadings = propertiableHeadings;
        this.dialogParam = dialogParam;
    }

    public Properties<DownloadItem> newProperties() {
        return new DownloadItemProperties(propertiableHeadings, dialogParam);
    }

    private static class DownloadItemProperties extends AbstractPropertiableFileDialog implements Properties<DownloadItem>{
        private @Resource Font smallFont;
        private @Resource Font mediumFont;
        private @Resource Font largeFont;
        private final JPanel download = newPanel(new MigLayout("fill", "[]", "[]"));
        
        private DownloadItemProperties(PropertiableHeadings propertiableHeadings, DialogParam dialogParam) {
            super(propertiableHeadings, dialogParam);
            GuiUtils.assignResources(this);
        }

        @Override
        protected Font getSmallFont() {
            return smallFont;
        }
        
        @Override
        protected Font getLargeFont() {
            return largeFont;
        }

        @Override
        protected Font getMediumFont() {
            return mediumFont;
        }

        @Override
        public void showProperties(final DownloadItem propertiable) {
            
            populateCommonFields(propertiable);
            
            readOnlyInfoModel.setColumnCount(2);
            readOnlyInfoModel.setColumnIdentifiers(new Object[]{tr("Address"), tr("Filename")});
            for(Address source : propertiable.getSources()) {
                readOnlyInfoModel.addRow(new Object[] {source.getAddressDescription(), 
                                                       propertiable.getDownloadingFile().getName() });
            }
            
            readOnlyInfo.setShowGrid(true);
            download.add(new JScrollPane(readOnlyInfo));
            final JLabel completionLabel = new JLabel();
            mainPanel.add(box(tr("Download from"), completionLabel, download), "wmin 250, grow, cell 1 3");
            propertiable.addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            completionLabel.setText(tr("{0}% complete", propertiable.getPercentComplete()));
                        }
                    });
                }
            });
            
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }

        @Override
        protected void commit() {
            //no-op... Downloads have no mutable fields
        }
    }
}
