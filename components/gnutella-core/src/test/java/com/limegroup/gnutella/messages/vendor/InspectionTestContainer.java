package com.limegroup.gnutella.messages.vendor;

import org.limewire.inspection.InspectablePrimitive;

import com.google.inject.Singleton;

@Singleton
public class InspectionTestContainer {
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
