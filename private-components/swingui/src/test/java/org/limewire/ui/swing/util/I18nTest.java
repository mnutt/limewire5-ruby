package org.limewire.ui.swing.util;

import java.util.Locale;

import org.limewire.util.BaseTestCase;

import junit.framework.Test;

public class I18nTest extends BaseTestCase {
    
    public I18nTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(I18nTest.class);
    }
    
    public void testQuoteNoArgs() {
        String result = I18n.tr("This is 'quoted'!");
        assertEquals("This is 'quoted'!", result);
    }
    
    public void testArgsWithQuote() {
        String result = I18n.tr("This is 'quoted' {0} !", "hello");
        assertEquals("This is 'quoted' hello !", result);
    }
    
    public void testSingularNoArgsQuote() {
        String result = I18n.trn("singular 'hi!'", "plural 'his!'", 1);
        assertEquals("singular 'hi!'", result); 
    }
    
    public void testPluralNoArgsQuote() {
        String result = I18n.trn("singular 'hi!'", "plural 'his!'", 2);
        assertEquals("plural 'his!'", result); 
    }

    public void testPluralArgsUsedForSubstitution() {
        String result = I18n.trn("you have 1 message", "you have {0} messages", 2);
        assertEquals("you have 2 messages", result); 
    }
    
    public void testSingularArgsQuote() {
        String result = I18n.trn("singular 'hi!' {0}", "plural 'his!' {0}", 1, "yuck");
        assertEquals("singular 'hi!' yuck", result); 
    }
    
    public void testPluralArgsQuote() {
        String result = I18n.trn("singular 'hi!' {0}", "plural 'his!' {0}", 2, "yuck");
        assertEquals("plural 'his!' yuck", result); 
    }

    public void testTrcFallsBackToText() {
        I18n.setLocale(Locale.GERMAN);
        assertEquals("baobab", I18n.trc("baobab (the tree)", "baobab"));
    }
}
