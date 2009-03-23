package com.limegroup.gnutella.version;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.limegroup.gnutella.ApplicationServices;

@Singleton
public class UpdateCollectionFactoryImpl implements UpdateCollectionFactory {

    private final ApplicationServices applicationServices;

    @Inject
    public UpdateCollectionFactoryImpl(ApplicationServices applicationServices) {
        this.applicationServices = applicationServices;
    }
    
    public UpdateCollection createUpdateCollection(String xml) {
        return new UpdateCollectionImpl(xml, applicationServices);
    }

}
