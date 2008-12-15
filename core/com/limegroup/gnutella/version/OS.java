package com.limegroup.gnutella.version;

import java.util.Arrays;
import java.util.Locale;
import java.util.StringTokenizer;

import org.limewire.util.OSUtils;
import org.limewire.util.Version;
import org.limewire.util.VersionFormatException;


/**
 * An abstraction for representing an operating system.
 */
class OS {
    
    /** any version */
    private static final Version STAR;
    /** bad version */
    private static final Version BAD;
    static {
        Version star,b;
        try {
            star = new Version("0");
            b = new Version("0");
        } catch (VersionFormatException bad) {
            throw new RuntimeException(bad);
        }
        STAR = star;
        BAD = b;
    }
    
    /**
     * The string representation of the OS.
     */
    private final String os;
    
    /** Min inclusive - max exclusive versions of this os */
    private final Version fromVersion, toVersion;
    
    /**
     * Whether or not the OS of this machine is a match.
     */
    private final boolean acceptable;
    
    /**
     * Constructs a new OS based on the given string representation.
     */
    OS(String s, Version fromVersion, Version toVersion) {
        this.os = s;
        this.fromVersion = fromVersion;
        this.toVersion = toVersion;
        this.acceptable = accept(s.toLowerCase(Locale.US));
    }
    
    /**
     * Returns the OS as a string.
     */
    @Override
    public String toString() {
        return os;
    }
    
    /**
     * Determines if the current machine's OS is a match for what this OS
     * object is representing.
     */
    public boolean isAcceptable() {
        return acceptable;
    }
    
    /**
     * Creates an array of OSes from a comma delimited list of strings.
     * Whitespace is ignored.
     */
    static OS[] createFromList(String oses, String versions) {
        StringTokenizer st = new StringTokenizer(oses, ",");
        Version[] versionsFrom = new Version[st.countTokens()];
        Version[] versionsTo = new Version[st.countTokens()];
        if (versions != null && versions.length() > 0) {
            StringTokenizer v = new StringTokenizer(versions,",");
            if (v.countTokens() == st.countTokens() * 2) {
                try {
                    for (int i = 0; i < versionsFrom.length; i++) {
                        String s = v.nextToken().trim();
                        if (s.equals("*"))
                            versionsFrom[i] = STAR;
                        else 
                            versionsFrom[i] = new Version(s);
                        s = v.nextToken().trim();
                        if (s.equals("*"))
                            versionsTo[i] = STAR;
                        else 
                            versionsTo[i] = new Version(s);
                    }
                } catch (VersionFormatException bad) {
                    Arrays.fill(versionsFrom, BAD);
                    Arrays.fill(versionsTo, BAD);
                }
            } else {
                // non empty versions element, but weird size? bad.
                Arrays.fill(versionsFrom, BAD);
                Arrays.fill(versionsTo, BAD);
            }
        }
        OS[] all = new OS[st.countTokens()];
        for(int i = 0; st.hasMoreTokens(); i++) {
            all[i] = new OS(st.nextToken().trim(), versionsFrom[i], versionsTo[i]);
        }
        return all;
    }
    
    /**
     * Determines if any OS object in the array matches the current machine.
     */
    static boolean hasAcceptableOS(OS[] oses) {
        for(int i = 0; i < oses.length; i++)
            if(oses[i].isAcceptable())
                return true;
        return false;
    }
    
    /**
     * Prints out a comma separated list of the OSes.
     */
    static String toString(OS[] oses) {
        if (oses == null)
            return "";
        
        String s = "";
        for(int i = 0; i < oses.length; i++) {
            s += oses[i].toString();
            if( i < oses.length - 2)
                s += ", ";
        }
        return s;
    }
    
    /**
     * Determines whether or not the current machine matches the string representation
     * of an OS.
     *
     * An exact match of System.getProperty("os.name") is allowed, as are the special:
     * "windows", "mac", "linux" and "unix" values (representing all OSes that are of
     * that variety).  "other" is allowed, representing all OSes not of those varieties.
     * "*" is also allowed, representing all OSes.
     */
    private boolean accept(String s) {
        
        if (fromVersion != null && toVersion != null) {
            if (fromVersion == BAD || toVersion == BAD)
                return false;
            try {
                Version ours = new Version(OSUtils.getOSVersion());
                if (fromVersion != STAR && ours.compareTo(fromVersion) < 0) // inclusive
                    return false;
                if (toVersion != STAR && ours.compareTo(toVersion) >= 0) //exclusive
                    return false;
            } catch (VersionFormatException ignore) {}
        }
        
        String os = OSUtils.getOS().toLowerCase(Locale.US);
        
        if(s.equals(os))
            return true;
        
        if("windows".equals(s))
            return OSUtils.isWindows();
        else if("mac".equals(s))
            return OSUtils.isAnyMac();
        else if("linux".equals(s))
            return OSUtils.isLinux();
        else if("unix".equals(s))
            return OSUtils.isUnix() && !OSUtils.isLinux();
        else if("other".equals(s))
            return !OSUtils.isWindows() && !OSUtils.isAnyMac() &&
                   !OSUtils.isUnix() && !OSUtils.isLinux();
        else if("*".equals(s))
            return true;
        
        return false;
    }
}