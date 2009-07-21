package org.limewire.inspection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

import org.limewire.inject.MoreScopes;
import org.limewire.util.OSUtils;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.ConvertedConstantBinding;
import com.google.inject.spi.ExposedBinding;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.spi.UntargettedBinding;


/**
 * Gets the value of an object that implements {@link Inspectable} or an object
 * with an annotation of {@link InspectableForSize @InspectableForSize} or 
 * {@link InspectablePrimitive @InspectablePrimitive}. 
 * <p>
 * See the Lime Wire Wiki for sample code using the <a href="http://www.limewire.org/wiki/index.php?title=Org.limewire.inspection">
 * org.limewire.inspection</a> package.
 * 
 */

public class InspectionUtils {
    
    private static class InspectionData {
        String encodedField;
        boolean isStatic;
        Field field;
        List<Annotation> annotations;
        Class<?> lookupClass;
        Class<?> actualClass;
        Object fieldContainerInstance;
        Object fieldValue;
    }

    /**
     * 
     * Inspects a field and returns a representation of that field. The field
     * must:
     * <p>
     * a) implement the <code>Inspectable</code> interface, in which case the
     * return value of the <code>inspect</code> method is returned, or else
     * <p>
     * b) be annotated with <code>@InspectablePrimitive</code>, in which case
     * the <code>String.valueOf</code> is returned, or else
     * <p>
     * c) be annotated with {@link InspectableForSize @InspectableForSize} and
     * have a <code>size</code> method in which case the return value of the
     * <code>size</code> method call is returned.
     * 
     * @param encodedField - name of the field we want to get, starting with a
     *        fully qualified class name, and followed by comma-separated field
     *        names that will help us reach the target field.
     * 
     * @return the object the field represents, or an Exception object if such
     *         was thrown trying to get it.
     */
    public static Object inspectValue(String encodedField, Injector injector)
            throws InspectionException {
        try {
            InspectionData data = createInspectionData(encodedField);
            validateField(data);
            setFieldContainerInstance(data, injector);
            setFieldValue(data);
            return inspect(data);
        } catch (Throwable e) {
            if (e instanceof InspectionException) {
                throw (InspectionException) e;
            } else {
                throw new InspectionException(e);
            }
        }
    }
    
    private static InspectionData createInspectionData(String encodedField) throws Throwable {
        InspectionData data;
        if(encodedField.contains(":")) {
            data = getStaticField(encodedField);
        } else {
            data = getInjectedField(encodedField);
        }
        
        data.annotations = Arrays.asList(data.field.getAnnotations());
        return data;
    }
    
    private static InspectionData getStaticField(String encodedField) throws Throwable {
        StringTokenizer t = new StringTokenizer(encodedField, ":");
        if (t.countTokens() != 2) {
            throw new InspectionException("invalid encoded field: " + encodedField);
        }
        
        Class clazz = Class.forName(t.nextToken());
        Field field = clazz.getDeclaredField(t.nextToken());
        field.setAccessible(true);
        
        InspectionData data = new InspectionData();
        data.encodedField = encodedField;
        data.isStatic = true;
        data.fieldValue = clazz;
        data.field = field;
        return data;
    }
    
    private static InspectionData getInjectedField(String encodedField) throws Throwable {
        InspectionData data = new InspectionData();
        
        Class lookupClass = null;
        if(encodedField.contains("|")) {
            StringTokenizer tokenizer = new StringTokenizer(encodedField, "|");
            if(tokenizer.countTokens() != 2) {
                throw new InspectionException("invalid encoded field: " + encodedField);
            }
            lookupClass = Class.forName(tokenizer.nextToken());
            encodedField = tokenizer.nextToken();
        }        
        
        StringTokenizer t = new StringTokenizer(encodedField, ",");
        if(t.countTokens() != 2) {
            throw new InspectionException("invalid encoded field: " + encodedField);
        }
        Class clazz = Class.forName(t.nextToken());
        Field field = clazz.getDeclaredField(t.nextToken());
        field.setAccessible(true);       
        
        data.encodedField = encodedField;
        data.lookupClass = lookupClass;
        data.actualClass = clazz;
        data.field = field;
        return data;
    }
    
    private static void validateField(InspectionData data) throws InspectionException {
        boolean valid = false;
        for(Annotation annotation : data.annotations) {
            if(annotation.annotationType() == InspectionPoint.class) {
                validateLimitations(((InspectionPoint)annotation).requires(), data);
                valid = true;
                break;
            } else if(annotation.annotationType() == InspectablePrimitive.class) {
                validateLimitations(((InspectablePrimitive)annotation).requires(), data);
                valid = true;
                break;
            } else if(annotation.annotationType() == InspectableForSize.class) {
                validateLimitations(((InspectableForSize)annotation).requires(), data);
                valid = true;
                break;
            }
        }
        if(!valid) {
            throw new InspectionException("field not annotated for inspection: " + data.field);
        }
    }
    
    private static void validateLimitations(InspectionRequirement[] limitations, InspectionData data) throws InspectionException {
        boolean valid = false;
        if(limitations != null && limitations.length > 0) {
            for(InspectionRequirement limitation : limitations) {
                switch(limitation) {
                case OS_LINUX:
                    valid |= OSUtils.isLinux();
                    break;
                case OS_OSX:
                    valid |= OSUtils.isMacOSX();
                    break;
                case OS_WINDOWS:
                    valid |= OSUtils.isWindows();
                    break;
                }
            }
        } else {
            valid = true;
        }
        
        if(!valid) {
            List<InspectionRequirement> requires = Arrays.asList(limitations);
            throw new InspectionException("invalid limitations: " + requires + " on field: " + data.field, requires);
        }
    }
    
