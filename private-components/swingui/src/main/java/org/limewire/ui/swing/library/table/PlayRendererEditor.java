package org.limewire.ui.swing.library.table;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JToggleButton;

import org.jdesktop.application.Resource;
import org.limewire.core.api.library.LocalFileItem;
import org.limewire.player.api.AudioPlayer;
import org.limewire.player.api.AudioPlayerEvent;
import org.limewire.player.api.AudioPlayerListener;
import org.limewire.ui.swing.components.Disposable;
import org.limewire.ui.swing.listener.ActionHandListener;
import org.limewire.ui.swing.player.PlayerUtils;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.NativeLaunchUtils;
import org.limewire.ui.swing.util.PropertyUtils;

import net.miginfocom.swing.MigLayout;

public class PlayRendererEditor extends TableRendererEditor implements AudioPlayerListener, Disposable {

    private JToggleButton playButton;
    private JLabel label;

    @Resource
    private Icon playIcon;

    @Resource
    private Icon pauseIcon;

    private File file;
    
    private LibraryTable table;
    private AudioPlayer player;

    public PlayRendererEditor(LibraryTable table, final AudioPlayer player) {
        GuiUtils.assignResources(this);
        
        this.table = table;
        this.player = player;
        
        setLayout(new MigLayout("insets 1, aligny 50%, hidemode 3"));
        
        playButton = new JToggleButton();
        playButton.setIcon(playIcon);
        playButton.setSelectedIcon(pauseIcon);
        playButton.setMargin(new Insets(0, 0, 0, 0));
        playButton.setBorderPainted(false);
        playButton.setContentAreaFilled(false);
        playButton.setFocusPainted(false);
        playButton.setRolloverEnabled(true);
        playButton.setHideActionText(true);
        playButton.setBorder(BorderFactory.createEmptyBorder());
        playButton.setOpaque(false);
        playButton.addMouseListener(new ActionHandListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                if (file == null) {
                    //do nothing
                } else if (PlayerUtils.isPlayableFile(file)){
                    if(player.isPlaying(file)){
                        player.pause();
                    } else if(player.isPaused(file)){
                        player.unpause();
                    } else {                        
                        player.loadSong(file);
                        player.playSong();
                    }
                } else {                
                    NativeLaunchUtils.safeLaunchFile(file);
                }
                cancelCellEditing();
            }
        }));
        
        label = new JLabel();
        
        add(playButton);
//        add(label, "growx, wmin 10");
        
        player.addAudioPlayerListener(this);
    }

    @Override
    public void dispose() {
        this.player.removeAudioPlayerListener(this);    
    }

    @Override
    public Component doTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        // Get file item associated with table row.  The value argument may not
        // be a LocalFileItem, as reported in JIRA item LWC-2208.
        Object fileItem = this.table.getLibraryTableModel().getFileItem(row);
        if (fileItem != null) {
            update(fileItem);
        }
        return this;
    }

    @Override
    public Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        // Get file item associated with table row.  The value argument may not
        // be a LocalFileItem, as reported in JIRA item LWC-2208.
        Object fileItem = this.table.getLibraryTableModel().getFileItem(row);
        if (fileItem != null) {
            file = update(fileItem);
        }
        return this;
    }
    
    private File update(Object value){
        if(value instanceof LocalFileItem){
            LocalFileItem item = (LocalFileItem)value;
               
            label.setText(PropertyUtils.getTitle(item));
            
            if(item.isIncomplete()){
                playButton.setVisible(false);
                return null;
            } else {
                File file = ((LocalFileItem) value).getFile();
                playButton.setVisible(player.isPaused(file) || player.isPlaying(file));           
                playButton.setSelected(player.isPlaying(file));

                return file;
            }      
        } else {
            throw new IllegalStateException(value.getClass() + " must be LocalFileItem");
        }
    }

    @Override
    public void progressChange(int bytesread) {
        // do nothing        
    }

    @Override
    public void songOpened(Map<String, Object> properties) {
        if(table.isVisible()){
            table.repaint();
        }       
    }

    @Override
    public void stateChange(AudioPlayerEvent event) {
       if(table.isVisible()){
           table.repaint();
       }
    }
}
