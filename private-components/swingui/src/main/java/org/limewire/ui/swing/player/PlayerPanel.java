package org.limewire.ui.swing.player;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.io.File;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicSliderUI;

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
import org.limewire.ui.swing.components.MarqueeButton;
import org.limewire.ui.swing.event.EventAnnotationProcessor;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.painter.BorderPainter;
import org.limewire.ui.swing.painter.BorderPainter.AccentType;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.ui.swing.util.ResizeUtils;

import com.google.inject.Inject;

/**
 * NOTE: This class should be treated as a singleton and the only proper media player (except the mini player)
 *        in the application otherwise there will be contention regarding play control and volume.
 * 
 */
public class PlayerPanel extends JXPanel {

    // TODO: Move somewhere better
    public static final String AUDIO_LENGTH_BYTES = "audio.length.bytes";
    public static final String AUDIO_TYPE = "audio.type";
    
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
    
    private final MarqueeButton titleLabel;
    
    private final AudioPlayer player;
    private final LibraryNavigator libraryNavigator;

    /**
     * Pointer to the last opened song's file
     */
    private File file = null;
    
    /**
     * Map of properties of the last opened song
     */
    private Map audioProperties = null;
    
    private static final String MP3 = "mp3";
    private static final String WAVE = "wave";
    
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
        initProgressControl();
        
        statusPanel = new JPanel(new MigLayout());
        
        titleLabel = new MarqueeButton("Stopped", 150);
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
        
        innerPanel.setVisible(false);
        add(innerPanel, "gaptop 2, gapbottom 2");
                
        EventAnnotationProcessor.subscribe(this);

        VolumeController volumeController = new VolumeController();
        volumeSlider.addChangeListener(volumeController);
        player.addAudioPlayerListener(new PlayerListener());      
        
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
                return;
            }
        }
        
        // If there is no next song because we are past the start of the list
        innerPanel.setVisible(false);
    }
    
    private void nextSong() {
        if (file != null) {
            player.stop();
            file = libraryNavigator.getNextInLibrary(file, Category.AUDIO);
            if (file != null) {
                player.loadSong(file);
                player.playSong();
                return;
            }
        }
        // If there is no next song because we are at the end of the list
        innerPanel.setVisible(false);
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
                if ((double)progressSlider.getValue() / (double)progressSlider.getMaximum() < .1) {
                    previousSong();
                }
                else {
                    player.stop();
                    
                    // If somehow the user is able to press the back button
                    //  when no song is loaded do not attempt to load an empty
                    //  deck
                    if (file != null) {
                        player.loadSong(file);
                        player.playSong();
                    }
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
       
        @Override
        public void stateChanged(ChangeEvent e) {
            
            if (progressSlider.getMaximum() != 0 && progressSlider.getValueIsAdjusting()) {
                if (!waiting) {
                    waiting = true;
                }
            } 
            else if (waiting) {
                waiting = false;
                double percent = (double)progressSlider.getValue() / (double)progressSlider.getMaximum();
                skip(percent);
                progressSlider.setValue((int)(percent * progressSlider.getMaximum()));
            }
        }
        
        /**
         * Skips the current song to a new position in the song. If the song's
         * length is unknown (streaming audio), then ignore the skip
         * 
         * @param percent of the song frames to skip from begining of file
         */
        public void skip(double percent) {
            
            // need to know something about the audio type to be able to skip
            if (audioProperties != null && audioProperties.containsKey(AUDIO_TYPE)) {
                String songType = (String) audioProperties.get(AUDIO_TYPE);
                
                // currently, only mp3 and wav files can be seeked upon
                if ( isSeekable(songType)
                        && audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                    final long skipBytes = Math.round((Integer) audioProperties
                            .get(AUDIO_LENGTH_BYTES)
                            * percent);

                    player.seekLocation(skipBytes);
                }
            }
        }
    }

    private class VolumeController implements ChangeListener {
        
        @Override
        public void stateChanged(ChangeEvent e) {
            setVolumeValue();
        }
    }
    
    private void setVolumeValue() {
        player.setVolume(((float) volumeSlider.getValue()) / volumeSlider.getMaximum());
    }
    
    private class PlayerListener implements AudioPlayerListener {
       
        @Override
        public void progressChange(int bytesread) {
            // if we know the length of the song, update the progress bar
            if (audioProperties.containsKey(AUDIO_LENGTH_BYTES)) {
                int byteslength = ((Integer) audioProperties.get(AUDIO_LENGTH_BYTES))
                        .intValue();

                float progressUpdate = bytesread * 1.0f / byteslength * 1.0f;

                if (!(progressSlider.getValueIsAdjusting() || player.getStatus() == PlayerState.SEEKING))
                    setProgressValue((int) (progressSlider.getMaximum() * progressUpdate));
            }
        }

        /**
         * Updates the current progress of the progress bar, on the Swing thread.
         */
        private void setProgressValue(final int update) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    progressSlider.setValue(update);
                }
            });
        }
        
        @Override
        public void songOpened(Map<String, Object> properties) {
           
           audioProperties = properties; 
           
           setVolumeValue();
           
           if (player.getCurrentSong() != null) {
               file = player.getCurrentSong().getFile();
           }
            
           String songText = null;
           
           if (isSeekable((String) properties.get(AUDIO_TYPE))) {
               progressSlider.setEnabled(true);
           } 
           else {
               progressSlider.setEnabled(false);
           }
           
           if (properties.get("author") == null || properties.get("title") == null) {
               if (file == null) {
                   songText = I18n.tr("Unknown");
               }
               else {
                   songText = file.getName();
               }
           } 
           else {
               songText = properties.get("author") + " - " + properties.get("title");
           }

           titleLabel.setText(songText);
           titleLabel.setToolTipText(songText);
           titleLabel.start();
        
           innerPanel.setVisible(true);
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            if (event.getState() == PlayerState.EOM) {
                nextSong();
            } 
            else if (event.getState() == PlayerState.OPENED || event.getState() == PlayerState.SEEKED) {
                setVolumeValue();
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
    
    private boolean isSeekable(String songType) {
        if( songType == null )
            return false;
        return songType.equalsIgnoreCase(MP3) || songType.equalsIgnoreCase(WAVE);
    }
    
    private Painter<JXPanel> createStatusBackgroundPainter() {
        
        CompoundPainter<JXPanel> compoundPainter = new CompoundPainter<JXPanel>();
        
        RectanglePainter<JXPanel> painter = new RectanglePainter<JXPanel>();
        
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
        
        compoundPainter.setPainters(painter, new BorderPainter<JXPanel>(this.arcWidth, this.arcHeight,
                this.innerBorder, this.bevelLeft, this.bevelTop1, this.bevelTop2, 
                this.bevelRight,  this.bevelBottom, AccentType.SHADOW));
        compoundPainter.setCacheable(true);
        
        return compoundPainter;
    }
}
