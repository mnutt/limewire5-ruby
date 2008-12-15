package com.limegroup.gnutella.bugs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.core.settings.ApplicationSettings;
import org.limewire.core.settings.BugSettings;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;
import org.limewire.io.IOUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.GenericsUtils;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;

import com.google.inject.Inject;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.LocalClientInfoFactory;
import com.limegroup.gnutella.gui.MessageService;
import com.limegroup.gnutella.gui.MultiLineLabel;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * Interface for reporting bugs.
 * This can do any of the following:
 * <ul>
 *  <li>Send the bug directly to the servlet.</li>
 *  <li>Allow the bug to be reviewed before sending.</li>
 *  <li>Allow the user to copy the bug & email it if sending fails.</li>
 *  <li>Suppress the bug entirely.</li>
 *  </ul>
 */
public final class BugManager {

    @Inject private static volatile LocalClientInfoFactory localClientInfoFactory;

    /**
     * The instance of BugManager -- follows a singleton pattern.
     */
    private static BugManager INSTANCE;
    
    /**
     * The error title
     */
    private final String TITLE =
        I18n.tr("Internal Error");
    
    /**
     * the width of the internal error dialog box
     */
    private static final int DIALOG_BOX_WIDTH = 300;
    /**
     * the height of the internal error dialog box
     */
    private static final int DIALOG_BOX_HEIGHT = 100;
       
    /**
     * The initial text displayed in the user comments area.
     */
    private static final String TEXT_AREA_DESCRIPTION_TEXT =  I18n.tr("Please add any comments you may have (e.g what caused the error). \nThank you and please use English.");
    
    /**
     * The queue that processes processes the bugs.
     */
    private final ExecutorService BUGS_QUEUE = ExecutorsHelper.newProcessingQueue(
                new ThreadFactory() {
                    public Thread newThread(Runnable r) {
                        Thread t = new Thread(r, "BugProcessor");
                        t.setDaemon(true);
                        return t;
                    }
                });
	
	/**
	 * A mapping of stack traces (String) to next allowed time (long)
	 * that the bug can be reported.
	 *
	 * Used only if reporting the bug to the servlet.
	 */
	private final Map<String, Long> BUG_TIMES = Collections.synchronizedMap(new HashMap<String, Long>());
	
	/**
	 * A lock to be used when writing to the logfile, if the log is to be
	 * recorded locally.
	 */
	private final Object WRITE_LOCK = new Object();
	
	/**
	 * A separator between bug reports.
	 */
	private final byte[] SEPARATOR = "-----------------\n".getBytes();
	
	/**
	 * The next time we're allowed to send any bug.
	 *
	 * Used only if reporting the bug to the servlet.
	 */
	private volatile long _nextAllowedTime = 0;
	
	/**
	 * The number of bug dialogs currently showing.
	 */
	private volatile int _dialogsShowing = 0;
	
	/**
	 * The maximum number of dialogs we're allowed to show.
	 */
	private final int MAX_DIALOGS = 3;
	
	/**
	 * Whether or not we have dirty data after the last save.
	 */
	private boolean dirty = false;
	
	public static synchronized BugManager instance() {
	    if(INSTANCE == null)
	        INSTANCE = new BugManager();
	    return INSTANCE;
	}
    
    /** Inspectable to allow pulling of bug reports */
    @SuppressWarnings("unused")
    @InspectionPoint("bug report")
    private static final Inspectable INSPECTABLE = new Inspectable() {
        public Object inspect() {
            
            if (!ApplicationSettings.USAGE_STATS.getValue() && !LimeWireUtils.isAlphaRelease())
                return "Denied";
            
            Exception e = new Exception();
            e.setStackTrace(new StackTraceElement[0]);
            LocalClientInfo info = localClientInfoFactory.createLocalClientInfo(e, "", "", false);
            return info.getShortParamList();
        }
    };
    
    /**
     * Private to ensure that only this class can construct a 
     * <tt>BugManager</tt>, thereby ensuring that only one instance is created.
     */
    private BugManager() {
        loadOldBugs();
    }
    
    /**
     * Shuts down the BugManager.
     */
    public void shutdown() {
        writeBugsToDisk();
    }
	
