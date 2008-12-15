package org.limewire.core.impl.mojito;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.limewire.core.api.mojito.MojitoManager;
import org.limewire.mojito.MojitoDHT;

import com.google.inject.Inject;
import com.limegroup.gnutella.dht.DHTManager;

/**
 * Live implementation of MojitoManager.
 */
public class MojitoManagerImpl implements MojitoManager {

    /** DHT manager object. */
    private final DHTManager manager;

    /**
     * Constructs the live implementation of MojitoManager using the specified
     * DHT manager.
     */
    @Inject
    public MojitoManagerImpl(DHTManager manager) {
        this.manager = manager;
    }
    
    /**
     * Invokes the specified command on the Mojito DHT, and forwards output
     * to the specified PrintWriter.
     * @return true if command was successfully invoked
     */
    @Override
    @SuppressWarnings("unchecked")
    public boolean handle(String command, PrintWriter out) {
        // Define result.
        boolean result = false;
        
        try {
            // Get DHT using manager.
            MojitoDHT dht = manager.getMojitoDHT();
            if (dht == null) {
                out.println("Mojito is not running");
                return result;
            }

            // Get command handler method.
            Class cmdHandler = Class.forName("org.limewire.mojito.CommandHandler");
            Method handle = cmdHandler.getMethod("handle", 
                    new Class[]{MojitoDHT.class, String.class, PrintWriter.class});

            // Invoke method to pass command to DHT.
            result = ((Boolean) handle.invoke(null, 
                    new Object[]{dht, command, out})).booleanValue();

        } catch (ClassNotFoundException e) {
            e.printStackTrace(out);
        } catch (NoSuchMethodException e) {
            e.printStackTrace(out);
        } catch (IllegalAccessException e) {
            e.printStackTrace(out);
        } catch (InvocationTargetException e) {
            e.printStackTrace(out);
        }
        
        // Return result.
        return result;
    }
}
