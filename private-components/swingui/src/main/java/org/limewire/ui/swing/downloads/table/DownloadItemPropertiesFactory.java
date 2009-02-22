package org.limewire.ui.swing.downloads.table;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.Category;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.io.Address;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.properties.AbstractPropertiableFileDialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.NativeLaunchUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Factory class for creating a dialog to display the download item properties.
 */
@Singleton
public class DownloadItemPropertiesFactory implements PropertiesFactory<DownloadItem> {
    
    private final DialogParam dialogParam;
    
    /**
     * Constructs a DownloadItemPropertiesFactory with the specified dialog
     * parameters.
     */
    @Inject
    public DownloadItemPropertiesFactory(DialogParam dialogParam) {
        this.dialogParam = dialogParam;
    }

    @Override
    public Properties<DownloadItem> newProperties() {
        return new DownloadItemProperties(dialogParam);
    }

    /**
     * A dialog to display the download item properties.
     */
    private static class DownloadItemProperties extends AbstractPropertiableFileDialog 
        implements Properties<DownloadItem> {
        
        private final JPanel download = newPanel(new MigLayout("fill", "[]", "[]"));
        private final LibraryNavigator libraryNavigator;
        
        /**
         * Constructs a properties dialog with the specified dialog parameters.
         */
        private DownloadItemProperties(DialogParam dialogParam) {
            super(dialogParam);
            this.libraryNavigator = dialogParam.getLibraryNavigator();
            GuiUtils.assignResources(this);
            disableEditForAllCommonFields();
        }

        @Override
        public void showProperties(final DownloadItem propertiable) {
            
            populateCommonFields(propertiable);
            
            addDownload(propertiable);
            
            showDialog(propertiable.getFileName(), propertiable.getCategory());
        }

        /**
         * Adds information about the downloading file to the dialog. 
         */
        private void addDownload(final DownloadItem propertiable) {
            readOnlyInfoModel.setColumnCount(2);
            readOnlyInfoModel.setColumnIdentifiers(new Object[]{tr("Address"), tr("Filename")});
            for(Address source : propertiable.getSources()) {
                readOnlyInfoModel.addRow(new Object[] {source.getAddressDescription(), 
                                                       propertiable.getDownloadingFile().getName() });
            }
            
            readOnlyInfo.setShowGrid(true);
            download.add(new JScrollPane(readOnlyInfo));
            addDownloadingFileLocation(propertiable.getDownloadingFile(), propertiable.getCategory());
            
            fileLocation.setText(propertiable.getDownloadingFile().getAbsolutePath());
            final JLabel completionLabel = new JLabel();
            mainPanel.add(box(tr("Download from"), completionLabel, download), "wmin 250, grow, cell 0 3");
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
        }

        /**
         * Prepares the actions to display the downloading file in the OS file
         * explorer or the library tables.
         */
        private void addDownloadingFileLocation(final File downloadingFile, final Category category) {
            location.setLayout(new MigLayout("", "[]10[]15[]", "[top]"));
            location.add(fileLocation, "gapbottom 5,push");
            location.add(locateOnDisk);
            location.add(locateInLibrary);
            locateOnDisk.setAction(new AbstractAction(I18n.tr("locate on disk")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    NativeLaunchUtils.launchExplorer(downloadingFile);
                }
            });

            locateInLibrary.setAction(new AbstractAction(I18n.tr("locate in library")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    libraryNavigator.selectInLibrary(downloadingFile, category);
                }
            });
        }

        @Override
        protected void commit() {
            //no-op... Downloads have no mutable fields
        }
    }
}
