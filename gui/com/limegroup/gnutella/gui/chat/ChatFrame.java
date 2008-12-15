package com.limegroup.gnutella.gui.chat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import org.limewire.i18n.I18nMarker;

import com.limegroup.gnutella.chat.InstantMessenger;
import com.limegroup.gnutella.gui.BoxPanel;
import com.limegroup.gnutella.gui.ButtonRow;
import com.limegroup.gnutella.gui.GUIConstants;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.PaddedPanel;
import com.limegroup.gnutella.gui.themes.ThemeObserver;

/**
 * The UI front end for the chat class.  it is a subclass of 
 * JFrame, and displays both user's messages, as well as allowing
 * message input for one user.
 */

//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|

public class ChatFrame extends JFrame implements ThemeObserver {

	/**
	 * Constant for the locale-specific resource key for the label of the 
	 * chat block sender button.
	 */
	private final String BLOCK_BUTTON_LABEL = I18nMarker.marktr("Block Host");

	/**
	 * Constant for the locale-specific resource key for the label of the 
	 * chat send button.
	 */
	private final String SEND_BUTTON_LABEL = I18nMarker.marktr("Send Message");

	/**
	 * Constant for the locale-specific resource key for the toolTip of the 
	 * chat block sender button.
	 */
	private final String BLOCK_BUTTON_TIP = I18nMarker.marktr("Block Current Host");

	/**
	 * Constant for the locale-specific resource key for the toolTip of the 
	 * chat send button.
	 */
	private final String SEND_BUTTON_TIP = I18nMarker.marktr("Send a Message to a Host");

	private final String WITH_LABEL = I18nMarker.marktr("Chatting with");

	private final String UNAVAILABLE_LABEL = I18nMarker.marktr("Host is unavailable");

	private final String YOU_LABEL = I18nMarker.marktr("You");

	private final int WINDOW_WIDTH  = 500;
	private final int WINDOW_HEIGHT = 300;
	
	/**
	 * Constants for preventing resizing the frame to an unusable size
	 */
	private final int WINDOW_MIN_WIDTH = 250;
	private final int WINDOW_MIN_HEIGHT = 200;
	
	private final int TEXT_FIELD_LIMIT = 500;
	
	private boolean connected = true;
	
	JTextArea _area;    /* where the conversation is displayed */
	JTextField _field;  /* where the user enters the message to send */
	JTextField _connectField;
	InstantMessenger _chat;      /* the interface to the backend */
	ButtonRow _buttons; /* block and send buttons */

