package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Component;
import java.awt.Dimension;

import javax.swing.JLabel;
import javax.swing.JTable;
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

    private LimeProgressBar progressBar;
    private JLabel timeLabel;
    
    @Inject
    public DownloadProgressRenderer(ProgressBarDecorator progressBarDecorator){
        super(new MigLayout("insets 0 0 0 0, gap 0 0 0 0, novisualpadding, nogrid, aligny center"));
        GuiUtils.assignResources(this);
        
        progressBar = new LimeProgressBar(0, 100);
        progressBarDecorator.decoratePlain(progressBar);        
        Dimension size = new Dimension(progressBarWidth, progressBarHeight);
        progressBar.setMaximumSize(size);
        progressBar.setMinimumSize(size);
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
            updateProgress(item.getState(), item.getPercentComplete());
            updateTime(item.getState(), item);
        } else {
            updateProgress(DownloadState.ERROR, 0);
            timeLabel.setVisible(false);
        }
        return this;
    }
    
    private void updateProgress(DownloadState state, int percentComplete) {
        progressBar.setValue(percentComplete);
        progressBar.setVisible(state == DownloadState.DOWNLOADING);
    }
    
    private void updateTime(DownloadState state, DownloadItem item){
        if (state != DownloadState.DOWNLOADING || item.getRemainingDownloadTime() > Long.MAX_VALUE - 1000) {
            timeLabel.setVisible(false);
        } else {
            timeLabel.setText(I18n.tr("{0} left", CommonUtils.seconds2time(item
                    .getRemainingDownloadTime())));
            timeLabel.setVisible(true);
        }
    }
}
