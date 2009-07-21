package org.limewire.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A helper class that makes setting and reverting the values of private fields easier,
 *  more consistent, and slightly safer.
 * <p> 
 * NOTE: This class should be avoided at all costs.  Currently it only exists for testing
 *  classes that use LimeWireUtils.  For static classes reset should always be called prior
 *  to any conditions that might end execution of a test case.
 */
public class PrivateAccessor {
    
    private final Field field;
    private final Object instance;
    private final Object originalValue;
    
    private final Object fieldAccessor;
    private final Method setMethod;
    
    /**
     * Constructs and initializes the PrivateAccessor.  Does not allow overriding of final.
     * 
     * @param clazz  the Class object to access the field in 
     * @param instance  the instance of the class, or null for static classes
     * @param fieldName  the name of a private field to access
     */
    public PrivateAccessor(Class<?> clazz, Object instance, String fieldName) throws 
        SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException,
        ClassNotFoundException, NoSuchMethodException, InvocationTargetException {
        
        this(clazz, instance, fieldName, false);
    }
    
    /**
     * Constructs and initializes the PrivateAccessor.  Allows overriding of final.
     * 
     * <p>Should not be used in real test cases, for adhoc testing only.
     * 
     * @param clazz  the Class object to access the field in 
     * @param instance  the instance of the class, or null for static classes
     * @param fieldName  the name of a private field to access
     * @param overrideFile  allows overriding of final variables, use with 
     *                       caution final primitives and strings
     *                       defined at compile time will usually be inlined and thus
     *                       this will not have predictable results.
     */
    @Deprecated
    public PrivateAccessor(Class<?> clazz, Object instance, String fieldName, boolean overrideFinal)
        throws SecurityException, NoSuchFieldException, IllegalArgumentException,
        IllegalAccessException, ClassNotFoundException, 
        NoSuchMethodException, InvocationTargetException {
        
        this.instance = instance;
        field = clazz.getDeclaredField(fieldName);
        
        if (!field.isAccessible()) {
            field.setAccessible(true);
            originalValue = field.get(instance);
            field.setAccessible(false);
    
            if (overrideFinal) {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                int modifiers = modifiersField.getInt(field);
                modifiers &= ~Modifier.FINAL;
                modifiersField.setInt(field, modifiers);
            
                Class<?> reflectionFactoryClass = Class.forName("sun.reflect.ReflectionFactory");
                Method reflectionFactoryGetter = reflectionFactoryClass.getMethod("getReflectionFactory");
            
                Object reflectionFactory = reflectionFactoryGetter.invoke(null);
            
                Method createFieldAccessorMethod = reflectionFactory.getClass().getDeclaredMethod("newFieldAccessor",
                    Field.class, boolean.class);
            
                fieldAccessor = createFieldAccessorMethod.invoke(reflectionFactory, field, false);
                setMethod = fieldAccessor.getClass().getMethod("set", Object.class, Object.class);
                setMethod.setAccessible(true);
            } 
            else {
                fieldAccessor = null;
                setMethod = null;
            }
        }
        else {
            throw new IllegalArgumentException("PrivateAcessor is not used for accessable fields");
        }
    }

    /**
     * Resets the field to its original value.
     */
    public void reset() {
        try {
            setValue(originalValue);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Field should be resetable after a successful construction?", e);
        }
    }
    
    /**
     * Changes the field's value to the one provided. 
     */
    public void setValue(Object value) throws InvocationTargetException {
        field.setAccessible(true);
        try {
            if (setMethod != null) {
                setMethod.invoke(fieldAccessor, instance, value);
            } 
            else {
                field.set(instance, value);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Field should be accessable after a successful construction?", e);
        } finally {
            field.setAccessible(false);
        }
    }
    
    /**
     * Returns the field's original value before any modifications were made.
     */
    public Object getOriginalValue() {
        return originalValue;
    }
    
    /**
     * Returns the field's current value.
     */
    public Object getValue() {
        Object value = null;
        
        field.setAccessible(true);
        try {
            value = field.get(instance);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Field should be accessable after a successful construction?");
        } finally {
            field.setAccessible(false);
        }

        return value;
    }
}
