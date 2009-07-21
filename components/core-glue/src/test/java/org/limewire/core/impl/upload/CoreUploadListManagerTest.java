package org.limewire.core.impl.upload;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.swing.SwingUtilities;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.limewire.core.api.upload.UploadItem;
import org.limewire.core.api.upload.UploadState;
import org.limewire.core.settings.SharingSettings;
import org.limewire.friend.api.FriendManager;
import org.limewire.friend.api.FriendPresence;
import org.limewire.lifecycle.ServiceScheduler;
import org.limewire.util.BaseTestCase;
import org.limewire.util.MatchAndCopy;

import ca.odell.glazedlists.EventList;

import com.limegroup.gnutella.UploadServices;
import com.limegroup.gnutella.Uploader;
import com.limegroup.gnutella.Uploader.UploadStatus;
import com.limegroup.gnutella.uploader.UploadType;

public class CoreUploadListManagerTest extends BaseTestCase {
    
    public CoreUploadListManagerTest(String name) {
        super(name);
    }

    /**
     * Ensure the periodic refresher is registered and the manager is
     *  registered as a listener to the upload listeners list.
     *  <p>
     *  Try to execute the associated runnable to ensure functionality. 
     */
    public void testRegister() {
        Mockery context = new Mockery();
        
        final UploadListenerList uploadListenerList = context.mock(UploadListenerList.class);
        final ServiceScheduler scheduler = context.mock(ServiceScheduler.class);
        final ScheduledExecutorService backgroundExecutor = context.mock(ScheduledExecutorService.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null, friendManager);
        
        final MatchAndCopy<Runnable> runnableMatcher = new MatchAndCopy<Runnable>(Runnable.class);
        
        context.checking(new Expectations() {
            {   exactly(1).of(scheduler).scheduleAtFixedRate(with(any(String.class)),with(runnableMatcher), 
                    with(any(Integer.class)), with(any(Integer.class)), with(any(TimeUnit.class)),
                    with(same(backgroundExecutor)));
            
                exactly(1).of(uploadListenerList).addUploadListener(manager);
            }
        });
        
        manager.register(uploadListenerList);
        manager.register(scheduler, backgroundExecutor);
        
        Runnable updater = runnableMatcher.getLastMatch();
        updater.run();
    }
    
