package org.limewire.ui.swing.search.resultpanel;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import javax.swing.AbstractAction;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;

import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.ui.swing.properties.PropertiesFactory;
import org.limewire.ui.swing.search.RemoteHostActions;
import org.limewire.ui.swing.search.model.VisualSearchResult;

/**
 * This class implements the menu that is displayed
 * when the user right clicks a search result.
 * 
 * @author R. Mark Volkmann, Object Computing, Inc.
 */
public class SearchResultMenu extends JPopupMenu {

    public SearchResultMenu(final DownloadHandler downloadHandler,
        final VisualSearchResult vsr,
        final RemoteHostActions fromActions,
        final PropertiesFactory<VisualSearchResult> propertiesFactory) {

        add(new AbstractAction(tr("Download")) {
            public void actionPerformed(ActionEvent e) {
                downloadHandler.download(vsr);
            }
        });

        add(new AbstractAction(vsr.isSpam() ? tr("Unmark as spam") : tr("Mark as spam")) {
            public void actionPerformed(ActionEvent e) {
                vsr.setSpam(!vsr.isSpam());
            }
        });

        add(new CopyMagnetLinkToClipboardAction(vsr));

        addSeparator();

        Collection<RemoteHost> sources = vsr.getSources();
        RemoteHost[] hosts = sources.toArray(new RemoteHost[] {});
        boolean multipleSources = sources.size() > 1;

        if (multipleSources) {
            for (RemoteHost host : hosts) {
                addSubmenu(host, fromActions);
            }
        } else {
            RemoteHost host = hosts[0];
            addSubmenu(host, fromActions);
        }

        addSeparator();

        add(new AbstractAction(tr("View File Info")) {
            public void actionPerformed(ActionEvent e) {
                propertiesFactory.newProperties().showProperties(vsr);
            }
        });
    }

    private void addSubmenu(final RemoteHost host, final RemoteHostActions fromActions) {
        JMenu menu = new JMenu(host.getFriendPresence().getFriend().getRenderName());

        menu.add(new AbstractAction(tr("View library")) {
            public void actionPerformed(ActionEvent e) {
                fromActions.viewLibraryOf(host);
            }
        });

        menu.add(new AbstractAction(tr("Chat")) {
            public void actionPerformed(ActionEvent e) {
                fromActions.chatWith(host);
            }
        });

        add(menu);
    }
}