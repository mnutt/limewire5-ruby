package com.limegroup.gnutella.gui.chat;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFrame;

import com.limegroup.gnutella.chat.InstantMessenger;

/**
 * Manages all chat session and provides an interface to access each
 * chat session.
 */
public final class ChatUIManager {

    /**
     * Constant for the single <tt>ChatManager</tt> instance for 
     * singleton.
     */
	private static final ChatUIManager INSTANCE = new ChatUIManager();
	
	/** 
	 * <tt>Map</tt> of <tt>Chatter</tt> instances.
	 */
    private Map<InstantMessenger, ChatFrame> _chats = new HashMap<InstantMessenger, ChatFrame>();

	/** 
	 * Private constructor to ensure that this class cannot be 
	 * constructed from any other class. 
	 */
	private ChatUIManager() {}
	
	/** 
	 * Returns the single instance of this class, following singleton.
	 * 
	 * @return the single <tt>ChatManager</tt> instance
	 */
	public static ChatUIManager instance() {
		return INSTANCE;
	}
	
	/**
     * Accepts a new chat session with a new user. A host may have closed a
     * previous connection and opened up another one. In this case the chat
     * window is reused and the old Chatter instance is closed.
	 * @return 
     * 
     * @chatter the new <tt>Chatter</tt> instance to chat with
     */
	public ChatFrame acceptChat(InstantMessenger chatter) {
	    ChatFrame frame = raiseExistingFrame(chatter);
	    if (frame != null) {
            _chats.remove(frame.getChat());
	        frame.chatAvailable(chatter);
	    } else {
	        final ChatFrame newFrame = new ChatFrame(chatter);
            newFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    newFrame.getChat().stop();
                    removeChat(newFrame.getChat());
                }
            });
            newFrame.setVisible(true);
            frame = newFrame;
        }
		_chats.put(chatter, frame);
        return frame;
	}
	
	/**
     * Raises an existing chat window.
     * 
     * @return the chat window, if window was raised; null, if no window for
     *         <code>chatter</code> exists
     */
	private ChatFrame raiseExistingFrame(InstantMessenger chatter) {
        for (InstantMessenger c : _chats.keySet()) {
			if (c.getHost().equals(chatter.getHost())) {
				ChatFrame frame = _chats.get(c);
				frame.setState(JFrame.NORMAL);
				frame.toFront();
				return frame;
			}
		}
        return null;
	}

    /**
     * Removes the specified chat session from the list of active 
     * sessions.
     *
     * @param the <tt>Chatter</tt> instance to remove
     */
	public void removeChat(InstantMessenger chatter) {
		ChatFrame cframe = _chats.remove(chatter);
		if (cframe != null) {
		    cframe.dispose();
			cframe.setVisible(false);
		}
			
	}
	
	/**
	 * Receives a message for the session associated with the specified
	 * <tt>Chatter</tt> instance.
	 *
	 * @param chatter the <tt>Chatter</tt> instance with which the new
	 *  message is associated
	 * @param message 
	 */
	public void receiveMessage(InstantMessenger chatter, String message) {
		ChatFrame cframe = _chats.get(chatter);
        
        if(cframe == null) {
            // The frame could be null if the user on this end already
            // removed it, for example.
            return;
        }
		cframe.addResponse(message);
	}

	/** 
	 * Lets the user know that a host is no longer available. 
	 *
	 * @param the <tt>Chatter</tt> instance for the host that is no longer
	 *  available
	 */
	public void chatUnavailable(InstantMessenger chatter) {
		ChatFrame cframe = _chats.get(chatter);
        if(cframe == null) {
            return;
        }
		cframe.chatUnavailable();
	}

	/** 
	 * Display an error message in the chat gui for the specified chat
	 * session.
	 *
	 * @param chatter the <tt>Chatter</tt> instance associated with the error
	 * @param str the error message to display
	 */
	public void chatErrorMessage(InstantMessenger chatter, String str) {
		ChatFrame cframe = _chats.get(chatter);
        if(cframe == null) {
            return;
        }
		cframe.displayErrorMessage(str);
    }

    public Collection<ChatFrame> getChatFrames() {
        return _chats.values();
    }

    /**
     * Closes all chat windows.
     */
    public void clear() {
        for (ChatFrame frame : getChatFrames()) {
            frame.getChat().stop();
            frame.dispose();
        }
        _chats.clear();
    }
    
}
