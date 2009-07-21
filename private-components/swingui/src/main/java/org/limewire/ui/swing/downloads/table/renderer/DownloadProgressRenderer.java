package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.LineBorder;
import javax.swing.table.TableCellRenderer;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.jdesktop.swingx.JXPanel;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.LimeProgressBar;
import org.limewire.ui.swing.components.decorators.ProgressBarDecorator;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.CommonUtils;

import com.google.inject.Inject;

public class DownloadProgressRenderer extends JXPanel implements TableCellRenderer {
    @Resource private int progressBarWidth;
    @Resource private int progressBarHeight;
    /**the progress bar disappears when the column width is less than this value*/
    @Resource private int progressBarCutoffWidth;
    @Resource private Color progressBarBorder;

    private LimeProgressBar progressBar;
    private JLabel timeLabel;
    
    @Inject
    public DownloadProgressRenderer(ProgressBarDecorator progressBarDecorator){
        super(new MigLayout("insets 0, gap 0, novisualpadding, nogrid, aligny center"));
        GuiUtils.assignResources(this);
        
        progressBar = new LimeProgressBar(0, 100);
        progressBarDecorator.decoratePlain(progressBar);  
        progressBar.setBorder(new LineBorder(progressBarBorder));
        Dimension size = new Dimension(progressBarWidth, progressBarHeight);
        progressBar.setMaximumSize(size);
        progressBar.setPreferredSize(size);
        
        timeLabel = new JLabel();
        new DownloadRendererProperties().decorateComponent(timeLabel);
        
        add(progressBar);
        add(timeLabel);
    }
    

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
            boolean hasFocus, int row, int column) {
        if(value instanceof DownloadItem) {
            DownloadItem item = (DownloadItem)value;
            updateProgress(item.getState(), item.getPercentComplete(), table.getColumnModel().getColumn(column).getWidth());
            updateTime(item.getState(), item);
        } else {
            updateProgress(DownloadState.ERROR, 0, 0);
            timeLabel.setVisible(false);
        }
        return this;
    }
    
    private void updateProgress(DownloadState state, int percentComplete, int columnWidth) {
        progressBar.setValue(percentComplete);
        progressBar.setVisible(columnWidth > progressBarCutoffWidth && (state == DownloadState.DOWNLOADING || state == DownloadState.PAUSED));
        progressBar.setEnabled(state != DownloadState.PAUSED);
    }
    
    private void updateTime(DownloadState state, DownloadItem item){
        if (state != DownloadState.DOWNLOADING || item.getRemainingDownloadTime() > Long.MAX_VALUE - 1000) {
            timeLabel.setVisible(false);
        } else {
            timeLabel.setText(I18n.tr("{0} left", CommonUtils.seconds2time(item
                    .getRemainingDownloadTime())));
            timeLabel.setMinimumSize(timeLabel.getPreferredSize());
            timeLabel.setVisible(true);
        }
    }
}
