package org.limewire.ui.swing.statusbar;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.connection.ConnectionStrength;
import org.limewire.core.api.connection.GnutellaConnectionManager;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.painter.BarPainterFactory;
import org.limewire.ui.swing.painter.StatusBarSectionPainter;
import org.limewire.ui.swing.player.MiniPlayerPanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class StatusPanel extends JXPanel {
    
    @Resource private int height;

    private final SharedFileCountPanel sharedFileCountPanel;
    
    @Inject
    public StatusPanel(GnutellaConnectionManager connectionManager, AudioPlayer player, 
            FriendStatusPanel friendStatusPanel, LibraryNavigator libraryNavigator,
            ConnectionStatusPanel connectionStatus, SharedFileCountPanel sharedFileCountPanel,
            BarPainterFactory barPainterFactory, MinimizedDownloadSummaryPanel minDownloadPanel) {
        
        GuiUtils.assignResources(this);
        
        this.sharedFileCountPanel = sharedFileCountPanel;
        
        setLayout(new BorderLayout());
        ResizeUtils.forceHeight(this, height);
        
        setBackgroundPainter(barPainterFactory.createStatusBarPainter());
 
        StatusBarSectionPainter<JComponent> sectionPainter = new StatusBarSectionPainter<JComponent>();
        sharedFileCountPanel.setBackgroundPainter(sectionPainter);
        
        MiniPlayerPanel miniPlayerPanel = new MiniPlayerPanel(player, libraryNavigator);
        miniPlayerPanel.setVisible(false);
        
        minDownloadPanel.setVisible(false);
        
        Component friendPanel = friendStatusPanel.getComponent();
        friendPanel.setVisible(false);
        ResizeUtils.forceHeight(friendPanel, height);
        
        JPanel leftPanel = new JPanel(new MigLayout("insets 0, gap 0, fill, nogrid"));
        JPanel centerPanel = new JPanel(new MigLayout("insets 0, gap 0, filly, nogrid, center"));
        JPanel rightPanel = new JPanel(new MigLayout("insets 0, gap 0, fill, nogrid"));
        
        leftPanel.setOpaque(false);
        centerPanel.setOpaque(false);
        rightPanel.setOpaque(false);
        
        leftPanel.add(connectionStatus, "growy, gapbefore 2, gaptop 2");
        leftPanel.add(sharedFileCountPanel, "growy, gaptop 2");
        centerPanel.add(minDownloadPanel, "growy, gaptop 2");
        rightPanel.add(miniPlayerPanel, "gapafter 4");
        rightPanel.add(friendPanel, "growy");
        
        add(leftPanel, BorderLayout.WEST);
        add(centerPanel, BorderLayout.CENTER);
        add(rightPanel, BorderLayout.EAST);
        
        connectionManager.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                if(evt.getPropertyName().equals("strength")) {
                    updateComponents((ConnectionStrength)evt.getNewValue());
                }
            }
        });
        updateComponents(connectionManager.getConnectionStrength());
    }
    
    private void updateComponents(ConnectionStrength strength) {
        
        boolean sharingVisible = false;
        
        switch(strength) {
            
        case DISCONNECTED:
        case CONNECTING:
        case NO_INTERNET:
            
            sharingVisible = false;
            break;
            
        default:

            sharingVisible = true;
            break;
        }
        
        this.sharedFileCountPanel.setVisible(sharingVisible);
        
    }
}
