package org.limewire.ui.swing.search.model;

import java.util.regex.Pattern;

import junit.framework.Assert;
import junit.framework.TestCase;

public class CleanStringCacheTest extends TestCase {
    private String REPLACE = "\\(\\d\\)|[-_.' ()]";
    private Pattern pattern = Pattern.compile(REPLACE);
    
    public void testEqualStrings() {
        Assert.assertTrue(new CleanStringCache(pattern,"").matches("123", "123"));
        Assert.assertTrue(new CleanStringCache(pattern,"").matches("abc", "abc"));
    }
    
    public void testEqualsIgnoreCase() {
        Assert.assertTrue(new CleanStringCache(pattern,"").matches("aBc", "AbC"));
    }
    
    public void testEqualsIgnoreSymbols() {
        Assert.assertTrue(new CleanStringCache(pattern,"").matches("aBC.-", "AbC_()"));
    }
    
    public void testEqualsIgnoreLeafNodes() {
        Assert.assertTrue(new CleanStringCache(pattern,"").matches("aBC(1)", "AbC"));
    }
}
