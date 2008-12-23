package org.limewire.ui.swing.statusbar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXHyperlink;
import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.VisibilityListener;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MinimizedDownloadSummaryPanel extends JPanel{
    
    private DownloadSummaryPanel downloadSummaryPanel;
    private JButton showButton;

    @Inject
    public MinimizedDownloadSummaryPanel(final DownloadSummaryPanel downloadSummaryPanel){
        
        GuiUtils.assignResources(this);
        
        this.downloadSummaryPanel = downloadSummaryPanel;
        
        downloadSummaryPanel.addVisibilityListener(new VisibilityListener() {
            @Override
            public void visibilityChanged(boolean visible) {
                // this guy is visible when DownloadSummaryPanel is minimized
                maybeSetVisible(!visible);
            }
        });
        
        setOpaque(false);
        
        showButton = new JXHyperlink();
        showButton.setName("MinimizedDownloadSummaryPanel.showButton");
        
        initShowButton();
        
        add(showButton);
    }
    
    private void initShowButton() {
        
        showButton.setText(I18n.tr("Downloads"));
        showButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                downloadSummaryPanel.forceInvisibility(false);
            }
        });
    }
    
    public void maybeSetVisible(boolean visible) {
        setVisible(downloadSummaryPanel.getDownloadCount() > 0 && visible);
        if(isVisible()){
            showButton.setText(I18n.tr("Downloads ({0})", downloadSummaryPanel.getDownloadCount()));
        }
    }

}
