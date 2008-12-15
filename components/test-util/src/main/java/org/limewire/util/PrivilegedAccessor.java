package org.limewire.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * a.k.a. The "ObjectMolester"
 * <p>
 * This class is used to access a method or field of an object no
 * matter what the access modifier of the method or field.  The syntax
 * for accessing fields and methods is out of the ordinary because this
 * class uses reflection to peel away protection.
 * <p>
 * Here is an example of using this to access a private member.
 * <code>resolveName</code> is a private method of <code>Class</code>.
 *
 * <pre>
 * Class c = Class.class;
 * System.out.println(
 *      PrivilegedAccessor.invokeMethod( c,
 *                                       "resolveName",
 *                                       "/net/iss/common/PrivilegeAccessor" ) );
 * </pre>
 *
 * @author Charlie Hubbard (chubbard@iss.net)
 * @author Prashant Dhokte (pdhokte@iss.net)
 * @author Christopher Rohrs (added setValue)
 * @author Sam Berlin (added support for static fields/methods,
 *                     native parameters in methods, invokeConstructor,
 *                     and getClass)
 */
public class PrivilegedAccessor {

    /**
     * Gets the value of the named field and returns it as an object.
     *
     * @param instance the object instance
     * @param fieldName the name of the field
     * @return an object representing the value of the field
     */
    public static Object getValue(Object instance, String fieldName )     
           throws IllegalAccessException, NoSuchFieldException {
        Field field;
        if ( instance instanceof Class )
            field = getFieldImpl((Class)instance, fieldName);
        else
            field = getFieldImpl(instance.getClass(), fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }

    /**
     * Sets the value of the named field.
     *
     * @param instance the object instance
     * @param fieldName the name of the field
     * @param value an object representing the value of the field
     */
    public static void setValue(Object instance, 
                                String fieldName, 
                                Object value) 
            throws IllegalAccessException, NoSuchFieldException {
        Field field;
        if ( instance instanceof Class )
            field = getFieldImpl((Class)instance, fieldName);
        else
            field = getFieldImpl(instance.getClass(), fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /**
     * Calls a method on the given object instance with the given arguments.
     *
     * @param instance the object instance
     * @param methodName the name of the method to invoke
     * @param args an array of objects to pass as arguments
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object)
     */
    public static Object invokeMethod(Object instance, 
                                      String methodName, 
                                      Object... args ) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        return invokeMethod(instance, methodName, args, getArgumentTypes(args));
    }
    
    /**
     * Calls a method on the given object instance with the given arguments
     * and types.
     * Necessary for using native-type parameters and when the arguments
     * are subclassed objects.
     *
     * @param instance the object instance
     * @param methodName the name of the method to invoke
     * @param args an array of objects to pass as arguments
     * @param classTypes an array of the types of the arguments.
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object)
     */
    public static Object invokeMethod(Object instance, 
                                      String methodName, 
                                      Object[] args,
                                      Class[] classTypes ) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        return getMethod(instance,methodName,classTypes).invoke(instance,args);
    }
    
    /**
     * Calls a static method on the given class with the given argument.
     * The static method is called on all superclasses of the class to.
     * The call chain starts with the supermost class, eventually leading
     * back to the class supplied as the argument.
     *
     * @param clazz the entry point class
     * @param methodName the name of the method to invoke
     * @param arg the argument to pass to the method
     * @see PrivilegedAccessor#invokeMethod(Object,String,Object[])
     */
    public static List invokeAllStaticMethods(Class clazz,
                                              String methodName, 
                                              Object arg ) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        Object[] args = new Object[1];
        args[0] = arg;
        return invokeAllStaticMethods(clazz, methodName, args);
    }    
    
