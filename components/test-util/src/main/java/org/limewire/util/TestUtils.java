package org.limewire.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class TestUtils {

    /**
     * Gets a resource file using the class loader
     * or the system class loader.
     */
    public static File getResourceFile(String location) {
        ClassLoader cl = TestUtils.class.getClassLoader();            
        URL resource = null;
    
        if(cl == null) {
            resource = ClassLoader.getSystemResource(location);
        } else {
            resource = cl.getResource(location);
        }
        
        if( resource == null ) {
            // note: this will probably not work,
            // but it will ultimately trigger a better exception
            // than returning null.
            return new File(location);
        }
        
        //NOTE: The resource URL will contain %20 instead of spaces.
        // This is by design, but will not work when trying to make a file.
        // See BugParadeID: 4466485
        //(http://developer.java.sun.com/developer/bugParade/bugs/4466485.html)
        // The recommended workaround is to use the URI class, but that doesn't
        // exist until Java 1.4.  So, we can't use it here.
        // Thus, we manually have to parse out the %20s from the URL
        return new File( decode(resource.getFile()) );
    }
    
    private static String decode(String s) {
        StringBuilder sb = new StringBuilder();
        for(int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    break;
                case '%':
                    try {
                        sb.append((char)Integer.parseInt(
                                        s.substring(i+1,i+3),16));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(s);
                    }
                    i += 2;
                    break;
                default:
                    sb.append(c);
                    break;
            }
        }
        // Undo conversion to external encoding
        String result = sb.toString();
        try {
            byte[] inputBytes = result.getBytes("8859_1");
            result = new String(inputBytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    public static File getResourceInPackage(String resourceName, Class nearResource) {
        String name = nearResource.getPackage().getName().replace(".", "/");
        return getResourceFile(name + "/" + resourceName);
    }

    public static <T, F extends T> Module createInstanceModule(final Class<T> interfaze, final F implemenation) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(interfaze).toInstance(implemenation);
            }
        };
    }
    
    /**
     * Experimental fluent interface to binding test classes. 
     */
    public static To bind(Class... interfaces) {
        return new To(interfaces);
    }
    
    public static class To {
        
        private final Class[] interfaces;

        public To(Class...interfaces) {
            this.interfaces = interfaces;
        }
        
        public Module to(final Class... implementations) {
            if (interfaces.length != implementations.length) {
                throw new IllegalArgumentException("length of interfaces doesn't not match implementations");
            }
            return new AbstractModule() {
                @SuppressWarnings("unchecked")
                @Override
                protected void configure() {
                    for (int i = 0; i < interfaces.length; i++) {
                        bind(interfaces[i]).to(implementations[i]);
                    }
                }
            };
        }
        
        public Module toInstances(final Object...instances) {
            if (interfaces.length != instances.length) {
                throw new IllegalArgumentException("length of interfaces doesn't not match instances");
            }
            return new AbstractModule() {
                @SuppressWarnings("unchecked")
                @Override
                protected void configure() {
                    for (int i = 0; i < interfaces.length; i++) {
                        bind(interfaces[i]).toInstance(instances[i]);
                    }
                }
            };
        }
        
    }

}
