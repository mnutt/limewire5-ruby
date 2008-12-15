package org.limewire.geocode;

import junit.framework.Test;

import org.limewire.geocode.GeocodeInformation;
import org.limewire.util.BaseTestCase;

public class GeocodeInformationTestCase extends BaseTestCase {
   
    public GeocodeInformationTestCase(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(GeocodeInformationTestCase.class);
    }
    
    private GeocodeInformation info;
    
    @Override
    protected void setUp() throws Exception {
        info = new GeocodeInformation();
    }
    
    @Override
    protected void tearDown() throws Exception {
        info = null;
    }
    
    public void testIp() {
        info.setProperty(GeocodeInformation.Property.Ip, "76.8.67.2");
        assertEquals("76.8.67.2", info.getProperty(GeocodeInformation.Property.Ip));
      }

      public void testIpString() {
        info.setProperty("Ip", "76.8.67.2");
        assertEquals("76.8.67.2", info.getProperty(GeocodeInformation.Property.Ip));
      }

      public void testCountryCode() {
        info.setProperty(GeocodeInformation.Property.CountryCode, "US");
        assertEquals("US", info.getProperty(GeocodeInformation.Property.CountryCode));
      }

      public void testCountryCodeString() {
        info.setProperty("CountryCode", "US");
        assertEquals("US", info.getProperty(GeocodeInformation.Property.CountryCode));
      }

      public void testCountryCode3() {
        info.setProperty(GeocodeInformation.Property.CountryCode3, "USA");
        assertEquals("USA", info.getProperty(GeocodeInformation.Property.CountryCode3));
      }

      public void testCountryCode3String() {
        info.setProperty("CountryCode3", "USA");
        assertEquals("USA", info.getProperty(GeocodeInformation.Property.CountryCode3));
      }

      public void testCountryName() {
        info.setProperty(GeocodeInformation.Property.CountryName, "United States");
        assertEquals("United States", info.getProperty(GeocodeInformation.Property.CountryName));
      }

      public void testCountryNameString() {
        info.setProperty("CountryName", "United States");
        assertEquals("United States", info.getProperty(GeocodeInformation.Property.CountryName));
      }

      public void testRegion() {
        info.setProperty(GeocodeInformation.Property.Region, "NY");
        assertEquals("NY", info.getProperty(GeocodeInformation.Property.Region));
      }

      public void testRegionString() {
        info.setProperty("Region", "NY");
        assertEquals("NY", info.getProperty(GeocodeInformation.Property.Region));
      }

      public void testRegion2() {
        info.setProperty(GeocodeInformation.Property.Region2, "New York");
        assertEquals("New York", info.getProperty(GeocodeInformation.Property.Region2));
      }

      public void testRegion2String() {
        info.setProperty("Region2", "New York");
        assertEquals("New York", info.getProperty(GeocodeInformation.Property.Region2));
      }

      public void testCity() {
        info.setProperty(GeocodeInformation.Property.City, "New York");
        assertEquals("New York", info.getProperty(GeocodeInformation.Property.City));
      }

      public void testCityString() {
        info.setProperty("City", "New York");
        assertEquals("New York", info.getProperty(GeocodeInformation.Property.City));
      }

      public void testPostalCode() {
        info.setProperty(GeocodeInformation.Property.PostalCode, "10004");
        assertEquals("10004", info.getProperty(GeocodeInformation.Property.PostalCode));
      }

      public void testPostalCodeString() {
        info.setProperty("PostalCode", "10004");
        assertEquals("10004", info.getProperty(GeocodeInformation.Property.PostalCode));
      }

      public void testLatitude() {
        info.setProperty(GeocodeInformation.Property.Latitude, "40.6888");
        assertEquals("40.6888", info.getProperty(GeocodeInformation.Property.Latitude));
      }

      public void testLatitudeString() {
        info.setProperty("Latitude", "40.6888");
        assertEquals("40.6888", info.getProperty(GeocodeInformation.Property.Latitude));
      }

      public void testLongitude() {
        info.setProperty(GeocodeInformation.Property.Longitude, "-74.0203");
        assertEquals("-74.0203", info.getProperty(GeocodeInformation.Property.Longitude));
      }

      public void testLongitudeString() {
        info.setProperty("Longitude", "-74.0203");
        assertEquals("-74.0203", info.getProperty(GeocodeInformation.Property.Longitude));
      }

      public void testDmaCode() {
        info.setProperty(GeocodeInformation.Property.DmaCode, "501");
        assertEquals("501", info.getProperty(GeocodeInformation.Property.DmaCode));
      }

      public void testDmaCodeString() {
        info.setProperty("DmaCode", "501");
        assertEquals("501", info.getProperty(GeocodeInformation.Property.DmaCode));
      }

      public void testAreaCode() {
        info.setProperty(GeocodeInformation.Property.AreaCode, "212");
        assertEquals("212", info.getProperty(GeocodeInformation.Property.AreaCode));
      }

      public void testAreaCodeString() {
        info.setProperty("AreaCode", "212");
        assertEquals("212", info.getProperty(GeocodeInformation.Property.AreaCode));
      }    
}
