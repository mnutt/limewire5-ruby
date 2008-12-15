package com.limegroup.gnutella.gui.actions;

import java.awt.Canvas;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import org.limewire.util.URIUtils;


import com.limegroup.gnutella.ConnectionManager;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.connection.ConnectionLifecycleEvent;
import com.limegroup.gnutella.connection.ConnectionLifecycleListener;
import com.limegroup.gnutella.gui.AutoCompleteTextField;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.ClearableAutoCompleteTextField;
import com.limegroup.gnutella.gui.FileChooserHandler;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GUIUtils;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.IconManager;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.gui.download.TorrentFileFilter;
import com.limegroup.gnutella.gui.search.MagnetClipboardListener;

public class FileMenuActions {
    
    static final int SPACE = 6;

    public static class ConnectAction extends AbstractAction {

        private final ConnectionManager manager;
        
        public ConnectAction(ConnectionManager manager) {
            super(I18n.tr("C&onnect"));
            putValue(Action.LONG_DESCRIPTION, I18n.tr("Connect to the Network"));
            this.manager = manager;
            setEnabled(!manager.isConnected() && !manager.isConnecting());
            // TODO fberger listener leaking
            manager.addEventListener(new ConnectionStateHandler());
        }
        
        public ConnectAction() {
            this(GuiCoreMediator.getConnectionManager());
        }

        public void actionPerformed(ActionEvent e) {
            GUIMediator.instance().connect();
        }

        private class ConnectionStateHandler implements ConnectionLifecycleListener {

