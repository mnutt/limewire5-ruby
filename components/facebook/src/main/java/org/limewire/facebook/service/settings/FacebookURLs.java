package org.limewire.facebook.service.settings;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * Settings annotation for facebook urls.
 */
@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD}) 
@Retention(RetentionPolicy.RUNTIME)
public @interface FacebookURLs {
    public static final String HOME_PAGE_URL = "HOME_PAGE_URL";
    
    public static final String PRESENCE_POPOUT_PAGE_URL = "PRESENCE_POPOUT_PAGE_URL";
    
    public static final String CHAT_SETTINGS_URL = "CHAT_SETTINGS_URL";
    
    public static final String RECONNECT_URL = "RECONNECT_URL";
    
    public static final String LOGOUT_URL = "LOGOUT_URL";
    
    public static final String SEND_CHAT_URL = "SEND_CHAT_URL";
    
    public static final String SEND_CHAT_STATE_URL = "SEND_CHAT_STATE_URL";
    
    public static final String UPDATE_PRESENCES_URL = "UPDATE_PRESENCES_URL";
    
    public static final String RECEIVE_CHAT_URL = "RECEIVE_CHAT_URL";
}
