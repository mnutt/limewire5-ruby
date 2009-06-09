package com.limegroup.gnutella.simpp;

import java.util.List;


import com.limegroup.gnutella.ReplyHandler;
import com.limegroup.gnutella.settings.SimppSettingsManager;

public interface SimppManager {
    
    public void initialize();

    public int getVersion();

    /**
     * @return the cached value of the simpp bytes. 
     */
    public byte[] getSimppBytes();

    public void addSimppSettingsManager(SimppSettingsManager simppSettingsManager);

    public List<SimppSettingsManager> getSimppSettingsManagers();

    public void addListener(SimppListener listener);

    public void removeListener(SimppListener listener);

    public void checkAndUpdate(final ReplyHandler handler, final byte[] data);

    public byte[] getOldUpdateResponse();

}
