package org.limewire.facebook.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * Generic facebook annotation for facebook specific bindings of common
 * interfaces.
 */
@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface Facebook {
}
