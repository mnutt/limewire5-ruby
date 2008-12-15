package org.limewire.ui.swing.images;

import com.google.inject.AbstractModule;

public class LimeWireUiImagesModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ThumbnailManager.class).to(ThumbnailManagerImpl.class);
    }
}