    /**
     * Calls a static method on the given class with the given arguments.
     * The static method is called on all superclasses of the class to.
     * The call chain starts with the supermost class, eventually leading
     * back to the class supplied as the argument.
     *
     * Returns an array of PairTuple of (Class, Object) where each entry
     * represents one return value, Class being the class that returned Object.
     *
     * @param clazz the class
     * @param methodName the name of the method to invoke
     * @param args an array of objects to pass as arguments
     * @see PrivilegedAccessor#invokeAllStaticMethods(Class,String,Object)
     */
    public static List invokeAllStaticMethods(Class clazz,
                                              String methodName, 
                                              Object[] args ) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        return invokeAllStaticMethods(clazz, methodName, args, getArgumentTypes(args));
    }

    /**
     * Calls a static method on the given class with the given arguments.
     * The static method is called on all superclasses of the class to.
     * The call chain starts with the supermost class, eventually leading
     * back to the class supplied as the argument.
     *
     * Returns an array of PairTuple of (Class, Object) where each entry
     * represents one return value, Class being the class that returned Object.
     *
     * Necessary for using native-type parameters and when the arguments
     * are subclassed objects.
     *
     * @param clazz the class
     * @param methodName the name of the method to invoke
     * @param args an array of objects to pass as arguments
     * @param classTypes an array of the types of the arguments.
     * @see PrivilegedAccessor#invokeAllStaticMethods(Class,String,Object)
     */
    public static List<PairTuple> invokeAllStaticMethods(Class clazz,
                                              String methodName, 
                                              Object[] args,
                                              Class[] classTypes ) 
        throws NoSuchMethodException,
               IllegalAccessException, 
               InvocationTargetException  {
        List<Method> methods = getAllStaticMethods(clazz ,methodName,classTypes);
        List<PairTuple> ret = new LinkedList<PairTuple>();
        for(Iterator<Method> i = methods.iterator(); i.hasNext(); ) {
            Method m = i.next();
            Object val = m.invoke(null, args);
            ret.add(new PairTuple(m.getDeclaringClass(), val));
        }
        return ret;
    }    

    /**
     *
     * @param instance the object instance
     * @param methodName the
     */
    public static Method getMethod(Object instance, 
                                   String methodName, 
                                   Class[] classTypes ) 
                                   throws NoSuchMethodException {
        Method accessMethod;
        if ( instance instanceof Class )
            accessMethod = getMethodImpl((Class)instance, methodName, classTypes);
        else
            accessMethod = getMethodImpl(instance.getClass(), methodName, classTypes);
        accessMethod.setAccessible(true);
        return accessMethod;
    }
    
    /**
     * @param class the class to find the statics methods in
     * @param methodName the name of the static method
     * @param the parameter classes
     */
    @SuppressWarnings("unchecked")
    public static List<Method> getAllStaticMethods(Class entryClass,
                                           String methodName, 
                                           Class[] classTypes ) 
                                           throws NoSuchMethodException {
        List<Method> methods = new LinkedList<Method>();
        Class clazz = entryClass;
        while(clazz != null) {
            try {
                Method add = clazz.getDeclaredMethod(methodName, classTypes);
                add.setAccessible(true);
                methods.add(0, add);
            } catch(NoSuchMethodException ignored) {}
            clazz = clazz.getSuperclass();
        }
        if(methods.isEmpty())
            throw new NoSuchMethodException("Invalid method: " + methodName);
        return methods;
    }    
    
    /**
     * Returns the class 'name' that was declared by class 'parent'.
     *
     * @param parent the class who you want to look in for this class
     * @param name the class you're looking for.
     */
    public static Class getClass(Class parent, String name)
      throws ClassNotFoundException {
        Class clazz = getClassImpl(parent, name);
        return clazz;
    }
    
    /**
     * Constructs an object with the given parameters.
     */
    public static Object invokeConstructor(Class clazz,
                                           Object[] args ) 
        throws NoSuchMethodException,
               IllegalAccessException,
               InvocationTargetException,
               InstantiationException {
        return invokeConstructor(clazz, args, getArgumentTypes(args));
    }
    
    /**
     * Returns the array of classes for the array of arguments. 
     */
    private static Class[] getArgumentTypes(Object[] args) {
        Class[] classTypes = null;
        if( args != null) {
            classTypes = new Class[args.length];
            for( int i = 0; i < args.length; i++ ) {
                if( args[i] != null )
                    classTypes[i] = args[i].getClass();
            }
        }
        return classTypes;
    }
    
    /**
     * Constructs an object with the given parameters and classtypes
     */
    public static Object invokeConstructor(Class clazz,
                                         Object[] args, Class[] classTypes)
        throws NoSuchMethodException,
               IllegalAccessException,
               InvocationTargetException,
               InstantiationException {
        Constructor cs = getConstructorImpl(clazz, classTypes);
        cs.setAccessible(true);
        return cs.newInstance(args);
    }                                                 
    
    

    /**
     * Return the named field from the given class.
     */
    private static Field getFieldImpl(Class thisClass, 
                                  String fieldName) 
                                  throws NoSuchFieldException {
        if (thisClass == null)
            throw new NoSuchFieldException("Invalid field : " + fieldName);
        try {
            return thisClass.getDeclaredField( fieldName );
        }
        catch(NoSuchFieldException e) {
            return getFieldImpl(thisClass.getSuperclass(), fieldName);
        }
    }

    /**
     * Return the named method with a method signature matching classTypes
     * from the given class.
     */
    @SuppressWarnings("unchecked")
    private static Method getMethodImpl(Class thisClass, 
                                    String methodName, 
                                    Class[] classTypes) 
                                    throws NoSuchMethodException {
        if (thisClass == null)
            throw new NoSuchMethodException("Invalid method : " + methodName);
        try {
            return thisClass.getDeclaredMethod( methodName, classTypes );
        }
        catch(NoSuchMethodException e) {
            return getMethodImpl(thisClass.getSuperclass(), methodName, classTypes);
        }
    }
    
    /**
     * Return the constructor with the given parameters.
     */
    private static Constructor getConstructorImpl(Class clazz, Class[] classTypes)
      throws NoSuchMethodException {
        Constructor[] cs = clazz.getDeclaredConstructors();
        for (int i = 0; i < cs.length; i++) {
            // check for constructors which have arguments
            if ( Arrays.equals(classTypes, cs[i].getParameterTypes()) )
                return cs[i];
            // check for no argument constructor                
            if ( cs[i].getParameterTypes().length == 0 && classTypes == null )
                return cs[i];
        }
        throw new NoSuchMethodException("invalid constructor for class: " + clazz);
    }
    
    /**
     * Return the class with the given name.
     */
    private static Class getClassImpl(Class parent, String name)
      throws ClassNotFoundException {
        Class[] clazzes = parent.getDeclaredClasses();
        for(int i = 0; i < clazzes.length; i++) {
            if ( clazzes[i].getName().equals(parent.getName() + "$" + name) )
                return clazzes[i];
        }
        throw new ClassNotFoundException("Invalid class : " + parent.getName() + "$" + name);
    }
}
