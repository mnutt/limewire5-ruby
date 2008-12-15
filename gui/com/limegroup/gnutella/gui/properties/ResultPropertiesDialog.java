package com.limegroup.gnutella.gui.properties;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.actions.AbstractAction;

/** The search result Properties dialog box. */
public class ResultPropertiesDialog {
    
    /** Margin space used in dialog layout. */
    public final int space = 6;

    /** The dialog box object. */
    private final JDialog dialog;

    /** Makes a new result properties dialog box from the given information. */
    public ResultPropertiesDialog(ResultProperties properties) {
        
        // Properties panel
        PropertiesPanel panel = new PropertiesPanel();
        if (properties.getFileProperties() != null)
            panel.add(properties.getFileProperties());
        if (properties.getMetaProperties() != null)
            panel.add(properties.getMetaProperties());
        
        // Properties tab
        JPanel tab = new JPanel();
        tab.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(space, space, space, space);
        tab.add(panel, c);
        
        // Tabs
        JTabbedPane tabs = new JTabbedPane();
        tabs.setPreferredSize(new Dimension(360, 420));
        tabs.addTab(I18n.tr("General"), tab);

        // Dialog top
        JPanel dialogPanel = new JPanel();
        dialogPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(space, space, space, space);
        dialogPanel.add(tabs, c);
        
        // Dialog bottom
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.anchor = GridBagConstraints.SOUTHEAST;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(space / 2, space, space, space);
        dialogPanel.add(new JButton(new CloseAction()), c);

        // Dialog
        dialog = new JDialog(GUIMediator.getAppFrame());
        dialog.setModal(false); // Let the user open more than one, and use the window behind
        dialog.setResizable(true);
        dialog.setTitle(ResultProperties.title());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setContentPane(dialogPanel);
        dialog.pack();
    }
    
    /** Gets the dialog box object. */
    public JDialog getDialog() {
        return dialog;
    }

    /** The Close button at the bottom of the dialog box. */
    private class CloseAction extends AbstractAction {

        public CloseAction() {
            super(I18n.tr("Close"));
        }

        public void actionPerformed(ActionEvent a) {
            dialog.setVisible(false);
            dialog.dispose();
        }
    }
}
