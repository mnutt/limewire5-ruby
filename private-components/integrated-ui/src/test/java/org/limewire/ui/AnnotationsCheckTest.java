package org.limewire.ui;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;

import org.limewire.inspection.InspectionTool;
import org.limewire.ui.swing.LimeWireModule;
import org.limewire.util.BaseTestCase;
import org.limewire.util.StringUtils;
import org.limewire.util.TestUtils;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class AnnotationsCheckTest extends BaseTestCase {

    public AnnotationsCheckTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(AnnotationsCheckTest.class);
    }
    
    private List<File> getClasses(File root) {
        final List<File> paths = new ArrayList<File>();
        // add all components that have a build/classes dir.
        root.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                File classes = new File(pathname, "build/classes");
                if(classes.exists()) {
                    paths.add(classes);
                }
                return false;
            }
        });
        
        return paths;
    }

    public void testAnnotations() throws Exception {
        File f = TestUtils.getResourceInPackage(this.getClass().getSimpleName() + ".class", this.getClass()).getCanonicalFile();
        // step out to find the root folder of the component
        int packageDepth = StringUtils.countOccurrences(this.getClass().getName(), '.');
        // + 1 to back out of top level package 
        for (int i = 0; i < packageDepth + 1; i++) {
            f = f.getParentFile();
        }
        // f now == <something>/limewire/[private-]components/component/build/tests
        // we want to back out to <something>/limewire
        //      build/          component/      components/      limewire/   
        f = f.getParentFile().getParentFile().getParentFile().getParentFile();
        
        List<File> paths = new ArrayList<File>();
        paths.addAll(getClasses(new File(f, "components")));
        paths.addAll(getClasses(new File(f, "private-components")));        
        assertGreaterThan(paths.toString(), 10, paths.size()); // make sure we got enough components

        Injector injector = Guice.createInjector(new LimeWireModule()); 
        Map<String, String> results = new HashMap<String, String>();
        for(File path : paths) {
            results.putAll(InspectionTool.generateMappings(path, injector, new String[0]));
        }
        assertFalse(results.isEmpty());
        assertGreaterThan(100, results.size()); // make sure we got some good # of inspections
    }
}

