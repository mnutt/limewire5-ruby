package com.limegroup.gnutella.auth;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.limewire.listener.EventListener;
import org.limewire.listener.EventMulticaster;
import org.limewire.listener.EventMulticasterImpl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.URN;

@Singleton
class UrnValidatorImpl implements UrnValidator {
    
    private final ContentManager contentManager;
    private final EventMulticaster<ValidationEvent> validationMulticaster;    
    private final Set<URN> requestingValidation;

    @Inject
    public UrnValidatorImpl(ContentManager contentManager) {
        this.contentManager = contentManager;
        this.validationMulticaster = new EventMulticasterImpl<ValidationEvent>();
        this.requestingValidation = Collections.synchronizedSet(new HashSet<URN>());
    }
    
    
    @Override
    public void addListener(EventListener<ValidationEvent> eventListener) {
        validationMulticaster.addListener(eventListener);
    }

    @Override
    public boolean isInvalid(URN urn) {
        ContentResponseData r = contentManager.getResponse(urn);
        return r != null && !r.isOK();
    }

    @Override
    public boolean isValid(URN urn) {
        return contentManager.isVerified(urn);
    }

    @Override
    public void validate(URN urn) {
        if(requestingValidation.add(urn)) {
            contentManager.request(urn, new ContentResponseObserver() {
               public void handleResponse(URN urn, ContentResponseData r) {
                   requestingValidation.remove(urn);
                   ValidationEvent.Type type;
                   if(r == null) {
                       type = ValidationEvent.Type.UNKNOWN;
                   } else if(r.isOK()) {
                       type = ValidationEvent.Type.VALID;
                   } else {
                       type = ValidationEvent.Type.INVALID;
                   }
                   validationMulticaster.broadcast(new ValidationEvent(type, urn));
               }
            }, 5000);
        }
    }

}
