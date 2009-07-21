package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicSliderUI;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.Painter;
import org.limewire.player.api.PlayerState;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeSliderBar;
import org.limewire.ui.swing.components.MarqueeButton;
import org.limewire.ui.swing.components.VolumeSlider;
import org.limewire.ui.swing.components.decorators.SliderBarDecorator;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.painter.ComponentBackgroundPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.settings.SwingUiSettings;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Main UI container for the media player.
 */
public class PlayerPanel extends JXPanel implements PlayerMediatorListener {
    
    @Resource private int arcWidth;
    @Resource private int arcHeight;
    @Resource private Color innerBorder;
    @Resource private Color innerBackground;
    @Resource private Color bevelLeft;
    @Resource private Color bevelTop1;
    @Resource private Color bevelTop2;
    @Resource private Color bevelRight;
    @Resource private Color bevelBottom;
    
    @Resource private Icon backIcon;
    @Resource private Icon backIconPressed;
    @Resource private Icon backIconRollover; 
    @Resource private Icon backIconDisabled;
    @Resource private Icon forwardIcon;
    @Resource private Icon forwardIconPressed;
    @Resource private Icon forwardIconRollover;
    @Resource private Icon forwardIconDisabled;
    @Resource private Icon playIcon;
    @Resource private Icon playIconPressed;
    @Resource private Icon playIconRollover;
    @Resource private Icon playIconDisabled;
    @Resource private Icon pauseIcon;
    @Resource private Icon pauseIconPressed;
    @Resource private Icon pauseIconRollover;
    @Resource private Icon volumeIcon;
    @Resource private Icon volumeIconPressed;
    @Resource private Icon volumeIconRollover;
    @Resource private Icon volumeIconDisabled;
    @Resource private Icon shuffleIcon;
    @Resource private Icon shuffleIconPressed;
    @Resource private Icon shuffleIconRollover;
    @Resource private Icon shuffleIconActive;
          
    @Resource private Font font;
    
    private final JXPanel innerPanel;
    private final JButton backButton;
    private final JButton playButton;
    private final JButton pauseButton;
    private final JButton forwardButton;
    private final LimeSliderBar progressSlider;
    private final JPanel statusPanel;
    private final JButton volumeButton;
    private final JButton shuffleButton;
    
    private final JPopupMenu volumeControlPopup;
    private final VolumeSlider volumeSlider; 
    
    private final MarqueeButton titleLabel;
    
    private static final String BACK = "BACK";
    private static final String PLAY = "PLAY";
    private static final String PAUSE = "PAUSE";
    private static final String FORWARD = "FORWARD";
    private static final String VOLUME = "VOLUME";
    private static final String SHUFFLE = "SHUFFLE";

    private final Provider<PlayerMediator> playerProvider;
    