    /** 
     * Add a browse host upload to the manager, ensure it is accessible,
     *  signal completion, and ensure it is removed.
     */
    public void testUploaderAddAndRemove() {
        
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final long testSize = 777;
        
        final CoreUploadListManager manager = new CoreUploadListManager(null, friendManager);
                
        context.checking(new Expectations() {
        {   
            exactly(3).of(friendManager).getMostRelevantFriendPresence("");
            will(returnValue(presence));
            
            allowing(uploader).getUploadType();
            will(returnValue(UploadType.BROWSE_HOST));
                
            allowing(uploader).getState();
            will(returnValue(UploadStatus.COMPLETE));
                
            // Assertions
            exactly(1).of(uploader).getFileSize();
            will(returnValue(testSize));
                
            // General
            allowing(uploader);
        }
        });
        
        List<UploadItem> items = manager.getUploadItems();
        
        manager.uploadAdded(uploader);
        assertEquals(testSize, items.get(0).getFileSize());
        manager.uploadRemoved(uploader);
        assertEmpty(items);
        
        // Test remove when item not managed
        manager.uploadRemoved(uploader);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Test various conditions where adding and removing an uploader will have no effect.
     * <p>
     * Add an internal uploader, remove an incomplete upload, remove an upload with the 
     *  setting disabled. 
     */
    public void testUploaderAddAndRemoveBlocked() {
        
        Mockery context = new Mockery();
        
        final Uploader uploaderInternal = context.mock(Uploader.class);
        final Uploader uploaderNotComplete = context.mock(Uploader.class);
        final Uploader uploaderSettingDisabled = context.mock(Uploader.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null, friendManager);
                
        context.checking(new Expectations() {
            {   
                exactly(2).of(friendManager).getMostRelevantFriendPresence("");
                will(returnValue(presence));
                
                allowing(uploaderInternal).getUploadType();
                will(returnValue(UploadType.MALFORMED_REQUEST));
                
                allowing(uploaderNotComplete).getState();
                will(returnValue(UploadStatus.UPLOADING));
                
                allowing(uploaderSettingDisabled).getState();
                will(returnValue(UploadStatus.COMPLETE));
                
                allowing(uploaderSettingDisabled).getUploadType();
                will(returnValue(UploadType.BROWSE_HOST));
                
                // General
                allowing(uploaderInternal);
                allowing(uploaderNotComplete);
                allowing(uploaderSettingDisabled);
            }});
     
        List<UploadItem> items = manager.getUploadItems();
        
        manager.uploadAdded(uploaderInternal);
        assertEmpty(items);
        
        items.add(new CoreUploadItem(uploaderNotComplete, presence));
        manager.uploadRemoved(uploaderNotComplete);
        assertGreaterThan(-1, items.size());   // Can't use assertNotEmpty() since it calls toString() on elements!
        items.clear();
        
        boolean clearUploadOriginal = SharingSettings.CLEAR_UPLOAD.getValue();
        SharingSettings.CLEAR_UPLOAD.setValue(false);
        items.add(new CoreUploadItem(uploaderSettingDisabled, presence));
        manager.uploadRemoved(uploaderSettingDisabled);
        SharingSettings.CLEAR_UPLOAD.setValue(clearUploadOriginal);
        assertGreaterThan(-1, items.size());   // Can't use assertNotEmpty() since it calls toString() on elements!
        
        context.assertIsSatisfied();
    }
    
    /**
     * Add a property change listener and trigger an event then verify it is handled, remove the 
     * listener then verify that it is not notified.  Verify nothing is notified there are active uploads.
     */
    public void testPropertyChangeListenerUpdates() {
        
        Mockery context = new Mockery();
        
        final PropertyChangeListener listener1 = context.mock(PropertyChangeListener.class);
        final PropertyChangeListener listener2 = context.mock(PropertyChangeListener.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        
        // Do we really need this dependence in CoreUploadListManager? 
        final UploadServices uploadServices = context.mock(UploadServices.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(uploadServices, friendManager);
        
        context.checking(new Expectations() {
            {   
                exactly(2).of(listener1).propertyChange(with(any(PropertyChangeEvent.class)));
                exactly(3).of(listener2).propertyChange(with(any(PropertyChangeEvent.class)));
                
                // First invocation -- normal empty
                one(uploadServices).getNumUploads();
                will(returnValue(0));
                
                // Second invocation -- bit strange
                one(uploadServices).getNumUploads();
                will(returnValue(-1));
                
                // Third invocation -- normal
                one(uploadServices).getNumUploads();
                will(returnValue(0));
                
                // Final invocation -- has active uploads
                one(uploadServices).getNumUploads();
                will(returnValue(5));
            }});
        
        manager.addPropertyChangeListener(listener1);
        manager.addPropertyChangeListener(listener2);
        manager.updateUploadsCompleted();
        
        // This should always fire the listeners
        manager.uploadsCompleted();
        
        manager.removePropertyChangeListener(listener1);
        manager.updateUploadsCompleted();
        manager.updateUploadsCompleted();
        
        manager.updateUploadsCompleted();
        
        context.assertIsSatisfied();
        
    }

    
    /** 
     * Ensure the thread safe upload list is consistent with the model. 
     */
    public void testGetSwingThreadSafeUploads() throws InterruptedException, InvocationTargetException {
        
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null, friendManager);
                
        context.checking(new Expectations() {
        {   
            exactly(2).of(uploader).getPresenceId();
            will(returnValue(""));
            
            exactly(2).of(friendManager).getMostRelevantFriendPresence("");
            will(returnValue(presence));
                           
            allowing(uploader).getUploadType();
            will(returnValue(UploadType.BROWSE_HOST));

        }});
        
        Runnable edtTask = new Runnable() {

            @Override
            public void run() {
                EventList<UploadItem> uploads = manager.getSwingThreadSafeUploads();
                assertEmpty(uploads);
                
                manager.uploadAdded(uploader);
                assertGreaterThan(-1, uploads.size());
                
                manager.remove(uploads.get(0));
                assertEmpty(uploads);
                
                uploads = manager.getSwingThreadSafeUploads();
                assertEmpty(uploads);
           }
        };
        
        SwingUtilities.invokeAndWait(edtTask);

//        context.assertIsSatisfied();
    }
    
    /** 
     * Adds a number of uploads in various states to the manager then fires the clear
     *  finished action and ensures the completed uploads are removed.
     */
    public void testClearFinished() {
        Mockery context = new Mockery();
        
        final UploadItem uploadDone = context.mock(UploadItem.class);
        final UploadItem uploadBrowseHostDone = context.mock(UploadItem.class);
        final UploadItem uploadUnableToUpload = context.mock(UploadItem.class);
        final UploadItem uploadUploading1 = context.mock(UploadItem.class);
        final UploadItem uploadUploading2 = context.mock(UploadItem.class);
        final UploadItem uploadUploading3 = context.mock(UploadItem.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null, friendManager);
                
        context.checking(new Expectations() {
            {   
                allowing(uploadDone).getState();
                will(returnValue(UploadState.DONE));
                ignoring(uploadDone);
                
                allowing(uploadBrowseHostDone).getState();
                will(returnValue(UploadState.BROWSE_HOST_DONE));
                ignoring(uploadBrowseHostDone);
                
                allowing(uploadUnableToUpload).getState();
                will(returnValue(UploadState.UNABLE_TO_UPLOAD));
                ignoring(uploadUnableToUpload);
                
                allowing(uploadUploading1).getState();
                will(returnValue(UploadState.QUEUED));
                ignoring(uploadUploading1);
                
                allowing(uploadUploading2).getState();
                will(returnValue(UploadState.WAITING));
                ignoring(uploadUploading2);
                
                allowing(uploadUploading3).getState();
                will(returnValue(UploadState.BROWSE_HOST));
                ignoring(uploadUploading3);
                
            }});
        
        List<UploadItem> items = manager.getUploadItems();
        
        // First pass: Do a clear with only a complete upload 
        items.add(uploadBrowseHostDone);
        manager.clearFinished();
        assertEmpty(items);
        
        // Second pass: Do a clear with incomplete uploads;
        items.add(uploadUploading1);
        items.add(uploadUploading2);
        manager.clearFinished();
        assertContains(items, uploadUploading1);
        assertContains(items, uploadUploading2);
        
        // Third pass: Updates of variegated status
        items.add(uploadUploading3);
        items.add(uploadDone);
        items.add(uploadUploading1);
        items.add(uploadBrowseHostDone);
        items.add(uploadUploading2);
        items.add(uploadUnableToUpload);
        items.add(uploadUploading2); // Duplicate
        
        manager.clearFinished();
        
        assertNotContains(items, uploadDone);
        assertNotContains(items, uploadBrowseHostDone);
        assertNotContains(items, uploadUnableToUpload);
        assertContains(items, uploadUploading1);
        assertContains(items, uploadUploading2);
        assertContains(items, uploadUploading3);
        
        context.assertIsSatisfied();
    }
    
    /** 
     * Verify property listener function on UploadItems by adding an upload and 
     *  force firing a change event.  Verify only cancelled uploads are
     *  removed from management. Tests end to end integration between listener and manager.
     */
    public void testPropertyListenerSimple() {
        Mockery context = new Mockery();
        
        final Uploader uploader = context.mock(Uploader.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        final FriendPresence presence = context.mock(FriendPresence.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null, friendManager);
                
        context.checking(new Expectations() {
            {   
                exactly(1).of(uploader).getPresenceId();
                will(returnValue(""));
                
                exactly(1).of(friendManager).getMostRelevantFriendPresence("");
                will(returnValue(presence));
                
                // Initial probe on add
                allowing(uploader).getUploadType();
                will(returnValue(UploadType.BROWSE_HOST));
                one(uploader).getState();
                will(returnValue(UploadStatus.CONNECTING));

                // First event listener firing should not remove the upload
                one(uploader).getState();
                will(returnValue(UploadStatus.UPLOADING));

                // Second event listener firing should remove the upload
                allowing(uploader).getState();
                will(returnValue(UploadStatus.CANCELLED));
                
                // General
                allowing(uploader);
            }
        });
        
        List<UploadItem> items = manager.getUploadItems();
        
        manager.uploadAdded(uploader);
        
        CoreUploadItem item = (CoreUploadItem)items.get(0);
        
        item.fireDataChanged();
        assertContains(items, item);
        
        item.fireDataChanged();
        assertNotContains(items, item);
        
        context.assertIsSatisfied();
    }
    
    /**
     * Manually add several upload items of different types to the manager and fire update.
     *  Ensure the correct items have their listeners fired.
     */
    public void testUpdate() {
        Mockery context = new Mockery() {
            {   setImposteriser(ClassImposteriser.INSTANCE);
            }};
        
        final CoreUploadItem itemDone = context.mock(CoreUploadItem.class);
        final CoreUploadItem itemBrowseHostDone = context.mock(CoreUploadItem.class); 
        final CoreUploadItem item1 = context.mock(CoreUploadItem.class);
        final CoreUploadItem item2 = context.mock(CoreUploadItem.class);
        final CoreUploadItem item3 = context.mock(CoreUploadItem.class);
        final FriendManager friendManager = context.mock(FriendManager.class);
        
        final CoreUploadListManager manager = new CoreUploadListManager(null, friendManager);
        
        context.checking(new Expectations() {
            {
                allowing(itemDone).getState();
                will(returnValue(UploadState.DONE));
                allowing(itemBrowseHostDone).getState();
                will(returnValue(UploadState.BROWSE_HOST_DONE));
                allowing(item1).getState();
                will(returnValue(UploadState.CANCELED));
                allowing(item2).getState();
                will(returnValue(UploadState.QUEUED));
                allowing(item3).getState();
                will(returnValue(UploadState.WAITING));
                
                // Assertions
                exactly(1).of(itemDone).addPropertyChangeListener(with(any(PropertyChangeListener.class)));
                exactly(1).of(itemBrowseHostDone).addPropertyChangeListener(with(any(PropertyChangeListener.class)));
                exactly(1).of(item1).addPropertyChangeListener(with(any(PropertyChangeListener.class)));
                exactly(1).of(item2).addPropertyChangeListener(with(any(PropertyChangeListener.class)));
                exactly(1).of(item3).addPropertyChangeListener(with(any(PropertyChangeListener.class)));
                
                never(itemDone).fireDataChanged();
                never(itemBrowseHostDone).fireDataChanged();
                exactly(1).of(item1).fireDataChanged();
                exactly(1).of(item2).fireDataChanged();
                exactly(1).of(item3).fireDataChanged();
            }});
        
        List<UploadItem> items = manager.getUploadItems();
        
        items.add(item1);
        items.add(itemDone);
        items.add(item2);
        items.add(item3);
        items.add(itemBrowseHostDone);
        
        manager.update();
        
        context.assertIsSatisfied();
    }
}

