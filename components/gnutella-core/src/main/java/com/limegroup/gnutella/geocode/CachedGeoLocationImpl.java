package com.limegroup.gnutella.geocode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.core.settings.GeocodeSettings;
import org.limewire.geocode.GeocodeInformation;
import org.limewire.geocode.Geocoder;
import org.limewire.geocode.GeocodeInformation.Property;
import org.limewire.io.NetworkUtils;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.limegroup.gnutella.NetworkManager;

@Singleton
public class CachedGeoLocationImpl implements CachedGeoLocation {

    private static final Log LOG = LogFactory.getLog(CachedGeoLocationImpl.class);
    
    private final Provider<Geocoder> geocoder;
    
    private volatile GeocodeInformation info;
    
    private final NetworkManager networkManager;
    
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Inject
    public CachedGeoLocationImpl(Provider<Geocoder> geocoder, NetworkManager networkManager) {
        this.geocoder = geocoder;
        this.networkManager = networkManager;
    }

    /* (non-Javadoc)
     * @see com.limegroup.gnutella.geocode.CachedGeoLocation#getGeocodeInformation()
     */
    public GeocodeInformation getGeocodeInformation() {
        // Note: if this is called fast enough in succession,
        // the second call may see null before this is initialized.        
        if (initialized.compareAndSet(false, true)) {
            LOG.debug("initializing");
            initialize();
        }
        return info;
    }

    void initialize() {
        info = GeocodeInformation.fromProperties(GeocodeSettings.GEO_LOCATION.getValue());
        if (info != null) {
            // check if the info is not stale
            byte[] currentAddress = networkManager.getExternalAddress();
            if (currentAddress == null) {
                LOG.debug("no address from networkmanager to compare with");
                // nothing to compare with
                return;
            }
            String lastAddress = info.getProperty(Property.Ip);
            if (lastAddress != null) {
                try {
                    byte[] lastIp = InetAddress.getByName(lastAddress).getAddress();
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("comparing addressed: " + lastAddress + " and " + NetworkUtils.ip2string(currentAddress));
                    }
                    if (NetworkUtils.isCloseIP(currentAddress, lastIp)) {
                        return;
                    }
                } catch (UnknownHostException e) {
                    LOG.warn("Unable to get host by name", e);	
                }
            }
        }
        Geocoder coder = geocoder.get();
        coder.initialize();
        info = coder.getGeocodeInformation();
        if (info != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("new info: " + info);
            }
            GeocodeSettings.GEO_LOCATION.setValue(info.toProperties());
        }
    }
}
