package org.limewire.ui.swing.options.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JDialog;

public class CancelOptionAction implements ActionListener {

    private JDialog optionDialog;
    
    public CancelOptionAction(JDialog optionDialog) {
        this.optionDialog = optionDialog;
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        optionDialog.dispose();
    }
}
