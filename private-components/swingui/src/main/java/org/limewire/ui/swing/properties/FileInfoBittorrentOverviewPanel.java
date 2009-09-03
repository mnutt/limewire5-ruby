package org.limewire.ui.swing.properties;

import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.bittorrent.TorrentFileEntry;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

/**
 * An overview panel when only a Torrent is available.
 */
class FileInfoBittorrentOverviewPanel implements FileInfoPanel {
    
    @Resource private Font smallFont;
    @Resource private Font smallBoldFont;
    
    private final JPanel component;
    private final Torrent torrent;
    
    public FileInfoBittorrentOverviewPanel(Torrent torrent) {
        this.torrent = torrent;
        
        GuiUtils.assignResources(this);
        
        component = new JPanel(new MigLayout("fillx, insets 10 3 10 10"));
        init();
    }
    
    @Override
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
        //no listeners registered
    }
    
    private void init() {
        component.setOpaque(false);

        component.add(createLabelField(torrent.getName()), "growx, span, wrap");
        
        long totalSize = 0;
        for(TorrentFileEntry entry : torrent.getTorrentFileEntries()) {
            totalSize += entry.getSize();
        }
        component.add(createLabel(I18n.tr("Size:")), "split 2");
        component.add(createLabelField(GuiUtils.toUnitbytes(totalSize) + "  (" + GuiUtils.toBytes(totalSize) + ")"), "growx, wrap");
        component.add(createLabel(I18n.tr("# files:")), "split 2");
        component.add(createLabelField(Integer.toString(torrent.getTorrentFileEntries().size())), "growx, wrap");
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(smallBoldFont);
        return label;
    }
    
    private JTextField createLabelField(String text) {
        JTextField field = new JTextField(text);
        field.setCaretPosition(0);
        field.setEditable(false);
        field.setOpaque(false);
        field.setFont(smallFont);
        field.setBorder(BorderFactory.createEmptyBorder(0,1,0,1));
        return field;
    }
}
