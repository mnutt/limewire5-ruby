package org.limewire.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import junit.framework.Test;

public class ConverterObjectInputStreamTest extends BaseTestCase {

    public ConverterObjectInputStreamTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(ConverterObjectInputStreamTest.class);
    }

    private ConverterObjectInputStream getSerializedInputStream(Serializable s) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream objectStream = new ObjectOutputStream(out);
        
        objectStream.writeObject(s);
        objectStream.flush();
        objectStream.close();
        
        return getSerializedInputStream(out);
    }
    
    private ConverterObjectInputStream getSerializedInputStream(byte[] out) throws Exception {
        return new ConverterObjectInputStream(new ByteArrayInputStream(out));
    }
    
    private ConverterObjectInputStream getSerializedInputStream(ByteArrayOutputStream out) throws Exception {
        return getSerializedInputStream(out.toByteArray());
    }
    
    @SuppressWarnings("unchecked")
    public void testLookupOfUnregisteredClass() throws Exception {
        ArrayList<SerializableClass> list = new ArrayList<SerializableClass>();
        list.add(new SerializableClass());
        
        ConverterObjectInputStream in = getSerializedInputStream(list);
        
        ArrayList<SerializableClass> list2 = (ArrayList<SerializableClass>)in.readObject();
        assertEquals(list.size(), list2.size());
    }
    
    public void testAddedClassMapping() throws Exception {
        ConverterObjectInputStream in = getSerializedInputStream(new SerializableClass());
        in.addLookup(SerializableClass.class.getName(), NewSerializableClass.class.getName());
        
        Object instance = in.readObject();
        assertTrue(instance instanceof NewSerializableClass);
    }
    
    private void assertObjectIsNotConstructable(byte[] in, String expectedClassName) throws Exception {
        // verify object can't be deserialized by normal ObjectInputStream
        try {
            ObjectInputStream normalIn = new ObjectInputStream(new ByteArrayInputStream(in));
            normalIn.readObject();
            fail("exception should have been thrown");
        }
        catch (ClassNotFoundException cnfe) {
            assertEquals(cnfe.getMessage(), expectedClassName);
        }
    }
    
    public void testOldCollectionClasses() throws Exception {
        // import com.sun.java.util.collections.*;
        // ArrayList list = new ArrayList();
        // list.add(new HashMap());
        byte[] hashMapInsideOfArrayList = new byte[] { -84, -19, 0, 5, 115, 114, 0, 39, 99, 111, 109, 46, 115, 117, 110, 46, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 99, 111, 108, 108, 101, 99, 116, 105, 111, 110, 115, 46, 65, 114, 114, 97, 121, 76, 105, 115, 116, 77, -94, -80, -33, 64, 10, 78, -108, 3, 0, 1, 73, 0, 4, 115, 105, 122, 101, 120, 112, 0, 0, 0, 1, 119, 4, 0, 0, 0, 10, 115, 114, 0, 37, 99, 111, 109, 46, 115, 117, 110, 46, 106, 97, 118, 97, 46, 117, 116, 105, 108, 46, 99, 111, 108, 108, 101, 99, 116, 105, 111, 110, 115, 46, 72, 97, 115, 104, 77, 97, 112, 80, 0, 33, 58, -95, -15, 79, 40, 3, 0, 2, 70, 0, 10, 108, 111, 97, 100, 70, 97, 99, 116, 111, 114, 73, 0, 9, 116, 104, 114, 101, 115, 104, 111, 108, 100, 120, 112, 63, 64, 0, 0, 0, 0, 0, 75, 119, 8, 0, 0, 0, 101, 0, 0, 0, 0, 120, 120 };

        assertObjectIsNotConstructable(hashMapInsideOfArrayList, "com.sun.java.util.collections.ArrayList");
        
        ConverterObjectInputStream in = getSerializedInputStream(hashMapInsideOfArrayList);
        
        Object instance = in.readObject();
        assertTrue(instance instanceof ArrayList);
        ArrayList list = (ArrayList)instance;
        assertEquals(1, list.size());
        instance = list.get(0);
        assertTrue(instance instanceof HashMap);
    }
    
    public void testDefaultPackageClassMappedToThisPackage() throws Exception {
        // serializable static inner class in default package
        // new ConverterObjectInputStreamTest.SerializableClass();
        byte[] serializbleClassInDefaultPackage = new byte[] { -84, -19, 0, 5, 115, 114, 0, 48, 67, 111, 110, 118, 101, 114, 116, 101, 114, 79, 98, 106, 101, 99, 116, 73, 110, 112, 117, 116, 83, 116, 114, 101, 97, 109, 84, 101, 115, 116, 36, 83, 101, 114, 105, 97, 108, 105, 122, 97, 98, 108, 101, 67, 108, 97, 115, 115, -6, 49, 117, -36, -108, -33, -69, 70, 2, 0, 0, 120, 112 };
        
        assertObjectIsNotConstructable(serializbleClassInDefaultPackage, "ConverterObjectInputStreamTest$SerializableClass");
        
        ConverterObjectInputStream in = getSerializedInputStream(serializbleClassInDefaultPackage);
        in.addLookup("", this.getClass().getName().substring(0, this.getClass().getName().lastIndexOf('.')));
        
        Object instance = in.readObject();
        assertTrue(instance instanceof SerializableClass);
    }
    
    static class SerializableClass implements Serializable {
        
    }
    
    static class NewSerializableClass implements Serializable {
        
    }
    
}
