package org.limewire.facebook.service;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.limewire.concurrent.ListeningFuture;
import org.limewire.concurrent.ScheduledListeningExecutorService;
import org.limewire.concurrent.ThreadExecutor;
import org.limewire.facebook.service.Facebook;
import org.limewire.facebook.service.livemessage.AddressHandler;
import org.limewire.facebook.service.livemessage.AddressHandlerFactory;
import org.limewire.facebook.service.livemessage.AuthTokenHandler;
import org.limewire.facebook.service.livemessage.AuthTokenHandlerFactory;
import org.limewire.facebook.service.livemessage.ConnectBackRequestHandler;
import org.limewire.facebook.service.livemessage.ConnectBackRequestHandlerFactory;
import org.limewire.facebook.service.livemessage.DiscoInfoHandler;
import org.limewire.facebook.service.livemessage.DiscoInfoHandlerFactory;
import org.limewire.facebook.service.livemessage.FileOfferHandler;
import org.limewire.facebook.service.livemessage.FileOfferHandlerFactory;
import org.limewire.facebook.service.livemessage.LibraryRefreshHandler;
import org.limewire.facebook.service.livemessage.LibraryRefreshHandlerFactory;
import org.limewire.facebook.service.settings.ChatChannel;
import org.limewire.facebook.service.settings.FacebookAPIKey;
import org.limewire.facebook.service.settings.FacebookAuthServerUrls;
import org.limewire.friend.api.ChatState;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendConnection;
import org.limewire.friend.api.FriendConnectionConfiguration;
import org.limewire.friend.api.FriendConnectionEvent;
import org.limewire.friend.api.FriendException;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.FriendPresenceEvent;
import org.limewire.friend.api.IncomingChatListener;
import org.limewire.friend.api.MessageReader;
import org.limewire.friend.api.MessageWriter;
import org.limewire.friend.api.MutableFriendManager;
import org.limewire.friend.api.Network;
import org.limewire.friend.api.feature.AddressFeature;
import org.limewire.friend.api.feature.AuthTokenFeature;
import org.limewire.friend.api.feature.ConnectBackRequestFeature;
import org.limewire.friend.api.feature.FeatureEvent;
import org.limewire.friend.api.feature.FileOfferFeature;
import org.limewire.friend.api.feature.LibraryChangedNotifierFeature;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.friend.impl.address.FriendAddress;
import org.limewire.friend.impl.util.PresenceUtils;
import org.limewire.http.httpclient.HttpClientUtils;
import org.limewire.inject.MutableProvider;
import org.limewire.listener.AsynchronousEventBroadcaster;
import org.limewire.listener.EventBroadcaster;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.security.SecurityUtils;

import com.google.code.facebookapi.FacebookException;
import com.google.code.facebookapi.FacebookJsonRestClient;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;

/**
 * Implements a {@link FriendConnection} using facebook.
 * <p>
 * There is no actual TCP connection kept with the facebook server. The connection
 * object keeps all the state necessary to send facebook api calls to the facebook
 * servers and also to listen for incoming chat messages.
 */
public class FacebookFriendConnection implements FriendConnection {

    private static final Log LOG = LogFactory.getLog(FacebookFriendConnection.class);

    /**
     * Maximum number of consecutive HTTP GET and POST tries before IOException
     * is thrown.
     * The value will be changed to 1, once n tries failed consecutively.
     */
    private volatile int maxTries = 2;
    
    private static final String HOME_PAGE = "http://www.facebook.com/home.php";
    private static final String PRESENCE_POPOUT_PAGE = "http://www.facebook.com/presence/popout.php";
    private static final String FACEBOOK_CHAT_SETTINGS_URL = "https://www.facebook.com/ajax/chat/settings.php?";
    private static final String FACEBOOK_RECONNECT_URL = "http://www.facebook.com/ajax/presence/reconnect.php?reason=3";
    private static final String USER_AGENT_HEADER = "User-Agent: Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.9.0.10) Gecko/2009042316 Firefox/3.0.10";

