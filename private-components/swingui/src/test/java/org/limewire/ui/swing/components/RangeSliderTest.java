package org.limewire.ui.swing.components;

import org.limewire.util.BaseTestCase;

/**
 * JUnit test case for RangeSlider.
 */
public class RangeSliderTest extends BaseTestCase {
    /** Instance of class being tested. */
    private RangeSlider rangeSlider;
    
    /**
     * Constructs a test case for the specified method name.
     */
    public RangeSliderTest(String name) {
        super(name);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // Create slider with minimum and maximum values.
        rangeSlider = new RangeSlider();
        rangeSlider.setMinimum(0);
        rangeSlider.setMaximum(10);
    }

    @Override
    protected void tearDown() throws Exception {
        rangeSlider = null;
        super.tearDown();
    }

    /** Tests no-arg constructor for initial state. */
    public void testRangeSlider() {
        // Verify both thumbs enabled.
        assertTrue("lower thumb enabled", rangeSlider.isLowerThumbEnabled());
        assertTrue("upper thumb enabled", rangeSlider.isUpperThumbEnabled());
    }
    
    /** Tests method to set lower value. */
    public void testSetValue() {
        // Initialize lower and upper values.
        rangeSlider.setValue(rangeSlider.getMinimum());
        rangeSlider.setUpperValue(rangeSlider.getMaximum());
        
        // Set lower value and verify both values.
        int oldUpper = rangeSlider.getUpperValue();
        rangeSlider.setValue(4);
        
        int expectedReturn = 4;
        int actualReturn = rangeSlider.getValue();
        assertEquals("lower value", expectedReturn, actualReturn);
        
        expectedReturn = oldUpper;
        actualReturn = rangeSlider.getUpperValue();
        assertEquals("upper value", expectedReturn, actualReturn);
    }

    /** Tests method to enable lower thumb. */
    public void testSetLowerThumbEnabled() {
        // Disable thumb and verify.
        rangeSlider.setLowerThumbEnabled(false);
        assertFalse("lower thumb disabled", rangeSlider.isLowerThumbEnabled());
    }

    /** Tests method to enable upper thumb. */
    public void testSetUpperThumbEnabled() {
        // Disable thumb and verify.
        rangeSlider.setUpperThumbEnabled(false);
        assertFalse("upper thumb disabled", rangeSlider.isUpperThumbEnabled());
    }

    /** Tests method to set upper value. */
    public void testSetUpperValue() {
        // Initialize lower and upper values.
        rangeSlider.setValue(rangeSlider.getMinimum());
        rangeSlider.setUpperValue(rangeSlider.getMaximum());
        
        // Set upper value and verify both values.
        int oldLower = rangeSlider.getValue();
        rangeSlider.setUpperValue(6);
        
        int expectedReturn = 6;
        int actualReturn = rangeSlider.getUpperValue();
        assertEquals("upper value", expectedReturn, actualReturn);
        
        expectedReturn = oldLower;
        actualReturn = rangeSlider.getValue();
        assertEquals("lower value", expectedReturn, actualReturn);
    }
}
