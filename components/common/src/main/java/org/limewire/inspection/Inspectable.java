package org.limewire.inspection;


/**
 * Defines the interface for objects that wish to be examined by 
 * {@link InspectionUtils}. When you implement this interface, you decided what
 * is returned as an <code>Object</code> via the <code>inspect</code> method.
 * <p>
 * See the Lime Wire Wiki for sample code using the <a href="http://www.limewire.org/wiki/index.php?title=Org.limewire.inspection">
 * org.limewire.inspection</a> package.
 */
public interface Inspectable {
    /**
     * @return a descriptive object for the target object.
     */
    public Object inspect();
}
