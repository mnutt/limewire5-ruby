package org.limewire.facebook.service;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.facebook.service.livemessage.LiveMessageHandler;
import org.limewire.facebook.service.livemessage.LiveMessageHandlerRegistry;
import org.limewire.facebook.service.settings.FacebookAppID;
import org.limewire.facebook.service.settings.FacebookReportBugs;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.MessageReader;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.util.ExceptionUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;

/**
 * This listens for new chat messages and live messages, and dispatches them to the
 * appropriate handlers.  It does this via http polling, where the requests are long running (COMET style).
 */
public class ChatListener implements Runnable {
    
    private static final Log LOG = LogFactory.getLog(org.limewire.facebook.service.ChatListener.class);
    
    private final FacebookFriendConnection connection;
    private final LiveMessageHandlerRegistry handlerRegistry;
    private final Provider<String> facebookAppID;

    private final ChatManager chatManager;
    private final String uid;
    private final String channel;
    private int seq;
    
    private volatile boolean done;

    private final Provider<Boolean> reportBugs;

    @Inject
    ChatListener(@Assisted FacebookFriendConnection connection,
                 LiveMessageHandlerRegistry handlerRegistry,
                 @FacebookAppID Provider<String> facebookAppID,
                 @FacebookReportBugs Provider<Boolean> reportBugs) {
        this.connection = connection;
        this.handlerRegistry = handlerRegistry;
        this.facebookAppID = facebookAppID;
        this.reportBugs = reportBugs;
        this.seq = -1;
        this.uid = connection.getUID();
        this.channel = connection.getChannel();
        this.chatManager = connection.getChatManager();
    }

    void setDone() {
        this.done = true;
    }

    @Override
    public void run() {
        done = false;
        try {
            seq = getSeq();
        } catch(IOException e1){
            LOG.debug("error getting initial sequence number", e1);
            connection.logout();
        } catch(JSONException e1){
            LOG.debug("error parsing initial sequence number", e1);
        }

        int currentSeq;
        while(!done) {
            try {
                currentSeq = getSeq();
                if(seq > currentSeq) {
                    seq = currentSeq;
                }
                
                while(seq <= currentSeq && !done) {
                    //get the old message between oldseq and seq
                    String msgResponseBody = connection.httpGET(getMessageRequestingUrl(seq));
                    if(msgResponseBody != null) {
                        String prefix = "for (;;);";
                        if(msgResponseBody.startsWith(prefix)) {
                            msgResponseBody = msgResponseBody.substring(prefix.length());
                        }
                        JSONObject response = FacebookUtils.parse(msgResponseBody);
                        LOG.debugf("message: {0}", response);
                        if(response.getString("t").equals("msg")) {
                            dispatchMessage(response);
                        } else if(response.getString("t").equals("refresh")) {
                            connection.reconnect();
                        }
                        //seq++;
                    }                        
                    seq++;
                }
            } catch (IOException e) {
                LOG.debug("error getting chat message", e);
                if(!done) {
                    connection.logout();
                }
                return;
            } catch (JSONException e) {
                LOG.debug("error parsing chat message", e);
                // only report exceptions if thread is not done yet
                if (!done && reportBugs.get()) {
                    ExceptionUtils.reportOrReturn(e);
                }
            }
        }
        LOG.debug("chat listener is done");
    }

    private void dispatchMessage(JSONObject message) throws JSONException {
        if(message.has("ms")) {
            JSONArray ms = message.getJSONArray("ms");
            final JSONObject payload = ms.getJSONObject(0);
            String msgType = payload.getString("type");
            String appId = payload.optString("app_id", "");
            
            if("app_msg".equals(msgType) && appId.equals(facebookAppID.get())) {
                processLiveMessage(payload);
            } else if ("msg".equals(msgType) || "typ".equals(msgType)) {
                // disable processing of chat messages
//                processChatMessage(payload, msgType);
            } else {
                LOG.debugf("unhandled payload: {0}", payload.toString());
            }
        } else {
            LOG.debugf("unhandled message: {0}", message.toString());    
        }
    }

