package org.limewire.geocode;



/**
 * Defines the interface for the finder and retriever or
 * {@link GeocodeInformationImpl}.
 * <p>
 * The portion of instances of this interface that implements
 * {@link SuccessOrFailureCallback<String>} should process Strings of the form:
 * 
 * <pre>
 * S    ::= T N Line*
 * T    ::= String
 * N    ::= String
 * Line ::= String(key) T String(value) N
 * </pre>
 *  producing a map.  Here is such an example:
 * 
 * <pre>
 *  CountryCode  US
 *  CountryCode3    USA
 *  CountryName United States
 *  Region  NY
 *  Region2 New York
 *  City    New York
 *  PostalCode  10004
 *  Latitude    40.6888
 *  Longitude   -74.0203
 *  DmaCode 501
 *  AreaCode    212
 * </pre>
 */
public interface Geocoder {

    /**
     * Loads geo location information and saves it for access via
     * {@link #getGeocodeInformation()}.
     */
    void initialize();

    /**
     * Returns the {@link GeocodeInformationImpl} obtained from
     * {@link #initialize()}.
     * 
     * @return the {@link GeocodeInformationImpl} obtained from
     *         {@link #initialize()}
     */
    GeocodeInformation getGeocodeInformation();

    /**
     * Returns <code>true</code> if {@link getGeocodeInformation()} will
     * return a non-null value or {@link #hasFailed()} is <code>true</code>.
     * 
     * @return <code>true</code> if {@link getGeocodeInformation()} will
     *         return a non-null value or {@link #hasFailed()} is
     *         <code>true</code>
     */
    boolean isReady();

    /**
     * Returns <code>true</code> if we've called {@link #initialize()} and
     * something happened.
     * 
     * @return <code>true</code> if we've called {@link #initialize()} and
     *         something happened
     */
    boolean hasFailed();

    /**
     * Clears the saved information, including that in the client. This is
     * mainly used for testing.
     */
    void clear();
}
