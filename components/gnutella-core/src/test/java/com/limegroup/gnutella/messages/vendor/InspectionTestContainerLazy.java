package com.limegroup.gnutella.messages.vendor;

import org.limewire.inject.LazySingleton;
import org.limewire.inspection.InspectablePrimitive;

@LazySingleton
public class InspectionTestContainerLazy {
    @InspectablePrimitive("")
    private String inspectedValue;

    private String otherValue;

    public String getInspectedValue() {
        return inspectedValue;
    }

    public void setInspectedValue(String inspectedValue) {
        this.inspectedValue = inspectedValue;
    }

    public String getOtherValue() {
        return otherValue;
    }

    public void setOtherValue(String otherValue) {
        this.otherValue = otherValue;
    }

}