    private void processLiveMessage(JSONObject payload) throws JSONException {
        if (!payload.has("response")) {
            LOG.debugf("no 'response' in message payload: {0}", payload);
            return;
        }
        
        JSONObject lwMessage = payload.getJSONObject("response");
        
        String presenceId = lwMessage.getString("from");
        connection.addPresence(presenceId);
            
        String to = lwMessage.getString("to");
        if (!to.equals(connection.getPresenceId())) {
            LOG.debugf("message not for us: {0}", payload);
            return;
        }
        
        String messageType = payload.getString("event_name");
        LiveMessageHandler handler = handlerRegistry.getHandler(messageType);
        if (handler != null) {
            handler.handle(messageType, lwMessage);
        } else {
            LOG.debugf("no handler for type: {0}", messageType);
        }
    }

    private void processChatMessage(JSONObject payload, String msgType) throws JSONException {
        String parsedSenderId = payload.getString("from");

        // look up the MessageReader based on the sender (friend) id
        if (parsedSenderId != null && !parsedSenderId.equals(uid)) {
            connection.addPresence(parsedSenderId);
            MessageReader handler = chatManager.getMessageReader(parsedSenderId);

            if (handler != null) {
                if (msgType.equals("msg")) {
                    JSONObject messageJson = payload.getJSONObject("msg");
                    String msg = messageJson.getString("text");
                    handler.readMessage(msg);
                } else {
                    ChatState state = payload.getInt("st") == 1 ? ChatState.composing : ChatState.active;
                    handler.newChatState(state);
                }
            } else {
                // this can happen, when we just signed off and removed all friend presences
                // but the friend is currently typing to us an the connection is not
                // closed yet, it's ok in this case
                LOG.debugf("no handler for sender: {0}", parsedSenderId);
            }
        } else {
            if(parsedSenderId == null) {
                LOG.debugf("no 'from' in message payload: {0}", payload);
            } else if(parsedSenderId.equals(uid)){
                LOG.debugf("ignoring chat message sent from logged in user: {0}", payload);
            } else {
                LOG.debugf("dropped chat message: {0}", payload);
            }
        }
    }

    private int getSeq() throws IOException, JSONException {
        for (int i = 0; i < 3; i++) {
            String seqResponseBody = connection.httpGET(getMessageRequestingUrl(-1));
            if (seqResponseBody == null) {
                LOG.debug("null response for seq");
                continue;
            }
            int sequenceNumber = parseSeq(seqResponseBody);
            if(sequenceNumber >= 0){
                return sequenceNumber;
            }
            try {
                LOG.debug("retrying to retrieve the seq code after 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOG.debug(e.getMessage(), e);
            }
        }
        throw new IOException("could not parse sequence number"); 
    }
    
    private String getMessageRequestingUrl(long seq) {
        return "http://0.channel" + channel + ".facebook.com/x/0/false/p_" + uid + "=" + seq;
    }
    
    /**
     * @return -1 if sequence number could not be parsed
     */
    private int parseSeq(String msgResponseBody) throws JSONException, IOException {
        LOG.debugf("parsing seq from: {0}", msgResponseBody);
        //for (;;);{"t":"refresh", "seq":0}
        String prefix = "for (;;);";
        if(msgResponseBody.startsWith(prefix)) {
            msgResponseBody = msgResponseBody.substring(prefix.length());
        }
        
        JSONObject body = FacebookUtils.parse(msgResponseBody);
        if(body.has("seq")) {
            return body.getInt("seq");
        } else if(body.has("t") && body.getString("t").equals("refresh")) {
            LOG.debug("refreshing post form id");
            connection.reconnect(); 
            return -1;
        }
        else {
            return -1;
        }
    }
}