    private final FriendConnectionConfiguration configuration;
    private final Provider<String> apiKey;
    private final ChatListenerFactory chatListenerFactory;
    private final ScheduledListeningExecutorService executorService;
    private final MutableProvider<String> chatChannel;
    private final AtomicBoolean loggedIn = new AtomicBoolean(false);
    private final AtomicBoolean loggingIn = new AtomicBoolean(false);
    private final AsynchronousEventBroadcaster<FriendConnectionEvent> connectionBroadcaster;
    private final Map<String, FacebookFriend> friends = Collections.synchronizedMap(new TreeMap<String, FacebookFriend>(String.CASE_INSENSITIVE_ORDER));
    private final BasicCookieStore cookieStore = new BasicCookieStore();
    private final AtomicReference<String> postFormID = new AtomicReference<String>();

    /**
     * Lock being held for adding and removing presences from friends.
     */
    private final Object presenceLock = new Object();

    private final EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster;
    private final AddressHandler addressHandler;
    private final AuthTokenHandler authTokenHandler;
    private final LibraryRefreshHandler libraryRefreshHandler;
    private final ConnectBackRequestHandler connectBackRequestHandler;
    private final FileOfferHandler fileOfferHandler;
    private final EventBroadcaster<FeatureEvent> featureEventBroadcaster;


    /**
     * Adapt connection configuration to ensure the facebook user id is returned in
     * {@link Network#getCanonicalizedLocalID()}.
     */
    private final Network network = new Network() {
        @Override
        public String getCanonicalizedLocalID() {
            return uid;
        }
        @Override
        public String getNetworkName() {
            return getConfiguration().getNetworkName();
        }
        @Override
        public Type getType() {
            return Type.FACEBOOK;
        }
    };
    private final MutableFriendManager friendManager;
    private final PresenceListenerFactory presenceListenerFactory;
    private final FacebookFriendFactory friendFactory;
    private final DiscoInfoHandlerFactory discoInfoHandlerFactory;
    private final ChatManager chatManager;
    /**
     * Session id as part of the full presence id. Follows the lifetime of
     * the connection object. A new connection object should have a new
     * session id.
     */
    private final String sessionId;

    private FacebookJsonRestClient facebookClient;
    private ChatListener chatListener;
    private ScheduledFuture presenceListenerFuture;
    private String logoutURL;
    private DiscoInfoHandler discoInfoHandler;
    private String session;
    private String uid;
    private String secret;

    private final Provider<String[]> authUrls;
    private final ClientConnectionManager httpConnectionManager;

    @Inject
    public FacebookFriendConnection(@Assisted FriendConnectionConfiguration configuration,
                                    @FacebookAPIKey Provider<String> apiKey,
                                    AsynchronousEventBroadcaster<FriendConnectionEvent> connectionBroadcaster,
                                    EventBroadcaster<FeatureEvent> featureEventBroadcaster,
                                    EventBroadcaster<FriendPresenceEvent> friendPresenceBroadcaster,
                                    MutableFriendManager friendManager,
                                    AddressHandlerFactory addressHandlerFactory,
                                    AuthTokenHandlerFactory authTokenHandlerFactory,
                                    ConnectBackRequestHandlerFactory connectBackRequestHandlerFactory,
                                    LibraryRefreshHandlerFactory libraryRefreshHandlerFactory,
                                    FileOfferHandlerFactory fileOfferHandlerFactory,
                                    PresenceListenerFactory presenceListenerFactory,
                                    FacebookFriendFactory friendFactory,
                                    ChatListenerFactory chatListenerFactory,
                                    DiscoInfoHandlerFactory discoInfoHandlerFactory,
                                    @Facebook ScheduledListeningExecutorService executorService,
                                    @ChatChannel MutableProvider<String> chatChannel,
                                    @FacebookAuthServerUrls Provider<String[]> authUrls,
                                    @Named("sslConnectionManager") ClientConnectionManager httpConnectionManager) {
        this.configuration = configuration;
        this.apiKey = apiKey;
        this.connectionBroadcaster = connectionBroadcaster;
        this.featureEventBroadcaster = featureEventBroadcaster;
        this.friendPresenceBroadcaster = friendPresenceBroadcaster;
        this.friendManager = friendManager;
        this.presenceListenerFactory = presenceListenerFactory;
        this.friendFactory = friendFactory;
        this.chatListenerFactory = chatListenerFactory;
        this.executorService = executorService;
        this.chatChannel = chatChannel;
        this.authUrls = authUrls;
        this.httpConnectionManager = httpConnectionManager;
        this.addressHandler = addressHandlerFactory.create(this);
        this.authTokenHandler = authTokenHandlerFactory.create(this);
        this.connectBackRequestHandler = connectBackRequestHandlerFactory.create(this);
        this.libraryRefreshHandler = libraryRefreshHandlerFactory.create(this);
        this.fileOfferHandler = fileOfferHandlerFactory.create(this);
        this.discoInfoHandlerFactory = discoInfoHandlerFactory;
        this.chatManager = new ChatManager(this);
        this.sessionId = createSessionId();

        for (Cookie cookie : parseCookies(configuration)) {
            cookieStore.addCookie(cookie);
        }
    }

