package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

import org.bushe.swing.event.annotation.EventSubscriber;
import org.limewire.core.api.Application;
import org.limewire.core.api.updates.UpdateEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.player.api.AudioPlayer;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeSplitPane;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.components.ShapeDialog;
import org.limewire.ui.swing.downloads.DownloadHeaderPanel;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.DownloadVisibilityEvent;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.pro.ProNagController;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.statusbar.SharedFileCountPopupPanel;
import org.limewire.ui.swing.statusbar.StatusPanel;
import org.limewire.ui.swing.update.UpdatePanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LimeWireSwingUI extends JPanel {
    
    private final MainPanel mainPanel;
    private final TopPanel topPanel;
    private final JLayeredPane layeredPane;
    private final ProNagController proNagController;
    private final LimeSplitPane splitPane;
    
	@Inject
    public LimeWireSwingUI(
            TopPanel topPanel, LeftPanel leftPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator,
            SearchHandler searchHandler, ChatFramePanel chatFrame,
            AudioPlayer player,
            ShapeDialog shapeDialog, ProNagController proNagController, 
            SharedFileCountPopupPanel connectionStatusPopup,
            MainDownloadPanel mainDownloadPanel, Provider<DownloadHeaderPanel> downloadHeaderPanelProvider) {
    	GuiUtils.assignResources(this);
    	        
    	this.topPanel = topPanel;
    	this.mainPanel = mainPanel;
    	this.layeredPane = new JLayeredPane();
    	this.proNagController = proNagController;
    	
    	JPanel centerPanel = new JPanel(new GridBagLayout());
    	
    	splitPane = createSplitPane(mainPanel, mainDownloadPanel, downloadHeaderPanelProvider.get());
    	mainDownloadPanel.setVisible(false);

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
        centerPanel.add(splitPane, gbc);
        
        layeredPane.addComponentListener(new MainPanelResizer(centerPanel));
        layeredPane.add(centerPanel, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(chatFrame, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new PanelResizer(chatFrame));
        layeredPane.add(connectionStatusPopup, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new PanelResizer(connectionStatusPopup));
        layeredPane.add(shapeDialog, JLayeredPane.POPUP_LAYER);
        layeredPane.addComponentListener(new PanelResizer(shapeDialog));
        add(layeredPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        EventAnnotationProcessor.subscribe(this);
    }
	
	void hideMainPanel() {
	    mainPanel.setVisible(false);
	}
	
	void showMainPanel() {
	    mainPanel.setVisible(true);
	}
	
	void loadProNag() {
	    proNagController.allowProNag(layeredPane);
	}
	
	public void goHome() {
        topPanel.goHome();
    }

    public void focusOnSearch() {
        topPanel.requestFocusInWindow();
    }
    
   private LimeSplitPane createSplitPane(JComponent top, final JComponent bottom, JComponent divider) {
        LimeSplitPane splitPane = new LimeSplitPane(JSplitPane.VERTICAL_SPLIT, true, top, bottom, divider);
        splitPane.getDivider().setVisible(false);
        bottom.setVisible(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        top.setMinimumSize(new Dimension(0, 0));
        bottom.setMinimumSize(new Dimension(0, 0));

        return splitPane;
    }
   
   @EventSubscriber
   public void handleDownloadVisibilityEvent(DownloadVisibilityEvent event){
       setDownloadPanelVisibility(event.getVisibility());
   }
   
   private void setDownloadPanelVisibility(boolean isVisible){
       splitPane.getDivider().setVisible(isVisible);
       splitPane.getBottomComponent().setVisible(isVisible);
       if (isVisible) {
            splitPane.setDividerLocation(splitPane.getSize().height - splitPane.getInsets().bottom
                    - splitPane.getDividerSize()
                    - splitPane.getBottomComponent().getPreferredSize().height);
        }
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
    
    /**
     * Listens for Update events and display a dialog if a update exists.
     * @param updateEvent
     */
    @Inject void register(ListenerSupport<UpdateEvent> updateEvent, final Application application) {
        updateEvent.addListener(new EventListener<UpdateEvent>() {
            @Override
            @SwingEDTEvent
            public void handleEvent(UpdateEvent event) {
                UpdatePanel updatePanel = new UpdatePanel(event.getData(), application);
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