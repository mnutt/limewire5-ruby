package com.limegroup.gnutella.gui;

import com.google.inject.AbstractModule;
import com.limegroup.gnutella.ActivityCallback;
import com.limegroup.gnutella.bugs.BugManager;
import com.limegroup.gnutella.bugs.DeadlockBugManager;
import com.limegroup.gnutella.bugs.FatalBugManager;
import com.limegroup.gnutella.bugs.SessionInfo;
import com.limegroup.gnutella.gui.options.panes.BugsPaneItem;

public class LimeWireGUIModule extends AbstractModule {

    @Override
    protected void configure() {
        //DPINJ: Temporary measures...
        requestStaticInjection(GuiCoreMediator.class);        
        requestStaticInjection(BugsPaneItem.class);
        requestStaticInjection(BugManager.class);
        requestStaticInjection(DeadlockBugManager.class);
        requestStaticInjection(FatalBugManager.class);
        requestStaticInjection(Console.class);

        bind(ActivityCallback.class).to(VisualConnectionCallback.class);
        
        bind(SessionInfo.class).to(LimeSessionInfo.class);
        bind(LocalClientInfoFactory.class).to(LocalClientInfoFactoryImpl.class);
        
    }
    
    

}
