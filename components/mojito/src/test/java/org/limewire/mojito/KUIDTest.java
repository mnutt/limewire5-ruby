/*
 * Mojito Distributed Hash Table (Mojito DHT)
 * Copyright (C) 2006-2007 LimeWire LLC
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
 
package org.limewire.mojito;

import junit.framework.TestSuite;

public class KUIDTest extends MojitoTestCase {
    
    public KUIDTest(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return buildTestSuite(KUIDTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
    
    public void testCompareTo() {
        KUID a = KUID.createWithHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID b = KUID.createWithHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868");
        
        assertEquals(0, a.compareTo(a));
        assertEquals(-1, a.compareTo(b));
        
        assertEquals(0, b.compareTo(b));
        assertEquals(1, b.compareTo(a));
    }
    
    public void testImmutability() {
        KUID a = KUID.createWithHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID b = KUID.createWithHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID c = null;
        
        c = b.flip(10); // tests set+unset
        assertEquals(a, b);
        assertNotEquals(b, c);
        
        byte[] bytes = b.getBytes();
        bytes[10] ^= bytes[10];
        c = KUID.createWithBytes(bytes);
        assertEquals(a, b);
        assertNotEquals(b, c);
    }
    
    public void testIsNearer() {
        
        KUID lookup = KUID.createWithHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID worst = KUID.createWithHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868");
        KUID best = KUID.createWithHexString("F2617265969422D11CFB73C75EE8B649132DFB37");
        
        assertTrue(worst.isNearerTo(lookup, best));
        assertFalse(best.isNearerTo(lookup, worst));
    }
    
    public void testSetBit() {
        KUID a = KUID.createWithHexString("0000000000000000000000000000000000000000");
        KUID b = KUID.createWithHexString("1000000000000000000000000000000000000000");
        KUID c = KUID.createWithHexString("0800000000000000000000000000000000000000");
        
        assertFalse(a.isBitSet(3));
        
        assertTrue(b.isBitSet(3));
        assertFalse(b.isBitSet(4));
        
        assertFalse(c.isBitSet(3));
        assertTrue(c.isBitSet(4));
        
        c = c.set(4);
        assertTrue(c.isBitSet(4));
        
        c = c.unset(4);
        assertFalse(c.isBitSet(4));
        
        c = c.flip(4);
        assertTrue(c.isBitSet(4));
        
        c = c.flip(4);
        assertFalse(c.isBitSet(4));
    }
    
    public void testXor() {
        KUID a = KUID.createWithHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID b = KUID.createWithHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868");
        KUID c = KUID.createWithHexString("1188A6A8CCB7E7831E1DDE2312074C663663B993");
        
        assertEquals(c, a.xor(b));
    }
    
    public void testGetCommonPrefixLength() {
        KUID a = KUID.createWithHexString("E3ED9650238A6C576C987793C01440A0EA91A1FB");
        KUID b = KUID.createWithHexString("F26530F8EF3D8BD47285A9B0D2130CC6DCF21868");
        KUID c = KUID.createWithHexString("F26830F8EF3D8BD47285A9B0D2130CC6DCF21868");
        KUID d = KUID.createWithHexString("126830F8EF3D8BD47285A9B0D2130CC6DCF21868");
        
        assertEquals(3, a.getCommonPrefixLength(b));
        assertEquals(160, a.getCommonPrefixLength(a));
        assertEquals(12, b.getCommonPrefixLength(c));
        assertEquals(0, d.getCommonPrefixLength(c));
    }
}
