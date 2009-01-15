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
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXButton;
import org.limewire.core.api.Category;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.player.api.AudioSource;
import org.limewire.player.api.PlayerState;
import org.limewire.ui.swing.components.MarqueeButton;
import org.limewire.ui.swing.library.nav.LibraryNavigator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

public class MiniPlayerPanel extends JPanel {
  
    @Resource
    private Icon pauseIcon;
    @Resource
    private Icon pauseIconPressed;
    @Resource
    private Icon pauseIconRollover;
    @Resource
    private Icon playIcon;
    @Resource
    private Icon playIconPressed;
    @Resource
    private Icon playIconRollover;
    @Resource 
    private Color foregroundColor;
    @Resource
    private Font font;

    private JButton playPauseButton;

    private MarqueeButton statusButton;
    
    private final AudioPlayer player;
    private final LibraryNavigator libraryNavigator;

    public MiniPlayerPanel(AudioPlayer player, LibraryNavigator libraryNavigator) {
        GuiUtils.assignResources(this);
        
        this.player = player;
        this.libraryNavigator = libraryNavigator;
        
        setLayout(new MigLayout("insets 0", "4[][]", "0[]0"));
        setOpaque(false);

        playPauseButton = new JXButton();
        playPauseButton.setMargin(new Insets(0, 0, 0, 0));
        playPauseButton.setBorderPainted(false);
        playPauseButton.setContentAreaFilled(false);
        playPauseButton.setFocusPainted(false);
        playPauseButton.setRolloverEnabled(true);
        playPauseButton.setIcon(playIcon);
        Dimension playPauseDimensions = new Dimension(playIcon.getIconWidth(), playIcon.getIconHeight());
        playPauseButton.setMaximumSize(playPauseDimensions);
        playPauseButton.setPreferredSize(playPauseDimensions);
        playPauseButton.setRolloverIcon(playIconRollover);
        playPauseButton.setPressedIcon(playIconPressed);
        playPauseButton.setHideActionText(true);
        playPauseButton.addActionListener(new PlayListener());

        statusButton = new MarqueeButton(I18n.tr("Nothing selected"), 16);
        Dimension statusButtonDimensions = new Dimension(Integer.MAX_VALUE, playIcon.getIconHeight());
        statusButton.setMaximumSize(statusButtonDimensions);
        statusButton.setFont(font);
        statusButton.setForeground(foregroundColor);    
        statusButton.addActionListener(new ShowPlayerListener());

        add(playPauseButton, "gapbottom 0, gaptop 0");
        add(statusButton, "gapbottom 0, gaptop 0");
     
        setMaximumSize(getPreferredSize());
        player.addAudioPlayerListener(new PlayerListener());
    }

    private class ShowPlayerListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            AudioSource currentSource = player.getCurrentSong();
            
            if (currentSource != null) { 
                libraryNavigator.selectInLibrary(currentSource.getFile(), Category.AUDIO);
            }
        }
    }
    
    private class PlayListener implements ActionListener {  
        @Override
        public void actionPerformed(ActionEvent e) {       
            setPlaying(!isPlaying());                        
        }

    }
    
    private boolean isPlaying(){
        return player.getStatus() == PlayerState.PLAYING || player.getStatus() == PlayerState.SEEKING_PLAY ;
    }
    
    private void setPlaying(boolean playing){
        if (playing){
            player.unpause();
        } else {
            player.pause();
        }
    }
    
    private class PlayerListener implements AudioPlayerListener{
        //duration in seconds
        private int durationSecs;
        private int byteLength;
        private int currentSecs;
        private String title;
        private String artist;

        @Override
        public void progressChange(int bytesread) {
            if (byteLength != 0) {
               currentSecs = durationSecs * bytesread / byteLength;
               statusButton.setToolTipText(title + " - " + artist + 
                       " (" + CommonUtils.seconds2time(currentSecs) + 
                       "/" + CommonUtils.seconds2time(durationSecs) + ")");
            }
            if (statusButton.getToolTip().isVisible()) {
                statusButton.getToolTip().setTipText(statusButton.getToolTipText());
                statusButton.getToolTip().repaint();
            }
        }

        @Override
        public void songOpened(Map<String, Object> properties) {
            //Show MiniPlayer when song is opened
            setVisible(true);
            title = (String) properties.get("title");
            artist = (String) properties.get("author");
            
            if (title != null && artist != null) {
                statusButton.setText(title + " - " + artist);
            } 
            else {
                String text = null;
                if (player.getCurrentSong() != null) {
                    File file = player.getCurrentSong().getFile();
                    if (file !=null) {
                        text = file.getName();
                    }
                }
                if (text == null) {
                    text = I18n.tr("Unknown");
                }
                statusButton.setText(text);
            }
            
            statusButton.start();
            
            Object duration = properties.get("duration");
            if (duration != null) {
                durationSecs = (int)(((Long)duration).longValue()/1000/1000);
            } 
            else {
                durationSecs = 0;
            }
            
            if (properties.get("audio.length.bytes") != null) {
                byteLength = (Integer)properties.get("audio.length.bytes");
            } 
            else {
                byteLength = 0;
            }
        }

        @Override
        public void stateChange(AudioPlayerEvent event) {
            if (player.getStatus() == PlayerState.PLAYING || player.getStatus() == PlayerState.SEEKING_PLAY){
                playPauseButton.setIcon(pauseIcon);
                playPauseButton.setRolloverIcon(pauseIconRollover);
                playPauseButton.setPressedIcon(pauseIconPressed);
            } else if (player.getStatus() == PlayerState.STOPPED) {
                setVisible(false);
            }
            else {
                playPauseButton.setIcon(playIcon);
                playPauseButton.setRolloverIcon(playIconRollover);
                playPauseButton.setPressedIcon(playIconPressed);
            }
        }
        
    }
    
    
}
