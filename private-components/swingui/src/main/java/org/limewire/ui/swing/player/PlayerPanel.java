package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.Painter;
import org.jdesktop.swingx.painter.RectanglePainter;
import org.limewire.core.api.Category;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.components.LimeSliderBarFactory;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class PlayerPanel extends JXPanel {

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
          
    @Resource private Font font;
    
    private final JXPanel innerPanel;
    private final JButton backButton;
    private final JButton playButton;
    private final JButton pauseButton;
    private final JButton forwardButton;
    private final JSlider progressSlider;
    private final JPanel statusPanel;
    private final JButton volumeButton;
    
    private final JPopupMenu volumeControlPopup;
    private final JSlider volumeSlider; 
    
    private final JLabel titleLabel;
    
    private final AudioPlayer player;
    private final LibraryNavigator libraryNavigator;
    
    /**
     * length of the current audio in seconds
     */
    private int durationSecs;

    /**
     * length of the current audio in bytes
     */
    private int byteLength;

    /**
     * Pointer to the current songs file
     */
    private File file = null;
    
    private static final String BACK = "BACK";
    private static final String PLAY = "PLAY";
    private static final String PAUSE = "PAUSE";
    private static final String FORWARD = "FORWARD";
    private static final String VOLUME = "VOLUME";

    @Inject
    public PlayerPanel(AudioPlayer player, LibraryNavigator libraryNavigator, LimeSliderBarFactory sliderBarFactory) {
        this.player = player;
        this.libraryNavigator = libraryNavigator;

        GuiUtils.assignResources(this);
        
        setLayout(new MigLayout("insets 0, filly, alignx center"));
        setOpaque(false);
        
        final ButtonListener playerListener = new ButtonListener();

        backButton = new IconButton(backIcon, backIconRollover, backIconPressed);
        backButton.addActionListener(playerListener);
        backButton.setActionCommand(BACK);
        backButton.setDisabledIcon(backIconDisabled);
        
        playButton = new IconButton(playIcon, playIconRollover, playIconPressed);
        playButton.addActionListener(playerListener);
        playButton.setActionCommand(PLAY);
        playButton.setDisabledIcon(playIconDisabled);

        pauseButton = new IconButton(pauseIcon, pauseIconRollover, pauseIconPressed);
        pauseButton.addActionListener(playerListener);
        pauseButton.setActionCommand(PAUSE);
        pauseButton.setVisible(false);
        
        pauseButton.setMinimumSize(playButton.getMinimumSize());
        pauseButton.setPreferredSize(playButton.getPreferredSize());

        forwardButton = new IconButton(forwardIcon, forwardIconRollover, forwardIconPressed);
        forwardButton.addActionListener(playerListener);
        forwardButton.setActionCommand(FORWARD);
        forwardButton.setDisabledIcon(forwardIconDisabled);
        
        volumeButton = new IconButton(volumeIcon, volumeIconRollover, volumeIconPressed);
        volumeButton.addActionListener(playerListener);
        volumeButton.setActionCommand(VOLUME);
        volumeButton.setDisabledIcon(volumeIconDisabled);
        
        volumeControlPopup = new JPopupMenu();
        volumeSlider = new JSlider(0,100);
        initVolumeControl();
        
        progressSlider = sliderBarFactory.create();
        progressSlider.addChangeListener(new AudioProgressListener());
        progressSlider.setMaximum(0);
        progressSlider.setMaximumSize(new Dimension(206, 6));
        progressSlider.setMinimumSize(new Dimension(206, 6));
        progressSlider.setPreferredSize(new Dimension(206, 6));
        progressSlider.setSize(new Dimension(206, 4));
        
        statusPanel = new JPanel(new MigLayout());
        
        titleLabel = new JLabel("Stopped");
        titleLabel.setFont(font);
        titleLabel.setMaximumSize(new Dimension(206, (int)titleLabel.getMaximumSize().getHeight()));
        titleLabel.setMinimumSize(new Dimension(206, (int)titleLabel.getMinimumSize().getHeight()));
        titleLabel.setPreferredSize(new Dimension(206, (int)titleLabel.getPreferredSize().getHeight()));
        titleLabel.setSize(new Dimension(206, (int)titleLabel.getSize().getHeight()));
        titleLabel.setVisible(false);
        
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
        
        innerPanel.setVisible(false);
        add(innerPanel, "gaptop 2, gapbottom 2");
                
        EventAnnotationProcessor.subscribe(this);

        VolumeController volumeController = new VolumeController();
        volumeSlider.addChangeListener(volumeController);
        player.addAudioPlayerListener(new PlayerListener(volumeController));      
        
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
    
    }
    
    private void initVolumeControl() {
        volumeSlider.setOrientation(JSlider.VERTICAL);
        volumeSlider.setMinimumSize(new Dimension((int)volumeSlider.getMinimumSize().getWidth(), 75));
        volumeSlider.setMaximumSize(new Dimension((int)volumeSlider.getMaximumSize().getWidth(), 75));
        volumeSlider.setPreferredSize(new Dimension((int)volumeSlider.getPreferredSize().getWidth(), 75));
        volumeSlider.setSize(new Dimension((int)volumeSlider.getSize().getWidth(), 75));
        
        volumeControlPopup.setMinimumSize(new Dimension(20, 80));
        volumeControlPopup.setMaximumSize(new Dimension(20, 80));
        volumeControlPopup.setPreferredSize(new Dimension(20, 80));
        volumeControlPopup.setSize(new Dimension(20, 80));
        
        volumeControlPopup.add(volumeSlider);
        

    }

    private void previousSong() {
        if (file != null) {
            player.stop();
            file = libraryNavigator.getPreviousInLibrary(file, Category.AUDIO);
            if (file != null) {
                player.loadSong(file);
                player.playSong();
            }
        }
    }
    
    private void nextSong() {
        if (file != null) {
            player.stop();
            file = libraryNavigator.getNextInLibrary(file, Category.AUDIO);
            if (file != null) {
                player.loadSong(file);
                player.playSong();
            }
        }
    }
    
    private class ButtonListener implements ActionListener {
        
        private long menuInvizTime = -1;
        
        public void clearMenu() {
            menuInvizTime = System.currentTimeMillis();
        }
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand() == PLAY){
                player.unpause();
            } else if (e.getActionCommand() == PAUSE){
                player.pause();
            } else if (e.getActionCommand() == FORWARD) {
                nextSong();
            } else if (e.getActionCommand() == BACK) {
                if (progressSlider.getValue() < 3) {
                    previousSong();
                }
                else {
                    player.stop();
                    player.loadSong(file);
                    player.playSong();
                }
            } else if (e.getActionCommand() == VOLUME) {
                if (System.currentTimeMillis() - menuInvizTime > 250f) {
                    volumeControlPopup.show(volumeButton, 0, 14);
                }
            }
        }
    }
  
    private class AudioProgressListener implements ChangeListener {
        
        private boolean waiting = false; 
        private boolean wasPlaying = false; 
        
        @Override
        public void stateChanged(ChangeEvent e) {
            
            if (progressSlider.getMaximum() != 0 && progressSlider.getValueIsAdjusting()) {
                if (!waiting) {
                    waiting = true;
//                    System.out.println("waiting to seek");
                    wasPlaying = player.getStatus() == PlayerState.PLAYING;
                    if (wasPlaying) {
//                        System.out.println("pausing");
                        player.pause();
                    }
                }
            } 
            else if (waiting) {
//                System.out.println("seeking");                
                int position = byteLength * progressSlider.getValue() / progressSlider.getMaximum();
//                System.out.println("seeking to " + position);
                player.seekLocation(position);                
                if (wasPlaying) {                
//                    System.out.println("unpausing");
                    player.unpause();
                }                
                waiting = false;
            }
        }
    }

    
    private class VolumeController implements ChangeListener {
        
        private int lastVolume = -1;  
        
        @Override
        public void stateChanged(ChangeEvent e) {
            lastVolume = volumeSlider.getValue();
            player.setVolume((double)lastVolume / 100);
        }
        
        public void resetVolume() {
            if (lastVolume != -1) {
                player.setVolume((double)lastVolume / 100);
            }
        }
    }
    
    
    private class PlayerListener implements AudioPlayerListener {
       
        private final VolumeController volumeController;
        
        public PlayerListener(VolumeController volumeController) {
            this.volumeController = volumeController;
        }
        
        @Override
        public void progressChange(int bytesread) {
            if (byteLength != 0 && !progressSlider.getValueIsAdjusting()) {
                progressSlider.setValue(durationSecs * bytesread / byteLength);
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
           
           if (player.getCurrentSong() != null) {
               file = player.getCurrentSong().getFile();
           }
            
           String songText = null;
           
           if (properties.get("author") == null || properties.get("title") == null) {
               songText = I18n.tr("Unknown");
           } 
           else {
               songText = properties.get("author") + " - " + properties.get("title");
           }
            
           volumeController.resetVolume();
           titleLabel.setText(songText);
        
           Object duration = properties.get("duration");
           if (duration != null) {
               durationSecs = (int)(((Long)duration).longValue()/1000/1000);
           } 
           else {
               durationSecs = 0;
           }
               
           progressSlider.setMaximum(durationSecs);
           
           if (properties.get("audio.length.bytes") != null) {
               byteLength = (Integer)properties.get("audio.length.bytes");
           } 
           else {
               byteLength = 0;
           }
                      
           titleLabel.setVisible(true);
           innerPanel.setVisible(true);
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            if (event.getState() == PlayerState.EOM) {
                nextSong();
            } 
            
            if (player.getStatus() == PlayerState.PLAYING || player.getStatus() == PlayerState.SEEKING_PLAY){
                playButton.setVisible(false);
                pauseButton.setVisible(true);
            } else {
                playButton.setVisible(true);
                pauseButton.setVisible(false);
            }            
        }
        
    }
    
    
    private Painter<JTextField> createStatusBackgroundPainter() {
        
        CompoundPainter<JTextField> compoundPainter = new CompoundPainter<JTextField>();
        
        RectanglePainter<JTextField> painter = new RectanglePainter<JTextField>();
        
        painter.setRounded(true);
        painter.setFillPaint(innerBackground);
        painter.setRoundWidth(this.arcWidth);
        painter.setRoundHeight(this.arcHeight);
        painter.setInsets(new Insets(2,2,2,2));
        painter.setBorderPaint(null);
        painter.setFillVertical(true);
        painter.setFillHorizontal(true);
        painter.setAntialiasing(true);
        painter.setCacheable(true);
        
        compoundPainter.setPainters(painter, new BorderPainter<JTextField>(this.arcWidth, this.arcHeight,
                this.innerBorder, this.bevelLeft, this.bevelTop1, this.bevelTop2, 
                this.bevelRight,  this.bevelBottom, AccentType.SHADOW));
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
    }
}
