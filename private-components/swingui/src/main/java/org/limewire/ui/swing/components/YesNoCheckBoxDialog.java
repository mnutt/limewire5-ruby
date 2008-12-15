package org.limewire.ui.swing.components;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.util.I18n;

public class YesNoCheckBoxDialog extends LimeJDialog {
    public static String YES_COMMAND = "YES";
    public static String NO_COMMAND = "NO";

    private JButton yesButton = null;
    private JButton noButton = null;
    private JCheckBox checkBox;

    public YesNoCheckBoxDialog(String message, String checkBoxMessage, boolean checked) {
        super();
        setModalityType(ModalityType.APPLICATION_MODAL);
        JPanel panel = new JPanel();
        MultiLineLabel messageLabel = new MultiLineLabel(message, 350);
        

        checkBox = new JCheckBox(checkBoxMessage);
        checkBox.setSelected(checked);

        yesButton = new JButton(I18n.tr("Yes"));
        yesButton.setActionCommand(YES_COMMAND);
        yesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                YesNoCheckBoxDialog.this.dispose();
            }
        });
        noButton = new JButton(I18n.tr("No"));
        noButton.setActionCommand(NO_COMMAND);
        noButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                YesNoCheckBoxDialog.this.dispose();
            }
        });
        panel.setLayout(new MigLayout("", "", ""));
        panel.add(messageLabel, "wrap");
        panel.add(checkBox, "wrap");
        panel.add(yesButton, "alignx right");
        panel.add(noButton, "alignx right, wrap");

        setContentPane(panel);
        pack();

    }

    public void addActionListener(ActionListener actionListener) {
        yesButton.addActionListener(actionListener);
        noButton.addActionListener(actionListener);
    }

    public void removeActionListener(ActionListener actionListener) {
        yesButton.removeActionListener(actionListener);
        noButton.removeActionListener(actionListener);
    }

    public synchronized boolean isCheckBoxSelected() {
        return checkBox.isSelected();
    }
}
