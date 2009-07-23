package org.limewire.http.webservice;

import org.cometd.Bayeux;
import org.cometd.Client;
import org.cometd.Message;
import org.mortbay.cometd.BayeuxService;
import org.mortbay.log.Log;

public class CometMonitorService extends BayeuxService
{
    public CometMonitorService(Bayeux bayeux)
    {
        super(bayeux,"monitor");
        subscribe("/meta/subscribe","monitorSubscribe");
        subscribe("/meta/unsubscribe","monitorUnsubscribe");
        // subscribe("/meta/*","monitorMeta");
        // subscribe("/**","monitorVerbose");
    }
    
    public void monitorSubscribe(Client client, Message message)
    {
        Log.info("Subscribe from "+client+" for "+message.get(Bayeux.SUBSCRIPTION_FIELD));
    }
    
    public void monitorUnsubscribe(Client client, Message message)
    {
         Log.info("Unsubscribe from "+client+" for "+message.get(Bayeux.SUBSCRIPTION_FIELD));
    }
    
    public void monitorMeta(Client client, Message message)
    {
        if (Log.isDebugEnabled())
            Log.debug(message.toString());
    }
    
    /*
    public void monitorVerbose(Client client, Message message)
    {
        System.err.println(message);
        try 
        {
            Thread.sleep(5000);
        }
        catch(Exception e)
        {
            Log.warn(e);
        }
    }
    */
}