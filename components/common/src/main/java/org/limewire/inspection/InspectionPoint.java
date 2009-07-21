package org.limewire.inspection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Named inspection point.  Not necessary at runtime. 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface InspectionPoint  {
    String value();
    InspectionRequirement[] requires() default {};
}
