package org.limewire.facebook.service.settings;

import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.google.inject.BindingAnnotation;

/**
 * Settings annotation for inspection urls.
 */
@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface InspectionsServerUrls {
    public static final String INSPECTION_SPEC_REQUEST_URL = "INSPECTION_SPEC_REQUEST_URL";
    public static final String INSPECTION_SPEC_SUBMIT_URL = "INSPECTION_SPEC_SUBMIT_URL";
}
