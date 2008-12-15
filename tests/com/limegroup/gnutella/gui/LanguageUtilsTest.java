package com.limegroup.gnutella.gui;

import java.awt.Font;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;
import org.limewire.util.FileUtils;

public class LanguageUtilsTest extends BaseTestCase {

    private Locale defaultLocale;

    public LanguageUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(LanguageUtilsTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        defaultLocale = Locale.getDefault();
    }
    
    @Override
    protected void tearDown() throws Exception {
        Locale.setDefault(defaultLocale);
    }
    
    public void testAddLocalesFromJar() {
        List<Locale> locales = new ArrayList<Locale>(); 
        File jar = FileUtils.getJarFromClasspath("org/limewire/i18n/Messages.class");
        LanguageUtils.addLocalesFromJar(locales, jar );
        assertTrue(locales.indexOf(new Locale("sr", "", "Latn")) != -1);
        assertTrue(locales.indexOf(Locale.GERMAN) != -1);
        assertTrue(locales.indexOf(Locale.ENGLISH) != -1);
        assertTrue(locales.indexOf(Locale.CANADA_FRENCH) == -1);
    }

    public void testGetLocales() {
        List<Locale> locales = Arrays.asList(LanguageUtils.getLocales(new Font("Dialog", Font.PLAIN, 0)));
        assertFalse(locales.isEmpty());
        assertEquals(Locale.ENGLISH, locales.get(0));
        assertTrue(locales.indexOf(new Locale("sr", "", "Latn")) != -1);
        assertTrue(locales.indexOf(Locale.GERMAN) != -1);
        
        // make sure English is in the list only once
        locales.set(0, null);
        assertEquals(-1, locales.indexOf(Locale.ENGLISH));
    }

    public void testMatchingScore() {
        assertEquals(3, LanguageUtils.getMatchScore(Locale.ENGLISH, Locale.ENGLISH));
        assertEquals(-1, LanguageUtils.getMatchScore(Locale.ENGLISH, Locale.GERMAN));
        assertEquals(2, LanguageUtils.getMatchScore(Locale.US, Locale.ENGLISH));
        assertEquals(2, LanguageUtils.getMatchScore(new Locale("en", "", "Latn"), Locale.ENGLISH));
        assertEquals(2, LanguageUtils.getMatchScore(new Locale("en", "US", "Latn"), Locale.US));
        assertEquals(1, LanguageUtils.getMatchScore(new Locale("en", "US", "Latn"), Locale.ENGLISH));
    }

    public void testMatchesDefaultLocale() {
        Locale.setDefault(Locale.US);
        assertTrue(LanguageUtils.matchesDefaultLocale(Locale.ENGLISH));
        assertTrue(LanguageUtils.matchesDefaultLocale(Locale.US));
        assertFalse(LanguageUtils.matchesDefaultLocale(new Locale("en", "", "Latn")));
        assertFalse(LanguageUtils.matchesDefaultLocale(Locale.GERMAN));
        
        Locale.setDefault(new Locale("en", "US", "Latn"));
        assertTrue(LanguageUtils.matchesDefaultLocale(Locale.ENGLISH));
        assertTrue(LanguageUtils.matchesDefaultLocale(Locale.US));
        assertTrue(LanguageUtils.matchesDefaultLocale(new Locale("en", "US", "Latn")));
    }
    
    public void testIsEnglishLocale() {
        assertTrue(LanguageUtils.isEnglishLocale(Locale.ENGLISH));
        assertTrue(LanguageUtils.isEnglishLocale(Locale.CANADA));
        assertTrue(LanguageUtils.isEnglishLocale(Locale.US));
        assertTrue(LanguageUtils.isEnglishLocale(Locale.UK));
        assertFalse(LanguageUtils.isEnglishLocale(Locale.GERMAN));
    }
    
}
