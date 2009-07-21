package org.limewire.inspection;

/**
 * Requirements before the field can be inspected. If none of the requirements
 * are met, the field will not be inspected.
 */
public enum InspectionRequirement {
    OS_WINDOWS, // must be running Windows to inspect this
    OS_LINUX,   // must be running Linux to inspect this
    OS_OSX      // must be running OS X to inspect this
}