	public ChatFrame(InstantMessenger chat) {
		super();
		setTitle(I18n.tr(WITH_LABEL) + " " 
				 + chat.getHost());
		setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
		_chat = chat;
    

		Frame parentFrame = GUIMediator.getAppFrame();

		int mwidth = parentFrame.getSize().width / 2;
		int mheight = parentFrame.getSize().height / 2;
		
		int fwidth = getSize().width / 2;
		int fheight = getSize().height / 2;

		int xlocation = mwidth - fwidth;
		int ylocation = mheight - fheight;

		int xstart = parentFrame.getLocation().x;
		int ystart = parentFrame.getLocation().y;
		
        int x = xstart+xlocation;
        int y = ystart+ylocation;
        
        // The location of the ChatFrame is set relative to the
        // main application frame but if the location is outside
        // of the visible area then center it on the screen.
        Dimension screenSize =
            Toolkit.getDefaultToolkit().getScreenSize();
        if (x < 0 || y < 0 || x >= screenSize.width || y >= screenSize.height) {
            x = (screenSize.width - getSize().width) / 2;
            y = (screenSize.height - getSize().height) / 2;
        }
        
		setLocation(x, y);

		BlockListener blockListener = new BlockListener();
		ActionListener sendListener = new SendListener();

		String[] buttonLabels = {
			BLOCK_BUTTON_LABEL,
			SEND_BUTTON_LABEL
		};

		String[] buttonTips = {
			BLOCK_BUTTON_TIP,
			SEND_BUTTON_TIP
		};

		ActionListener[] buttonListeners = {
			blockListener, sendListener
		};

		_buttons = new ButtonRow(buttonLabels,
										  buttonTips,
										  buttonListeners,
										  ButtonRow.X_AXIS,
										  ButtonRow.NO_GLUE);
		_buttons.setButtonEnabled(1, false);

		TextPanel tp = new TextPanel();
		BoxPanel mainPanel = new BoxPanel(BoxLayout.Y_AXIS);
		PaddedPanel myPanel = new PaddedPanel();
		myPanel.setPreferredSize(new Dimension(1000,1000));		
		myPanel.add(tp);

		mainPanel.add(myPanel);
        mainPanel.add(Box.createVerticalStrut(GUIConstants.SEPARATOR));
		mainPanel.add(_buttons);
		mainPanel.add(Box.createVerticalStrut(GUIConstants.SEPARATOR));
		getContentPane().add(mainPanel);
		
		// establish minimum size of the chat window
		addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
            	JFrame src = (JFrame)e.getSource();
                int width = src.getWidth();
                int height = src.getHeight();

                int newWidth = Math.max(width, WINDOW_MIN_WIDTH);
                int newHeight = Math.max(height, WINDOW_MIN_HEIGHT);

                if(newWidth != width || newHeight != height)
            		src.setSize(newWidth, newHeight);
            }
        });		

		updateTheme();
	}

    public void chatAvailable(InstantMessenger chatter) {
        if (this._chat != chatter) {
            if (this._chat != null) {               
                // if a chat with localhost is attempted this will cause the
                // outgoing connection to be closed since both connections will
                // get associated with this single chat frame
                this._chat.stop();
            }
            this._chat = chatter;
        }
        _area.setForeground(UIManager.getColor("TextArea.foreground"));
        connected = true;
        _buttons.setButtonEnabled(1, true);
    }
    
	// inherit doc comment
	public void updateTheme() {
		ImageIcon plugIcon = GUIMediator.getThemeImage(GUIConstants.LIMEWIRE_ICON);
		setIconImage(plugIcon.getImage());		
	}

	/** displays an incoming message from a specific host */
	public void addResponse(String str) {
		String host = _chat.getHost();
		_area.setText(_area.getText() + host + ": " + str + "\n");
	}
	
	public void chatUnavailable() {
		_area.setForeground(Color.red);
		appendMessage(I18n.tr(UNAVAILABLE_LABEL));
		connected = false;
		_buttons.setButtonEnabled(1, false);
	}

    private void appendMessage(String text) {
        _area.append(text + "\n");
    }
	
	/** display an error message in red in the chat gui */
	public void displayErrorMessage(String str) {
		_area.setForeground(Color.red);
        if (str == null) {
            str = I18n.tr("A connection error has occurred");
        }
		appendMessage(str);
	}
	
	/**
     * Displays the current message on the screen, and sends it to the chat
     * partner.
     */
    public void send() {
        if (!connected)
            return;
        String str = _field.getText();
        if (str.length() == 0)
            return;
        if (_chat.send(str)) {
            appendMessage(I18n.tr(YOU_LABEL) + ": " + str);
            _field.setText("");
            _buttons.setButtonEnabled(1, false);
        } else {
            displayErrorMessage(I18n
                    .tr("Could not send chat message"));
        }
    } 

    public InstantMessenger getChat() {
        return _chat;
    }

    public void setMessage(String message) {
        _field.setText(message);
    }
    
    public String getText() {
        return _area.getText();
    }
    
    public String getMessage() {
        return _field.getText();
    }
    
    public boolean isConnected() {
        return connected;
    }
    
	/**
	 * limits the JTextField input to the specified number of characters, 
	 * including pastes
	 */
	private class JTextFieldLimit extends PlainDocument {
		private int limit;
		
		public JTextFieldLimit(int limit) {
			super();
			this.limit = limit;
		}
		   
		@Override
        public void insertString(int offset, String str, AttributeSet attr)
			throws BadLocationException {
			if (str == null) return;
			if ((getLength() + str.length()) <= limit)
				super.insertString(offset, str, attr);
		}
	}

	private class TextPanel extends JPanel {
		public TextPanel() {
			setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
			BoxPanel fieldPanel = new BoxPanel(BoxLayout.X_AXIS);
			_area = new JTextArea();
			_area.setLineWrap(true);
			_field = new JTextField();
			_field.setDocument(new JTextFieldLimit(TEXT_FIELD_LIMIT));
			JScrollPane areaScrollPane = new JScrollPane(_area);

			_area.setEditable(false);
			
		    _field.addKeyListener(new JTextFieldKeyListener());

		    fieldPanel.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR));
			fieldPanel.add(_field);
			fieldPanel.add(Box.createHorizontalStrut(GUIConstants.SEPARATOR));
			fieldPanel.setPreferredSize(new Dimension(1000,20));
			fieldPanel.setMaximumSize(new Dimension(1000, 20));
			add(areaScrollPane);
			add(Box.createVerticalStrut(GUIConstants.SEPARATOR));
			add(fieldPanel);
		}
	}
	
	
	/*****************************************************************
	 *                        LISTENERS
	 *
	 *****************************************************************/
		
	/** send a message to the specified host */
	private class SendListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
            send();
		}
	}
	
	/** connect to the specified host */
	private class BlockListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String host = _chat.getHost();
			GuiCoreMediator.getSpamServices().blockHost(host);
			_chat.stop();
		}
	}

	/** enable state for the send button */
	private class JTextFieldKeyListener implements KeyListener {
		public void keyTyped(KeyEvent k) {
			if (k.getKeyChar() == KeyEvent.VK_ENTER)
				send();                              // send the message
		}
		public void keyPressed(KeyEvent k) {}
		public void keyReleased(KeyEvent k) {
			String text = _field.getText();
			if (text.length() == 0)
				_buttons.setButtonEnabled(1, false); // disable send button
			else if (connected) {
				_buttons.setButtonEnabled(1, true);  // enable send button
            }
		}
	}
	
}



