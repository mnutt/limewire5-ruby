package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;

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
        final VisualSearchResult vsr,
        final PropertiesFactory<VisualSearchResult> propertiesFactory) {

        add(new AbstractAction(tr("Download")) {
            public void actionPerformed(ActionEvent e) {
                downloadHandler.download(vsr);
            }
        }).setEnabled(vsr.getDownloadState() == BasicDownloadState.NOT_STARTED);

        add(new AbstractAction(vsr.isSpam() ? tr("Unmark as spam") : tr("Mark as spam")) {
            public void actionPerformed(ActionEvent e) {
                vsr.setSpam(!vsr.isSpam());
            }
        }).setEnabled(vsr.getDownloadState() == BasicDownloadState.NOT_STARTED);;

        addSeparator();

        if (vsr.getSimilarResults().size() > 0) {
            add(new AbstractAction(tr(vsr.isChildrenVisible() ? "Hide Similar Files" : "Show Similar Files")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    vsr.toggleChildrenVisibility();
                }
            }).setEnabled(vsr.getDownloadState() == BasicDownloadState.NOT_STARTED);
            
            addSeparator();
        }

        add(new AbstractAction(tr("View File Info...")) {
            public void actionPerformed(ActionEvent e) {
                propertiesFactory.newProperties().showProperties(vsr);
            }
        });
    }
}