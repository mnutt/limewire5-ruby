package com.limegroup.gnutella;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.OSUtils;
import org.limewire.util.PrivilegedAccessor;

/* This extends BaseTestCase on purpose!  We don't want the overhead of LimeTestCase! */
public class LimeCoreGlueTest extends BaseTestCase {
    
    public LimeCoreGlueTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(LimeCoreGlueTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    // This test requires that the ClassLoader is implemented as we expect it.
    // If a ClassLoader implementation changes, this test needs to change.
    public void testPreinstallDoesntLoadExtraClasses() throws Exception {
        
        List<Class> loaded = getLoadedClasses();
        
        assertFalse("CommonUtils can't already be loaded!", 
                    containsClass(loaded, "org.limewire.util.CommonUtils"));
        
        LimeCoreGlue.preinstall();
        
        List<Class> nextLoaded = getLoadedClasses();
        
        List<String> expected = new LinkedList<String>(Arrays.asList(new String[]
                            {"com.limegroup.gnutella.LimeCoreGlue", 
                             "com.limegroup.gnutella.LimeCoreGlue$InstallFailedException",
                             "org.limewire.setting.RemoteSettingManager",
                             "com.limegroup.gnutella.util.LimeWireUtils", 
                             "org.limewire.util.CommonUtils", 
                             "org.limewire.util.OSUtils",
                             "com.limegroup.gnutella.util.Portable",
                             "com.limegroup.gnutella.util.PortableImpl",
                             "org.limewire.lifecycle.Service",
                             "org.limewire.logging.LogFactory",
                             "org.limewire.logging.Log",
                             "org.limewire.logging.LogImpl"}));
        
        if(OSUtils.isWindows() || OSUtils.isMacOSX()) {
            expected.add("org.limewire.util.SystemUtils");
            expected.add("org.limewire.util.SystemUtils$SpecialLocations");
        }
        
        if(!OSUtils.isPOSIX()) {
            expected.add("org.limewire.util.FileUtils");
        }
        
        removeClasses(nextLoaded, expected);
        
        ArrayList<Class> extraClasses = new ArrayList<Class>(nextLoaded);
        extraClasses.removeAll(loaded);
        assertEquals("loaded more classes than expected " + extraClasses +   
                     " -- make sure nothing is using CommonUtils.getUserSettingsDir too early!",
                     loaded, nextLoaded);
    }
    
    @SuppressWarnings("unchecked")
    private List<Class> getLoadedClasses() throws Exception {
        ClassLoader loader = LimeCoreGlueTest.class.getClassLoader();
        List<Class> list = new ArrayList<Class>((List<Class>)PrivilegedAccessor.getValue(loader, "classes"));
        for(Iterator<Class> i = list.iterator(); i.hasNext(); ) {
            Class next = i.next();
            if(!next.getName().startsWith("org.limewire.") && !next.getName().startsWith("com.limegroup"))
                i.remove();
        }
        return list;
    }
    
    private boolean containsClass(List<Class> classes, String expected) {
        for(Class clazz : classes)
            if(clazz.getName().equals(expected))
                return true;
        return false;
    }
    
    private void removeClasses(List<Class> classes, List<String> expected) {
        List<String> found = new ArrayList<String>();
        for(Iterator<Class> it = classes.iterator(); it.hasNext(); ) {
            Class next = it.next();
            String name = next.getName();
            // remove artificial class name postfix added by Clover 
            int i = name.indexOf("$__CLR2_");
            if (i != -1) {
                name = name.substring(0, i);
            }
            if (expected.contains(name)) {
                it.remove();
                found.add(name);
            }
        }
        
        expected.removeAll(found);
        
        assertTrue("didn't find all expected classes: " + expected, expected.isEmpty());
    }
}
