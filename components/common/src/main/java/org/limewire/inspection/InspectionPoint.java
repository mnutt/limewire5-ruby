package org.limewire.inspection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Named inspection point.  Not necessary at runtime. 
 */
@Target(ElementType.FIELD)
public @interface InspectionPoint  {
    String value();
}
