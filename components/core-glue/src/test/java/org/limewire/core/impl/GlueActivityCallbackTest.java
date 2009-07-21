package org.limewire.core.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.bittorrent.Torrent;
import org.limewire.core.api.callback.GuiCallback;
import org.limewire.core.api.download.DownloadAction;
import org.limewire.core.api.download.DownloadException;
import org.limewire.core.api.magnet.MagnetLink;
import org.limewire.core.impl.download.DownloadListener;
import org.limewire.core.impl.monitor.IncomingSearchListener;
import org.limewire.core.impl.search.QueryReplyListener;
import org.limewire.core.impl.upload.UploadListener;
import org.limewire.io.GUID;
import org.limewire.io.IpPort;
import org.limewire.service.ErrorCallback;
import org.limewire.service.ErrorService;
import org.limewire.service.MessageCallback;
import org.limewire.service.MessageService;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import com.limegroup.gnutella.DownloadManager;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.RemoteFileDesc;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.browser.MagnetOptions;
import com.limegroup.gnutella.messages.QueryReply;
import com.limegroup.gnutella.messages.QueryRequest;

/**
 * Tests methods in {@link GlueActivityCallback} for functionality and linkage.
 */
public class GlueActivityCallbackTest extends BaseTestCase {

    public GlueActivityCallbackTest(String name) {
        super(name);
    }
    
