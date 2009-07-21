package org.limewire.ui.swing.upload;

import org.limewire.inject.LazySingleton;
import org.limewire.ui.swing.nav.NavMediator;

import com.google.inject.Inject;
import com.google.inject.Provider;

@LazySingleton
public class UploadMediator implements NavMediator<UploadPanel> {
    
    public static final String NAME = "UploadPanel";
    
    private final Provider<UploadPanel> uploadPanel;
    private UploadPanel upload;
    
    @Inject
    public UploadMediator(Provider<UploadPanel> uploadPanel) {
        this.uploadPanel = uploadPanel;
    }
        
    @Override
    public UploadPanel getComponent() {
        if(upload == null)
            upload = uploadPanel.get();
        return upload;
    }
}
