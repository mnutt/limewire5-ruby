package org.limewire.ui.swing.options;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class ManageFileExtensionsOptionPanel extends OptionPanel {

    private final JPanel contentPanel;
    
    private final JButton okButton;
    private final JButton cancelButton;
    
    private final Provider<FileTypeOptionPanelManager> managerProvider;
    private FileTypeOptionPanelManager manager = null;
    
    @Inject
    public ManageFileExtensionsOptionPanel(Provider<FileTypeOptionPanelManager> managerProvider) {
        this.managerProvider = managerProvider;
        
        this.setLayout(new MigLayout("gapy 10, nogrid, fill"));
        
        ActionListener disposeListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComponent comp = (JComponent)e.getSource();
                Container dialog = comp.getRootPane().getParent();
                if(dialog != null && dialog instanceof JDialog) {
                    ((JDialog)dialog).dispose();
                }
                disposeOldPanelIfExists();
            }
        };
        
        okButton = new JButton(new OKDialogAction());
        
        cancelButton = new JButton(CancelDialogAction.NAME);
        cancelButton.setToolTipText(CancelDialogAction.SHORT_DESCRIPTION);
        cancelButton.addActionListener(disposeListener);
        
        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        
        add(new JLabel(I18n.tr("Select which file extensions belong in each category")), "span, wrap");
        add(contentPanel, "span, wrap");
        add(okButton, "tag ok");
        add(cancelButton, "tag cancel");
    }
    
    private void disposeOldPanelIfExists() {
        if (manager != null) {
            manager.dispose();
            manager = null;
            contentPanel.removeAll();
            contentPanel.validate();
            contentPanel.repaint();
        }
    }
    
    public void reset() {
        disposeOldPanelIfExists();
    }
    
    @Override
    public void initOptions() {
        
        if (manager == null) {
            manager = managerProvider.get();
        
            manager.initOptions();
            manager.buildUI();
        
            if (manager.getContainer() != null)
                contentPanel.add(manager.getContainer(), BorderLayout.CENTER);
        }
    }

    @Override
    public boolean applyOptions() {
        if (manager != null) {
            boolean result = manager.applyOptions();
            disposeOldPanelIfExists();
            return result;
        }
        else {
            return false;
        }
    }

    @Override
    boolean hasChanged() {
        if (manager != null) {
            return this.manager.hasChanged();
        }
        else {
            return false;
        }
    }
}
