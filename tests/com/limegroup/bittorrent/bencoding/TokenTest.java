package com.limegroup.bittorrent.bencoding;

import java.io.EOFException;
import java.util.List;
import java.util.Map;

import org.limewire.util.BaseTestCase;

public class TokenTest extends BaseTestCase {

    public TokenTest(String name) {
        super(name);
    }

    public void testParseString() throws Exception {
        Object parsedObject = Token.parse("4:test".getBytes());
        byte[] result = (byte[]) parsedObject;
        assertEquals("test", new String(result));

        parsedObject = Token.parse("44:the rain in spain stays mostly on the plains".getBytes());
        result = (byte[]) parsedObject;
        assertEquals("the rain in spain stays mostly on the plains", new String(result));
    }

    public void testParseInt() throws Exception {
        Object parsedObject = Token.parse("i12345e".getBytes());
        Long result = (Long) parsedObject;
        assertEquals(new Long(12345), result);

        parsedObject = Token.parse("i12345678910e".getBytes());
        result = (Long) parsedObject;
        assertEquals(new Long(12345678910L), result);
    }

    @SuppressWarnings("unchecked")
    public void testParseList() throws Exception {
        Object parsedObject = Token.parse("l5:test1e".getBytes());
        List<Object> result = (List<Object>) parsedObject;
        assertEquals(1, result.size());
        byte[] index0 = (byte[]) result.get(0);
        assertEquals("test1", new String(index0));

        parsedObject = Token.parse("l5:test14:blah5:test23:ende".getBytes());
        result = (List<Object>) parsedObject;
        assertEquals(4, result.size());

        index0 = (byte[]) result.get(0);
        byte[] index1 = (byte[]) result.get(1);
        byte[] index2 = (byte[]) result.get(2);
        byte[] index3 = (byte[]) result.get(3);

        assertEquals("test1", new String(index0));
        assertEquals("blah", new String(index1));
        assertEquals("test2", new String(index2));
        assertEquals("end", new String(index3));
    }

    @SuppressWarnings("unchecked")
    public void testParseDictionary() throws Exception {
        Object parsedObject = Token
                .parse("d4:ainti12345e3:key5:value4:type4:test4:listl5:test14:blahee".getBytes());
        Map<String, Object> result = (Map<String, Object>) parsedObject;
        assertEquals(4, result.size());

        String key1 = "aint";
        String key2 = "key";
        String key3 = "type";
        String key4 = "list";

        Long value1 = (Long) result.get(key1);
        byte[] value2 = (byte[]) result.get(key2);
        byte[] value3 = (byte[]) result.get(key3);
        List<Object> value4 = (List<Object>) result.get(key4);

        assertEquals(new Long(12345), value1);
        assertEquals("value", new String(value2));
        assertEquals("test", new String(value3));
        assertEquals(2, value4.size());

        String index0 = new String((byte[]) value4.get(0));
        String index1 = new String((byte[]) value4.get(1));

        assertEquals("test1", index0);
        assertEquals("blah", index1);

    }

    public void testParseEmptyByteArray() throws Exception {
        try {
            Token.parse(new byte[] {});
            fail("There should be nothing ot read.");
            //TODO potentially parsing this should just return null, should revisit
        } catch (EOFException e) {
            // expected
        }
    }
}
