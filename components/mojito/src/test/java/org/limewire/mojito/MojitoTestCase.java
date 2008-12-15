package org.limewire.mojito;

import org.limewire.io.SimpleNetworkInstanceUtils;
import org.limewire.mojito.settings.ContextSettings;
import org.limewire.mojito.settings.MojitoProps;
import org.limewire.mojito.settings.NetworkSettings;
import org.limewire.mojito.util.ContactUtils;
import org.limewire.util.BaseTestCase;

public abstract class MojitoTestCase extends BaseTestCase {
    
    protected MojitoTestCase(String name) {
        super(name);
    }
    
    /**
     * Called statically before any settings.
     */
    public static void beforeAllTestsSetUp() throws Throwable {
        MojitoProps.instance().revertToDefault();
        MojitoProps.instance().getFactory().getRevertSetting().setValue(false);
    }
    
    @Override
    public void preSetUp() throws Exception {
        super.preSetUp();
        
        MojitoProps.instance().revertToDefault();
        MojitoProps.instance().getFactory().getRevertSetting().setValue(false);
        
        // DHT Settings
        ContextSettings.SHUTDOWN_MESSAGES_MULTIPLIER.setValue(0);
        
        setLocalIsPrivate(false);
        
        // We're working on the loopback. Everything should be done
        // in less than 500ms
        NetworkSettings.DEFAULT_TIMEOUT.setValue(500);
        
        // Nothing should take longer than 1.5 seconds. If we start seeing
        // LockTimeoutExceptions on the loopback then check this Setting!
        ContextSettings.WAIT_ON_LOCK.setValue(1500);
    }
    
    @Override
    public void postTearDown() {
        super.postTearDown();
        setLocalIsPrivate(true);
    }
    
    public void setLocalIsPrivate(boolean localIsPrivate) {
        NetworkSettings.LOCAL_IS_PRIVATE.setValue(localIsPrivate);
        NetworkSettings.FILTER_CLASS_C.setValue(localIsPrivate);
        ContactUtils.setNetworkInstanceUtils(new SimpleNetworkInstanceUtils(localIsPrivate));
    }
}
