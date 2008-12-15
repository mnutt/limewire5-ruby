package org.limewire.inspection;

/**
 * Thrown upon a problem with inspection.
 * <p>
 * For example, {@link InspectionUtils#inspectValue(String)} must have at least two 
 * commas in the String argument; otherwise, <code>InspectionUtils</code> throws 
 * <code>InspectionException</code>. 
 * <p>
 * Additionally, <code>InspectionUtils</code> throws this exception if the 
 * inspection is called for an object which is not an
 * {@link Inspectable}, and if the object set to 
 * inspect is not annotated with {@link InspectablePrimitive @InspectablePrimitive},
 * and is not annotated with {@link InspectableForSize @InspectableForSize}.
 * <p>
 * See the Lime Wire Wiki for sample code using the <a href="http://www.limewire.org/wiki/index.php?title=Org.limewire.inspection">
 * org.limewire.inspection</a> package.
 * 
 */
public class InspectionException extends Exception {

    public InspectionException() {
    }

    public InspectionException(String msg) {
        super(msg);
    }

    public InspectionException(Throwable cause) {
        super(cause);
    }
}
