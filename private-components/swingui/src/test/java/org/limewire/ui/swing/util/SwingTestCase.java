package org.limewire.ui.swing.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;
import org.limewire.util.TestUtils;

// TODO: Rood dir stuff is broken!!
public abstract class SwingTestCase extends BaseTestCase {

    protected static File baseDir;
    
    public SwingTestCase(String name) {
        super(name);
    }
    
    /**
     * Called statically before any settings.
     */
    public static void beforeAllTestsSetUp() throws Throwable {        
        setupUniqueDirectories();
    }

    /**
     * Sets this test up to have unique directories.
     */
    protected static void setupUniqueDirectories() throws Exception {
        
        if( baseDir == null ) {
            baseDir = createNewBaseDirectory( _testClass.getName() );
        }

        baseDir.mkdirs();

        baseDir.deleteOnExit();
    }
    
    /**
     * Helper function to create a new temporary file of the given size,
     * with the given name & extension, in the given directory.
     */
    public static File createNewNamedTestFile(int size, String name,
                                              String extension, File directory) throws Exception {
        if(name.length() < 3) {
            name = name + "___";
        }
        File file = File.createTempFile(name, "." + extension, directory);
        file.deleteOnExit();
        OutputStream out = new FileOutputStream(file);

        for (int i=0; i<size; i++) {
            out.write(name.charAt(i % name.length()));
        }
        out.flush();
        out.close();
            
        //Needed for comparisons between "C:\Progra~1" and "C:\Program Files".
        return FileUtils.getCanonicalFile(file);
    }
    
    /**
     * Creates a new directory prepended by the given name.
     */
    protected static File createNewBaseDirectory(String name) throws Exception {
        File t = getTestDirectory();
        File f = new File(t, name);
        
        int append = 1;
        while ( f.exists() ) {
            f = new File(t, name + "_" + append);
            append++;
        }
        
        return f.getCanonicalFile();
    }
    
    /**
     * Get tests directory from a marker resource file.
     */
    protected static File getTestDirectory() throws Exception {
        return new File(getRootDir(), "testData");
    }   
    
    protected static File getRootDir() throws Exception {
        // Get a marker file.
        File f = TestUtils.getResourceFile("swingui/src/test/java/org/limewire/ui/swing/util/SwingTestCase.java");
        f = f.getCanonicalFile();
                 //util          // swing       // ui           // limewire       //org          //java         // test         //src             //swingui       //private-components
        return f.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();        
    }

}
