package org.limewire.ui.swing.options;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.options.actions.CancelDialogAction;
import org.limewire.ui.swing.options.actions.OKDialogAction;
import org.limewire.ui.swing.util.I18n;

import com.google.inject.Inject;

public class ManageFileExtensionsOptionPanel extends OptionPanel {

    private final JButton okButton;
    private final JButton cancelButton;
    
    private final FileTypeOptionPanelManager manager;

    @Inject
    public ManageFileExtensionsOptionPanel(FileTypeOptionPanelManager fileTypeOptionPanelManager) {
        
        this.manager = fileTypeOptionPanelManager;
        
        this.setLayout(new MigLayout("gapy 10, nogrid, fill"));
        
        Action okAction = new OKDialogAction(); 
        CancelDialogAction cancelAction = new CancelDialogAction();

        cancelAction.setOptionPanel(this);
            
        this.okButton = new JButton(okAction);
        this.cancelButton = new JButton(cancelAction);
    }
    
    @Override
    public void initOptions() {
        this.removeAll();
        
        this.add(new JLabel(I18n.tr("Select which file extensions belong in each category")), "span, wrap");
        
        this.manager.initOptions();
        this.manager.buildUI();
        
        if (this.manager.getContainer() != null)
            this.add(this.manager.getContainer(), "span, wrap");
        
        this.add(okButton, "tag ok");
        this.add(cancelButton, "tag cancel");
    }

    @Override
    public boolean applyOptions() {
        return this.manager.applyOptions();
    }

    @Override
    boolean hasChanged() {
        return this.manager.hasChanged();
    }
}
