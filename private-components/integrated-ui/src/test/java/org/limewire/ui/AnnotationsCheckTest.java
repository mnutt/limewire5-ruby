package org.limewire.ui;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingUtilities;

import junit.framework.Test;

import org.limewire.gnutella.tests.LimeTestUtils;
import org.limewire.inject.GuiceUtils;
import org.limewire.inspection.InspectionException;
import org.limewire.inspection.InspectionRequirement;
import org.limewire.inspection.InspectionTool;
import org.limewire.inspection.Inspector;
import org.limewire.ui.swing.AllLimeWireModules__DO_NOT_USE;
import org.limewire.util.BaseTestCase;
import org.limewire.util.OSUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AnnotationsCheckTest extends BaseTestCase {

    public AnnotationsCheckTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AnnotationsCheckTest.class);
    }
    
    public void testAnnotations() throws Exception {
        final List<File> buildFolders = LimeTestUtils.getBuildFolders(getClass());
        final Map<String, String> results = new ConcurrentHashMap<String, String>();
        final AtomicReference<Injector> injectorRef = new AtomicReference<Injector>();
        // This is explicitly using the DoNotUse module because that's what the build uses.
        // (do it in the Swing thread to account for UI injectables that need the Swing thread)
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                Injector injector = Guice.createInjector(new AllLimeWireModules__DO_NOT_USE());
                GuiceUtils.loadEagerSingletons(injector);
                injectorRef.set(injector); 
                for(File path : buildFolders) {
                    results.putAll(InspectionTool.generateMappings(path, injectorRef.get(), new String[0]));
                }
            }
        });
        assertFalse(results.isEmpty());
        assertGreaterThan(100, results.size()); // make sure we got some good # of inspections
        
        // and run through them all and make sure they work!
        Inspector inspector = injectorRef.get().getInstance(Inspector.class);
        for(String key : results.keySet()) {
            try {
                inspector.inspect(results.get(key), true);
            } catch(InspectionException ie) {
                boolean validFailure = false;
                if(OSUtils.isLinux()) {
                    validFailure = ie.getRequirements().size() > 0 && !ie.getRequirements().contains(InspectionRequirement.OS_LINUX);
                } else if(OSUtils.isMacOSX()) {
                    validFailure = ie.getRequirements().size() > 0 && !ie.getRequirements().contains(InspectionRequirement.OS_OSX);
                } else if(OSUtils.isWindows()) {
                    validFailure = ie.getRequirements().size() > 0 && !ie.getRequirements().contains(InspectionRequirement.OS_WINDOWS);
                }
                if(!validFailure) {
                    fail("failed on key: " + key + ", value: " + results.get(key), ie);
                }
            }
        }
    }
}

