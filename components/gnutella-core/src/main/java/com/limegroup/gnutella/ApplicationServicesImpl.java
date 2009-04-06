package com.limegroup.gnutella;

import org.limewire.core.settings.ApplicationSettings;
import org.limewire.io.GUID;
import org.limewire.util.StringUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.util.LimeWireUtils;

@Singleton
public class ApplicationServicesImpl implements ApplicationServices {
    
    private final byte[] bittorrentGUID;
    private final byte[] limewireGUID;
    
    @Inject
    ApplicationServicesImpl() {
        byte [] myguid=null;
        try {
            myguid = GUID.fromHexString(ApplicationSettings.CLIENT_ID.get());
        }catch(IllegalArgumentException iae) {
            myguid = GUID.makeGuid();
            ApplicationSettings.CLIENT_ID.set((new GUID(myguid)).toHexString());
        }
        limewireGUID = myguid;
        
        byte []mybtguid = new byte[20];
        mybtguid[0] = 0x2D; // - 
        mybtguid[1] = 0x4C; // L
        mybtguid[2] = 0x57; // W
        System.arraycopy(StringUtils.toAsciiBytes(LimeWireUtils.BT_REVISION),0, mybtguid,3, 4);
        mybtguid[7] = 0x2D; // -
        System.arraycopy(limewireGUID,0,mybtguid,8,12);
        bittorrentGUID = mybtguid;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ApplicationServices#getMyBTGUID()
     */
    public byte [] getMyBTGUID() {
    	return bittorrentGUID;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ApplicationServices#getMyGUID()
     */
    public byte [] getMyGUID() {
        return limewireGUID;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.ApplicationServices#setFullPower(boolean)
     */
    public void setFullPower(boolean newValue) {
        // does nothing right now.
       // FIXME implement throttle switching for uploads and downloads
    }

}
