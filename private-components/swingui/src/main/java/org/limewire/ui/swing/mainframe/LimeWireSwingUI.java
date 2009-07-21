package org.limewire.ui.swing.mainframe;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
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
import javax.swing.SwingUtilities;

import org.limewire.core.api.Application;
import org.limewire.core.api.updates.UpdateEvent;
import org.limewire.core.settings.DownloadSettings;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.listener.EventListener;
import org.limewire.listener.ListenerSupport;
import org.limewire.listener.SwingEDTEvent;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.ui.swing.components.LimeSplitPane;
import org.limewire.ui.swing.components.PanelResizer;
import org.limewire.ui.swing.downloads.DownloadHeaderPanel;
import org.limewire.ui.swing.downloads.DownloadVisibilityListener;
import org.limewire.ui.swing.downloads.MainDownloadPanel;
import org.limewire.ui.swing.event.DownloadVisibilityEvent;
import org.limewire.ui.swing.friends.chat.ChatFramePanel;
import org.limewire.ui.swing.friends.login.LoginPopupPanel;
import org.limewire.ui.swing.nav.Navigator;
import org.limewire.ui.swing.pro.ProNagController;
import org.limewire.ui.swing.search.SearchHandler;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.statusbar.SharedFileCountPopupPanel;
import org.limewire.ui.swing.statusbar.StatusPanel;
import org.limewire.ui.swing.update.UpdatePanel;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class LimeWireSwingUI extends JPanel {
    
    private final JPanel centerPanel;
    private final TopPanel topPanel;
    private final JLayeredPane layeredPane;
    private final ProNagController proNagController;
    private final LimeSplitPane splitPane;
    private final Provider<SignOnMessageLayer> signOnMessageProvider;
    private final MainDownloadPanel mainDownloadPanel;
    private final DownloadHeaderPanel downloadHeaderPanel;
    
	@Inject
    public LimeWireSwingUI(
            TopPanel topPanel, MainPanel mainPanel,
            StatusPanel statusPanel, Navigator navigator,
            SearchHandler searchHandler, ChatFramePanel chatFrame,
            ProNagController proNagController, 
            SharedFileCountPopupPanel sharedFileCountPopup,
            LoginPopupPanel loginPopup,
            Provider<SignOnMessageLayer> signOnMessageProvider,
            MainDownloadPanel mainDownloadPanel, DownloadHeaderPanel downloadHeaderPanel, @GlobalLayeredPane JLayeredPane limeWireLayeredPane) {
    	GuiUtils.assignResources(this);
    	        
    	this.topPanel = topPanel;  	
    	this.layeredPane = limeWireLayeredPane;
    	this.proNagController = proNagController;
    	this.signOnMessageProvider = signOnMessageProvider;
        this.centerPanel = new JPanel(new GridBagLayout());   
        this.mainDownloadPanel = mainDownloadPanel;
        this.downloadHeaderPanel = downloadHeaderPanel;
    	
    	splitPane = createSplitPane(mainPanel, mainDownloadPanel, downloadHeaderPanel);
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
//        gbc.fill = GridBagConstraints.VERTICAL;
//        gbc.anchor = GridBagConstraints.WEST;
//        gbc.weightx = 0;
//        gbc.weighty = 1;
//        gbc.gridwidth = 1;
//        gbc.gridheight = 3;
//        centerPanel.add(leftPanel, gbc);
        
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
        layeredPane.add(sharedFileCountPopup, JLayeredPane.PALETTE_LAYER);
        layeredPane.addComponentListener(new PanelResizer(sharedFileCountPopup));
        layeredPane.add(loginPopup, JLayeredPane.POPUP_LAYER);
        layeredPane.addComponentListener(new PanelResizer(loginPopup));
//        layeredPane.add(shapeDialog, JLayeredPane.POPUP_LAYER);
//        layeredPane.addComponentListener(new PanelResizer(shapeDialog));
        add(layeredPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
        
    }
	
	@Inject
	public void registerListener(){
	    mainDownloadPanel.addDownloadVisibilityListener(new DownloadVisibilityHandler());
	}
	
	private boolean isFirstPainting = true;
	@Override
    public void paint(Graphics g){
	    if(isFirstPainting && splitPane.getHeight() > 0){
	        isFirstPainting = false;
	        if(DownloadSettings.ALWAYS_SHOW_DOWNLOADS_TRAY.getValue()){
	            handleDownloadVisibiltyChange(true);
	        }
	    }
	    super.paint(g);
	}
	
	void hideMainPanel() {
	    layeredPane.setVisible(false);
        centerPanel.setVisible(false);
    }
	
	void showMainPanel() {
        layeredPane.setVisible(true);
        centerPanel.setVisible(true);
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
    
   private LimeSplitPane createSplitPane(final JComponent top, final MainDownloadPanel bottom, JComponent divider) {
        final LimeSplitPane splitPane = new LimeSplitPane(JSplitPane.VERTICAL_SPLIT, true, top, bottom, divider);
        splitPane.setDividerSize(0);
        bottom.setVisible(false);
        splitPane.setBorder(BorderFactory.createEmptyBorder());

        //Allow bottom panel to be minimized
        bottom.setMinimumSize(new Dimension(0, 0));
        
        //The bottom panel remains the same size when the splitpane is resized
        splitPane.setResizeWeight(1);  
        
        //set top panel's minimum height to half of split pane height 
        //(this fires when the app is initialized)
        splitPane.addComponentListener(new ComponentAdapter(){            
            @Override
            public void componentResized(ComponentEvent e) {
                top.setMinimumSize(new Dimension(0, splitPane.getHeight()/2));
            }
        });
        
        mainDownloadPanel.addComponentListener(new ComponentAdapter(){
            @Override
            public void componentResized(ComponentEvent e) {
                int height = bottom.getHeight();
                if(height > bottom.getDefaultPreferredHeight()){
                    SwingUiSettings.DOWNLOAD_TRAY_SIZE.setValue(height);
                } else {
                    SwingUiSettings.DOWNLOAD_TRAY_SIZE.setValue(bottom.getDefaultPreferredHeight());
                }
            }
        });

        return splitPane;
    }

    private class DownloadVisibilityHandler implements DownloadVisibilityListener {
        @Override
        public void updateVisibility(DownloadVisibilityEvent event) {
            handleDownloadVisibiltyChange(event.getVisibility());
        }
    }
   
    
   private void handleDownloadVisibiltyChange(boolean isVisible){
       assert(SwingUtilities.isEventDispatchThread());
       splitPane.getBottomComponent().setVisible(isVisible);
       if (isVisible) {
           splitPane.setDividerSize(downloadHeaderPanel.getPreferredSize().height);           
           int preferredDividerPosition = splitPane.getSize().height - splitPane.getInsets().bottom
           - splitPane.getDividerSize()
           - splitPane.getBottomComponent().getPreferredSize().height;
           if (preferredDividerPosition < (splitPane.getHeight()/2)){
               preferredDividerPosition = splitPane.getHeight()/2;
           }
           splitPane.setDividerLocation(preferredDividerPosition);
        } else {            
            splitPane.setDividerSize(0);
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
    @Inject void register(ListenerSupport<UpdateEvent> updateEvent,
            ListenerSupport<FriendConnectionEvent> connectionSupport,
            final Application application) {
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
        
        // Add listener to display sign-on message if enabled.
        if (SignOnMessageLayer.isSignOnMessageEnabled()) {
            connectionSupport.addListener(new EventListener<FriendConnectionEvent>() {
                @Override
                @SwingEDTEvent
                public void handleEvent(FriendConnectionEvent event) {
                    if (event.getType() == FriendConnectionEvent.Type.CONNECTED) {
                        signOnMessageProvider.get().showMessage();
                    }
                }
            });
        }
    }
}