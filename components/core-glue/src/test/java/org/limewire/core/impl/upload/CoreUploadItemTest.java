package org.limewire.core.impl.upload;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.endpoint.RemoteHost;
import org.limewire.core.api.upload.UploadErrorState;
import org.limewire.core.api.upload.UploadItem.BrowseType;
import org.limewire.core.api.upload.UploadItem.UploadItemType;
import org.limewire.friend.api.Friend;
import org.limewire.friend.api.FriendPresence;
import org.limewire.friend.api.feature.LimewireFeature;
import org.limewire.util.BaseTestCase;

import com.limegroup.bittorrent.BTUploader;
import com.limegroup.gnutella.CategoryConverter;
import com.limegroup.gnutella.InsufficientDataException;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.library.FileDesc;
import com.limegroup.gnutella.uploader.UploadType;
import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

public class CoreUploadItemTest extends BaseTestCase {
    
    public CoreUploadItemTest(String name) {
        super(name);
    }

    public void testGetCategory() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        context.checking(new Expectations() {
            {
                allowing(uploader).getFileName();
                will(returnValue("thing.bmp"));
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader, presence);
        
        assertEquals(upload.getCategory(), CategoryConverter.categoryForExtension("bmp"));
        context.assertIsSatisfied();
    }
    
    public void testCancel() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final PropertyChangeListener changeListener = context.mock(PropertyChangeListener.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
 
        context.checking(new Expectations() {
            {
                exactly(1).of(uploader).stop();
                
                atLeast(1).of(uploader).getState();
                will(returnValue(UploadStatus.CANCELLED));
                
                exactly(1).of(changeListener).propertyChange(with(any(PropertyChangeEvent.class)));
                
                allowing(uploader);
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader, presence);
        
        upload.addPropertyChangeListener(changeListener);
                
        upload.cancel();
                
        context.assertIsSatisfied();
    }
    
    /**
     * equals() is overridden to check if uploaders are equal.
     */
    public void testEquals() {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        context.checking(new Expectations() {
            { 
                allowing(uploader1);
                allowing(uploader2);
            }});
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(null, presence);
        CoreUploadItem upload4 = new CoreUploadItem(null, presence);
        
        assertEquals(upload1, upload1);
        assertNotEquals(upload1, upload2);
        assertNotEquals(upload1, upload3);
        assertNotEquals(upload2, upload1);
        assertEquals(upload2, upload2);
        assertNotEquals(upload2, upload3);
        assertNotEquals(upload3, upload1);
        assertNotEquals(upload3, upload2);
        assertEquals(upload3, upload3);
        assertEquals(upload3, upload4);

        assertFalse(upload1.equals(null));  // MUST assertFalse here since assertNotEquals bails with null
        assertNotEquals("not equals", upload1);
                
        context.assertIsSatisfied();
    }
    
    public void testRemovePropertyChangeListener() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final PropertyChangeListener changeListener = context.mock(PropertyChangeListener.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
 
        context.checking(new Expectations() {
            {
                exactly(1).of(uploader).stop();
                
                atLeast(1).of(uploader).getState();
                will(returnValue(UploadStatus.CANCELLED));
                
                never(changeListener).propertyChange(with(any(PropertyChangeEvent.class)));
                
                allowing(uploader);
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader, presence);
        
        upload.addPropertyChangeListener(changeListener);
        upload.removePropertyChangeListener(changeListener);
                
        upload.cancel();
                
        context.assertIsSatisfied();
    }

    public void testGetRemoteHostGnutella() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        final Friend friend = context.mock(Friend.class);
        
        context.checking(new Expectations() {
        { 
            exactly(3).of(presence).getFriend();
            will(returnValue(friend));
            exactly(3).of(friend).isAnonymous();
            will(returnValue(true));
            exactly(1).of(uploader).isBrowseHostEnabled();
            will(returnValue(true));
        }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader, presence);
        RemoteHost remoteHost = upload.getRemoteHost();
        
        assertNotNull(remoteHost);
        
        assertEquals(presence, remoteHost.getFriendPresence());
        assertFalse(remoteHost.isChatEnabled());
        assertTrue(remoteHost.isBrowseHostEnabled());
        assertFalse(remoteHost.isSharingEnabled());
        
        context.assertIsSatisfied();
    }
    
    public void testGetRemoteHostFriend() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        final Friend friend = context.mock(Friend.class);
        
        context.checking(new Expectations() {
        { 
            exactly(3).of(presence).getFriend();
            will(returnValue(friend));
            exactly(3).of(friend).isAnonymous();
            will(returnValue(false));
            exactly(3).of(presence).hasFeatures(LimewireFeature.ID);
            will(returnValue(true));
        }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader, presence);
        RemoteHost remoteHost = upload.getRemoteHost();
        
        assertNotNull(remoteHost);
        
        assertEquals(presence, remoteHost.getFriendPresence());
        assertTrue(remoteHost.isChatEnabled());
        assertTrue(remoteHost.isBrowseHostEnabled());
        assertTrue(remoteHost.isSharingEnabled());
        
        context.assertIsSatisfied();
    }
    
    private void testGetBrowseType(final UploadStatus state, final String fileName,
            final BrowseType type) {
        
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        context.checking(new Expectations() {
            {
                allowing(uploader).getState();
                will(returnValue(state));
                allowing(uploader).getFileName();
                will(returnValue(fileName));
               
                allowing(uploader).getUploadType();
                will(returnValue(UploadType.BROWSE_HOST));
                
                allowing(uploader).getLastTransferState();
                will(returnValue(state));
                
                allowing(uploader);
            }});
        
        CoreUploadItem upload = new CoreUploadItem(uploader, presence);
        
        assertEquals(type, upload.getBrowseType());
   
        context.assertIsSatisfied();        
    }
    
    public void testGetBrowseTypeWithBrowse() {
        testGetBrowseType(UploadStatus.BROWSE_HOST, "asd", BrowseType.FRIEND);
    }
    public void testGetBrowseTypeWithDoneBrowse() {
        testGetBrowseType(UploadStatus.COMPLETE, "asd", BrowseType.FRIEND);
    }
    public void testGetBrowseTypeWithBrowseAndNoFile() {
        testGetBrowseType(UploadStatus.BROWSE_HOST, "", BrowseType.P2P);
    }    
    public void testGetBrowseTypeWithDoneBrowseAndNoFile() {
        testGetBrowseType(UploadStatus.COMPLETE, "", BrowseType.P2P);
    }
    public void testGetBrowseTypeWithOther() {
        testGetBrowseType(UploadStatus.CANCELLED, "", BrowseType.NONE);
        testGetBrowseType(UploadStatus.FILE_NOT_FOUND, "fde", BrowseType.NONE);
        testGetBrowseType(UploadStatus.INTERRUPTED, "", BrowseType.NONE);
        testGetBrowseType(UploadStatus.CONNECTING, "", BrowseType.NONE);
        testGetBrowseType(UploadStatus.THEX_REQUEST, "asd", BrowseType.NONE);
        testGetBrowseType(UploadStatus.LIMIT_REACHED, "asds", BrowseType.NONE);
    }
    
    public void testGetUploadItemType() {
        
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};
        
        final Uploader uploaderNormal = context.mock(Uploader.class);
        final Uploader uploaderBittorrent = context.mock(BTUploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        context.checking(new Expectations() {
            { 
            }});
        
        CoreUploadItem upload1 = new CoreUploadItem(uploaderNormal, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploaderBittorrent, presence);
        CoreUploadItem upload3 = new CoreUploadItem(null, presence);
        
        assertEquals(UploadItemType.GNUTELLA, upload1.getUploadItemType());
        assertEquals(UploadItemType.BITTORRENT, upload2.getUploadItemType());
        assertEquals(UploadItemType.GNUTELLA, upload3.getUploadItemType());
        
        context.assertIsSatisfied();        
    }
    
    public void testGetQueuePosition() {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final Uploader uploader3 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final int queuePos1 = 0;
        final int queuePos2 = Integer.MAX_VALUE;
        final int queuePos3 = Integer.MIN_VALUE;
        
        context.checking(new Expectations() {
            {   allowing(uploader1).getQueuePosition();
                will(returnValue(queuePos1));
                allowing(uploader2).getQueuePosition();
                will(returnValue(queuePos2));
                allowing(uploader3).getQueuePosition();
                will(returnValue(queuePos3));
            }});
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(uploader3, presence);
        
        assertEquals(queuePos1, upload1.getQueuePosition());
        assertEquals(queuePos2, upload2.getQueuePosition());
        assertEquals(queuePos3, upload3.getQueuePosition());
        
        context.assertIsSatisfied();                
    }
    
    public void testGetNumUploadConnections() {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final Uploader uploader3 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final int numConnections1 = 0;
        final int numConnections2 = Integer.MAX_VALUE;
        final int numConnections3 = Integer.MIN_VALUE;
        
        context.checking(new Expectations() {
            {   allowing(uploader1).getNumUploadConnections();
                will(returnValue(numConnections1));
                allowing(uploader2).getNumUploadConnections();
                will(returnValue(numConnections2));
                allowing(uploader3).getNumUploadConnections();
                will(returnValue(numConnections3));
            }});
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(uploader3, presence);
        
        assertEquals(numConnections1, upload1.getNumUploadConnections());
        assertEquals(numConnections2, upload2.getNumUploadConnections());
        assertEquals(numConnections3, upload3.getNumUploadConnections());
        
        context.assertIsSatisfied();                
    }
    
    public void testGetFile() {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final Uploader uploader3 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final File file1 = new File("abc.t");
        final File file2 = new File("/a/b/c.t");
        final File file3 = null;
        
        context.checking(new Expectations() {
            {   allowing(uploader1).getFile();
                will(returnValue(file1));
                allowing(uploader2).getFile();
                will(returnValue(file2));
                allowing(uploader3).getFile();
                will(returnValue(file3));
            }});
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(uploader3, presence);
        
        assertEquals(file1, upload1.getFile());
        assertEquals(file2, upload2.getFile());
        assertEquals(file3, upload3.getFile());
        
        context.assertIsSatisfied();                
    }
    
    public void testGetUploadSpeed() throws InsufficientDataException {
       Mockery context = new Mockery();
        
        final Uploader uploaderException = context.mock(Uploader.class);
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final Uploader uploader3 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final float speed1 = 0;
        final float speed2 = Float.MAX_VALUE;
        final float speed3 = Float.MIN_VALUE;
        
        context.checking(new Expectations() {
            {
                allowing(uploaderException).measureBandwidth();
                allowing(uploader1).measureBandwidth();
                allowing(uploader2).measureBandwidth();
                allowing(uploader3).measureBandwidth();
                
                allowing(uploaderException).getMeasuredBandwidth();
                will(throwException(new InsufficientDataException()));
                
                allowing(uploader1).getMeasuredBandwidth();
                will(returnValue(speed1));
                allowing(uploader2).getMeasuredBandwidth();
                will(returnValue(speed2));
                allowing(uploader3).getMeasuredBandwidth();
                will(returnValue(speed3));
            }});
        
        CoreUploadItem uploadException = new CoreUploadItem(uploaderException, presence);
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(uploader3, presence);
        
        assertEquals(0f, uploadException.getUploadSpeed());
        assertEquals(speed1, upload1.getUploadSpeed());
        assertEquals(speed2, upload2.getUploadSpeed());
        assertEquals(speed3, upload3.getUploadSpeed());
        
        context.assertIsSatisfied();                
    }
    
    public void testGetRemainingUploadTime() throws InsufficientDataException  {
        Mockery context = new Mockery();
        
        final Uploader uploaderException = context.mock(Uploader.class);
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final Uploader uploader3 = context.mock(Uploader.class);
        final Uploader uploader4 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
         
        final float speed1 = 0;
        final float speed2 = Float.MAX_VALUE;
        final float speed3 = Float.MIN_VALUE;
        final float speed4 = 10;

        final long fileSize1 = 0;
        final long fileSize2 = Long.MAX_VALUE;
        final long fileSize3 = Long.MIN_VALUE;
        final long fileSize4 = 10000;

        final long progress1 = 0;
        final long progress2 = Long.MAX_VALUE;
        final long progress3 = Long.MIN_VALUE;
        final long progress4 = 5000;
         
        context.checking(new Expectations() {
            {
                allowing(uploaderException).measureBandwidth();
                allowing(uploader1).measureBandwidth();
                allowing(uploader2).measureBandwidth();
                allowing(uploader3).measureBandwidth();
                allowing(uploader4).measureBandwidth();
                 
                allowing(uploaderException).getMeasuredBandwidth();
                will(throwException(new InsufficientDataException()));
                
                allowing(uploader1).getMeasuredBandwidth();
                will(returnValue(speed1));
                allowing(uploader2).getMeasuredBandwidth();
                will(returnValue(speed2));
                allowing(uploader3).getMeasuredBandwidth();
                will(returnValue(speed3));
                allowing(uploader4).getMeasuredBandwidth();
                will(returnValue(speed4));
                 
                allowing(uploader1).getFileSize();
                will(returnValue(fileSize1));
                allowing(uploader2).getFileSize();
                will(returnValue(fileSize2));
                allowing(uploader3).getFileSize();
                will(returnValue(fileSize3));
                allowing(uploader4).getFileSize();
                will(returnValue(fileSize4));
                
                allowing(uploader1).getTotalAmountUploaded();
                will(returnValue(progress1));
                allowing(uploader2).getTotalAmountUploaded();
                will(returnValue(progress2));
                allowing(uploader3).getTotalAmountUploaded();
                will(returnValue(progress3));
                allowing(uploader4).getTotalAmountUploaded();
                will(returnValue(progress4));
             }});
         
        CoreUploadItem uploadException = new CoreUploadItem(uploaderException, presence);
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(uploader3, presence);
        CoreUploadItem upload4 = new CoreUploadItem(uploader4, presence);
         
        assertEquals(CoreUploadItem.UNKNOWN_TIME, uploadException.getRemainingUploadTime());
        assertEquals(CoreUploadItem.UNKNOWN_TIME, upload1.getRemainingUploadTime());
        assertEquals((long)(((fileSize2 - progress2) / 1024.0) / speed2), upload2.getRemainingUploadTime());
        assertEquals((long)(((fileSize3 - progress3) / 1024.0) / speed3), upload3.getRemainingUploadTime());
        assertEquals((long)(((fileSize4 - progress4) / 1024.0) / speed4), upload4.getRemainingUploadTime());
        
        context.assertIsSatisfied();   
    }   
    
    public void testHashCode() {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final Uploader uploader3 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(uploader3, presence);
        CoreUploadItem upload4 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload5 = new CoreUploadItem(null, presence);
        CoreUploadItem upload6 = new CoreUploadItem(null, presence);
        
        assertEquals(upload1.hashCode(), upload1.hashCode());
        assertNotEquals(upload2.hashCode(), upload1.hashCode());
        assertNotEquals(upload3.hashCode(), upload1.hashCode());
        assertEquals(upload4.hashCode(), upload1.hashCode());
        
        assertNotEquals(upload1.hashCode(), upload2.hashCode());
        assertEquals(upload2.hashCode(), upload2.hashCode());
        assertNotEquals(upload3.hashCode(), upload2.hashCode());
        assertNotEquals(upload4.hashCode(), upload2.hashCode());
        
        assertNotEquals(upload1.hashCode(), upload3.hashCode());
        assertNotEquals(upload2.hashCode(), upload3.hashCode());
        assertEquals(upload3.hashCode(), upload3.hashCode());
        assertNotEquals(upload4.hashCode(), upload3.hashCode());
        
        assertEquals(upload1.hashCode(), upload4.hashCode());
        assertNotEquals(upload2.hashCode(), upload4.hashCode());
        assertNotEquals(upload3.hashCode(), upload4.hashCode());
        assertEquals(upload4.hashCode(), upload4.hashCode());
        
        assertEquals(upload5.hashCode(), upload5.hashCode());
        assertEquals(upload5.hashCode(), upload6.hashCode());
        assertNotEquals(upload5.hashCode(), upload1.hashCode());
    }
    
    public void testGetUrn() throws IOException {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final URN urn = URN.createUrnFromString("urn:sha1:NETZHKEJKTCM74ZQQALJWSLWQHQJ7N6Q");
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        
        context.checking(new Expectations() {
            { 
                allowing(uploader1).getUrn();
                will(returnValue(urn));
                
                allowing(uploader2).getUrn();
                will(returnValue(null));
                
            }});

        assertEquals(urn, upload1.getUrn());
        assertEquals(null, upload2.getUrn());
        
        context.assertIsSatisfied();
    }
    
    public void testGetErrorState() {
        Mockery context = new Mockery();
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final Uploader uploader3 = context.mock(Uploader.class);
        final Uploader uploader4 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader2, presence);
        CoreUploadItem upload3 = new CoreUploadItem(uploader3, presence);
        CoreUploadItem upload4 = new CoreUploadItem(uploader4, presence);
        
        context.checking(new Expectations() {
            { 
                allowing(uploader1).getState();
                will(returnValue(UploadStatus.BANNED_GREEDY));
                
                allowing(uploader2).getState();
                will(returnValue(UploadStatus.INTERRUPTED));
                
                allowing(uploader3).getState();
                will(returnValue(UploadStatus.MALFORMED_REQUEST));
                
                allowing(uploader4).getState();
                will(returnValue(UploadStatus.UPLOADING));
                
            }});

        assertEquals(UploadErrorState.LIMIT_REACHED, upload1.getErrorState());
        assertEquals(UploadErrorState.INTERRUPTED, upload2.getErrorState());
        assertEquals(UploadErrorState.FILE_ERROR, upload3.getErrorState());
        assertEquals(UploadErrorState.NO_ERROR, upload4.getErrorState());
        
        context.assertIsSatisfied();
    }
    
    public void testProperties() {
        
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};
        
        final Uploader uploader1 = context.mock(Uploader.class);
        final Uploader uploader2 = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final FileDesc fd1 = context.mock(FileDesc.class);
        final LimeXMLDocument doc1 = context.mock(LimeXMLDocument.class);
        
        final String obj1 = "erica";
        final String defReturn1 = "eliefynafgrd";
        
        CoreUploadItem upload1 = new CoreUploadItem(uploader1, presence);
        CoreUploadItem upload2 = new CoreUploadItem(uploader1, presence);

        context.checking(new Expectations() {
            {   allowing(uploader1).getFileDesc();
                will(returnValue(fd1));
                
                allowing(uploader2).getFileDesc();
                will(returnValue(null));
                
                allowing(fd1).getFileName();
                will(returnValue("booc.ogg"));
                
                allowing(fd1).getFileSize();
                will(returnValue(888l));
                
                allowing(fd1).getFile();
                will(returnValue(new File("asdsa")));
                
                allowing(fd1).getXMLDocument();
                will(returnValue(doc1));
                
                allowing(doc1).getValue(LimeXMLNames.AUDIO_ARTIST);
                will(returnValue(obj1));
                
                allowing(doc1).getValue(with(any(String.class)));
                will(returnValue(defReturn1));
                
            }});
        
        assertEquals(obj1, upload1.getPropertyString(FilePropertyKey.AUTHOR));
        assertEquals(defReturn1, upload1.getPropertyString(FilePropertyKey.GENRE));
        assertNull(upload1.getPropertyString(FilePropertyKey.HEIGHT));
        assertNull(upload2.getPropertyString(FilePropertyKey.BITRATE));
        
       context.assertIsSatisfied();
    }
}
