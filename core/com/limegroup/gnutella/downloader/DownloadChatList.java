package com.limegroup.gnutella.downloader;

import java.util.ArrayList;
import java.util.List;

import com.limegroup.gnutella.Endpoint;

/**
 * This class keeps track of chattable <tt>HTTPDownloader</tt> instances for
 * a single <tt>ManagedDownloader</tt>.  It maintains a <tt>List</tt> of
 * chat-enabled hosts, and returns the most recently added (the most recently
 * downloaded-from) chat-enabled host upon request.
 */
final class DownloadChatList {
	
	/**
	 * Constant for the <tt>List</tt> of chattable <tt>HTTPDownloader</tt>s
	 * for this <tt>ManagedDownloader</tt>.
	 */
	private final List<HTTPDownloader> CHAT_LIST = new ArrayList<HTTPDownloader>();


	/**
	 * Returns whether or not there is a chat-enabled host for this download.
	 */
	synchronized boolean hasChatEnabledHost() {
		return !CHAT_LIST.isEmpty();
	}

	/**
	 * Returns the first chattable <tt>Endpoint</tt> instance in the list,
	 * or <tt>null</tt> if the list is empty.
	 *
	 * @return the first chattable <tt>Endpoint</tt> instance in the list,
	 *  or <tt>null</tt> if the list is empty.
	 */
	synchronized Endpoint getChatEnabledHost() {		
		if(CHAT_LIST.isEmpty()) return null;
		HTTPDownloader downloader = CHAT_LIST.get(CHAT_LIST.size()-1);
		return new Endpoint(downloader.getInetAddress().getHostAddress(),
							downloader.getPort());
	}

	/**
	 * Adds a new <tt>HTTPDownloader</tt> to the list of hosts that we
	 * can chat with for this download if the host is chat-enabled.  If the
	 * added host is already in the list, this removes it and adds it again
	 * as the freshest host (the one that we will chat with).  Similarly,
	 * if the list is getting large (by chat standards), this will remove
	 * the oldest host and add the new one as the freshest one.
	 *
	 * @param host the new <tt>HTTPDownloader</tt> to add if it is chat-enabled
	 */
	synchronized void addHost(HTTPDownloader host) {
		if(!host.chatEnabled()) return;

		// if this host is already in the list, remove it from the list and 
		// add it as the freshest one
		if(CHAT_LIST.contains(host)) {
			CHAT_LIST.remove(host);
		}

		// if our list of chat-enabled host is growing a bit, just get rid of
		// the oldest and add the new one
		if(CHAT_LIST.size() >= 5) {
			CHAT_LIST.remove(0);
		}

		CHAT_LIST.add(host);
	}

	/**
	 * Checks if the specified <tt>HTTPDownloader</tt> instance is chat-enabled,
	 * and removes it if it is in the list.
	 *
	 * @param host the <tt>HTTPDownloader</tt> to remove
	 */
	synchronized void removeHost(HTTPDownloader host) {
		if(!host.chatEnabled()) return;
		CHAT_LIST.remove(host);
	}
}
