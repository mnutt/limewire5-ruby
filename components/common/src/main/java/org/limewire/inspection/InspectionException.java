package org.limewire.inspection;

import java.util.Collections;
import java.util.List;

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
    
    private final List<InspectionRequirement> requirements;

    public InspectionException() {
        this.requirements = Collections.emptyList();
    }

    public InspectionException(String msg) {
        super(msg);
        this.requirements = Collections.emptyList();
    }

    public InspectionException(Throwable cause) {
        super(cause);
        this.requirements = Collections.emptyList();
    }
    
    public InspectionException(String msg, List<InspectionRequirement> requirements) {
        super(msg);
        this.requirements = requirements;
    }
    
    public List<InspectionRequirement> getRequirements() {
        return requirements;
    }
}
