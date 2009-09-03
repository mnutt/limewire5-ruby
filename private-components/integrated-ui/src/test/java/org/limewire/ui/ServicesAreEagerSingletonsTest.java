package org.limewire.ui;

import java.io.File;
import java.util.Set;

import org.apache.bcel.classfile.AnnotationEntry;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.inject.EagerSingleton;
import org.limewire.lifecycle.Service;
import org.limewire.util.BaseTestCase;

import com.google.common.collect.ImmutableSet;

public class ServicesAreEagerSingletonsTest extends BaseTestCase {

    private final static ImmutableSet<String> exceptions = ImmutableSet.of("org.limewire.lifecycle.ServiceRegistryImpl$ServiceHolder$AnnotatedService",
            "org.limewire.ui.swing.dock.DockIconMacOSXImpl", "org.limewire.lifecycle.ServiceSchedulerImpl"); 
    
    public void testAllServicesHaveEagerSingletonAnnotations() throws Throwable {
        Set<File> classFiles = LimeTestUtils.getAllClassFiles(this.getClass());
        for (File classFile : classFiles) {
            JavaClass javaClass = new ClassParser(classFile.getAbsolutePath()).parse();
            if (javaClass.isAbstract()) {
                continue;
            }
            if (exceptions.contains(javaClass.getClassName())) {
                continue;
            }
            if (implementsInterface(javaClass, Service.class)) {
                if (javaClass.isNested() || javaClass.isAnonymous()) {
                    String className = javaClass.getClassName();
                    int lastDot = className.lastIndexOf('.');
                    String outerClassName = className.substring(lastDot == -1 ? 0 : lastDot + 1, className.lastIndexOf('$')).concat(".class");
                    File outerClassFile = new File(classFile.getParent(), outerClassName);
                    assertTrue(outerClassFile.toString(), outerClassFile.exists());
                    assertIsAnnotatedWith(new ClassParser(outerClassFile.getAbsolutePath()).parse(), EagerSingleton.class);
                } else {
                    assertIsAnnotatedWith(javaClass, EagerSingleton.class);
                }
            }
        }
    }
    
    private void assertIsAnnotatedWith(JavaClass javaClass, Class clazz) {
        if (exceptions.contains(javaClass.getClassName())) {
            return;
        }
        String className = clazz.getName().replace('.', '/');
        for (AnnotationEntry entry :javaClass.getAnnotationEntries()) {
            if (entry.getAnnotationType().contains(className)) {
                return;
            }
        }
        fail(javaClass.getClassName() + " is not annotated with " + clazz.getName());
    }

    private static boolean implementsInterface(JavaClass javaClass, Class clazz) throws Throwable {
        String interfaceName = clazz.getName();
        JavaClass[] interfaces = javaClass.getAllInterfaces();
        for (JavaClass interfaze : interfaces) {
            if (interfaze.getClassName().equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }
}
