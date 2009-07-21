package org.limewire.facebook.service.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * Settings annotation for facebook auth server urls.
 */
@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface FacebookAuthServerUrls {
}