    private static void setFieldContainerInstance(InspectionData data, Injector injector) throws Throwable {
        // no container instance for static data.
        if(!data.isStatic) {
            if(data.lookupClass == null) {
                // if the container had an enclosing class and it's not static, make sure the lookup
                // points to the enclosing class.
                if(data.actualClass.getEnclosingClass() != null && !Modifier.isStatic(data.actualClass.getModifiers())) {
                    data.lookupClass = data.actualClass.getEnclosingClass();
                }
            }
            
            // check if this is an enclosed class
            if (data.lookupClass == null) {
                Binding<?> binding = injector.getBinding(data.actualClass);
                validateBindingIsSingleton(injector, binding);
                data.fieldContainerInstance = binding.getProvider().get();
            } else {            
                // inner classes must be annotated properly
                Binding<?> binding = injector.getBinding(data.lookupClass);
                validateBindingIsSingleton(injector, binding);
                Object lookupObj = binding.getProvider().get();

                // If this was an enclosed class, validate that it has InspectableContainer.
                if(data.actualClass.getEnclosingClass() != null && !Modifier.isStatic(data.actualClass.getModifiers())) {
                    if (data.actualClass.getAnnotation(InspectableContainer.class) == null) {
                        throw new InspectionException("container must be annotated with InspectableContainer");
                    }
                    Constructor[] constructors = data.actualClass.getDeclaredConstructors();
                    if (constructors.length != 1) {
                        throw new InspectionException("wrong constructors length: " + constructors.length);
                    }
                    Class[] parameters = constructors[0].getParameterTypes();
                    if (parameters.length != 1 || !data.lookupClass.isAssignableFrom(parameters[0])) {
                        throw new InspectionException("wrong parameter count or type for constructor");
                    }
                    constructors[0].setAccessible(true);
                    data.fieldContainerInstance = constructors[0].newInstance(lookupObj);
                } else {
                    data.fieldContainerInstance = lookupObj;
                }
                
            }
        }
    }
    
    private static void setFieldValue(InspectionData data) throws Throwable {        
        if (data.isStatic) {
            data.fieldValue = data.field.get(null);
        } else {
            data.fieldValue = data.field.get(data.fieldContainerInstance);
        }
    }
    
    private static void validateBindingIsSingleton(Injector injector, Binding<?> binding) throws InspectionException {
        binding = resolveBinding(injector, binding);
        boolean singleton = binding.acceptScopingVisitor(new BindingScopingVisitor<Boolean>() {
            public Boolean visitEagerSingleton() { return true; }
            public Boolean visitNoScoping() { return false; }
            public Boolean visitScope(com.google.inject.Scope scope) { return scope == Scopes.SINGLETON || scope == MoreScopes.LAZY_SINGLETON; }
            public Boolean visitScopeAnnotation(java.lang.Class<? extends Annotation> scopeAnnotation) { return false; }
        });
            
        if(!singleton) {
            throw new InspectionException("must be singleton or lazysingleton annotation or be interface!");
        }
    }
    
    /** Resolves a binding to the ultimate destination, ensuring we can check the scope of the proper link. */
    private static Binding<?> resolveBinding(final Injector injector, final Binding<?> binding) {
        return binding.acceptTargetVisitor(new BindingTargetVisitor<Object, Binding<?>>() {

            @Override
            public Binding<?> visit(InstanceBinding<? extends Object> link) {
                return binding;
            }

            @Override
            public Binding<?> visit(ProviderInstanceBinding<? extends Object> link) {
                return binding;
            }

            @Override
            public Binding<?> visit(ProviderKeyBinding<? extends Object> link) {
                return resolveBinding(injector, injector.getBinding(link.getProviderKey()));
            }

            @Override
            public Binding<?> visit(LinkedKeyBinding<? extends Object> link) {
                return resolveBinding(injector, injector.getBinding(link.getLinkedKey()));
            }

            @Override
            public Binding<?> visit(ExposedBinding<? extends Object> link) {
                return binding;
            }

            @Override
            public Binding<?> visit(UntargettedBinding<? extends Object> link) {
                return binding;
            }

            @Override
            public Binding<?> visit(ConstructorBinding<? extends Object> link) {
                return binding;
            }

            @Override
            public Binding<?> visit(ConvertedConstantBinding<? extends Object> link) {
                return binding;
            }

            @Override
            public Binding<?> visit(ProviderBinding<? extends Object> link) {
                return resolveBinding(injector, injector.getBinding(link.getProvidedKey()));
            }
            
        });
    }
    
    /**
     * Gets a string representation of an object.
     * 
     * @param o the object to be inspected
     * @param annotations annotations that were found in the last field traversed 
     * while looking for this object
     * @return a String representation taken either from Inspectable.inspect(),
     * String.valueOf or size() depending on the annotation or type of the field.
     */
    private static Object inspect(InspectionData data) throws Exception {
        if (data.fieldValue instanceof Inspectable) {
            Inspectable i = (Inspectable)data.fieldValue;
            return i.inspect();
        }
        
        for (Annotation a : data.annotations) {
            if (a instanceof InspectablePrimitive)
                return String.valueOf(data.fieldValue);

            if (a instanceof InspectableForSize) {
                Method m = data.fieldValue.getClass().getMethod("size", new Class[0]);
                m.setAccessible(true);
                return m.invoke(data.fieldValue).toString();
            }
        }
        
        throw new InspectionException("no valid Inspectable annotation!");
    }
}
