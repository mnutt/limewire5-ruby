package com.limegroup.gnutella.uploader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.altlocs.AltLocManager;
import com.limegroup.gnutella.altlocs.AlternateLocation;
import com.limegroup.gnutella.altlocs.DirectAltLoc;
import com.limegroup.gnutella.altlocs.PushAltLoc;
import com.limegroup.gnutella.helpers.UrnHelper;
import com.limegroup.gnutella.http.AltLocTracker;

class StubAltLocTracker extends AltLocTracker {
    private List<AlternateLocation> addedLocs = new ArrayList<AlternateLocation>();
    
    private Collection<DirectAltLoc> nextSetOfAltsToSend = null;
    private Collection<PushAltLoc> nextSetOfPushAltsToSend = null;

    public StubAltLocTracker() {
        super(UrnHelper.SHA1);
    }
    
    public StubAltLocTracker(URN urn) {
        super(urn);
    }

    @Override
    public void addLocation(AlternateLocation al) {
        addedLocs.add(al);
        super.addLocation(al);
    }
    
    public List<AlternateLocation> getAddedLocs() {
        return addedLocs;
    }

    @Override
    public Collection<DirectAltLoc> getNextSetOfAltsToSend(AltLocManager altLocManager) {
        if(nextSetOfAltsToSend == null)
            return super.getNextSetOfAltsToSend(altLocManager);
        else
            return nextSetOfAltsToSend;
    }

    @Override
    public Collection<PushAltLoc> getNextSetOfPushAltsToSend(AltLocManager altLocManager) {
        if(nextSetOfPushAltsToSend == null)
            return super.getNextSetOfPushAltsToSend(altLocManager);
        else
            return nextSetOfPushAltsToSend;
    }

    public void setNextSetOfAltsToSend(Collection<DirectAltLoc> nextSetOfAlts) {
        this.nextSetOfAltsToSend = nextSetOfAlts;
    }

    public void setNextSetOfPushAltsToSend(Collection<PushAltLoc> nextSetOfPushAlts) {
        this.nextSetOfPushAltsToSend = nextSetOfPushAlts;
    }
    
    
}