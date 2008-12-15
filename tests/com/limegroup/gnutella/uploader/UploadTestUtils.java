package com.limegroup.gnutella.uploader;

import org.apache.http.Header;
import org.apache.http.HttpMessage;

public class UploadTestUtils {

    public static boolean containsHeader(HttpMessage message, String expectedHeader) {
        for (Header header : message.getAllHeaders()) {
            if (expectedHeader.equals(header.toString())) {
                return true;
            }
        }
        return false;
    }

    public static String toString(HttpMessage message) {
        StringBuilder sb = new StringBuilder();
        for (Header header : message.getAllHeaders()) {
            sb.append(header.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
    
    public static void assertHasHeader(HttpMessage message, String expectedHeader) {
        if (!containsHeader(message, expectedHeader)) {
            throw new AssertionError("Expected header '" + expectedHeader + "' not found in '" + toString(message) + "'");
        }
    }
    
    public static void assertNotHasHeader(HttpMessage message, String unexpectedHeader) {
        if (containsHeader(message, unexpectedHeader)) {
            throw new AssertionError("Unexpected header '" + unexpectedHeader + "' in '" + toString(message) + "'");
        }
    }
}