	/**
	 * Handles a single bug report.
	 * If bug is a ThreadDeath, rethrows it.
	 * If the user wants to ignore all bugs, this effectively does nothing.
	 * The the server told us to stop reporting this (or any) bug(s) for
	 * awhile, this effectively does nothing.
	 * Otherwise, it will either send the bug directly to the servlet
	 * or ask the user to review it before sending.
	 */
	public void handleBug(Throwable bug, String threadName, String detail) {
        if( bug instanceof ThreadDeath ) // must rethrow.
	        throw (ThreadDeath)bug;
	        
        // Try to dispatch the bug to a friendly handler.
        if(bug instanceof IOException && 
           IOUtils.handleException((IOException)bug, IOUtils.ErrorType.GENERIC))
           return; // handled already.
            
	    
        bug.printStackTrace();
        
        // Build the LocalClientInfo out of the info ...
        LocalClientInfo info = localClientInfoFactory.createLocalClientInfo(bug, threadName, detail, false);

        if( BugSettings.LOG_BUGS_LOCALLY.getValue() )
            logBugLocally(info);
                    
        boolean sent = false;
        // never ignore bugs or auto-send when developing.
        if(!LimeWireUtils.isTestingVersion()) {
    	    if(!BugSettings.REPORT_BUGS.getValue() )
    	        return; // ignore.
    	        
            // If we have already sent information about this bug, leave.
            if( !shouldInform(info) )
               return; // ignore.

            // If the user wants to automatically send to the servlet, do so.
            // Otherwise, display it for review.
            if( isSendableVersion()) {
            	if (LimeWireUtils.isAlphaRelease() || !BugSettings.SHOW_BUGS.getValue())
            		sent = true;
            }
            
            if (sent) 
            	sendToServlet(info);
        }
        
        if (!sent &&  _dialogsShowing < MAX_DIALOGS )
            reviewBug(info);
    }
    
    /**
     * Logs the bug report to a local file.
     * If the file reaches a certain size it is erased.
     */
    private void logBugLocally(LocalClientInfo info) {
        File f = BugSettings.BUG_LOG_FILE.getValue();
        FileUtils.setWriteable(f);
        OutputStream os = null;
        try {
            synchronized(WRITE_LOCK) {
                if ( f.length() > BugSettings.MAX_BUGFILE_SIZE.getValue() )
                    f.delete();
                os = new BufferedOutputStream(
                        new FileOutputStream(f.getPath(), true));
                os.write((new Date().toString() + "\n").getBytes());
                os.write(info.toBugReport().getBytes());
                os.write(SEPARATOR);
                os.flush();
            }
        } catch(IOException ignored) {
        } finally {
            IOUtils.close(os);
        }
    }
    
    /**
     * Loads bugs from disk.
     */
    private void loadOldBugs() {
        ObjectInputStream in = null;
        File f = BugSettings.BUG_INFO_FILE.getValue();
        try {
            // Purposely not a ConverterObjectInputStream --
            // we never want to read old version's bug info.
            in = new ObjectInputStream(
                    new BufferedInputStream(
                        new FileInputStream(f)));
            String version = (String)in.readObject();
            long nextTime = in.readLong();
            if( version.equals(LimeWireUtils.getLimeWireVersion()) ) {
                Map<String, Long> bugs = GenericsUtils.scanForMap(
                        in.readObject(), String.class, Long.class,
                        GenericsUtils.ScanMode.REMOVE);
                // Only load them if we're continuing to use the same version
                // This way bugs for newer versions get reported.
                // We could check to make sure this is a newer version,
                // but it's not all that necessary.
                _nextAllowedTime = nextTime;
                long now = System.currentTimeMillis();
                for(Map.Entry<String, Long> entry : bugs.entrySet()) {
                    // Only insert those whose times haven't expired.
                    Long allowed = entry.getValue();
                    if( allowed != null && now < allowed.longValue() )
                        BUG_TIMES.put(entry.getKey(), allowed);
                }
            } else {
                // Otherwise, we're using a different version than the last time.
                // Unset 'discard all bugs'.
                if(!BugSettings.REPORT_BUGS.getValue()) {
                    BugSettings.REPORT_BUGS.setValue(true);
                    BugSettings.SHOW_BUGS.setValue(true);
                }
            }
        } catch(Throwable t) {
            // ignore errors from disk.
        } finally {
            IOUtils.close(in);
        }
                    
    }
    
