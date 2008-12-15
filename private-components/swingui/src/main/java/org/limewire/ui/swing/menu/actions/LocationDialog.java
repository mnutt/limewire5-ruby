package org.limewire.ui.swing.menu.actions;

import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.util.I18n;
import org.limewire.util.URIUtils;

class LocationDialog extends LimeJDialog {
    private JButton openButton = null;

    private JTextField urlField = null;

    public LocationDialog() {
        this((Frame) null);
    }

    public LocationDialog(Frame owner) {
        super(owner);
        setModalityType(ModalityType.APPLICATION_MODAL);
        JPanel urlPanel = new JPanel();
        urlField = new JTextField(30);
        urlField.setText("");

        final JLabel errorLabel = new JLabel(I18n.tr("Invalid Link"));
        errorLabel.setForeground(Color.RED);
        errorLabel.setVisible(false);

        urlField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                URI uri = getURI();
                if (uri == null || uri.getScheme() == null) {
                    errorLabel.setVisible(true);
                    openButton.setEnabled(false);
                } else {
                    errorLabel.setVisible(false);
                    openButton.setEnabled(true);
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                if (openButton.isEnabled() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    openButton.doClick();
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {

            }
        });

        openButton = new JButton(I18n.tr("Open"));
        openButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                LocationDialog.this.dispose();
            }
        });
        openButton.setEnabled(false);

        JButton cancelButton = new JButton(I18n.tr("Cancel"));
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                LocationDialog.this.dispose();
            }
        });
        urlPanel.setLayout(new MigLayout("", "[]5[]5[]", "[]5[]"));
        urlPanel.add(new JLabel(I18n.tr("Enter a magnet or torrent link below:")), "span 3, wrap");
        urlPanel.add(urlField, "span 3");
        urlPanel.add(errorLabel, "wrap");
        urlPanel.add(openButton, "skip 2, alignx right");
        urlPanel.add(cancelButton, "alignx right");

        setContentPane(urlPanel);
        pack();
    }

    void addActionListener(ActionListener actionListener) {
        openButton.addActionListener(actionListener);
    }

    void removeActionListener(ActionListener actionListener) {
        openButton.removeActionListener(actionListener);
    }

    /**
     * Returns a uri typed into this dialogue. Will return null if the last uri
     * typed into the dialogue was invalid.
     */
    public synchronized URI getURI() {
        try {
            return URIUtils.toURI(urlField.getText());
        } catch (URISyntaxException e) {
            // eating exception and returning null for bad uris
            return null;
        }
    }
}
