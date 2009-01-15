package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.search.SearchResult;
import org.limewire.ui.swing.action.BitziLookupAction;
import org.limewire.ui.swing.listener.MousePopupListener;
import org.limewire.ui.swing.properties.Dialog;
import org.limewire.ui.swing.properties.DialogParam;
import org.limewire.ui.swing.properties.FilterList;
import org.limewire.ui.swing.properties.Properties;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.VisualSearchResult;
import org.limewire.ui.swing.util.CategoryIconManager;
import org.limewire.ui.swing.util.GuiUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SearchResultPropertiesFactory implements PropertiesFactory<VisualSearchResult> {
    private final DialogParam dialogParam;
    
    @Inject
    public SearchResultPropertiesFactory(DialogParam dialogParam) {
        this.dialogParam = dialogParam;
    }
    
    @Override
    public Properties<VisualSearchResult> newProperties() {
        return new SearchResultProperties(dialogParam);
    }

    public static class SearchResultProperties extends Dialog implements Properties<VisualSearchResult> {
        private final CategoryIconManager categoryIconManager;
        private final FilterList filterList;
        
        public SearchResultProperties(DialogParam dialogParam) {
            super(dialogParam);
            this.categoryIconManager = dialogParam.getCategoryIconManager();
            this.filterList = dialogParam.getFilterList();
            GuiUtils.assignResources(this);
            disableEditForAllCommonFields();

            location.setLayout(new MigLayout("nocache", "[50%!]", "[]"));
            readOnlyInfoModel.setColumnIdentifiers(new Object[] { tr("Address"), tr("Filename") });
            location.add(new JScrollPane(readOnlyInfo));
        }

        @Override
        public void showProperties(VisualSearchResult vsr) {
            icon.setIcon(categoryIconManager.getIcon(vsr, iconManager));
            heading.setText(vsr.getHeading());
            filename.setText(vsr.getPropertyString(FilePropertyKey.NAME));
            fileSize.setText(vsr.getPropertyString(FilePropertyKey.FILE_SIZE));
            genre.setModel(new DefaultComboBoxModel(new String[]{ vsr.getPropertyString(FilePropertyKey.GENRE) }));
            rating.setModel(new DefaultComboBoxModel(new String[]{ vsr.getPropertyString(FilePropertyKey.RATING) }));
            populateMetadata(vsr);
            copyToClipboard.setAction(new CopyMagnetLinkToClipboardAction(vsr));
            moreFileInfo.setAction(new BitziLookupAction(vsr));
            album.setText(vsr.getPropertyString(FilePropertyKey.ALBUM));
            title.setText(vsr.getPropertyString(FilePropertyKey.TITLE));
            year.setText(vsr.getPropertyString(FilePropertyKey.YEAR));
            description.setText(vsr.getPropertyString(FilePropertyKey.DESCRIPTION));

            // Clear the table
            readOnlyInfoModel.setRowCount(0);

            for (SearchResult result : vsr.getCoreSearchResults()) {
                for (RemoteHost host : result.getSources()) {
                    readOnlyInfoModel.addRow(new Object[] { host.getFriendPresence().getFriend().getRenderName(),
                            result.getFileName() });
                }
            }
            
            readOnlyInfo.addMouseListener(new MousePopupListener() {
                @Override
                public void handlePopupMouseEvent(final MouseEvent e) {
                    JPopupMenu blockingMenu = new JPopupMenu();
                    blockingMenu.add(new AbstractAction(tr("Block Address")) {
                        @Override
                        public void actionPerformed(ActionEvent actionEvent) {
                            int blockRow = readOnlyInfo.rowAtPoint(e.getPoint());
                            Object value = readOnlyInfoModel.getValueAt(blockRow, 0);
                            if (value != null) {
                                filterList.addIPToFilter(value.toString());
                            }
                        }
                    });
                    blockingMenu.show(readOnlyInfo, e.getX(), e.getY());
                }
            });

            showDialog(vsr.getPropertyString(FilePropertyKey.NAME), vsr.getCategory());
        }
        
        @Override
        protected void commit() {
            // no-op
        }
    }
}