            public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
                setEnabledLater(!manager.isConnected() && !manager.isConnecting());
            }
        }
    }
    
    public static class DisconnectAction extends AbstractAction {

        private final ConnectionManager manager;
        
        public DisconnectAction(ConnectionManager manager) {
            super(I18n.tr("D&isconnect"));
            putValue(Action.LONG_DESCRIPTION, I18n.tr("Disconnect from the Network"));
            this.manager = manager;
            setEnabled(manager.isConnected() || manager.isConnecting());
            // TODO fberger listener leaking
            manager.addEventListener(new ConnectionStateHandler());
        }
        
        public DisconnectAction() {
            this(GuiCoreMediator.getConnectionManager());
        }
        
        public void actionPerformed(ActionEvent e) {
            GUIMediator.instance().disconnect();
        }
        
        private class ConnectionStateHandler implements ConnectionLifecycleListener {

            public void handleConnectionLifecycleEvent(ConnectionLifecycleEvent evt) {
                setEnabled(manager.isConnected() || manager.isConnecting());
            }
            
        }
    }

    /** Shows the File, Open Magnet or Torrent dialog box to let the user enter a magnet or torrent. */
    public static class OpenMagnetTorrentAction extends AbstractAction {
        
        private JDialog dialog = null;
        private AutoCompleteTextField PATH_FIELD = new ClearableAutoCompleteTextField(34);

        public OpenMagnetTorrentAction() {
            super(I18n.tr("O&pen Magnet or Torrent"));
            putValue(Action.LONG_DESCRIPTION, I18n.tr("Opens a magnet link or torrent file"));
        }

        public void actionPerformed(ActionEvent e) {
            if (dialog == null)
                createDialog();

            // clear input before dialog is shown
            PATH_FIELD.setText("");
            
            // display modal dialog centered
            dialog.pack();
            GUIUtils.centerOnScreen(dialog);
            dialog.setVisible(true);
        }

        private class OpenDialogWindowAdapter extends WindowAdapter {
            @Override
            public void windowOpened(WindowEvent e) {
                PATH_FIELD.requestFocusInWindow();
            }
        }

        private void createDialog() {

            dialog = new JDialog(GUIMediator.getAppFrame(), I18n.tr("Open Magnet or Torrent"), true);
            dialog.addWindowListener(new OpenDialogWindowAdapter());
            JPanel panel = (JPanel)dialog.getContentPane();
            GUIUtils.addHideAction(panel);
            panel.setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            
            panel.setBorder(new EmptyBorder(2 * SPACE, SPACE, SPACE, SPACE));
            
            // download icon
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.gridwidth = 1;
            constraints.gridheight = 1;
            constraints.weightx = 0.0;
            constraints.weighty = 0.0;
            constraints.anchor = GridBagConstraints.CENTER;
            constraints.insets = new Insets(0, 0, 2 * SPACE, 0);
            panel.add(new JLabel(IconManager.instance().getIconForButton("SEARCH_DOWNLOAD")), constraints);
            
            // instructions label
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.gridheight = 1;
            constraints.weightx = 1.0;
            constraints.weighty = 0.0;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.insets = new Insets(0, SPACE, 2 * SPACE, 0);
            panel.add(new MultiLineLabel(I18n.tr(
                    "Type a magnet link, or the path or Web address of a torrent file, and LimeWire will download it for you."),
                    true), constraints);

            // open label
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            panel.add(new JLabel(I18n.tr("Open:")), constraints);
            
            // spacer between the open label and the text field
            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 1;
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.weightx = 1.0;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(0, SPACE, 0, 0);
            panel.add(PATH_FIELD, constraints);

            ButtonRow row = new ButtonRow(
                    new Action[] { new PasteAction(), new BrowseAction() },
                    ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);

            constraints = new GridBagConstraints();
            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.insets = new Insets(SPACE, SPACE, 0, 0);
            constraints.anchor = GridBagConstraints.WEST;
            panel.add(row, constraints);
            
            // add vertical spacer/spring
            constraints = new GridBagConstraints();
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 3;
            constraints.gridwidth = GridBagConstraints.REMAINDER;
            constraints.weighty = 1;
            panel.add(new Canvas(), constraints);

            row = new ButtonRow(
                    new Action[] { new OkAction(), new CancelAction() },
                    ButtonRow.X_AXIS, ButtonRow.LEFT_GLUE);

            constraints = new GridBagConstraints();
            constraints.gridx = 4;
            constraints.gridy = 4;
            constraints.insets = new Insets(2 * SPACE, 0, 0, 0);
            constraints.anchor = GridBagConstraints.EAST;
            panel.add(row, constraints);
            
            dialog.getRootPane().setDefaultButton(row.getButtonAtIndex(0));
        }
        
        private class PasteAction extends AbstractAction {

            public PasteAction() {
                super(I18n.tr("Paste"));
            }
            
            public void actionPerformed(ActionEvent a) {
                PATH_FIELD.paste();
            }
        }
        
        private class BrowseAction extends AbstractAction {

            public BrowseAction() {
                super(I18n.tr("Browse..."));
            }
            
            public void actionPerformed(ActionEvent a) {
                File file = FileChooserHandler.getInputFile(GUIMediator.getAppFrame(), TorrentFileFilter.INSTANCE);
                if (file != null)
                    PATH_FIELD.setText(file.getAbsolutePath());
            }
        }

        private class OkAction extends AbstractAction {

            public OkAction() {
                super(I18n.tr("OK"));
            }

            public void actionPerformed(ActionEvent a) {
                if (openMagnetOrTorrent(PATH_FIELD.getText())) {
                    dialog.setVisible(false);
                    dialog.dispose();
                } else {
                    GUIMediator.showError(I18n.tr("LimeWire cannot open this file or address. Make sure you typed it correctly, and then try again."));
                }
            }
        }
        
        private class CancelAction extends AbstractAction {
            
            public CancelAction() {
                super(I18n.tr("Cancel"));
            }
            
            public void actionPerformed(ActionEvent a) {
                GUIUtils.getDisposeAction().actionPerformed(a);
            }
        }
    }

    /**
     * Opens a magnet link, a Web address to a torrent file, or a path to a
     * torrent file on the disk.
     * 
     * Note that DNDUtils performs similar steps when the user drops a magnet
     * or torrent on the window.
     * 
     * @param userText The text of the path, link, or address the user entered
     * @return true if it was valid and we opened it
     */
    private static boolean openMagnetOrTorrent(String userText) {

        // See if it's a magnet link
        MagnetOptions[] magnets = MagnetOptions.parseMagnets(userText);
        if (magnets.length != 0) {
            MagnetClipboardListener.handleMagnets(magnets, false); // Open the magnet link
            return true;

        // Not a magnet
        } else {
            
            // See if it's a path to a file on the disk
            File file = new File(userText);
            if (isFileSystemPath(file)) {
                if(file.exists()) {
                    GUIMediator.instance().openTorrent(file); // Open the torrent file
                    return true;
                } else {
                    // Return false, which'll show a prompt about a bad addr.
                }
            // Not a file
            } else {

                // See if it's a Web address
                try {
                    URI uri = URIUtils.toURI(userText);
                    String scheme = uri.getScheme();
                    if (scheme != null && scheme.equalsIgnoreCase("http")) {
                        // TODO what about https or tls?
                        String authority = uri.getAuthority(); // Check the authority
                        if (authority != null && authority.length() != 0 && authority.indexOf(' ') == -1) {
                            uri = URIUtils.toURI(uri.toString()); // Extra checks, what happens in HTTPClient

                            // The text is a valid Web address
                            GUIMediator.instance().openTorrentURI(uri); // Open the torrent address
                            return true;
                        }
                    }
                } catch (URISyntaxException e) {
                    // This'll end up returning false, which shows a prompt about a bad addr.
                }
            }
        }

        // Invalid text, nothing opened
        return false;
    }

    private static boolean isFileSystemPath(File file) {
        return file.isAbsolute();
    }

    /**
     * Exits the application.
     */
    public static class ExitAction extends AbstractAction {

        public ExitAction() {
            super(I18n.tr("&Close"));
            putValue(Action.LONG_DESCRIPTION, I18n.tr("Close the Program"));
        }
        
        public void actionPerformed(ActionEvent e) {
            GUIMediator.close(false);
        }
    }
}