    /**
     * Constructs a PlayerPanel with the specified component providers and
     * decorators.
     */
    @Inject
    public PlayerPanel(Provider<PlayerMediator> playerProvider,
            SliderBarDecorator sliderBarDecorator) {
        
        this.playerProvider = playerProvider;

        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, filly, alignx center"));
        setOpaque(false);
        
        final ButtonListener playerListener = new ButtonListener();

        backButton = new IconButton(backIcon, backIconRollover, backIconPressed);
        backButton.addActionListener(playerListener);
        backButton.setActionCommand(BACK);
        backButton.setDisabledIcon(backIconDisabled);
        backButton.setToolTipText(I18n.tr("Skip Back"));
        
        playButton = new IconButton(playIcon, playIconRollover, playIconPressed);
        playButton.addActionListener(playerListener);
        playButton.setActionCommand(PLAY);
        playButton.setDisabledIcon(playIconDisabled);
        playButton.setToolTipText(I18n.tr("Play"));

        pauseButton = new IconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
        pauseButton.addActionListener(playerListener);
        pauseButton.setActionCommand(PAUSE);
        pauseButton.setVisible(false);
        pauseButton.setToolTipText(I18n.tr("Pause"));
        
        pauseButton.setMinimumSize(playButton.getMinimumSize());
        pauseButton.setPreferredSize(playButton.getPreferredSize());

        forwardButton = new IconButton(forwardIcon, forwardIconRollover, forwardIconPressed);
        forwardButton.addActionListener(playerListener);
        forwardButton.setActionCommand(FORWARD);
        forwardButton.setDisabledIcon(forwardIconDisabled);
        forwardButton.setToolTipText(I18n.tr("Skip Forward"));
        
        volumeButton = new IconButton(volumeIcon, volumeIconRollover, volumeIconPressed);
        volumeButton.addActionListener(playerListener);
        volumeButton.setActionCommand(VOLUME);
        volumeButton.setDisabledIcon(volumeIconDisabled);
        volumeButton.setToolTipText(I18n.tr("Volume"));
        
        volumeSlider = new VolumeSlider(0, 100);
        volumeControlPopup = volumeSlider.createPopup();
        
        shuffleButton = new IconButton(shuffleIcon, shuffleIconRollover, shuffleIconPressed, shuffleIconActive);
        shuffleButton.addActionListener(playerListener);
        shuffleButton.setActionCommand(SHUFFLE);
        shuffleButton.setRolloverSelectedIcon(shuffleIconActive);
        shuffleButton.setToolTipText(I18n.tr("Shuffle"));
        
        progressSlider = new LimeSliderBar();
        sliderBarDecorator.decoratePlain(progressSlider);
        initProgressControl();
        
        statusPanel = new JPanel(new MigLayout());
        
        titleLabel = new MarqueeButton(I18n.tr("Stopped"), 150);
        titleLabel.setFont(font);

        ResizeUtils.forceSize(titleLabel, new Dimension(206, (int)
                font.getMaxCharBounds(new FontRenderContext(null, false, false)).getHeight()));
        
        statusPanel.add(titleLabel);
        statusPanel.add(progressSlider, "dock south");
        statusPanel.setOpaque(false);
        
        int buttonWidth = backButton.getPreferredSize().width + 
        playButton.getPreferredSize().width + forwardButton.getPreferredSize().width; 
                
        Dimension statusSize = new Dimension(buttonWidth, statusPanel.getPreferredSize().height);
        statusPanel.setPreferredSize(statusSize);

        innerPanel = new JXPanel(new MigLayout("insets 4 10 4 10, filly, gapy 5, alignx center"));
        innerPanel.setOpaque(false);
        innerPanel.setBackgroundPainter(createStatusBackgroundPainter());
        
        innerPanel.add(backButton, "gapright 1");
        innerPanel.add(pauseButton, "hidemode 3");
        innerPanel.add(playButton, "hidemode 3");
        innerPanel.add(forwardButton, "gapright 3");
        innerPanel.add(statusPanel, "gapbottom 2, hidemode 2");
        innerPanel.add(volumeButton, "gapleft 2");
        innerPanel.add(shuffleButton, "gapleft 2");
        
        innerPanel.setVisible(SwingUiSettings.PLAYER_ENABLED.getValue());
        add(innerPanel, "gaptop 2, gapbottom 2");
                
        EventAnnotationProcessor.subscribe(this);

        VolumeController volumeController = new VolumeController();
        volumeSlider.addChangeListener(volumeController);
        
        volumeControlPopup.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                playerListener.clearMenu();
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                playerListener.clearMenu();
            }
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
            }
        });
        
        // Stop player if disabled, and show/hide player.
        SwingUiSettings.PLAYER_ENABLED.addSettingListener(new SettingListener(){
            @Override
            public void settingChanged(final SettingEvent evt) {
                SwingUtilities.invokeLater(new Runnable(){
                    public void run() {
                        boolean enabled = SwingUiSettings.PLAYER_ENABLED.getValue();
                        if (!enabled) {
                            getPlayerMediator().stop();
                        }
                        PlayerPanel.this.innerPanel.setVisible(enabled);
                    }
                });
            }
        });
    }
    
    /**
     * Registers listeners for player events.
     */
    @Inject
    void register() {
        getPlayerMediator().addMediatorListener(this);
    }
    
    /**
     * Initializes the progress component.
     */
    private void initProgressControl() {
        progressSlider.addChangeListener(new AudioProgressListener());
        progressSlider.setMaximum(Integer.MAX_VALUE);
        progressSlider.setMaximumSize(new Dimension(206, 6));
        progressSlider.setMinimumSize(new Dimension(206, 6));
        progressSlider.setPreferredSize(new Dimension(206, 6));
        progressSlider.setSize(new Dimension(206, 4));
        progressSlider.addMouseListener(new MouseAdapter() {
            /**
             * Reposition the thumb on the jslider to the location of the mouse
             * click
             */
            @Override
            public void mousePressed(MouseEvent e) {
                if (!progressSlider.isEnabled())
                    return;

                mouseSkip(e.getX());
            }
            
            /**
             * Overrides the mouse press increment when a mouse click occurs in the 
             * jslider. Repositions the jslider directly to the location the mouse
             * click avoiding the standard step increment with each click
             * @param x - location of mouse click
             */
            protected void mouseSkip(int x) {
                if (progressSlider.getUI() instanceof BasicSliderUI) {
                    progressSlider.setValue(((BasicSliderUI)progressSlider.getUI()).valueForXPosition(x));
                }
            }
        });
    }
    
    /**
     * Creates a background painter for the container.
     */
    private Painter<JXPanel> createStatusBackgroundPainter() {
        return new ComponentBackgroundPainter<JXPanel>(innerBackground, innerBorder, 
                bevelLeft, bevelTop1, bevelTop2, bevelRight, bevelBottom, 
                arcWidth, arcHeight, AccentType.SHADOW);
    }
    
    /**
     * Returns the mediator component that controls the player.
     */
    public PlayerMediator getPlayerMediator() {
        return playerProvider.get();
    }
    
    /**
     * Handles update to the specified progress value to adjust the visual
     * position of the slider.
     */
    @Override
    public void progressUpdated(float progress) {
        if (!(progressSlider.getValueIsAdjusting() || getPlayerMediator().getStatus() == PlayerState.SEEKING)) {
            progressSlider.setValue((int) (progressSlider.getMaximum() * progress));
        }
    }
    
    /**
     * Handles song change to the specified song name.
     */
    @Override
    public void songChanged(String name) {
        // Update volume.
        updateVolume();
        
        // Enable progress slider.
        progressSlider.setEnabled(getPlayerMediator().isSeekable());
        
        // Set song text.
        titleLabel.setText(name);
        titleLabel.setToolTipText(name);
        titleLabel.start();

        if (!innerPanel.isVisible()) {
            innerPanel.setVisible(true);
        }
    }
    
    /**
     * Hanldes state change in the player to the specified state.
     */
    @Override
    public void stateChanged(PlayerState playerState) {
        if ((playerState == PlayerState.OPENED) || (playerState == PlayerState.SEEKED)) {
            updateVolume();
        } else if (playerState == PlayerState.GAIN) {
            // Exit on volumn change.
            return;
        }
        
        // Update buttons based on player status.
        PlayerState status = getPlayerMediator().getStatus();
        if ((status == PlayerState.PLAYING) || (status == PlayerState.SEEKING_PLAY)) {
            playButton.setVisible(false);
            pauseButton.setVisible(true);
            titleLabel.start();
            
        } else if ((status == PlayerState.PAUSED) || (status == PlayerState.SEEKING_PAUSED)) {
            playButton.setVisible(true);
            pauseButton.setVisible(false);
            titleLabel.stop();
            
        } else {
            playButton.setVisible(true);
            pauseButton.setVisible(false);
            titleLabel.setText(I18n.tr("Stopped"));
            titleLabel.setToolTipText(I18n.tr("Stopped"));
            titleLabel.stop();
        }            
    }
    
    /**
     * Updates the volume in the player.
     */
    private void updateVolume() {
        getPlayerMediator().setVolume(((double) volumeSlider.getValue()) / volumeSlider.getMaximum());
    }
    
    /**
     * Listener to handle user actions on various player buttons.
     */
    private class ButtonListener implements ActionListener {
        
        private long menuInvizTime = -1;
        
        public void clearMenu() {
            menuInvizTime = System.currentTimeMillis();
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == PLAY){
                getPlayerMediator().resume();
                
            } else if (e.getActionCommand() == PAUSE){
                getPlayerMediator().pause();
                
            } else if (e.getActionCommand() == FORWARD) {
                getPlayerMediator().nextSong();
                
            } else if (e.getActionCommand() == BACK) {
                getPlayerMediator().prevSong();
                    
            } else if (e.getActionCommand() == VOLUME) {
                if (System.currentTimeMillis() - menuInvizTime > 250f) {
                    volumeControlPopup.show(volumeButton, 0, 14);
                }
                
            } else if (e.getActionCommand() == SHUFFLE) {
                // Toggle shuffle mode.
                getPlayerMediator().setShuffle(!getPlayerMediator().isShuffle());
                // Update button state.
                shuffleButton.setSelected(getPlayerMediator().isShuffle());
                shuffleButton.setPressedIcon(getPlayerMediator().isShuffle() ?
                        shuffleIconActive : shuffleIconPressed);
            }
        }
    }
  
    /**
     * Listener to handle change to progress bar to skip to a new position in 
     * the song.
     */
    private class AudioProgressListener implements ChangeListener {
        
        private boolean waiting = false; 
       
        @Override
        public void stateChanged(ChangeEvent e) {
            
            if (progressSlider.getMaximum() != 0 && progressSlider.getValueIsAdjusting()) {
                if (!waiting) {
                    waiting = true;
                }
                
            } else if (waiting) {
                waiting = false;
                double percent = (double)progressSlider.getValue() / (double)progressSlider.getMaximum();
                getPlayerMediator().skip(percent);
                progressSlider.setValue((int)(percent * progressSlider.getMaximum()));
            }
        }
    }

    /**
     * Listener to update volume when volume slider is adjusted.
     */
    private class VolumeController implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            updateVolume();
        }
    }
}
