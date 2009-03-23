package org.limewire.swarm.file;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import junit.framework.Assert;
import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

public class SwarmFileSystemImplTest extends BaseTestCase {

    private final File TEST_DIR;

    public SwarmFileSystemImplTest(String name) {
        super(name);
        this.TEST_DIR = new File(System.getProperty("java.io.tmpdir") + "/limetests/");
    }
    
    public static Test suite() {
        return buildTestSuite(SwarmFileSystemImplTest.class);
    }
    

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteRecursive(TEST_DIR);
    }

    public void testGetCompleteSize() throws Exception {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testGetCompleteSize.1.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 1024));
        Assert.assertEquals(1024, swarmFileSystem.getCompleteSize());
        File file2 = createFile("testGetCompleteSize.2.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file2, 1023));
        swarmFileSystem.close();
        Assert.assertEquals(2047, swarmFileSystem.getCompleteSize());
    }

    private File createFile(String fileName) {
        TEST_DIR.mkdirs();
        File file = new File(TEST_DIR.getAbsolutePath() + "/" + fileName);
        file.delete();
        file.deleteOnExit();
        return file;
    }

    public void testWrite1() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWrite.1.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 1024));

        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(StringUtils.toAsciiBytes(testWrite1));
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 0);
        swarmFileSystem.close();
        Assert.assertTrue(file1.exists());
        Assert.assertEquals(testWrite1.length(), file1.length());
        Assert.assertEquals(testWrite1, StringUtils.getASCIIString(FileUtils.readFileFully(file1)));

    }

    public void testRead1() throws IOException {

        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File fileWrite1 = createFile("testWrite.1.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(fileWrite1, 1024));
        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(StringUtils.toAsciiBytes(testWrite1));
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 0);
        swarmFileSystem.close();

        SwarmFileSystemImpl swarmFileSystem2 = new SwarmFileSystemImpl();
        swarmFileSystem2.addSwarmFile(new SwarmFileImpl(fileWrite1, 1024));
        byteBuffer.flip();
        swarmFileSystem2.read(byteBuffer, 0);
        swarmFileSystem2.close();
        Assert.assertTrue(fileWrite1.exists());
        Assert.assertEquals(testWrite1.length(), byteBuffer.position());
        Assert.assertEquals(testWrite1, StringUtils.getASCIIString(byteBuffer.array()));
    }

    public void testWriteMulti() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWriteMulti.1.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 10));
        File file2 = createFile("testWriteMulti.2.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file2, 6));
        String testWrite1 = "testWrite File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(StringUtils.toAsciiBytes(testWrite1));
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 0);
        swarmFileSystem.close();

        Assert.assertTrue(file1.exists());
        Assert.assertEquals(10, file1.length());
        Assert
                .assertEquals(testWrite1.substring(0, 10), StringUtils.getASCIIString(FileUtils
                        .readFileFully(file1)));
        Assert.assertTrue(file2.exists());
        Assert.assertEquals(6, file2.length());
        Assert.assertEquals(testWrite1.substring(10, 16),
                StringUtils.getASCIIString(FileUtils.readFileFully(file2)));

    }

    public void testWriteMultiPartial() throws IOException {
        SwarmFileSystemImpl swarmFileSystem = new SwarmFileSystemImpl();
        File file1 = createFile("testWriteMultiPartial.1.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file1, 10));

        File file2 = createFile("testWriteMultiPartial.2.txt");
        swarmFileSystem.addSwarmFile(new SwarmFileImpl(file2, 6));

        String testWrite1 = "File 1";
        ByteBuffer byteBuffer = ByteBuffer.allocate(testWrite1.length());
        byteBuffer.put(StringUtils.toAsciiBytes(testWrite1));
        byteBuffer.flip();
        swarmFileSystem.write(byteBuffer, 10);
        swarmFileSystem.close();

        Assert.assertFalse(file1.exists());
        Assert.assertEquals(0, file1.length());
        Assert.assertTrue(file2.exists());
        Assert.assertEquals(6, file2.length());
        Assert.assertEquals(testWrite1, StringUtils.getASCIIString(FileUtils.readFileFully(file2)));

    }

}
