package com.limegroup.gnutella.downloader;


import com.limegroup.gnutella.lws.server.LWSManager;
import com.limegroup.gnutella.util.LimeWireUtils;

/**
 * A class that initializes listeners to the passed in instance of
 * {@link LWSManager}.
 */
public interface LWSIntegrationServices {

    /** Constants for querying the information about the running client. */
    public enum Info {

        /** Constant key for the value of {@link LimeWireUtils#isAlphaRelease()}. */
        IsAlphaRelease("is.alpha.release"),

        /**
         * Constant key for the value of
         * {@link LimeWireUtils#getMinorVersionNumber()}.
         */
        MinorVersionNumber("minor.version.number"),

        /** Constant key for the value of {@link LimeWireUtils#isPro()}. */
        IsPro("is.pro"),

        /**
         * Constant key for the value of
         * {@link LimeWireUtils#getMajorVersionNumber()}.
         */
        MajorVersionNumber("major.version.number"),

        /**
         * Constant key for the value of
         * {@link LimeWireUtils#getServiceVersionNumber()}.
         */
        ServiceVersionNumber("service.version.number"),

        /** Constant key for the value of {@link LimeWireUtils#isBetaRelease()}. */
        IsBetaRelease("is.beta.release"),

        /** Constant key for the value of {@link LimeWireUtils#getVendor()}. */
        Vendor("vendor"),

        /**
         * constant key for the value of
         * {@link LimeWireUtils#getLimeWireVersion()}.
         */
        Version("version"),

        /** Constant key for the value of {@link LimeWireUtils#isTestingVersion()}. */
        IsTestingVersion("is.testing.version");
        
        private final String value;

        Info(String s) {
            this.value = s;
        }

        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value;
        }
    }

    /**
     * The prefix with which to start download URLS. For example, if we call the
     * <code>Download</code> command with a <code>url</code> argument
     * <code>/SomeURL</code>, then if we passed in <code>limewire.org</code>
     * to this method, the resulting download would come from
     * <code>http://limewire.org/SomeURL</code>.
     * 
     * @param downloadPrefix new download prefix
     */
    void setDownloadPrefix(String downloadPrefix);
    
}
