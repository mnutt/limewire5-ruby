package org.limewire.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * A generic class that allows you to mimic an interface.
 * See: http://www.coconut-palm-software.com/the_visual_editor/?p=25
 * for more information
 */
public class DuckType implements InvocationHandler {

    private final Object object;
    private final Class objectClass;
    
    private DuckType(Object object) {
        this.object = object;
        this.objectClass = object.getClass();
    }

    @SuppressWarnings("unchecked")
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Method realMethod = objectClass.getMethod(method.getName(), method.getParameterTypes());
        return realMethod.invoke(object, args);
    }
    
    /**
     * Returns an object that implements the given interface and
     * delegates all calls (through reflection) to the given object.
     * 
     * @param interfaceToImplement The interface that must be implemented
     * @param object The object calls will delegate to
     * @return A new object that implements the interface.
     */
    public static Object implement(Class interfaceToImplement, Object object) {
        if(!instanceOf(interfaceToImplement, object))
            throw new IllegalArgumentException("object: " + object + " does not have all the methods of: " + interfaceToImplement);
        return Proxy.newProxyInstance(interfaceToImplement.getClassLoader(), 
            new Class[] {interfaceToImplement}, new DuckType(object));
    }
    
    /**
     * Indicates if object is a (DuckType) instance of intrface.  That is,
     * is every method in intrface present on object.
     * 
     * @param intrface The interface to implement
     * @param object The object to test
     * @return true if every method in intrface is present on object.  false otherwise
     */
    @SuppressWarnings("unchecked")
    public static boolean instanceOf(Class intrface, Object object) {
        final Method[] methods = intrface.getMethods();
        Class candclass=object.getClass();
        for (int methodidx = 0; methodidx < methods.length; methodidx++) {
            Method method=methods[methodidx];
            try {
                candclass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException e) {
                return false;
            }
        }
        return true;
    }


}