package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JPopupMenu;

import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.model.BasicDownloadState;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class implements the menu that is displayed
 * when the user right clicks a search result.
 */
public class SearchResultMenu extends JPopupMenu {

    public SearchResultMenu(final DownloadHandler downloadHandler,
        final List<VisualSearchResult> selectedItems,
        final PropertiesFactory<VisualSearchResult> propertiesFactory) {

        final VisualSearchResult firstItem = selectedItems.get(0);
        
        
        boolean downloadEnabled = false;
        for(VisualSearchResult visualSearchResult : selectedItems) {
            if(visualSearchResult.getDownloadState() == BasicDownloadState.NOT_STARTED) {
                downloadEnabled = true;
                break;
            }
        }
        
        boolean showHideSimilarFileVisible = selectedItems.size() == 1 && firstItem.getSimilarResults().size() > 0;
        boolean showHideSimilarFileEnabled = selectedItems.size() == 1 && firstItem.getDownloadState() == BasicDownloadState.NOT_STARTED;
        boolean viewFileInfoEnabled = selectedItems.size() == 1;
        
        add(new AbstractAction(tr("Download")) {
            public void actionPerformed(ActionEvent e) {
                for(VisualSearchResult visualSearchResult : selectedItems) {
                    if(visualSearchResult.getDownloadState() == BasicDownloadState.NOT_STARTED) {
                        downloadHandler.download(visualSearchResult);
                    }
                }
            }
        }).setEnabled(downloadEnabled);

        add(new AbstractAction(firstItem.isSpam() ? tr("Unmark as spam") : tr("Mark as spam")) {
            public void actionPerformed(ActionEvent e) {
                boolean spam = !firstItem.isSpam();
                for(VisualSearchResult visualSearchResult : selectedItems) {
                    visualSearchResult.setSpam(spam);
                }
            }
        });

        addSeparator();

        if (showHideSimilarFileVisible) {
            add(new AbstractAction(tr(firstItem.isChildrenVisible() ? "Hide Similar Files" : "Show Similar Files")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    firstItem.toggleChildrenVisibility();
                }
            }).setEnabled(showHideSimilarFileEnabled);
            
            addSeparator();
        }

        add(new AbstractAction(tr("View File Info...")) {
            public void actionPerformed(ActionEvent e) {
                propertiesFactory.newProperties().showProperties(firstItem);
            }
        }).setEnabled(viewFileInfoEnabled);
    }
}