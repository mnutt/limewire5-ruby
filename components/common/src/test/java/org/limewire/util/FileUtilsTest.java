package org.limewire.util;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.limewire.util.SystemUtils.SpecialLocations;

import junit.framework.Test;

/**
 * Class for testing the FileUtils utility methods.
 */
public final class FileUtilsTest extends BaseTestCase {

    /**
     * @param name
     */
    public FileUtilsTest(String name) {
        super(name);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public static Test suite() {
        return buildTestSuite(FileUtilsTest.class);
    }

    /**
     * Tests the method for extracting the file extension from a file.
     * 
     * @throws Exception if any error occurs.
     */
    public void testGetFileExtension() throws Exception {
        final String EXT = "ext";
        File[] testFiles = new File[] { new File("standard." + EXT),
                new File("a.little.weird." + EXT), new File(".ext." + EXT), };

        for (File testFile : testFiles) {
            assertEquals("unexpected extension extracted", EXT, FileUtils
                    .getFileExtension(testFile));
        }

        // test files that should return null
        File[] nullFiles = new File[] { new File("standard."), new File("." + EXT),
                new File("noextension"), };

        for (File nullFile : nullFiles) {
            assertEquals("extension should be empty string", "", FileUtils
                    .getFileExtension(nullFile));
        }
    }

    public void testGetFilesRecursive() throws Exception {
        File tmpFile = File.createTempFile("tmp", "file");
        File tmpDir = tmpFile.getParentFile();
        tmpFile.delete();

        File emptyDir = new File(tmpDir, "emptydir");
        File emptyNameDir = new File(tmpDir, "emptyname");
        File emptyExtensionDir = new File(tmpDir, "emptyextension");
        File recursiveDir = new File(tmpDir, "recursivedir");

        emptyDir.mkdir();
        emptyNameDir.mkdir();
        emptyExtensionDir.mkdir();
        recursiveDir.mkdir();

        emptyDir.deleteOnExit();
        emptyNameDir.deleteOnExit();
        emptyExtensionDir.deleteOnExit();
        recursiveDir.deleteOnExit();

        File subDir = new File(emptyDir, "subdir");
        subDir.mkdir();
        subDir.deleteOnExit();

        File f = new File(emptyNameDir, ".emptyname");
        f.createNewFile();
        f.deleteOnExit();

        f = new File(emptyExtensionDir, "emptyextension.");
        f.createNewFile();
        f.deleteOnExit();

        f = new File(recursiveDir, "afile.txt");
        f.createNewFile();
        f.deleteOnExit();

        f = new File(recursiveDir, "subdir");
        f.mkdir();
        f.deleteOnExit();
        f = new File(f, "subfile.txt");
        f.createNewFile();
        f.deleteOnExit();

        File[] fa = FileUtils.getFilesRecursive(emptyDir, null);
        assertEquals("directory should have no files, only a subdir", 0, fa.length);

        fa = FileUtils.getFilesRecursive(emptyNameDir, null);
        assertEquals("directory should have 1 hidden file", 1, fa.length);

        fa = FileUtils.getFilesRecursive(emptyNameDir, new String[] { "emptyname" });
        assertEquals("directory should have no file matching extension \"emptyname\"", 0, fa.length);

        fa = FileUtils.getFilesRecursive(emptyExtensionDir, null);
        assertEquals("directory should have one file", 1, fa.length);

        fa = FileUtils.getFilesRecursive(emptyExtensionDir, new String[] { "" });
        assertEquals("directory should have no file matching empty extension", 0, fa.length);

        // test if files in subdirectories are found too
        fa = FileUtils.getFilesRecursive(recursiveDir, null);
        assertEquals("wrong number of files found", 2, fa.length);

        // test if files in subdirectories are found with filter
        fa = FileUtils.getFilesRecursive(recursiveDir, new String[] { "unmatchedextension", "",
                "txt" });
        assertEquals("wrong number of matching files found", 2, fa.length);
    }

    /**
     * Tests the setWriteable method.
     */
    public void testSetWriteable() throws Exception {
        PrivilegedAccessor.setValue(SystemUtils.class, "isLoaded", Boolean.TRUE);

        File testFile = File.createTempFile("test", "file");
        testFile.deleteOnExit();
        testFile.setReadOnly();
        assertTrue(testFile.exists());
        assertTrue(testFile.isFile());
        assertTrue(!FileUtils.canWrite(testFile));
        assertTrue("Cannot set file writable: " + testFile, FileUtils.setWriteable(testFile));
        assertTrue(FileUtils.canWrite(testFile));
        SystemUtils.setWriteable(testFile.getPath());
        assertTrue(FileUtils.setWriteable(testFile));
        File testDir = new File("directory");
        testDir.deleteOnExit();
        testDir.mkdirs();
        assertTrue(testDir.exists());
        assertTrue(testDir.isDirectory());
        testDir.setReadOnly();
        assertTrue(!FileUtils.canWrite(testDir));
        File testInTestDir = new File(testDir, "testDirTest");
        testInTestDir.deleteOnExit();
        try {
            testInTestDir.createNewFile();
            // This test doesn't work and is not important.
            // fail("created file in test dir");
        } catch (IOException expected) {
        }
        assertTrue(FileUtils.setWriteable(testDir));
        testInTestDir.delete();
        assertTrue(testInTestDir.createNewFile());
        assertTrue(FileUtils.canWrite(testDir));
        // Make sure it doesn't die if called on a file that doesn't exist
        File nowhere = new File("m'kay");
        assertTrue(!nowhere.exists());
        assertTrue(FileUtils.setWriteable(nowhere));
        assertTrue(!FileUtils.canWrite(nowhere)); // doesn't exist, can't write.
    }

    public void testIsReallyParent() throws Exception {
        /* Simple benign case */
        File testFile = new File("/a/b/c/d.txt");
        File testDirectory = new File("/a/b/c");
        assertTrue("Simple benign case failed.", FileUtils.isReallyParent(testDirectory, testFile));

        /* Simple malicious case */
        testFile = new File("/e/a/b/c/d.txt");
        testDirectory = new File("/a/b/c");
        assertFalse("Simple malicious case failed.", FileUtils.isReallyParent(testDirectory,
                testFile));

        /* Benign use of ../ */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/b/../b/c/../c/d.txt");
        assertTrue("Benign ../ case failed.", FileUtils.isReallyParent(testDirectory, testFile));

        /* Malicious use of ../ */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/b/c/../e/d.txt");
        assertFalse("Malicious ../ case failed.", FileUtils.isReallyParent(testDirectory, testFile));

        /* Complex benign case */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/e/../b/c/d.txt");
        assertTrue("Complex benign case failed.", FileUtils.isReallyParent(testDirectory, testFile));

        /* Complex malicious case */
        testDirectory = new File("/a/b/c");
        testFile = new File("/a/e/../b/c/d.txt");
        assertTrue("Complex benign case failed.", FileUtils.isReallyParent(testDirectory, testFile));

        /* Simple relative benign case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/d.txt");
        assertTrue("Simple relative benign case failed.", FileUtils.isReallyParent(testDirectory,
                testFile));

        /* Simple relative malicious case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/e/c/d.txt");
        assertFalse("Simple relative malicious case failed.", FileUtils.isReallyParent(
                testDirectory, testFile));

        /* Benign use of ../ in relative paths */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/../b/c/../c/d.txt");
        assertTrue("Benign use of ../ in relative case failed.", FileUtils.isReallyParent(
                testDirectory, testFile));

        /* Malicious use of ../ in relative paths */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../e/d.txt");
        assertFalse("Malicious use of ../ in relative case failed.", FileUtils.isReallyParent(
                testDirectory, testFile));

        /* Multi-level benign case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../../../a/b/c/d.txt");
        assertTrue("Multi-level benign case failed.", FileUtils.isReallyParent(testDirectory,
                testFile));

        /* Multi-level malicious case */
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../../../e/b/c/d.txt");
        assertFalse("Multi-level malicious case failed.", FileUtils.isReallyParent(testDirectory,
                testFile));
    }

    public void testReallyInParentPath() throws Exception {
        File testDirectory;
        File testFile;
        testDirectory = new File("a/b/c");
        testFile = new File("a/b/c/../../../a/b/c/d/e/f.txt");
        assertTrue(FileUtils.isReallyInParentPath(testDirectory, testFile));
        testFile = new File("a/b/c/../../../e/b/c/d.txt");
        assertFalse(FileUtils.isReallyInParentPath(testDirectory, testFile));
        testFile = new File("/");
        assertNull(testFile.getParentFile());
        assertFalse(FileUtils.isReallyInParentPath(testDirectory, testFile));
    }

    private File createHierarchy() throws IOException {
        File tmpFile = File.createTempFile("test", "test");
        tmpFile.deleteOnExit();

        File tmpDir = tmpFile.getParentFile();

        File dir = new File(tmpDir, "testRecursiveDeleteDir");
        assertTrue(dir.mkdirs());
        dir.deleteOnExit();

        File hiddenFile = new File(dir, ".hidden");
        assertTrue(hiddenFile.createNewFile());
        hiddenFile.deleteOnExit();

        File file = new File(dir, "file.file");
        assertTrue(file.createNewFile());
        file.deleteOnExit();

        File subDir = new File(dir, "subdir");
        assertTrue(subDir.mkdirs());
        subDir.deleteOnExit();

        File hiddenSubFile = new File(subDir, ".hidden.file");
        assertTrue(hiddenSubFile.createNewFile());
        hiddenSubFile.deleteOnExit();

        File hiddenSubdir = new File(dir, ".hiddenSubdir");
        assertTrue(hiddenSubdir.mkdirs());
        hiddenSubdir.deleteOnExit();

        return dir;
    }

    public void testDeleteRecursive() throws IOException {
        File dir = createHierarchy();
        assertTrue(FileUtils.deleteRecursive(dir));
        assertFalse(dir.exists());
    }
    
    public void testForceDeleteRecursive() throws IOException {
        File dir = createHierarchy();
        assertTrue(FileUtils.forceDeleteRecursive(dir));
        assertFalse(dir.exists());
    }

    public void testGetJarFromClasspath() throws Exception {
        assertNull(FileUtils.getJarFromClasspath("does/not/exist"));
        File file = FileUtils.getJarFromClasspath("org/apache/commons/logging/Log.class");
        assertNotNull(file);
        assertTrue(file.exists());
        // there might be version numbers between commons-logging & .jar, so
        // ignore them.
        assertTrue(file.getName(), file.getName().startsWith("commons-logging"));
        assertTrue(file.getName(), file.getName().endsWith(".jar"));
    }

    public void testGetJarFromClasspathClassLoader() throws Exception {
        ClassLoader classLoader = FileUtils.class.getClassLoader();
        assertNull(FileUtils.getJarFromClasspath(classLoader, "does/not/exist"));
        File file = FileUtils.getJarFromClasspath(classLoader,
                "org/apache/commons/logging/Log.class");
        assertNotNull(file);
        assertTrue(file.exists());
        // there might be version numbers between commons-logging & .jar, so
        // ignore them.
        assertTrue(file.getName(), file.getName().startsWith("commons-logging"));
        assertTrue(file.getName(), file.getName().endsWith(".jar"));

        try {
            FileUtils.getJarFromClasspath(null, "org/apache/commons/logging/Log.class");
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
        }
    }

    public void testReadAndWriteProperties() throws Exception {
        File path = null;
        try {

            // clean up beforehand
            path = new File("test.props");
            if (path.exists())
                path.delete();

            // make some properties
            Properties p = new Properties();
            p.setProperty("name1", "value1");
            p.setProperty("name2", "value2");
            p.setProperty("name3", "value3");

            // write them to disk
            FileUtils.writeProperties(path, p);

            // read them from disk
            Properties p2 = FileUtils.readProperties(path);

            // confirm that didn't change them
            assertTrue(p.equals(p2));

        } finally {

            // clean it up
            if (path != null)
                path.delete();
        }
    }

    public void testResolveSpecialPath() throws Exception {
        File expected, actual;

        // current working directory
        expected = (new File("")).getCanonicalFile();
        actual = FileUtils.resolveSpecialPath("");
        assertEquals(expected, actual);

        // current working directory, with subfolders
        expected = (new File("folder")).getCanonicalFile();
        actual = FileUtils.resolveSpecialPath("folder");
        assertEquals(expected, actual);
        expected = (new File("folder/subfolder")).getCanonicalFile();
        actual = FileUtils.resolveSpecialPath("folder/subfolder");
        assertEquals(expected, actual);

        // moving up
        expected = (new File("..")).getCanonicalFile();
        actual = FileUtils.resolveSpecialPath("..");
        assertEquals(expected, actual);
        expected = (new File("../folder")).getCanonicalFile();
        actual = FileUtils.resolveSpecialPath("../folder");
        assertEquals(expected, actual);
        expected = (new File("../folder/subfolder")).getCanonicalFile();
        actual = FileUtils.resolveSpecialPath("../folder/subfolder");
        assertEquals(expected, actual);

        // user's home directory
        expected = (new File(System.getProperty("user.home")));
        actual = FileUtils.resolveSpecialPath("Home>");
        assertEquals(expected, actual);
        expected = (new File(System.getProperty("user.home"), "folder"));
        actual = FileUtils.resolveSpecialPath("Home>folder");
        assertEquals(expected, actual);
        expected = (new File(System.getProperty("user.home"), "folder/subfolder"));
        actual = FileUtils.resolveSpecialPath("Home>folder/subfolder");
        assertEquals(expected, actual);

        // platform shell documents directory
        expected = new File(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS));
        actual = FileUtils.resolveSpecialPath("Documents>");
        assertEquals(expected, actual);
        expected = new File(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS), "folder");
        actual = FileUtils.resolveSpecialPath("Documents>folder");
        assertEquals(expected, actual);
        expected = new File(SystemUtils.getSpecialPath(SpecialLocations.DOCUMENTS),
                "folder/subfolder");
        actual = FileUtils.resolveSpecialPath("Documents>folder/subfolder");
        assertEquals(expected, actual);

