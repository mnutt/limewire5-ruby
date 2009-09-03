package org.limewire.ui.swing.properties;

import static org.limewire.ui.swing.util.I18n.tr;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.settings.BittorrentSettings;
import org.limewire.ui.swing.action.AbstractAction;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

public class TorrentSelectorPanel {
    @Resource private Color backgroundColor;
    @Resource private Font largeFont;
    
    private final JPanel component;
   
    private FileInfoPanel bittorrentPanel;
    private JButton okButton;
    private JCheckBox checkBox;
    private int closeValue = JOptionPane.CLOSED_OPTION;
    
    public TorrentSelectorPanel(Torrent torrent, FileInfoPanelFactory factory) {        
        GuiUtils.assignResources(this);
        component = new JPanel(new MigLayout("fill, gap 0, insets 0, "));
        component.setPreferredSize(new Dimension(440, 500));
        component.setBackground(backgroundColor);
        
        bittorrentPanel = factory.createBittorentPanel(torrent);
        
        component.add(factory.createOverviewPanel(torrent).getComponent(), "growx, wrap");
        component.add(createHeaderLabel(I18n.tr("Select files to download")), "wrap");
        component.add(bittorrentPanel.getComponent(), "grow");
        createFooter();
    }
    
    public int getCloseValue() {
        return closeValue;
    }
    
    public JComponent getComponent() {
        return component;
    }
    
    private void close() {
        Window window = SwingUtilities.getWindowAncestor(component);
        window.setVisible(false);
    }
    
    /**
     * Adds a footer with the cancel/ok button to close the dialog.
     */
    private void createFooter() {
        okButton = new JButton(new OKAction());
        checkBox = new JCheckBox(I18n.tr("Always ask before starting torrent"));
        checkBox.setOpaque(false);
        checkBox.setSelected(BittorrentSettings.SHOW_POPUP_BEFORE_DOWNLOADING.getValue());
        
        JPanel footerPanel = new JPanel(new MigLayout("fill, insets 0 15 10 15"));
        footerPanel.add(checkBox, "span, wrap");
        footerPanel.add(okButton, "alignx right, aligny bottom, split, tag ok");
        footerPanel.add(new JButton(new CancelAction()), "aligny bottom, tag cancel");
        footerPanel.setOpaque(false);
        
        component.add(footerPanel, "grow, south");
    }
    
    private JLabel createHeaderLabel(String text) { 
        JLabel label = new JLabel(text);
        label.setFont(largeFont);
        return label;
    }
    
    /**
     * Closes the dialog and saves any data that may have changed.
     */
    private class OKAction extends AbstractAction {
        public OKAction() {
            super(tr("OK"));
            closeValue = JOptionPane.OK_OPTION;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            bittorrentPanel.save();
            BittorrentSettings.SHOW_POPUP_BEFORE_DOWNLOADING.setValue(checkBox.isSelected());
            close();
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
            closeValue = JOptionPane.CANCEL_OPTION;
            close();
        }
    }
}
