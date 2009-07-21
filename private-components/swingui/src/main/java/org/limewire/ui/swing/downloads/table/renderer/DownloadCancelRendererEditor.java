package org.limewire.ui.swing.downloads.table.renderer;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.application.Resource;
import org.limewire.core.api.download.DownloadItem;
import org.limewire.core.api.download.DownloadState;
import org.limewire.ui.swing.components.IconButton;
import org.limewire.ui.swing.downloads.table.DownloadActionHandler;
import org.limewire.ui.swing.table.TableRendererEditor;
import org.limewire.ui.swing.util.GuiUtils;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

/**
 * Cancel and remove buttons for DownloadTable
 */
public class DownloadCancelRendererEditor extends TableRendererEditor {

    private final JButton cancelButton;
    private final JButton removeButton;
    private DownloadItem item;
   
    @Resource
    private Icon cancelIcon;
    @Resource
    private Icon cancelIconPressed;
    @Resource
    private Icon cancelIconRollover;

    
    /**
     * Create the DownloadCancelRendererEditor.
     */
    @Inject
    public DownloadCancelRendererEditor() {
        setLayout(new MigLayout("insets 0, gap 0, nogrid, novisualpadding, alignx center, aligny center"));
        
        GuiUtils.assignResources(this);

        cancelButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        cancelButton.setActionCommand(DownloadActionHandler.CANCEL_COMMAND);
        cancelButton.setToolTipText(I18n.tr("Cancel download"));

        removeButton = new IconButton(cancelIcon, cancelIconRollover, cancelIconPressed);
        removeButton.setActionCommand(DownloadActionHandler.REMOVE_COMMAND);
        removeButton.setToolTipText(I18n.tr("Remove download"));

        add(cancelButton, "hidemode 3");
        add(removeButton, "hidemode 3");
    }
    
    @Inject
    public void setActionHandler(final DownloadActionHandler actionHandler) {
        ActionListener listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionHandler.performAction(e.getActionCommand(), item);
                cancelCellEditing();
            }
        };
        cancelButton.addActionListener(listener);
        removeButton.addActionListener(listener);
    }

    public void updateButtons(DownloadState state) {
        cancelButton.setVisible(state != DownloadState.DONE);
        removeButton.setVisible(state == DownloadState.DONE);
    }
    
    @Override    
    protected Component doTableCellEditorComponent(JTable table, Object value, boolean isSelected,
            int row, int column) {
        if(value instanceof DownloadItem) {
            item = (DownloadItem)value;
            updateButtons(item.getState());
            return this;
        } else {
            return emptyPanel;
        }
    }
    @Override
    protected Component doTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        if(value instanceof DownloadItem) {
            updateButtons(((DownloadItem)value).getState());
            return this;
        } else {
            return emptyPanel;
        }
    }

}
