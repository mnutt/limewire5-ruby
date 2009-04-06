package org.limewire.geocode;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.geocode.GeocodeInformation.Property;
import org.limewire.inject.MutableProvider;
import org.limewire.io.NetworkUtils;
import org.limewire.net.ExternalIP;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class CachedGeoLocationImpl implements Provider<GeocodeInformation> {

    private static final Log LOG = LogFactory.getLog(CachedGeoLocationImpl.class);

    private final MutableProvider<Properties> geoLocation;
    private final Provider<Geocoder> geocoder;
    
    private volatile GeocodeInformation info;
    private final Provider<byte[]> externalAddress;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Inject
    public CachedGeoLocationImpl(@GeoLocation MutableProvider<Properties> geoLocation,
                                 Provider<Geocoder> geocoder,
                                 @ExternalIP Provider<byte []> externalAddress) {
        this.geoLocation = geoLocation;
        this.geocoder = geocoder;
        this.externalAddress = externalAddress;
    }

    public GeocodeInformation get() {
        // Note: if this is called fast enough in succession,
        // the second call may see null before this is initialized.        
        if (initialized.compareAndSet(false, true)) {
            LOG.debug("initializing");
            initialize();
        }
        return info;
    }

    void initialize() {
        info = GeocodeInformation.fromProperties(geoLocation.get());
        if (info != null) {
            // check if the info is not stale
            byte[] currentAddress = externalAddress.get();
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
            geoLocation.set(info.toProperties());
        }
    }
}