    /**
     * Write bugs out to disk.
     */
    private void writeBugsToDisk() {
        synchronized(WRITE_LOCK) {
            if(!dirty)
                return;
            
            ObjectOutputStream out = null;
            try {
                File f = BugSettings.BUG_INFO_FILE.getValue();
                out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
                String version = LimeWireUtils.getLimeWireVersion();
                out.writeObject(version);
                out.writeLong(_nextAllowedTime);
                out.writeObject(BUG_TIMES);
                out.flush();
            } catch(Exception e) {
                // oh well, no biggie if we couldn't write to disk.
            } finally {
                IOUtils.close(out);
            }
            
            dirty = false;
        }
    }
    
    /**
     * Determines if the bug has already been reported enough.
     * If it has, this returns false.  Otherwise (if the bug should
     * be reported) this returns true.
     */
    private boolean shouldInform(LocalClientInfo info) {
        long now = System.currentTimeMillis();
        
        // If we aren't allowed to report a bug, exit.
        if( now < _nextAllowedTime )
            return false;

        Long allowed = BUG_TIMES.get(info.getParsedBug());
        return allowed == null || now >= allowed.longValue();
    }
    
    /**
     * Determines if we're allowed to send a bug report.
     */
    private boolean isSendableVersion() {
        Version myVersion;
        Version lastVersion;
        try {
            myVersion = new Version(LimeWireUtils.getLimeWireVersion());
            lastVersion = new Version(BugSettings.LAST_ACCEPTABLE_VERSION.getValue());
        } catch(VersionFormatException vfe) {
            return false;
        }
        
        return myVersion.compareTo(lastVersion) >= 0;
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
     * Displays a message to the user informing them an internal error
     * has occurred.  The user is asked to click 'send' to send the bug
     * report to the servlet and has the option to review the bug
     * before it is sent.
     */
    private void reviewBug(final LocalClientInfo info) {
        _dialogsShowing++;
        
		final JDialog DIALOG =
		    new JDialog(GUIMediator.getAppFrame(), TITLE, true);
		final Dimension DIALOG_DIMENSION = new Dimension(DIALOG_BOX_WIDTH, DIALOG_BOX_HEIGHT);
		DIALOG.setSize(DIALOG_DIMENSION);
		

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
		
		boolean sendable = isSendableVersion();

        String msg;
        if(sendable)
            msg = I18n.tr("LimeWire has encountered an internal error. It is possible for LimeWire to recover and continue running normally. To aid with debugging, please click \'Send\' to notify LimeWire about the problem. If desired, you can click \'Review\' to look at the information that will be sent. Thank you.");
        else
            msg = I18n.tr("LimeWire has encountered an internal error. It is possible for LimeWire to recover and continue running normally. To continue using LimeWire, click \'Discard\'. If desired, you can click \'Review\' to look at the information about the error.");
        
        msg = warning() + "\n\n" + msg;
       
        MultiLineLabel label = new MultiLineLabel(msg, 400);        
        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new GridBagLayout());
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.gridheight = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;        
        labelPanel.add(label, constraints); 
                
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
        userCommentsScrollPane.setPreferredSize( new Dimension(400, 80) );        
        
        JPanel buttonPanel = new JPanel();
        JButton sendButton = new JButton(I18n.tr("Send"));
        sendButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    String userComments = userCommentsTextArea.getText(); 
			    if(!userComments.equals(TEXT_AREA_DESCRIPTION_TEXT))
			        info.addUserComments(userComments);
			    sendToServlet(info);
				DIALOG.dispose();
				_dialogsShowing--;
			}
		});
        JButton reviewButton = new JButton(I18n.tr("Review"));
        reviewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {			    
			    String userComments = userCommentsTextArea.getText(); 
                if(!userComments.equals(TEXT_AREA_DESCRIPTION_TEXT))
                    info.addUserComments(userComments);
		        JTextArea textArea = new JTextArea(info.toBugReport());
                textArea.setColumns(50);
                textArea.setEditable(false);
                textArea.setCaretPosition(0);
                JScrollPane scroller = new JScrollPane(textArea);
                scroller.setBorder(BorderFactory.createEtchedBorder());
                scroller.setPreferredSize( new Dimension(500, 200) );
                MessageService.instance().showMessage(scroller);
			}
		});
		JButton discardButton = new JButton(I18n.tr("Discard"));
		discardButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		        DIALOG.dispose();
		        _dialogsShowing--;
		    }
		});
		if(sendable)
            buttonPanel.add(sendButton);
        buttonPanel.add(reviewButton);
        buttonPanel.add(discardButton);
        
        JPanel optionsPanel = new JPanel();
        JPanel innerPanel = new JPanel();
        ButtonGroup bg = new ButtonGroup();
        innerPanel.setLayout( new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        optionsPanel.setLayout(new BorderLayout());
        final JRadioButton alwaysSend = new JRadioButton(I18n.tr("Always Send Immediately"));
        final JRadioButton alwaysReview = new JRadioButton(I18n.tr("Always Ask For Review"));
        final JRadioButton alwaysDiscard = new JRadioButton(I18n.tr("Always Discard All Errors"));
		innerPanel.add(Box.createVerticalStrut(6));        
        if(!LimeWireUtils.isTestingVersion()) {
    		if(sendable)
                innerPanel.add(alwaysSend);
            innerPanel.add(alwaysReview);
            innerPanel.add(alwaysDiscard);
        }
		innerPanel.add(Box.createVerticalStrut(6));        
        optionsPanel.add( innerPanel, BorderLayout.WEST );
        bg.add(alwaysSend);
        bg.add(alwaysReview);
        bg.add(alwaysDiscard);
        bg.setSelected(alwaysReview.getModel(), true);
        ActionListener alwaysListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if( e.getSource() == alwaysSend ) {
                    BugSettings.REPORT_BUGS.setValue(true);
                    BugSettings.SHOW_BUGS.setValue(false);
                } else if (e.getSource() == alwaysReview ) {
                    BugSettings.REPORT_BUGS.setValue(true);
                    BugSettings.SHOW_BUGS.setValue(true);
                } else if( e.getSource() == alwaysDiscard ) {                    
                    BugSettings.REPORT_BUGS.setValue(false);
                }
            }
        };
        alwaysSend.addActionListener(alwaysListener);
        alwaysReview.addActionListener(alwaysListener);
        alwaysDiscard.addActionListener(alwaysListener);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        mainPanel.add(labelPanel, constraints);              
        
        if(sendable)
        {
            constraints = new GridBagConstraints();
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;            
            constraints.weighty = 1.0;
            constraints.insets = new Insets(20, 0, 6, 0);
            mainPanel.add(userCommentsScrollPane, constraints);
        }
            
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 2;

        mainPanel.add(optionsPanel, constraints);
        
        constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 3;
        
        mainPanel.add(buttonPanel, constraints);   
        
        DIALOG.getContentPane().add(mainPanel);
		DIALOG.pack();

		if(GUIMediator.isAppVisible())
			DIALOG.setLocationRelativeTo(MessageService.getParentComponent());
		else {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    		Dimension dialogSize = DIALOG.getSize();
    		DIALOG.setLocation((screenSize.width - dialogSize.width)/2,
    						   (screenSize.height - dialogSize.height)/2);
		}

		try {
		    DIALOG.setVisible(true);
        } catch(InternalError ie) {
            //happens occasionally, ignore.
        } catch(ArrayIndexOutOfBoundsException npe) {
            //happens occasionally, ignore.
        }
    }
    
    /**
     * Displays a message to the user informing them an internal error
     * has occurred and the send to the servlet has failed, asking
     * the user to email the bug to us.
     */
    private void servletSendFailed(final LocalClientInfo info) {
        _dialogsShowing++;

		final JDialog DIALOG =
		    new JDialog(GUIMediator.getAppFrame(), TITLE, true);
		final Dimension DIALOG_DIMENSION = new Dimension(350, 300);
		final Dimension ERROR_DIMENSION = new Dimension(300, 200);
		DIALOG.setSize(DIALOG_DIMENSION);

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        MultiLineLabel label = new MultiLineLabel(I18n.tr("LimeWire was unable to connect to the bug server in order to send the below bug report. For further help and to aid with debugging, please visit www.limewire.com and click \'Support\'. Thank you."), 400);
		JPanel labelPanel = new JPanel();
		JPanel innerPanel = new JPanel();
		labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
		innerPanel.add(label);
		innerPanel.add(Box.createVerticalStrut(6));
		labelPanel.add(innerPanel);
		labelPanel.add(Box.createHorizontalGlue());
		
		// Add 'FILES IN CURRENT DIRECTORY [text]
		//      SIZE: 0'
		// So that the script processing the emails still
		// works correctly.  [It uses the info as markers
		// of when to stop reading -- if it wasn't present
		// it failed processing the email correctly.]
		String bugInfo = info.toBugReport().trim() + "\n\n" + 
		                 "FILES IN CURRENT DIRECTORY NOT LISTED.\n" +
		                 "SIZE: 0";
        final JTextArea textArea = new JTextArea(bugInfo);
        textArea.selectAll();
        textArea.copy();        
        textArea.setColumns(50);
        textArea.setEditable(false);
        JScrollPane scroller = new JScrollPane(textArea);
        scroller.setBorder(BorderFactory.createEtchedBorder());
        scroller.setPreferredSize(ERROR_DIMENSION);		

        JPanel buttonPanel = new JPanel();
        JButton copyButton = new JButton(I18n.tr("Copy Report"));
        copyButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			    textArea.selectAll();
				textArea.copy();
				textArea.setCaretPosition(0);
			}
		});
        JButton quitButton = new JButton(I18n.tr("OK"));
        quitButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e){
				DIALOG.dispose();
				_dialogsShowing--;
			}
		});
        buttonPanel.add(copyButton);
        buttonPanel.add(quitButton);

        mainPanel.add(labelPanel);
        mainPanel.add(scroller);
        mainPanel.add(buttonPanel);

        DIALOG.getContentPane().add(mainPanel);
        try {
		    DIALOG.pack();
        } catch(OutOfMemoryError oome) {
            // we couldn't put this dialog together, discard it entirely.
            return;
        }

		if(GUIMediator.isAppVisible())
			DIALOG.setLocationRelativeTo(MessageService.getParentComponent());
		else {
			Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    		Dimension dialogSize = DIALOG.getSize();
    		DIALOG.setLocation((screenSize.width - dialogSize.width)/2,
    						   (screenSize.height - dialogSize.height)/2);
		}

		DIALOG.setVisible(true);
    }    
    
    /**
     * Sends the bug to the servlet and updates the next allowed times
     * that this bug (or any bug) can be sent.
     * This is done in another thread so the current thread does not block
     * while connecting and transferring information to/from the servlet.
     * If the send failed, displays another message asking the user to email
     * the error.
     */
    private void sendToServlet(final LocalClientInfo info) {
        BUGS_QUEUE.execute(new ServletSender(info));
    }

    /**
     * Sends a single bug report.
     */
    private class ServletSender implements Runnable {
        final LocalClientInfo INFO;
        
        ServletSender(LocalClientInfo info) {
            INFO = info;
        }
        
        public void run() {
            // Send this bug to the servlet & store its response.
            // THIS CALL BLOCKS.
            RemoteClientInfo remoteInfo =
                new ServletAccessor().getRemoteBugInfo(INFO);
            
            if( remoteInfo == null ) { // could not connect
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        servletSendFailed(INFO);
                    }
                });
                return;
            }
            
            long now = System.currentTimeMillis();
            long thisNextTime = remoteInfo.getNextThisBugTime();
            long anyNextTime = remoteInfo.getNextAnyBugTime();

            synchronized(WRITE_LOCK) {    
                if( anyNextTime != 0 ) {
                    _nextAllowedTime = now + thisNextTime;
                    dirty = true;
                }
                
                if( thisNextTime != 0 ) {
                    BUG_TIMES.put(INFO.getParsedBug(), new Long(now + thisNextTime));
                    dirty = true;
                }
                
                writeBugsToDisk();
            }
        }
    }   
}