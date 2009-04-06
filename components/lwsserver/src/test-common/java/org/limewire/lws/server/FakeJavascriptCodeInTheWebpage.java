package org.limewire.lws.server;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.limewire.lws.server.StringCallback;
import org.limewire.net.SocketsManager;


/**
 * This class represents fake javascript code that can
 * communicate to the remote server and local server on the client.
 */
public final class FakeJavascriptCodeInTheWebpage {

	private final LocalServerDelegate toLocalServer;
	private final LocalServerDelegate toRemoteServer;
	
	public interface Handler {

		public final static Handler ALERT = new Handler() {
			public void handle(String res) {
				JOptionPane.showMessageDialog(null, res);
			}
		};

		void handle(String res);
	}
	
	FakeJavascriptCodeInTheWebpage(SocketsManager socketsManager, LocalServerImpl local, RemoteServerImpl remote) {
		this.toLocalServer = new LocalServerDelegate(socketsManager, "localhost", local.getPort());
        this.toRemoteServer = new LocalServerDelegate(socketsManager, "localhost", remote.getPort());
	}
	
	protected final void sendLocalMsg(String msg, Map<String, String> args, final Handler h) {
        toLocalServer.sendMessageToServer(msg, args, new StringCallback() {
            public void process(String response) {
                h.handle(removeHeaders(response));
            }
        }, LocalServerDelegate.NormalStyleURLConstructor.INSTANCE);
	}

	protected final void sendRemoteMsg(String msg, Map<String, String> args, final Handler h) {
        toRemoteServer.sendMessageToServer(msg, args, new StringCallback() {
            public void process(String response) {
                h.handle(removeHeaders(response));
            }
        }, LocalServerDelegate.WicketStyleURLConstructor.INSTANCE);
	}
	
	/**
	 * Sends a <code>ping</code> message, whose response will be an image.
	 * 
	 * @param privateKey the private key
	 * @param publicKey the public key
	 * @param h handles the response.
	 */
	protected final void sendPing(String privateKey, String sharedKey, Handler h) {
	    Map<String,String> args = new HashMap<String,String>();
        args.put(LWSDispatcherSupport.Parameters.PRIVATE, privateKey);
        args.put(LWSDispatcherSupport.Parameters.SHARED, sharedKey);
        args.put(LWSDispatcherSupport.Parameters.CALLBACK, "_dummy");
	    sendLocalMsg(LWSDispatcherSupport.Commands.PING, args, h);
	}
	
	private String removeHeaders(String response) {
		if (response == null) return null;
		//
		// Search for two NEWLINEs
		// This totally sucks, but it's just testing
		//
		String target = "\n\n";
		int itarget = response.indexOf(target);
		String res = itarget == -1 ? response : response.substring(itarget+target.length());
		res = res.trim();
		return res;
	}
}
