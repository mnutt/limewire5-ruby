package org.limewire.bittorrent.bencoding;

import java.util.List;
import java.util.Map;

import org.limewire.util.BaseTestCase;
import org.limewire.util.ReadBufferChannel;
import org.limewire.util.StringUtils;

public class TokenTest extends BaseTestCase {

    public TokenTest(String name) {
        super(name);
    }

    public void testParseString() throws Exception {
        Object parsedObject = Token.parse(new ReadBufferChannel(StringUtils.toAsciiBytes("4:test")));
        byte[] result = (byte[]) parsedObject;
        assertEquals("test", StringUtils.getASCIIString(result));

        parsedObject = Token.parse(new ReadBufferChannel(StringUtils.toAsciiBytes("44:the rain in spain stays mostly on the plains")));
        result = (byte[]) parsedObject;
        assertEquals("the rain in spain stays mostly on the plains", StringUtils.getASCIIString(result));
    }

    public void testParseInt() throws Exception {
        Object parsedObject = Token.parse(new ReadBufferChannel(StringUtils.toAsciiBytes("i12345e")));
        Long result = (Long) parsedObject;
        assertEquals(new Long(12345), result);

        parsedObject = Token.parse(new ReadBufferChannel(StringUtils.toAsciiBytes("i12345678910e")));
        result = (Long) parsedObject;
        assertEquals(new Long(12345678910L), result);
    }

    @SuppressWarnings("unchecked")
    public void testParseList() throws Exception {
        Object parsedObject = Token.parse(new ReadBufferChannel(StringUtils.toAsciiBytes("l5:test1e")));
        List<Object> result = (List<Object>) parsedObject;
        assertEquals(1, result.size());
        byte[] index0 = (byte[]) result.get(0);
        assertEquals("test1", StringUtils.getASCIIString(index0));

        parsedObject = Token.parse(new ReadBufferChannel(StringUtils.toAsciiBytes("l5:test14:blah5:test23:ende")));
        result = (List<Object>) parsedObject;
        assertEquals(4, result.size());

        index0 = (byte[]) result.get(0);
        byte[] index1 = (byte[]) result.get(1);
        byte[] index2 = (byte[]) result.get(2);
        byte[] index3 = (byte[]) result.get(3);

        assertEquals("test1", StringUtils.getASCIIString(index0));
        assertEquals("blah", StringUtils.getASCIIString(index1));
        assertEquals("test2", StringUtils.getASCIIString(index2));
        assertEquals("end", StringUtils.getASCIIString(index3));
    }

    @SuppressWarnings("unchecked")
    public void testParseDictionary() throws Exception {
        Object parsedObject = Token
			.parse(new ReadBufferChannel(StringUtils.toAsciiBytes("d4:ainti12345e3:key5:value4:type4:test4:listl5:test14:blahee")));
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
        assertEquals("value", StringUtils.getASCIIString(value2));
        assertEquals("test", StringUtils.getASCIIString(value3));
        assertEquals(2, value4.size());

        String index0 = StringUtils.getASCIIString((byte[]) value4.get(0));
        String index1 = StringUtils.getASCIIString((byte[]) value4.get(1));

        assertEquals("test1", index0);
        assertEquals("blah", index1);

    }

    public void testParseEmptyByteArray() throws Exception {
        Object parsedObject = Token.parse(new ReadBufferChannel(new byte[] {}));
        assertNull(parsedObject);
    }
}
