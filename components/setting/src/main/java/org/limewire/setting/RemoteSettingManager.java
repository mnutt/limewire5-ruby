package org.limewire.setting;

/**
 * 
 * Defines the interface to control retrieving a remote setting.
 * Additionally, this interface lists a method to set the
 * {@link RemoteSettingController} which in turn updates a key-value setting.
 * <p>
 * A setting must be created via 
 * {@link SettingsFactory SettingsFactory's createRemoteXSetting} in order to 
 * be remotely controlled.
 * 
 */
public interface RemoteSettingManager {
    
    /**
     * Retrieves a remote value for the given key.
     * This is intended to be used to retrieve values
     * for settings that are created after the RemoteManager
     * has initialized.  Because of this, after the values
     * are retrieved in this method, it is okay for the
     * RemoteSettingManager to delete the value.  When a new
     * setting arrives, the controller should be used to update
     * the value for the given remoteKey.
     * 
     * If there is no value loaded, this should return null.
     */
    public String getUnloadedValueFor(String remoteKey);
    
    /**
     * Sets a controller that should be used to update settings
     * when new remote values are read.
     */
    public void setRemoteSettingController(RemoteSettingController controller);

}