    private static String createSessionId() {
        byte[] sessionId = new byte[8];
        SecurityUtils.createSecureRandomNoBlock().nextBytes(sessionId);
        return org.limewire.util.StringUtils.getUTF8String(Base64.encodeBase64(sessionId));
    }

    private void setPostFormID(String postFormID) {
        this.postFormID.set(postFormID);
    }

    @Override
    public ListeningFuture<Void> login() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                loginImpl();
                return null;
            }
        });
    }

    @Override
    public ListeningFuture<Void> logout() {
        return executorService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                logoutImpl();
                return null;
            }
        });
    }

    synchronized void logoutImpl() {
        closeConnection(true);
        connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.DISCONNECTED));
    }

    /**
     * Close the connection by logging out of facebook and cleaning up objects
     * associated with this connection.
     *
     * @param shouldCleanUpFacebookClient true if we need to or should
     * attempt to expire our facebook JSON client. This should be
     * false when we know for sure the facebook session is invalid.
     */
    private void closeConnection(boolean shouldCleanUpFacebookClient) {
        LOG.debug("logging out from facebook...");
        loggedIn.set(false);
        loggingIn.set(false);

        // clean up data structures associated with this connection
        cancelListeners();
        
        // over-the-network logout activities
        endChatSession(shouldCleanUpFacebookClient);
        
        // remove all friends
        synchronized (friends) {
            for (FacebookFriend friend : friends.values()) {
                removeAllPresences(friend);
            }
            friends.clear();
        }

        LOG.debug("logged out from facebook.");
    }
    
    public void reconnect() throws IOException {
        LOG.debug("reconnecting...");
        String response = httpGET(FACEBOOK_RECONNECT_URL + "&post_form_id=" + postFormID.get()); 
        LOG.debugf("reconnect response: {0}", response);
    }

    /**
     * Performs all network related steps to ending the chat, such as
     * signing out of facebook website, sending messages to other
     * presences to let them know we are going offline, etc.
     *
     * @param shouldCleanUpFacebookClient same as in {@link #closeConnection}
     */
    private void endChatSession(boolean shouldCleanUpFacebookClient) {

        if (shouldCleanUpFacebookClient) {
            sendOfflinePresences();
            try {
                expireSession();
            } catch (FacebookException e) {
                LOG.debug("error expiring facebook session", e);
            } catch (IOException e) {
                LOG.debug("error expiring facebook session", e);
            }
        }

        // todo: what to do in case of error?  what are the repercussions of not logging out of fb
        try {
            logoutFromFacebook();
        } catch (IOException e) {
            LOG.debug("logout from facebook failed", e);
        }
    }

    /**
     * Cancels presence listenting thread, chat listener, thread, and other listeners
     */
    private void cancelListeners() {
        // stop and remove essential listeners/handlers
        if (chatListener != null) {
            chatListener.setDone();
            chatListener = null;
        }
        if (presenceListenerFuture != null) {
            presenceListenerFuture.cancel(false);
            presenceListenerFuture = null;
        }
        if (discoInfoHandler != null) {
            discoInfoHandler.unregister();
            discoInfoHandler = null;
        }
    }

    private void sendOfflinePresences() {
        for(FacebookFriend friend : friends.values()) {
            for (FriendPresence presence : friend.getPresences().values()) {
                if (presence.hasFeatures(LimewireFeature.ID)) {
                    Map<String, Object> message = new HashMap<String, Object>();
                    message.put("type", "unavailable");

                    try {
                        sendLiveMessageDirect(presence, "presence", message);
                    } catch (FacebookException e) {
                        LOG.debug("error sending offline presence notification", e);
                    } catch (IOException e) {
                        LOG.debug("error sending offline presence notification", e);
                    }
                }
            }
        }
    }

    private void expireSession() throws FacebookException, IOException {
        synchronized (this) {
            if(facebookClient != null) {
                try {
                    facebookClient.auth_expireSession();
                } catch (RuntimeException re) {
                    handleFacebookAPIRuntimeException(re);
                }
            }
        }
    }

    private void handleFacebookAPIRuntimeException(RuntimeException re) throws IOException {
        //LWC-3678
        if(re.getCause() == null || !(re.getCause() instanceof IOException)) {
            throw re;
        } else {
            throw (IOException)re.getCause();
        }
    }

    private void logoutFromFacebook() throws IOException {
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("confirm", "1"));
        URL logout = new URL(logoutURL);
        String logouthost = logout.getProtocol() + "://" + logout.getHost();
        String logoutpath = logout.getPath();
        httpPOST(logouthost, logoutpath, nvps);
    }

    @Override
    public boolean isLoggedIn() {
        return loggedIn.get();
    }

    @Override
    public boolean isLoggingIn() {
        return loggingIn.get();
    }

    void loginImpl() throws FriendException {
        synchronized (this) {
            try {
                loggingIn.set(true);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTING));
                requestSession();
                fetchAllFriends();
                readMetadataFromPages();
                discoInfoHandler = discoInfoHandlerFactory.create(this);
                chatListener = chatListenerFactory.createChatListener(this);
                ThreadExecutor.startThread(chatListener, "chat-listener-thread");
                setVisible();
                PresenceListener presenceListener = presenceListenerFactory.createPresenceListener(this);
                presenceListenerFuture = executorService.scheduleWithFixedDelay(presenceListener, 0, 60, TimeUnit.SECONDS);
                loggedIn.set(true);
                loggingIn.set(false);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECTED));
            } catch (IOException e) {
                LOG.debug("login error", e);
                closeConnection(true);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw new FriendException(e);
            } catch (JSONException e) {
                LOG.debug("login error", e);
                closeConnection(true);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw new FriendException(e);
            } catch (RuntimeException e) {
                LOG.debug("unexpected login error; probable bug", e);
                closeConnection(true);
                connectionBroadcaster.broadcast(new FriendConnectionEvent(this, FriendConnectionEvent.Type.CONNECT_FAILED, e));
                throw e;
            } 
        }
    }
    
    /**
     * Sets this facebook user visible for chat.
     */ 
    private void setVisible() throws IOException {
        HttpPost httpPost = new HttpPost(FACEBOOK_CHAT_SETTINGS_URL);
        httpPost.addHeader("User-Agent", USER_AGENT_HEADER);

        List <NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("visibility", "true"));
        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }

        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

        HttpClient httpClient = createHttpClient();
        HttpResponse response = httpClient.execute(httpPost);
        HttpClientUtils.releaseConnection(response);
    }

    String getUID() {
        return uid;
    }

    /**
     * Fetches all friends and adds them as known friends.
     */
    private void fetchAllFriends() throws IOException {
        try {
            JSONArray friends = null;

            try {
                friends = facebookClient.friends_get();
            } catch (RuntimeException re) {
                handleFacebookAPIRuntimeException(re);
            }

            // friends is null when i have no friends
            if (friends == null) {
                return;
            }
            List<Long> friendIds = new ArrayList<Long>(friends.length());
            for (int i = 0; i < friends.length(); i++) {
                friendIds.add(friends.getLong(i));
            }
            JSONArray users = new JSONArray();
            try {
                users = (JSONArray) facebookClient.users_getInfo(friendIds, new HashSet<CharSequence>(Arrays.asList("uid", "first_name", "name", "status")));
            } catch (RuntimeException re) {
                handleFacebookAPIRuntimeException(re);
            }
            Set<String> limeWireFriends = fetchLimeWireFriends();
            LOG.debugf("all friends: {0}", users);
            for (int i = 0; i < users.length(); i++) {
                JSONObject user = users.getJSONObject(i);
                String id = user.getString("uid");
                FacebookFriend friend = friendFactory.create(id, user,
                        getNetwork(), limeWireFriends.contains(id), this);
                LOG.debugf("adding {0}", friend);
                addKnownFriend(friend);
            }
        } catch (FacebookException e) {
            LOG.debug("friend error", e);
            throw new IOException(e);
        } catch (JSONException e) {
            LOG.debug("json error", e);
            throw new RuntimeException("FIX ME!", e);
        }
    }

    /**
     * Fetches friend ids that have the LimeWire application installed
     * and marks the existing friends as LimeWire capable.
     */
    private Set<String> fetchLimeWireFriends() throws FacebookException, IOException {
        JSONArray limeWireFriendIds;
        try {
            Set<String> limeWireIds = new HashSet<String>();
            Object friends = null;
            try {
                friends = facebookClient.friends_getAppUsers();
            } catch (RuntimeException re) {
                handleFacebookAPIRuntimeException(re);
            }
            if(friends instanceof JSONArray) { // is JSONObject when user has no friends with LW installed
                limeWireFriendIds = (JSONArray)friends;
                LOG.debugf("limewire friends: {0}", limeWireFriendIds);
                for (int i = 0; i < limeWireFriendIds.length(); i++) {
                    limeWireIds.add(limeWireFriendIds.getString(i));
                }
            }
            return limeWireIds;
        } catch (JSONException e) {
            throw new RuntimeException("FIX ME!",e);
        }
    }

    private void requestSession() throws IOException, JSONException {
        String authToken = (String)configuration.getAttribute("auth-token");
        String authUrl = FacebookUtils.getRandomElement(authUrls.get()) + "getsession/" + authToken + "/";
        LOG.debugf("requesting session from {0}...", authUrl);
        HttpGet sessionRequest = new HttpGet(authUrl);
        // keep alive in-between getToken and getSession. Close after.
        sessionRequest.addHeader("Connection", "close");
        HttpClient httpClient = createHttpClient();
        HttpResponse response = httpClient.execute(sessionRequest);
        parseSessionResponse(response);
        HttpClientUtils.releaseConnection(response);
    }

    private void parseSessionResponse(HttpResponse response) throws IOException, JSONException {
        String responseBody = EntityUtils.toString(response.getEntity());
		JSONObject json = new JSONObject(responseBody);
        session = json.getString("session_key");
        secret = json.getString("secret");
        uid = json.getString("uid");
        LOG.debugf("received session {0}, secret {1}, uid: {2}", session, secret, uid);
        facebookClient = new FacebookJsonRestClient(apiKey.get(), secret, session);
    }

    public void readMetadataFromPages() throws IOException {
        String homePage = httpGET(HOME_PAGE);

        if(homePage == null){
            throw new IOException("no response");
        }
        if(uid == null){
            throw new IOException("no uid");
        }

        readLogoutURL(homePage);

        String presencePopoutPage = httpGET(PRESENCE_POPOUT_PAGE);
        readChannel(presencePopoutPage);
        readPOSTFormID(presencePopoutPage);
    }

    // the logout url is dynamic - it contains a few query params that seem to change across sessions.
    // read the logout url here
    private void readLogoutURL(String homePage) throws IOException {
        String logoutURLPrefix = "<a href=\"http://www.facebook.com/logout.php?";
        int logoutURLBeginPos = homePage.indexOf(logoutURLPrefix);
        int logoutURLEndPos = homePage.indexOf("\">", logoutURLBeginPos);
        if (logoutURLBeginPos < 0){
            LOG.debugf("logout url not in homepage: {0}", homePage);
            throw new IOException("can't find logout URL");
        }
        else {
            logoutURL = homePage.substring(logoutURLBeginPos + "<a href=\"".length(),
                    logoutURLEndPos);
        }
    }

    // the post_form_id is a hidden input to the form used for posting chat messages.
    // Its value is dynamic and changes across sessions, sometimes during a session.
    // We read the value here.
    private void readPOSTFormID(String homePage) throws IOException {
        String post_form_id;
        String postFormIDPrefix = "<input type=\"hidden\" id=\"post_form_id\" name=\"post_form_id\" value=\"";
        int formIdBeginPos = homePage.indexOf(postFormIDPrefix)
                + postFormIDPrefix.length();
        if (formIdBeginPos < postFormIDPrefix.length()){
            throw new IOException("can't find post form id");
        }
        else {
            post_form_id = homePage.substring(formIdBeginPos,
                    formIdBeginPos + 32);
        }

        setPostFormID(post_form_id);
    }

    // There is a pool of chat servers.  Each user is assigned a single chat server forever (it appears).
    // That server is identified by a number called a "channel".
    // We read its value here.
    private void readChannel(String page) throws IOException {
        String channel;
        String channelPrefix = " \"channel";
        int channelBeginPos = page.indexOf(channelPrefix)
                + channelPrefix.length();
        if (channelBeginPos < channelPrefix.length()){
            channel = chatChannel.get();
            // no cached value
            if (channel.length() == 0) {
                throw new IOException("can't find channel");
            }
            LOG.debugf("using cached channel: {0}", channel);
        }
        else {
            channel = page.substring(channelBeginPos,
                    channelBeginPos + 2);
            chatChannel.set(channel);
        }
    }

    public String httpGET(String url) throws IOException {
        LOG.debugf("facebook GET: {0}", url);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("User-Agent", USER_AGENT_HEADER);
        httpGet.addHeader("Connection", "close");
        return executeRequest(httpGet);
    }

    /**
     * Executes the http request potentially several times catching any 
     * {@link IOException} occurring and sleeping between requests to make
     * failure less likely the next time.
     */
    private String executeRequest(HttpUriRequest request) throws IOException {
        for (int i = 0; i < maxTries; i++) {
            HttpClient httpClient = createHttpClient();
            try {
                HttpResponse postResponse = httpClient.execute(request);
                HttpEntity entity = postResponse.getEntity();
                if (entity != null) {
                    String responseStr = EntityUtils.toString(entity);
                    HttpClientUtils.releaseConnection(postResponse);
                    return responseStr;
                } else {
                    return null;
                }
            } catch (IOException ie) {
                // throw exception if max tries have been done
                if (i == maxTries - 1) {
                    maxTries = 1;
                    LOG.debug("all tries failed", ie);
                    throw ie;
                } else {
                    LOG.debug("ignoring intermittent error", ie);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
        return null;
    }
    
    /**
     *
     * @return null if there is no response data
     */
    public String httpPOST(String host, String urlPostfix, List <NameValuePair> nvps) throws IOException {
        LOG.debugf("facebook POST: {0}", host + urlPostfix);
        HttpPost httpPost = new HttpPost(host + urlPostfix);
        httpPost.addHeader("Connection", "close");
        httpPost.addHeader("User-Agent", USER_AGENT_HEADER);
        httpPost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        return executeRequest(httpPost);
    }

    @Override
    public FriendConnectionConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public boolean supportsMode() {
        return false;
    }

    @Override
    public ListeningFuture<Void> setMode(FriendPresence.Mode mode) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean supportsAddRemoveFriend() {
        return false;
    }

    @Override
    public ListeningFuture<Void> addNewFriend(String id, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListeningFuture<Void> removeFriend(String id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FacebookFriend getFriend(String id) {
        return friends.get(id);
    }

    @Override
    public Collection<Friend> getFriends() {
        synchronized (friends) {
            return new ArrayList<Friend>(friends.values());
        }
    }

    public String getChannel() {
        return chatChannel.get();
    }

    public String getPresenceId() {
        return uid + "/" + configuration.getResource() + sessionId;
    }

    ChatManager getChatManager() {
        return chatManager;
    }

    public void sendLiveMessage(final FriendPresence presence,
                                final String type, final Map<String, Object> messageMap) {

        executorService.submit(new Runnable() {
            public void run() {
                synchronized (FacebookFriendConnection.this) {
                    try {
                        sendLiveMessageDirect(presence, type, messageMap);
                    }
                    catch (FacebookException e) {
                        LOG.debug("Error sending live message:", e);

                        if (loggedIn.get()) {
                            closeConnection(false);
                            // ok to broadcast. connectionBroadcaster must be async broadcaster
                            FriendException fe = new FriendException("chat session expired", e);
                            connectionBroadcaster.broadcast(new FriendConnectionEvent(
                                    FacebookFriendConnection.this, FriendConnectionEvent.Type.DISCONNECTED, fe));
                        }
                    } catch (IOException e) {
                        LOG.debug("Error sending live message:", e);
                        // TODO logout?
                    }
                }
            }
        });
    }

    private void sendLiveMessageDirect(FriendPresence presence,
                                       String type, Map<String, Object> messageMap) throws FacebookException, IOException {
        messageMap.put("to", presence.getPresenceId());
        messageMap.put("from", getPresenceId());
        final Long userId = Long.parseLong(presence.getFriend().getId());

        JSONObject message = new JSONObject(messageMap);
        LOG.debugf("live message {0} to {1} : {2}", type, userId, message);
        try {
            facebookClient.liveMessage_send(userId, type, message);
        } catch (RuntimeException re) {
            handleFacebookAPIRuntimeException(re);
        }
    }

    void sendChatMessage(String friendId, String message) throws FriendException {
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("msg_text", (message == null)? "":message));
        nvps.add(new BasicNameValuePair("msg_id", (int)(Math.random() * 1000000000) + ""));
        nvps.add(new BasicNameValuePair("client_time", System.currentTimeMillis() + ""));
        nvps.add(new BasicNameValuePair("to", friendId));

        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }
        try {
            String resp = httpPOST("http://www.facebook.com", "/ajax/chat/send.php", nvps);
            handleChatResponseError(friendId, resp);
        } catch (IOException e) {
            throw new FriendException(e);
        }
    }

    /**
     * Sends a chat state update to a friend.
     * <p>
     * Side effect: If the friend is offline, all presences of the friend are
     * removed and he's no longer available.
     *
     * @return true if the friend is online, false otherwise
     */
    boolean sendChatStateUpdate(String friendId, ChatState state) throws FriendException {
        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        nvps.add(new BasicNameValuePair("typ", (state == ChatState.composing)? "1" : "0"));
        nvps.add(new BasicNameValuePair("to", friendId));

        String post_form_id = postFormID.get();
        if(post_form_id != null) {
            nvps.add(new BasicNameValuePair("post_form_id", post_form_id));
        }
        try {
            String resp = httpPOST("http://www.facebook.com", "/ajax/chat/typ.php", nvps);
            return handleChatResponseError(friendId, resp);
        } catch (IOException e) {
            LOG.debug("error sending chat update", e);
            throw new FriendException(e);
        }
    }

    /**
     * Sends a chat state update which causes a friend to be removed from available
     * friends if he's no longer online. This is a blocking call.
     *
     * @return false if the friend is offline, true otherwise
     */
    public boolean sendFriendIsOnline(String friendId) throws FriendException {
        return sendChatStateUpdate(friendId, ChatState.active);
    }

    /**
     * @return false if the friend is offline, otherwise true, also true in other
     * error cases
     */
    private boolean handleChatResponseError(String friendId, String response) {
        String prefix = "for (;;);";
        if (response.startsWith(prefix)) {
            response = response.substring(prefix.length());
        }
        try {
            JSONObject json = FacebookUtils.parse(response);
            int error = json.getInt("error");
            if (error == 1356003) {
                LOG.debugf("friend offline: {0}, full response: {1}", friendId, response);
                FacebookFriend friend = getFriend(friendId);
                if (friend != null) {
                    removeAllPresences(friend);
                } else {
                    LOG.debug("friend already removed");
                }
                return false;
            } else if (error != 0) {
                LOG.debugf("unhandled error: {0}", response);
            }
        } catch (JSONException e) {
            LOG.debugf(e, "error parsing chat response {0}", response);
        }
        return true;
    }

    private Network getNetwork() {
        return network;
    }

    private HttpClient createHttpClient() {
        DefaultHttpClient httpClient = new DefaultHttpClient(httpConnectionManager, null);
        httpClient.setCookieStore(cookieStore);
        cookieStore.clearExpired(new Date());
        return httpClient;
    }


    /**
     * Adds a friend to connection and friend manager.
     */
    void addKnownFriend(FacebookFriend friend) {
        if(loggedIn.get() || loggingIn.get()) {
            String friendId = friend.getId();
            boolean added = false;
            synchronized (friends) {
                if (!friends.containsKey(friendId)) {
                    friends.put(friendId, friend);
                    added = true;
                }
            }
            if (added) {
                friendManager.addKnownFriend(friend);
            }
        }
    }

    /**
     * Creates a <code>presence</code> for a <code>friend</code> if the friend doesn't have
     * a presence yet. If that's the case also notifies friend manager that the
     * friend is available now.
     *
     */
    public void addPresence(String presenceId) {
        FacebookFriendPresence newPresence = null;
        synchronized (presenceLock) {
            String friendId = PresenceUtils.parseBareAddress(presenceId);
            FacebookFriend facebookFriend = getFriend(friendId);
            if(facebookFriend != null) {
                Map<String, FriendPresence> presences = facebookFriend.getPresences();
                if(!presences.containsKey(presenceId)) {
                    // remove old presence with same resource prefix
                    String newResourcePrefix = FriendAddress.parseIdPrefix(presenceId);
                    for (String id : presences.keySet()) {
                        String fullResource = PresenceUtils.parseResource(id);
                        if (fullResource.startsWith(newResourcePrefix)) {
                            LOG.debugf("found old presence to replace: {0}, new id: {1}", id, presenceId);
                            removePresence(id);
                        }
                    }
                    boolean firstPresence = facebookFriend.getPresences().isEmpty();
                    LOG.debugf("new friend is available: {0}", presenceId);
                    newPresence = new FacebookFriendPresence(presenceId, facebookFriend, featureEventBroadcaster);
                    if (facebookFriend.hasLimeWireAppInstalled()) {
                        addTransports(newPresence);
                    }
                    facebookFriend.addPresence(newPresence);
                    if(firstPresence) {
                        friendManager.addAvailableFriend(facebookFriend);
                    }
                    friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(newPresence, FriendPresenceEvent.Type.ADDED));
                }
            } else {
                LOG.debugf("friend not known yet: {0}", presenceId);
            }
        }
    }

    private void addTransports(FacebookFriendPresence presence) {
        presence.addTransport(AddressFeature.class, addressHandler);
        presence.addTransport(AuthTokenFeature.class, authTokenHandler);
        presence.addTransport(ConnectBackRequestFeature.class, connectBackRequestHandler);
        presence.addTransport(LibraryChangedNotifierFeature.class, libraryRefreshHandler);
        presence.addTransport(FileOfferFeature.class, fileOfferHandler);
    }

    public void removePresence(String presenceId) {
        synchronized (presenceLock) {
            String friendId = PresenceUtils.parseBareAddress(presenceId);
            FacebookFriend facebookFriend = getFriend(friendId);
            if(facebookFriend != null) {
                FriendPresence presence = facebookFriend.getPresences().get(presenceId);
                if(presence != null) {
                    LOG.debugf("removing presence {0}", presence.getPresenceId());
                    FacebookFriendPresence facebookFriendPresence = (FacebookFriendPresence)presence;
                    facebookFriend.removePresence(facebookFriendPresence);
                    friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(facebookFriendPresence, FriendPresenceEvent.Type.REMOVED));
                    if(!facebookFriend.isSignedIn()) {
                        removeIncomingChatListener(presence.getFriend().getId());
                        friendManager.removeAvailableFriend(facebookFriend);
                    }
                } else {
                    LOG.debugf("remove presence, no presence to remove: {0}", presenceId);
                }
            } else {
                LOG.debugf("remove presence, no friend found for id {0}", presenceId);
            }
        }
    }

    void removeAllPresences(FacebookFriend friend) {
        synchronized (presenceLock) {
            Map<String, FriendPresence> presenceMap = friend.getPresences();
            LOG.debugf("removing all presences for {0}", friend.getId());
            for(FriendPresence presence : presenceMap.values()) {
                LOG.debugf("removing presence {0}", presence);
                friend.removePresence((FacebookFriendPresence)presence);
                friendPresenceBroadcaster.broadcast(new FriendPresenceEvent(presence, FriendPresenceEvent.Type.REMOVED));
            }
            friendManager.removeAvailableFriend(friend);
        }
    }


    void setIncomingChatListener(String friendId, IncomingChatListener listener) {
        chatManager.setIncomingChatListener(friendId, listener);
    }

    void removeIncomingChatListener(String friendId) {
        chatManager.removeChat(friendId);
    }

    MessageWriter createChat(String friendId, MessageReader reader) {
        return chatManager.addMessageReader(friendId, reader);
    }

    @SuppressWarnings("unchecked")
    private static List<Cookie> parseCookies(FriendConnectionConfiguration configuration) {
        return (List<Cookie>)configuration.getAttribute("cookie");
    }
}
