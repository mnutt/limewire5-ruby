package org.limewire.mojito.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.limewire.concurrent.OnewayExchanger;
import org.limewire.mojito.Context;
import org.limewire.mojito.KUID;
import org.limewire.mojito.MojitoDHT;
import org.limewire.mojito.concurrent.DHTTask;
import org.limewire.mojito.manager.BootstrapManager;


public class UnitTestUtils {
    
    private UnitTestUtils() {
        
    }
    
    @SuppressWarnings("unchecked")
    public static void setBootstrapping(MojitoDHT dht, boolean bootstrapping) throws Exception {
        Context context = (Context)dht;
        Field bmField = Context.class.getDeclaredField("bootstrapManager");
        bmField.setAccessible(true);
        
        BootstrapManager bootstrapManager = (BootstrapManager)bmField.get(context);
        Field futureField = BootstrapManager.class.getDeclaredField("future");
        futureField.setAccessible(true);
        
        synchronized (bootstrapManager) {
            if (bootstrapping) {
                Class clazz = Class.forName("org.limewire.mojito.manager.BootstrapManager$BootstrapFuture");
                Constructor con = clazz.getDeclaredConstructor(BootstrapManager.class, DHTTask.class);
                con.setAccessible(true);
                
                Object future = con.newInstance(bootstrapManager, new DHTTask() {
                    public void cancel() {
                        throw new UnsupportedOperationException();
                    }

                    public long getWaitOnLockTimeout() {
                        throw new UnsupportedOperationException();
                    }

                    public void start(OnewayExchanger exchanger) {
                        throw new UnsupportedOperationException();
                    } 
                });
                
                futureField.set(bootstrapManager, future);
            } else {
                futureField.set(bootstrapManager, null);
            }
        }
    }
    
    public static void setBootstrapped(MojitoDHT dht, boolean bootstrapped) throws Exception {
        Context context = (Context)dht;
        Field bmField = Context.class.getDeclaredField("bootstrapManager");
        bmField.setAccessible(true);
        
        BootstrapManager bootstrapManager = (BootstrapManager)bmField.get(context);
        bootstrapManager.setBootstrapped(bootstrapped);
    }
    
    public static void setNodeID(MojitoDHT dht, KUID nodeId) throws Exception {
        Method m = dht.getClass().getDeclaredMethod("setLocalNodeID", new Class[]{KUID.class});
        m.setAccessible(true);
        m.invoke(dht, new Object[]{nodeId});
    }
   
}