        // platform shell desktop directory
        expected = new File(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP));
        actual = FileUtils.resolveSpecialPath("Desktop>");
        assertEquals(expected, actual);
        expected = new File(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP), "folder");
        actual = FileUtils.resolveSpecialPath("Desktop>folder");
        assertEquals(expected, actual);
        expected = new File(SystemUtils.getSpecialPath(SpecialLocations.DESKTOP),
                "folder/subfolder");
        actual = FileUtils.resolveSpecialPath("Desktop>folder/subfolder");
        assertEquals(expected, actual);
    }

    public void testMakeFolder() throws Exception {
        File path = null;
        try {

            // clean up beforehand
            path = new File("testfolder");
            if (path.isDirectory())
                path.delete();

            // make a folder that isn't there
            FileUtils.makeFolder(path);

            // confirm it's there now
            assertTrue(path.isDirectory());

            // use the same method to check that it's there
            FileUtils.makeFolder(path);

        } finally {

            // clean it up
            if (path != null)
                path.delete();
        }
    }

    public void testCantMakeFolder() throws Exception {
        File path = null;
        try {

            // write a little properties file
            path = new File("testfile.props");
            Properties p = new Properties();
            p.put("hello", "you");
            FileUtils.writeProperties(path, p);

            // we shouldn't be able to make a folder there
            try {
                FileUtils.makeFolder(path);
                fail("expected exception");
            } catch (IOException e) {
            }

        } finally {

            // clean it up
            if (path != null)
                path.delete();
        }
    }

    public void testCopyDirectory() throws Exception {
        File folder = new File("testfolder");
        File file = new File("testfolder/test.props");
        File subfolder = new File("testfolder/subfolder");
        File destinationFolder = new File("testdestination");
        File destinationFile = new File("testdestination/test.props");
        File destinationSubfolder = new File("testdestination/subfolder");
        try {

            // clean up beforehand
            copyDirectoryClean();

            // make a folder of files
            folder.mkdir();
            file.createNewFile();
            subfolder.mkdir();

            // copy it to a new location
            FileUtils.copyDirectory(folder, destinationFolder);

            // confirm its there
            assertTrue(destinationFolder.isDirectory());
            assertTrue(destinationFile.exists());
            assertTrue(destinationSubfolder.isDirectory());

        } finally {

            // clean it up
            copyDirectoryClean();
        }
    }

    // Don't use tests as reminders for TODOs, use JIRA.
    // Otherwise people stop looking at failures as "things gone wrong"
    // and failures stop being things that are bad (and become things
    // that are expected and thus ignored).
    // public void testCopyNonExistingFile() {
    // File nonexistent = new File("dkfdnslkdfa");
    // File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    // assertTrue(tmpDir.isDirectory());
    // assertTrue(!nonexistent.exists());
    // // TODO: what's the expected API behavior? return false if file to copy
    // does not exist
    // // check usage of return value
    // assertFalse(FileUtils.copy(nonexistent, new File(tmpDir,
    // nonexistent.getName())));
    // }

    private void copyDirectoryClean() throws Exception {
        (new File("testfolder/test.props")).delete();
        (new File("testfolder/subfolder")).delete();
        (new File("testfolder")).delete();
        (new File("testdestination/test.props")).delete();
        (new File("testdestination/subfolder")).delete();
        (new File("testdestination")).delete();
    }
}
