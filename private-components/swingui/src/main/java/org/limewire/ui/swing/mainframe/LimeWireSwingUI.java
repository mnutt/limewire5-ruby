package org.limewire.ui.swing.mainframe;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import org.limewire.core.api.Application;
import org.limewire.core.api.updates.UpdateEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.BoxPanel;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.pro.ProNag;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.statusbar.StatusPanel;
import org.limewire.ui.swing.update.UpdatePanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class LimeWireSwingUI extends JPanel {
    
    private final TopPanel topPanel;
    
	@Inject
    public LimeWireSwingUI(
            TopPanel topPanel, LeftPanel leftPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator,
            SearchHandler searchHandler, ChatFramePanel friendsPanel,
            AudioPlayer player, DownloadSummaryPanel downloadSummaryPanel,
            Application application, ProNag proNag, ShapeDialog shapeDialog) {
    	GuiUtils.assignResources(this);
    	        
    	this.topPanel = topPanel;
        
        GridBagLayout layout = new GridBagLayout();
        setLayout(layout);

        GridBagConstraints gbc = new GridBagConstraints();
                
        // The top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        add(topPanel, gbc);
                
        // The left panel
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        add(leftPanel, gbc);
        
        // The main panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        JLayeredPane layeredPane = new JLayeredPane();
        layeredPane.addComponentListener(new MainPanelResizer(mainPanel));
        layeredPane.addComponentListener(new PanelResizer(friendsPanel));
        layeredPane.add(mainPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(friendsPanel, JLayeredPane.PALETTE_LAYER);
        if(!application.isProVersion()) {
            layeredPane.addComponentListener(new PanelResizer(proNag));
            layeredPane.add(proNag, JLayeredPane.MODAL_LAYER);
            proNag.loadContents();
        }
        layeredPane.add(shapeDialog, JLayeredPane.POPUP_LAYER);
        layeredPane.addComponentListener(new PanelResizer(shapeDialog));
        add(layeredPane, gbc);
                
        JPanel southPanel = new BoxPanel(BoxPanel.Y_AXIS);
        southPanel.add(downloadSummaryPanel);
        southPanel.add(statusPanel);
        
        // The download summary panel and statusbar
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;
        add(southPanel, gbc);
    }

    
    public void goHome() {
        topPanel.goHome();
    }

    public void focusOnSearch() {
        topPanel.requestFocusInWindow();
    }
    
    private static class MainPanelResizer extends ComponentAdapter {
        private final MainPanel target;

        public MainPanelResizer(MainPanel target) {
            this.target = target;
        }
        
        @Override
        public void componentResized(ComponentEvent e) {
            Rectangle parentBounds = e.getComponent().getBounds();
            target.setBounds(0, 0, (int)parentBounds.getWidth(), (int)parentBounds.getHeight());
        }
    }
    
    private static class PanelResizer extends ComponentAdapter {
        private final Resizable target;
        
        public PanelResizer(Resizable target) {
            this.target = target;
        }
        
        @Override
        public void componentResized(ComponentEvent e) {
            target.resize();
        }
    }
    
    /**
     * Listens for Update events and display a dialog if a update exists.
     * @param updateEvent
     */
    @Inject void register(ListenerSupport<UpdateEvent> updateEvent, final Application application) {
        updateEvent.addListener(new EventListener<UpdateEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(UpdateEvent event) {
                UpdatePanel updatePanel = new UpdatePanel(event.getSource(), application);
                JDialog dialog = FocusJOptionPane.createDialog(I18n.tr("New Version Available!"), null, updatePanel);
                dialog.setLocationRelativeTo(GuiUtils.getMainFrame());
                dialog.getRootPane().setDefaultButton(updatePanel.getDefaultButton());
                dialog.setSize(new Dimension(500, 300));
                dialog.setModal(false);
                dialog.setVisible(true);
            }
        });
    }
}