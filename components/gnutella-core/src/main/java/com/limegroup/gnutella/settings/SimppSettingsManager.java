package com.limegroup.gnutella.settings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.service.ErrorService;
import org.limewire.setting.RemoteSettingController;
import org.limewire.setting.RemoteSettingManager;

/**
 * A RemoteSettingManager that uses SIMPP to remotely control the settings.
 * <p>
 * Before this can be used, you MUST call setRemoteSettingController,
 * otherwise activating any simpp settings will fail.
 * (Note: this is typically done by calling SettingsFactory.setRemoteSettingManager
 *        with this instance.)
 */
public class SimppSettingsManager implements RemoteSettingManager {

    private static final Log LOG = LogFactory.getLog(SimppSettingsManager.class);
    
    /** 
     * The properties we create from the string we get via simpp message
     * <p>
     * LOCKING: _remoteController 
     */
    private final Properties _simppProps = new Properties();

    /**
     * A mapping of simppKeys to simppValues which have not been initialized
     * yet. Newly created settings must check with this map to see if they
     * should load default value or the simpp value.
     * <p>
     * LOCKING: _remoteController
     */
    private final Map<String, String> _remainderSimppSettings = new HashMap<String, String>();
    
    /** The controller used to set remote settings. */
    private volatile RemoteSettingController _remoteController;
    
    /**
     * Call this method with the verified simppSettings which are used to
     * replace other settings if they exist in the system.
     */
    public void updateSimppSettings(String simppSettings) {
        if(_remoteController == null)
            throw new IllegalStateException("No RemoteSettingController set!");
        
        synchronized(_remoteController) {
            byte[] settings = null;
            try {            
                settings = simppSettings.getBytes("UTF-8");
            } catch (UnsupportedEncodingException uex) {
                ErrorService.error(uex);
                return;
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(settings);
            _simppProps.clear();
            try {
                _simppProps.load(bais);
            } catch(IOException iox) {
                LOG.error("IOX reading simpp properties", iox);
                return;
            }
            activateSimppSettings();
        }
    }

    /**
     * Call this method if you want to activate the settings to the ones in
     * this.simppProps.
     */
    @SuppressWarnings({ "unchecked" })
    public void activateSimppSettings() {
        LOG.debug("activating new settings");
        if(_remoteController == null)
            throw new IllegalStateException("No RemoteSettingController set!");
        synchronized(_remoteController) {
            for(Map.Entry<Object, Object> entry : _simppProps.entrySet()) {
                String key = (String)entry.getKey();
                String value = (String)entry.getValue();
                if(!_remoteController.updateSetting(key, value))
                    _remainderSimppSettings.put(key, value);
            }
            Set keys = _simppProps.keySet();
            _remoteController.revertRemoteSettingsUnlessIn(keys);
        }
    }
    
    /**
     * @return the simpp value for a simppkey from the map that remembers simpp
     * settings which have not been loaded yet. Removes the entry from the
     * mapping since it is no longer needed, now that the setting has been
     * created.
     */
    public String getUnloadedValueFor(String simppKey) {
        synchronized(_remoteController) {
            return _remainderSimppSettings.remove(simppKey);
        }
    }

    public void setRemoteSettingController(RemoteSettingController controller) {
        _remoteController = controller;
    }

}
