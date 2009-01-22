package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

import org.limewire.concurrent.FutureEvent;
import org.limewire.core.api.Application;
import org.limewire.core.api.updates.UpdateEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.Resizable;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.downloads.DownloadSummaryPanel;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.pro.ProNag;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.settings.InstallSettings;
import org.limewire.ui.swing.statusbar.StatusPanel;
import org.limewire.ui.swing.update.UpdatePanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class LimeWireSwingUI extends JPanel {
    
    private final MainPanel mainPanel;
    private final TopPanel topPanel;
    private final ProNag proNag;
    private final Application application;
    private final JLayeredPane layeredPane;
    private final boolean isFirstLaunch;
    
	@Inject
    public LimeWireSwingUI(
            TopPanel topPanel, LeftPanel leftPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator,
            SearchHandler searchHandler, ChatFramePanel friendsPanel,
            AudioPlayer player, DownloadSummaryPanel downloadSummaryPanel,
            Application application, ProNag proNag, ShapeDialog shapeDialog) {
    	GuiUtils.assignResources(this);
    	        
    	this.topPanel = topPanel;
    	this.mainPanel = mainPanel;
    	this.proNag = proNag;
    	this.application = application;
    	this.layeredPane = new JLayeredPane();
    	
    	JPanel centerPanel = new JPanel(new GridBagLayout());
        setLayout(new BorderLayout());

        GridBagConstraints gbc = new GridBagConstraints();
                
        // The top panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = 1;
        centerPanel.add(topPanel, gbc);
                
        // The left panel
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        centerPanel.add(leftPanel, gbc);
        
        // The main panel
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        centerPanel.add(mainPanel, gbc);
        
        // The download summary panel
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.SOUTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridheight = GridBagConstraints.REMAINDER;

        centerPanel.add(downloadSummaryPanel, gbc);
        
        layeredPane.addComponentListener(new MainPanelResizer(centerPanel));
        layeredPane.add(centerPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.addComponentListener(new PanelResizer(friendsPanel));
        layeredPane.add(friendsPanel, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new PanelResizer(shapeDialog));
        layeredPane.add(shapeDialog, JLayeredPane.POPUP_LAYER);
        add(layeredPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        
        isFirstLaunch = !InstallSettings.UPGRADED_TO_5.getValue();
    }
	
	void hideMainPanel() {
	    mainPanel.setVisible(false);
	}
	
	void showMainPanel() {
	    mainPanel.setVisible(true);
	}
	
	void loadProNag() {
        if(!application.isProVersion() && !isFirstLaunch) {
	        proNag.loadContents().addFutureListener(new EventListener<FutureEvent<Void>>() {
	            @Override
	            @SwingEDTEvent
	            public void handleEvent(FutureEvent<Void> event) {
	                switch(event.getType()) {
	                case SUCCESS:
	                    layeredPane.addComponentListener(new PanelResizer(proNag));
	                    layeredPane.add(proNag, JLayeredPane.MODAL_LAYER);
	                    proNag.resize();
	                }
	            }
	        });
	    }
	}
    
    public void goHome() {
        topPanel.goHome();
    }

    public void focusOnSearch() {
        topPanel.requestFocusInWindow();
    }
    
    private static class MainPanelResizer extends ComponentAdapter {
        private final JComponent target;

        public MainPanelResizer(JComponent target) {
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
            if(target.isVisible()) {
                target.resize();
            }
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