package org.limewire.ui.swing.util;

/**
 * This stub gives public constructor visibility to harness classes outside of its
 * package for the purpose of using in testing.
 */
public class IconManagerStub extends IconManager {

    public IconManagerStub() {
        //TODO: Inject
        super(new BasicFileIconController(new CategoryIconManager()));
    }
    
}
