package org.limewire.promotion;

import java.io.InputStream;

/**
 * Defines an interface to create {@link PromotionBinder}s given an input
 * stream.
 */
public interface PromotionBinderFactory {

    /**
     * Returns a new {@link PromotionBinder}s given an input stream.
     * 
     * @param in given bytes defining the binder -- these <b>can</b> be
     *        <code>null</code>
     * @return a new {@link PromotionBinder}s given the bytes or
     *         <code>null</code> if <code>in</code> is <code>null</code>.
     */
    PromotionBinder newBinder(InputStream in);
}
