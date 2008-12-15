package org.limewire.ui.support;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.limewire.core.api.support.LocalClientInfo;
import org.limewire.core.api.support.LocalClientInfoFactory;
import org.limewire.core.api.support.SessionInfo;
import org.limewire.core.impl.support.LocalClientInfoImpl;
import org.limewire.ui.swing.components.LimeJDialog;
import org.limewire.ui.swing.components.MultiLineLabel;

import com.google.inject.Inject;


/**
 * A bare-bones bug manager, for fatal errors.
 */
public final class FatalBugManager {
    
    @Inject private static volatile LocalClientInfoFactory localClientInfoFactory;    
    
    /**
     * The initial text displayed in the user comments area.
     */
    private static final String TEXT_AREA_DESCRIPTION_TEXT =  "Please add any comments you may have (e.g what caused the error). \nThank you and please use English.";
    
    private FatalBugManager() {}
    
    /**
     * Handles a fatal bug.
     */
    public static void handleFatalBug(Throwable bug) {
        if( bug instanceof ThreadDeath ) // must rethrow.
	        throw (ThreadDeath)bug;
	        
        bug.printStackTrace();
        
        final LocalClientInfo info;
        if(localClientInfoFactory != null) {
            info = localClientInfoFactory.createLocalClientInfo(bug, Thread.currentThread().getName(), null, true);
        } else {
            info = new LocalClientInfoImpl(bug, Thread.currentThread().getName(), null, true, new FatalSessionInfo());
        }
        
        if(!SwingUtilities.isEventDispatchThread()) {
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        reviewBug(info);                    
                    }
                });
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        } else {
            reviewBug(info);
        }
    }
    
    private static String warning() {
        String msg = "Ui" + "jt!j" + "t!Mjn" + "fXjs" + "f/!U" + "if!pg"+
                     "gjdjbm!xfc" + "tjuf!j" + "t!xx" + "x/mj" + "nfxjs" + "f/d" + "pn/";
        StringBuilder ret = new StringBuilder(msg.length());
        for(int i = 0; i < msg.length(); i++) {
            ret.append((char)(msg.charAt(i) - 1));
        }
        return ret.toString();
    }
    
    /**
     * Reviews the bug.
     */
    public static void reviewBug(final LocalClientInfo info) {
        final JDialog DIALOG = new LimeJDialog();
        DIALOG.setTitle("Fatal Error");
		final Dimension DIALOG_DIMENSION = new Dimension(100, 300);
		DIALOG.setSize(DIALOG_DIMENSION);

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        MultiLineLabel label = new MultiLineLabel(
            warning() + "\n\n" +
            "LimeWire has encountered a fatal internal error and will now exit. " +
            "This is generally caused by a corrupted installation.  Please try " + 
            "downloading and installing LimeWire again.\n\n" +
            "To aid with debugging, please click 'Send' to notify LimeWire about the problem. " +
            "If desired, you can click 'Review' to look at the information that will be sent. " + 
            "If the problem persists, please visit www.limewire.com and click the 'Support' " + 
            "link.\n\n" +
            "Thank You.", 300);        
		JPanel labelPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		labelPanel.add(Box.createHorizontalGlue());
		labelPanel.add(label);
		
		String textAreaDescription;
        textAreaDescription = TEXT_AREA_DESCRIPTION_TEXT;
        
        final JTextArea userCommentsTextArea = new JTextArea(textAreaDescription);        
        userCommentsTextArea.setLineWrap(true);
        userCommentsTextArea.setWrapStyleWord(true);                        
        
        // When the user clicks anywhere in the text field, it highlights the whole text
        // so that user could just type over it without having to delete it manually
        userCommentsTextArea.addFocusListener(new FocusAdapter() {
             @Override
            public void focusGained(FocusEvent e) {
                 if(userCommentsTextArea.getText().equals(TEXT_AREA_DESCRIPTION_TEXT))
                     userCommentsTextArea.selectAll();                 
             }
        });
        JScrollPane userCommentsScrollPane = new JScrollPane(userCommentsTextArea);
        userCommentsScrollPane.setBorder(BorderFactory.createEtchedBorder());
        userCommentsScrollPane.setPreferredSize( new Dimension(300, 80) ); 

        JPanel buttonPanel = new JPanel();
        JButton sendButton = new JButton("Send");
        sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    String userComments = userCommentsTextArea.getText(); 
                if(!userComments.equals(TEXT_AREA_DESCRIPTION_TEXT))
                    info.addUserComments(userComments);
				sendToServlet(info);
				DIALOG.dispose();
				System.exit(1);
			}
		});

        JButton reviewButton = new JButton("Review");
        reviewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    String userComments = userCommentsTextArea.getText(); 
                if(!userComments.equals(TEXT_AREA_DESCRIPTION_TEXT))
                    info.addUserComments(userComments);
                JTextArea textArea = new JTextArea(info.toBugReport());
                textArea.setColumns(50);
                textArea.setEditable(false);
                textArea.selectAll();
                textArea.copy();
                textArea.setCaretPosition(0);                
                JScrollPane scroller = new JScrollPane(textArea);
                scroller.setBorder(BorderFactory.createEtchedBorder());
                scroller.setPreferredSize( new Dimension(500, 200) );
                showMessage(DIALOG, scroller);
			}
		});

		JButton discardButton = new JButton("Discard");
		discardButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        DIALOG.dispose();
		        System.exit(1);
		    }
		});
        buttonPanel.add(sendButton);
        buttonPanel.add(reviewButton);
        buttonPanel.add(discardButton);

        mainPanel.add(labelPanel);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;            
        constraints.weighty = 1.0;
        constraints.insets = new Insets(20, 0, 6, 0);
        mainPanel.add(userCommentsScrollPane, constraints);        
        
        mainPanel.add(buttonPanel);

        DIALOG.getContentPane().add(mainPanel);
		DIALOG.pack();

		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		Dimension dialogSize = DIALOG.getSize();
		DIALOG.setLocation((screenSize.width - dialogSize.width)/2,
						   (screenSize.height - dialogSize.height)/2);
       
		// Get rid of all other windows.
        for(Window window : Window.getWindows()) {
            if(window == DIALOG) {
                continue;
            }
            window.setVisible(false);
            window.dispose();
        }
        
        DIALOG.setVisible(true);
        DIALOG.toFront();
    }
    
    /**
     * Sends a bug to the servlet & then exits.
     */
    private static void sendToServlet(LocalClientInfo info) {
        new ServletAccessor().getRemoteBugInfo(info);
    }
    
    /**
     * Shows a message.
     */
    private static void showMessage(Component parent, Component toDisplay) {
		JOptionPane.showMessageDialog(parent,
				  toDisplay,
				  "Fatal Error - Review",
				  JOptionPane.INFORMATION_MESSAGE);	
    }
    
    private static class FatalSessionInfo implements SessionInfo {

        public boolean acceptedIncomingConnection() {
            return false;
        }

        public boolean canReceiveSolicited() {
            return false;
        }

        public long getByteBufferCacheSize() {
            return 0;
        }

        public long getContentResponsesSize() {
            return 0;
        }

        public long getCreationCacheSize() {
            return 0;
        }

        public long getCurrentUptime() {
            return 0;
        }

        public long getDiskControllerByteCacheSize() {
            return 0;
        }

        public int getDiskControllerQueueSize() {
            return 0;
        }

        public long getDiskControllerVerifyingCacheSize() {
            return 0;
        }

        public int getNumIndividualDownloaders() {
            return 0;
        }

        public int getNumLeafToUltrapeerConnections() {
            return 0;
        }

        public int getNumOldConnections() {
            return 0;
        }

        public int getNumUltrapeerToLeafConnections() {
            return 0;
        }

        public int getNumUltrapeerToUltrapeerConnections() {
            return 0;
        }

        public int getNumWaitingDownloads() {
            return 0;
        }

        public int getNumberOfPendingTimeouts() {
            return 0;
        }

        public int getNumberOfWaitingSockets() {
            return 0;
        }

        public int getPort() {
            return 0;
        }

        public boolean isGUESSCapable() {
            return false;
        }

        @Override
        public boolean canDoFWT() {
            return false;
        }

        @Override
        public int getNumActiveDownloads() {
            return 0;
        }

        @Override
        public int getNumActiveUploads() {
            return 0;
        }

        @Override
        public int getNumConnectionCheckerWorkarounds() {
            return 0;
        }

        @Override
        public int getNumQueuedUploads() {
            return 0;
        }

        @Override
        public long[] getSelectStats() {
            return null;
        }

        @Override
        public int getSharedFileListSize() {
            return 0;
        }

        @Override
        public int getSimppVersion() {
            return 0;
        }

        @Override
        public String getUploadSlotManagerInfo() {
            return null;
        }

        @Override
        public boolean isConnected() {
            return false;
        }

        @Override
        public boolean isLifecycleLoaded() {
            return false;
        }

        @Override
        public boolean isShieldedLeaf() {
            return false;
        }

        @Override
        public boolean isSupernode() {
            return false;
        }

        @Override
        public boolean isUdpPortStable() {
            return false;
        }

        @Override
        public int lastReportedUdpPort() {
            return 0;
        }

        @Override
        public int receivedIpPong() {
            return 0;
        }
        
    }
}