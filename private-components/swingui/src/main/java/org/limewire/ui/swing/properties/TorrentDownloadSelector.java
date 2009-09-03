package org.limewire.ui.swing.properties;

import javax.swing.JDialog;

import org.limewire.bittorrent.Torrent;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class TorrentDownloadSelector extends LimeJDialog {
    
    public static int showBittorrentSelector(Torrent torrent, FileInfoPanelFactory factory) {

        TorrentSelectorPanel panel = new TorrentSelectorPanel(torrent, factory);
        
        JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("Torrent Download"), 
                null, panel.getComponent());
        dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
        dialog.setResizable(true);
        dialog.setVisible(true);
        dialog.dispose();
        return panel.getCloseValue();
    }
}
