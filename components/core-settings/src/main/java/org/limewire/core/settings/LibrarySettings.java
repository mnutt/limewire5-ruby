package org.limewire.core.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

public class LibrarySettings extends LimeProps {
    
    /**
     * A saved list of names of all the friendLists that have been created.
     */
    public static final StringArraySetting SHARED_FRIEND_LIST_NAMES = 
        (StringArraySetting)FACTORY.createStringArraySetting("SHARED_FRIEND_LIST_NAMES", new String[0]).setPrivate(true);
    
    /**
     * Removes a name of a saved friendlist.
     */
    public static final void removeFriendListName(String id) {
        String[] names = SHARED_FRIEND_LIST_NAMES.getValue();
        List<String> nameList = new ArrayList<String>(Arrays.asList(names));
        nameList.remove(id);
        
        if(nameList.size() != names.length) {
            SHARED_FRIEND_LIST_NAMES.setValue(nameList.toArray(new String[nameList.size()]));
        }
    }
    /**
     * Adds a new name to the list of shared friend list names if one does not 
     * already exist. Returns true if the name was added or false if it 
     * already existed
     */
    public static final boolean addFriendListName(String id) {
        String[] names = SHARED_FRIEND_LIST_NAMES.getValue();
        List<String> nameList = new ArrayList<String>(Arrays.asList(names));
        if(nameList.contains(id))
            return false;
        else {
            nameList.add(id);
            SHARED_FRIEND_LIST_NAMES.setValue(nameList.toArray(new String[nameList.size()]));
            return true;
        }
    }
    /**
     * A list of all friend names with whom all images in the library will be shared with.
     * When a friend exists in this list, all new images added to the library will automatically
     * be shared with that friend. 
     */
    public static StringArraySetting SHARE_NEW_IMAGES_ALWAYS =
        (StringArraySetting)FACTORY.createStringArraySetting("SHARE_NEW_IMAGES_ALWAYS", new String[0]).setPrivate(true);
    /**
     * Returns true if this friend is sharing all images and new images, false
     * otherwise.
     */
    public static final boolean containsFriendShareNewImages(String id) {
        String[] names = SHARE_NEW_IMAGES_ALWAYS.getValue();
        List<String> nameList = new ArrayList<String>(Arrays.asList(names));
        return nameList.contains(id);
    }
    /**
     * A list of all friend names with whom all audio files in the library will be shared with.
     * When a friend exists in this list, all new audio files added to the library will automatically
     * be shared with that friend. 
     */
    public static StringArraySetting SHARE_NEW_AUDIO_ALWAYS =
        (StringArraySetting)FACTORY.createStringArraySetting("SHARE_NEW_AUDIO_ALWAYS", new String[0]).setPrivate(true);
    /**
     * Returns true if this friend is sharing all audio files and new audio files,
     * false otherwise.
     */
    public static final boolean containsFriendShareNewAudio(String id) {
        String[] names = SHARE_NEW_AUDIO_ALWAYS.getValue();
        List<String> nameList = new ArrayList<String>(Arrays.asList(names));
        return nameList.contains(id);
    }
    /**
     * A list of all friend names with whom all videos in the library will be shared with.
     * When a friend exists in this list, all new videos added to the library will automatically
     * be shared with that friend. 
     */
    public static StringArraySetting SHARE_NEW_VIDEO_ALWAYS =
        (StringArraySetting)FACTORY.createStringArraySetting("SHARE_NEW_VIDEO_ALWAYS", new String[0]).setPrivate(true);
    /**
     * Returns true if this friend is sharing all videos and new video files,
     * false otherwise.
     */
    public static final boolean containsFriendShareNewVideo(String id) {
        String[] names = SHARE_NEW_VIDEO_ALWAYS.getValue();
        List<String> nameList = new ArrayList<String>(Arrays.asList(names));
        return nameList.contains(id);
    }
    /**
     * Adds a friend to the list of names to always files with. 
     * @param id - name of the friend to share with
     * @return true if the name was added, false if the name already existed.
     */
    public static final boolean addFriendShareNewFiles(StringArraySetting stringArray, String id) {
        String[] names = stringArray.getValue();
        List<String> nameList = new ArrayList<String>(Arrays.asList(names));
        if(nameList.contains(id))
            return false;
        else {
            nameList.add(id);
            stringArray.setValue(nameList.toArray(new String[nameList.size()]));
            return true;
        }
    }
    /**
     * Removes a friend from the list of names to always share files with. This friend
     * will no longer automatically share new files of this type that are added to the library.
     * @param id - friend to remove
     */
    public static final void removeFiendShareNewFiles(StringArraySetting stringArray, String id) {
        String[] names = stringArray.getValue();
        List<String> nameList = new ArrayList<String>(Arrays.asList(names));
        nameList.remove(id);
        
        if(nameList.size() != names.length) {
            stringArray.setValue(nameList.toArray(new String[nameList.size()]));
        }
    }
    
    /**True, only takes a snapshot of your current files, false enables current and future files to be automatically shared */
    public static final BooleanSetting SNAPSHOT_SHARING_ENABLED =
        FACTORY.createBooleanSetting("SNAPSHOT_SHARING_ENABLED", true);
    
    /** True if documents can be shared with gnutella. */
    public static final BooleanSetting ALLOW_DOCUMENT_GNUTELLA_SHARING =
        FACTORY.createBooleanSetting("DOCUMENT_SHARING_ENABLED", false);
    
    /** True if programs are allowed in the library at all. */
    public static final BooleanSetting ALLOW_PROGRAMS =
        FACTORY.createBooleanSetting("PROGRAMS_ALLOWED", false);
    
    /** The current version of the library. */
    public static final StringSetting VERSION =
        FACTORY.createStringSetting("LIBRARY_VERSION", LibraryVersion.FOUR_X.name());
    
    /** True if AUDIO files are managed. */
    public static final BooleanSetting MANAGE_AUDIO =
        FACTORY.createBooleanSetting("MANAGE_AUDIO_FILES", true);
    
    /** True if VIDEO files are managed. */
    public static final BooleanSetting MANAGE_VIDEO =
        FACTORY.createBooleanSetting("MANAGE_VIDEO_FILES", true);
    
    /** True if IMAGES files are managed. */
    public static final BooleanSetting MANAGE_IMAGES =
        FACTORY.createBooleanSetting("MANAGE_IMAGES_FILES", true);
    
    /** True if DOCUMENTS files are managed. */
    public static final BooleanSetting MANAGE_DOCUMENTS =
        FACTORY.createBooleanSetting("MANAGE_DOCUMENTS_FILES", true);
    
    /** True if PROGRAMS files are managed. */
    public static final BooleanSetting MANAGE_PROGRAMS =
        FACTORY.createBooleanSetting("MANAGE_PROGRAMS_FILES", false);
    
    /** True if OTHER files are managed. */
    public static final BooleanSetting MANAGE_OTHER =
        FACTORY.createBooleanSetting("MANAGE_OTHER_FILES", true);
    
    public static enum LibraryVersion {
        FOUR_X, FIVE_0_0;
    }
    
}