    /**
     * Adds and removes an {@link UploadListener} ensures events are and are not fired
     *  according to the situation.
     */
    public void testUploadListenerLinkage() {
        Mockery context = new Mockery();
        
        final UploadListener listener1 = context.mock(UploadListener.class);
        final UploadListener listener2 = context.mock(UploadListener.class);
        final UploadListener listener3 = context.mock(UploadListener.class);
        
        final Uploader uploaderA = context.mock(Uploader.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(listener1).uploadAdded(uploaderA);
            exactly(1).of(listener2).uploadAdded(uploaderA);
            exactly(1).of(listener3).uploadAdded(uploaderA);
            
            exactly(1).of(listener1).uploadRemoved(uploaderA);
            
            exactly(1).of(listener1).uploadsCompleted();
            exactly(1).of(listener2).uploadsCompleted();
        }});
        
        activityCallback.addUploadListener(listener1);
        activityCallback.addUploadListener(listener2);
        activityCallback.addUploadListener(listener3);
        
        activityCallback.addUpload(uploaderA);
        
        activityCallback.removeUploadListener(listener2);
        activityCallback.removeUploadListener(listener3);
        
        activityCallback.removeUpload(uploaderA);
        
        activityCallback.addUploadListener(listener2);
            
        activityCallback.uploadsComplete();
        
        context.assertIsSatisfied();
    }
    

    /**
     * Adds and removes an {@link DownloadListener} ensures events are and are not fired
     *  according to the situation.
     */
    public void testDownloadListenerLinkage() {
        Mockery context = new Mockery();
        
        final DownloadListener listener1 = context.mock(DownloadListener.class);
        final DownloadListener listener2 = context.mock(DownloadListener.class);
        final DownloadListener listener3 = context.mock(DownloadListener.class);
        
        final Downloader downloaderA = context.mock(Downloader.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(listener1).downloadAdded(downloaderA);
            exactly(1).of(listener2).downloadAdded(downloaderA);
            exactly(1).of(listener3).downloadAdded(downloaderA);
            
            exactly(1).of(listener1).downloadRemoved(downloaderA);
            
            exactly(1).of(listener1).downloadsCompleted();
            exactly(1).of(listener2).downloadsCompleted();
        }});
        
        activityCallback.addDownloadListener(listener1);
        activityCallback.addDownloadListener(listener2);
        activityCallback.addDownloadListener(listener3);
        
        activityCallback.addDownload(downloaderA);
        
        activityCallback.removeDownloadListener(listener2);
        activityCallback.removeDownloadListener(listener3);
        
        activityCallback.removeDownload(downloaderA);
        
        activityCallback.addDownloadListener(listener2);
            
        activityCallback.downloadsComplete();
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the restoreApplication method with and without the callback set.
     */
    public void testRestoreApplication() {
        Mockery context = new Mockery();
        
        final GuiCallback callback = context.mock(GuiCallback.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(callback).restoreApplication();
        }});
        
        activityCallback.restoreApplication();
        activityCallback.setGuiCallback(callback);
        activityCallback.restoreApplication();
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the dangerousDownloadDeleted method with and without the callback set.
     */
    public void testDangerousDownloadDeleted() {
        Mockery context = new Mockery();
        
        final GuiCallback callback = context.mock(GuiCallback.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(callback).dangerousDownloadDeleted("file");
        }});
        
        activityCallback.dangerousDownloadDeleted("file");
        activityCallback.setGuiCallback(callback);
        activityCallback.dangerousDownloadDeleted("file");
        
        context.assertIsSatisfied();
    }
    
    /**
     * Signal a save location exception before and after a gui callback is installed.
     *  For before the error should be passed along to {@link ErrorService}, when after it
     *  should go to the set {@link GuiCallback}.
     */
    public void testHandleDownloadException() {
        Mockery context = new Mockery();
        
        final GuiCallback callback = context.mock(GuiCallback.class);
        final ErrorCallback mockErrorCallback = context.mock(ErrorCallback.class);
        
        final DownloadException e = new DownloadException(new IOException(), new File("a"));
        final DownloadAction downloadAction = context.mock(DownloadAction.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(mockErrorCallback).error(with(same(e)), with(any(String.class)));
            exactly(1).of(callback).handleDownloadException(downloadAction, e, false);
        }});
    
        ErrorCallback originalErrorCallback = ErrorService.getErrorCallback();
        ErrorService.setErrorCallback(mockErrorCallback);        
        activityCallback.handleDownloadException(downloadAction, e, true);
        ErrorService.setErrorCallback(originalErrorCallback);
        
        
        activityCallback.setGuiCallback(callback);
        activityCallback.handleDownloadException(downloadAction, e, false);
        
        context.assertIsSatisfied();
    }
    
    public void testHandleTorrent() throws DownloadException {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
     
        final DownloadManager downloadManager = context.mock(DownloadManager.class);
        final GuiCallback guiCallback = context.mock(GuiCallback.class);        
        
        final File mockFile = context.mock(File.class);
        final DownloadException e = new DownloadException(new IOException(), mockFile);
        final File goodFile = new File("asdsadsad");
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(downloadManager);
        
        final MatchAndCopy<DownloadAction> actionCollector = new MatchAndCopy<DownloadAction>(DownloadAction.class);
        
        context.checking(new Expectations() {{
            allowing(mockFile).length();
            will(returnValue(10L));
            allowing(mockFile).exists();
            will(returnValue(true));
            allowing(mockFile);
            
            one(downloadManager).downloadTorrent(with(same(mockFile)), with(any(File.class)), with(any(boolean.class)));
            
            one(downloadManager).downloadTorrent(with(same(mockFile)), with(any(File.class)), with(any(boolean.class)));
            will(throwException(e));
            
            exactly(1).of(guiCallback).handleDownloadException(with(actionCollector), with(same(e)), with(any(boolean.class)));
            exactly(1).of(downloadManager).downloadTorrent(mockFile, goodFile, true);
        }});
        
        // Torrent that does not exist
        activityCallback.handleTorrent(new File("f"));
        
        // Good file, torrent download good
        activityCallback.handleTorrent(mockFile);
        
        // Set callback to handle pending exception
        activityCallback.setGuiCallback(guiCallback);
        
        // Good file, torrent download returns exception
        activityCallback.handleTorrent(mockFile);
        
        // Try to redownload through the DownloadAction provided by
        // DownloadException handling
        actionCollector.getLastMatch().download(goodFile, true);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the magnet handler function and make sure it appropriately delegates the 
     *  magnets to the guiCallback.
     */
    public void testHandleMagnets() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};
        
        final GuiCallback guiCallback = context.mock(GuiCallback.class);
        final MagnetOptions[] magnets = new MagnetOptions[] {
                context.mock(MagnetOptions.class),
                context.mock(MagnetOptions.class),
                context.mock(MagnetOptions.class)
        };
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(3).of(guiCallback).handleMagnet(with(any(MagnetLink.class)));
        }});
        
        activityCallback.handleMagnets(magnets);
        activityCallback.setGuiCallback(guiCallback);
        activityCallback.handleMagnets(magnets);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the code relating to {@link QueryReplyListener}.  Tests adding and removing the listeners
     *  and instances that fire them.
     */
    public void testQueryReplyListeners() {
        Mockery context = new Mockery();
        
        final QueryReplyListener listener1 = context.mock(QueryReplyListener.class);
        final byte[] guid1 = new byte[] {'x','x','x',1,2,3,4,5,6,9,'n',10,'x','x','x','x'};
        final QueryReplyListener listener2a = context.mock(QueryReplyListener.class);
        final QueryReplyListener listener2b = context.mock(QueryReplyListener.class);
        final byte[] guid2 = new byte[] {'x','x','x','x',1,2,3,4,5,6,9,'n',10,'x','x','x'};
        
        final RemoteFileDesc rfd = context.mock(RemoteFileDesc.class);
        final QueryReply queryReply1 = context.mock(QueryReply.class);
        final QueryReply queryReply2 = context.mock(QueryReply.class);
        final Set<? extends IpPort> locs = new HashSet<IpPort>();
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            allowing(queryReply1).getGUID();
            will(returnValue(guid1));
            allowing(queryReply2).getGUID();
            will(returnValue(guid2));
            
            exactly(1).of(listener1).handleQueryReply(rfd, queryReply1, locs);
            exactly(1).of(listener2a).handleQueryReply(rfd, queryReply2, locs);
            exactly(2).of(listener2b).handleQueryReply(rfd, queryReply2, locs);
        }});
        
        activityCallback.addQueryReplyListener(guid1, listener1);
        assertFalse(activityCallback.isQueryAlive(new GUID(guid2)));
        assertTrue(activityCallback.isQueryAlive(new GUID(guid1)));
        activityCallback.addQueryReplyListener(guid2, listener2a);
        activityCallback.addQueryReplyListener(guid2, listener2b);
        activityCallback.handleQueryResult(rfd, queryReply1, locs);
        activityCallback.handleQueryResult(rfd, queryReply2, locs);   
        activityCallback.removeQueryReplyListener(guid1, listener1);
        activityCallback.removeQueryReplyListener(guid1, listener1);
        activityCallback.removeQueryReplyListener(guid2, listener2a);
        activityCallback.handleQueryResult(rfd, queryReply1, locs);
        activityCallback.handleQueryResult(rfd, queryReply2, locs); 
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests adding and removing {@link IncomingSearchListener} instances along with firing them.
     */
    public void testIncomingSearchListeners() {
        Mockery context = new Mockery();
        
        final IncomingSearchListener listener1 = context.mock(IncomingSearchListener.class);
        final IncomingSearchListener listener2 = context.mock(IncomingSearchListener.class);
        
        final QueryRequest queryRequest1 = context.mock(QueryRequest.class);
        final QueryRequest queryRequest2 = context.mock(QueryRequest.class);
                
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            allowing(queryRequest1).getQuery();
            will(returnValue("querty"));
            allowing(queryRequest2).getQuery();
            will(returnValue("dvoraq"));
            
            exactly(2).of(listener1).handleQueryString("querty");
            exactly(2).of(listener1).handleQueryString("dvoraq");
            exactly(1).of(listener2).handleQueryString("querty");
            exactly(1).of(listener2).handleQueryString("dvoraq");
        }});
        
        
        // Add two listeners
        activityCallback.addIncomingSearchListener(listener1);
        activityCallback.addIncomingSearchListener(listener2);
        
        // Pass two quieres in to be handled - picked up by the two listeners
        activityCallback.handleQuery(queryRequest1, "address-that-is-ignored", 555);
        activityCallback.handleQuery(queryRequest2, "address-that-is-ignored", 555);
        
        // Remove a listener and pass in the quieres again - make sure they are only caught
        //  by the remaining listener
        activityCallback.removeIncomingSearchListener(listener2);
        activityCallback.handleQuery(queryRequest1, "address-that-is-ignored-again", 555);
        activityCallback.handleQuery(queryRequest2, "address-that-is-ignored-again", 555);
        
        // Remove the last listener - make sure further queries
        activityCallback.removeIncomingSearchListener(listener1);
        activityCallback.handleQuery(queryRequest1, "address-that-is-ignored-again", 555);
        activityCallback.handleQuery(queryRequest2, "address-that-is-ignored-again", 555);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test the promptAboutCurruptDownload() method.  The method does not actually
     *  do any prompting, it just stops the downloader.  Test will confirm that the
     *  correct call is made, if the method is changed in the future then this 
     *  test will need updating.
     */
    public void testPromptAboutCorruptDownload() {
        Mockery context = new Mockery();
        
        final Downloader downloader = context.mock(Downloader.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(downloader).discardCorruptDownload(with(any(boolean.class)));
        }});
        
        activityCallback.promptAboutCorruptDownload(downloader);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Tests the translate method.  Ensures that it returns the untranslated string if there 
     *  is no {@link GuiCallback} instance otherwise delegate to it for the translation.
     */
    public void testTranslate() {
        Mockery context = new Mockery();
        
        final GuiCallback callback = context.mock(GuiCallback.class);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(callback).translate("hello");
            will(returnValue("goodbye"));
        }});
        
        assertEquals("hello", activityCallback.translate("hello"));
        activityCallback.setGuiCallback(callback);
        assertEquals("goodbye", activityCallback.translate("hello"));
        
        context.assertIsSatisfied();
    }
    
    /**
     * This method currently does nothing.  Call it anyways, when it starts doing something
     *  this test will fail and should be updated accordingly.
     */
    public void testHandleSharedFileUpdate() {
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        activityCallback.handleSharedFileUpdate(null);
    }
    
    /**
     * Tests the {@link GlueActivityCallback#installationCurrupted()} and ensures it links 
     *  properly with {@link MessageService}.
     */
    public void testIntallationCorrupted() {
        Mockery context = new Mockery();
        
        final MessageCallback messageCallback = context.mock(MessageCallback.class);
        MessageCallback originalMessageCallback = MessageService.getCallback();
        MessageService.setCallback(messageCallback);
        
        GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            exactly(1).of(messageCallback).showError(with(any(String.class)));
        }});
        
        activityCallback.installationCorrupted();
        
        MessageService.setCallback(originalMessageCallback);
        
        context.assertIsSatisfied();
    }
    
    public void testPromptTorrentUploadCancel() {
        Mockery context = new Mockery() {{
            setImposteriser(ClassImposteriser.INSTANCE);
        }};

        final GuiCallback guiCallbackYes = context.mock(GuiCallback.class);
        final GuiCallback guiCallbackNo = context.mock(GuiCallback.class);
        
        final Torrent torrentNotComplete = context.mock(Torrent.class);
        final Torrent torrentInactive = context.mock(Torrent.class);
        final Torrent torrentLowRatio = context.mock(Torrent.class);
        final Torrent torrentHighRatio = context.mock(Torrent.class);
        
        final GlueActivityCallback activityCallback = new GlueActivityCallback(null);
        
        context.checking(new Expectations() {{
            allowing(guiCallbackYes).promptUserQuestion(with(any(String.class)));
            will(returnValue(true));
            allowing(guiCallbackNo).promptUserQuestion(with(any(String.class)));
            will(returnValue(false));
            
            allowing(torrentInactive).isStarted();
            will(returnValue(false));
            
            allowing(torrentNotComplete).isFinished();
            will(returnValue(false));
            allowing(torrentNotComplete).isStarted();
            will(returnValue(true));
            
            allowing(torrentLowRatio).getSeedRatio();
            will(returnValue(.5f));
            allowing(torrentLowRatio).isFinished();
            will(returnValue(true));
            allowing(torrentLowRatio).isStarted();
            will(returnValue(true));
            
            allowing(torrentHighRatio).getSeedRatio();
            will(returnValue(1.5f));
            allowing(torrentHighRatio).isFinished();
            will(returnValue(true));
            allowing(torrentHighRatio).isStarted();
            will(returnValue(true));
            
        }});
        
        // Selecting yes with a torrent in progress should stop.
        activityCallback.setGuiCallback(guiCallbackYes);        
        assertTrue(activityCallback.promptTorrentUploadCancel(torrentNotComplete));
        
        // Selecting no with torrent not complete should not result in stop.
        activityCallback.setGuiCallback(guiCallbackNo);
        assertFalse(activityCallback.promptTorrentUploadCancel(torrentNotComplete));
        
        // Ensure an inactive torrent will not be stopped. 
        activityCallback.setGuiCallback(guiCallbackYes);        
        assertFalse(activityCallback.promptTorrentUploadCancel(torrentInactive));
        
        // Selecting yes with a torrent with a low ratio should stop.
        activityCallback.setGuiCallback(guiCallbackYes);        
        assertTrue(activityCallback.promptTorrentUploadCancel(torrentLowRatio));
        
        // Selecting no with torrent and a low ratio should not result in stop.
        activityCallback.setGuiCallback(guiCallbackNo);
        assertFalse(activityCallback.promptTorrentUploadCancel(torrentLowRatio));
        
        // The user should not be prompted if there is a high ratio the torrent should be stopped
        activityCallback.setGuiCallback(guiCallbackNo);        
        assertTrue(activityCallback.promptTorrentUploadCancel(torrentHighRatio));

        context.assertIsSatisfied();        
    }
}
