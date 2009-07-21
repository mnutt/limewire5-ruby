package org.limewire.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.limewire.inspection.Inspectable;
import org.limewire.inspection.InspectionPoint;

/**
 * Provides methods to get operating system properties, resources and versions, 
 * and determine operating system criteria.
 */
public class OSUtils {
    
    private static final Log LOG = LogFactory.getLog(OSUtils.class);
    
    static {
        setOperatingSystems();
    }
    
    /** 
     * Variable for whether or not we're on Windows.
     */
    private static boolean _isWindows;
    
    /** 
     * Variable for whether or not we're on Windows NT.
     */
    private static boolean _isWindowsNT;

    /** 
     * Variable for whether or not we're on Windows XP.
     */
    private static boolean _isWindowsXP;
    
    /** 
     * Variable for whether or not we're on Windows 7.
     */
    private static boolean _isWindows7;

    /** 
     * Variable for whether or not we're on Windows 95.
     */
    private static boolean _isWindows95;

    /** 
     * Variable for whether or not we're on Windows 98.
     */
    private static boolean _isWindows98;

    /** 
     * Variable for whether or not we're on Windows Me.
     */
    private static boolean _isWindowsMe;
       
    /** 
     * Variable for whether or not we're on Windows Vista.
     */
    private static boolean _isWindowsVista;
    
    /**
     * Variable for whether or not we're on Windows Vista SP2 or higher.
     */
    private static boolean _isSlightlyLessBrokenVersionOfWindowsVista;

    /** 
     * Variable for whether or not the operating system allows the 
     * application to be reduced to the system tray.
     */
    private static boolean _supportsTray;

    /** 
     * Variable for whether or not we're on Mac OS X.
     */
    private static boolean _isMacOSX;
    
    /** 
     * Variable for whether or not we're on Linux.
     */
    private static boolean _isLinux;

    /** 
     * Variable for whether or not we're on Solaris.
     */
    private static boolean _isSolaris;

    /**
     * Variable for whether or not we're on OS/2.
     */
    private static boolean _isOS2;

    /** Operating System information */
    @SuppressWarnings("unused") @InspectionPoint("os_info")
    private static final Inspectable osInspect = new OSInspecter();

    /**
     * Sets the operating system variables.
     */
    public static void setOperatingSystems() {
    	_isWindows = false;
    	_isWindowsVista = false;
        _isSlightlyLessBrokenVersionOfWindowsVista = false;
    	_isWindowsNT = false;
    	_isWindowsXP = false;
    	_isWindows7 = false;
    	_isWindows95 = false;
    	_isWindows98 = false;
    	_isWindowsMe = false;
    	_isSolaris = false;
    	_isLinux = false;
    	_isOS2 = false;
    	_isMacOSX = false;
    
    	String os = System.getProperty("os.name").toLowerCase(Locale.US);
    	String version = System.getProperty("os.version").toLowerCase(Locale.US);
    	
    	// set the operating system variables
    	_isWindows = os.indexOf("windows") != -1;
    	
    	if (os.indexOf("windows nt") != -1) {
    		_isWindowsNT = true;
    	} else if (os.indexOf("windows xp") != -1) { 
    		_isWindowsXP = true;
    	} else if(os.indexOf("windows 7") != -1) {
            _isWindows7 = true;
        } else if (os.indexOf("windows vista") != -1 && version.startsWith("6.1")) {
            //In jdk 1.6 before update 14 the os.name system property still returns Windows Vista
            //The version number is set to 6.1 however, so we can check for that and windows vista 
            //together to determine if it is windows 7
            _isWindows7 = true;        
        } else if (os.indexOf("windows vista") != -1) {
            _isWindowsVista = true;        
    	} else if(os.indexOf("windows 95") != -1) {
    	   _isWindows95 = true;
    	} else if(os.indexOf("windows 98") != -1) {
    	   _isWindows98 = true;
    	} else if(os.indexOf("windows me") != -1) {
    	   _isWindowsMe = true;
    	} else if(os.indexOf("solaris") != -1) {
    	    _isSolaris = true;    
    	} else if(os.indexOf("linux")   != -1) {
    	    _isLinux   = true;    
    	} else if(os.indexOf("os/2")    != -1) {
    	    _isOS2     = true;    
    	}
        
        if(_isWindows || _isLinux)
            _supportsTray = true;
        
    	if(os.startsWith("mac os")) {
    		if(os.endsWith("x")) {
    			_isMacOSX = true;
    		}
    	}
        
        // If this is Windows Vista, try to find out whether SP2 (or higher)
        // is installed, which removes the half-open TCP connection limit.
    	if(_isWindowsVista) {
            BufferedReader br = null;
    	    try {
                // Execute reg.exe to query a registry key
    	        Process p = Runtime.getRuntime().exec(new String[] {
    	                "reg",
    	                "query",
    	                "HKLM\\Software\\Microsoft\\Windows NT\\CurrentVersion",
    	                "/v",
    	                "CSDVersion"
    	        });
                // Parse the output
    	        br = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
    	        String line = null;
    	        while((line = br.readLine()) != null) {
    	            if(line.matches(".*CSDVersion.*"))
    	                break;
    	        }
                // Assume there won't be more than 9 service packs for Vista
    	        if(line != null && line.matches(".*Service Pack [2-9]")) {
                    LOG.debug("Slightly less broken version of Windows Vista");
    	            _isSlightlyLessBrokenVersionOfWindowsVista = true;
                }
    	    } catch(Throwable t) {
    	        LOG.debug("Failed to determine Windows version", t);
    	    } finally {
                if(br != null) {
                    try {
                        br.close();
                    } catch(IOException ignored) {}
                }
            }
    	}
    }    

    /**
     * Returns the operating system.
     */
    public static String getOS() {
        return System.getProperty("os.name");
    }

    /**
     * Returns the operating system version.
     */
    public static String getOSVersion() {
        return System.getProperty("os.version");
    }

    /**
     * Returns the operating system architecture.
     */
    public static String getOSArch() {
        return System.getProperty("os.arch");
    }

    /**
     * Returns true if this is Windows NT or Windows 2000 and
     * hence can support a system tray feature.
     */
    public static boolean supportsTray() {
    	return _supportsTray;
    }

    /**
     * Returns whether or not the OS is some version of Windows.
     *
     * @return <tt>true</tt> if the application is running on some Windows 
     *         version, <tt>false</tt> otherwise
     */
    public static boolean isWindows() {
    	return _isWindows;
    }

    /**
     * Returns whether or not the OS is WinXP.
     *
     * @return <tt>true</tt> if the application is running on WinXP,
     *  <tt>false</tt> otherwise
     */
    public static boolean isWindowsXP() {
    	return _isWindowsXP;
    }
    
    /**
     * Returns whether or not the OS is Windows 7..
     *
     * @return <tt>true</tt> if the application is running on Windows 7,
     *  <tt>false</tt> otherwise
     */
    public static boolean isWindows7() {
        return _isWindows7;
    }
    
    /**
     * @return true if the application is running on Windows NT
     */
    public static boolean isWindowsNT() {
        return _isWindowsNT;
    }
    
    /**
     * @return true if the application is running on Windows 95
     */
    public static boolean isWindows95() {
        return _isWindows95;
    }
    
    /**
     * @return true if the application is running on Windows 98
     */
    public static boolean isWindows98() {
        return _isWindows98;
    }
    
    /**
     * @return true if the application is running on Windows ME
     */
    public static boolean isWindowsMe() {
        return _isWindowsMe;
    }
    
    /**
     * @return true if the application is running on Windows Vista
     */
    public static boolean isWindowsVista() {
        return _isWindowsVista;
    }
    
    /**
     * @return true if the application is running on a windows with
     * the 10 socket limit.
     */
    public static boolean isSocketChallengedWindows() {
        return _isWindowsXP || (_isWindowsVista && !_isSlightlyLessBrokenVersionOfWindowsVista);
    }

    /**
     * Returns whether or not the OS is OS/2.
     *
     * @return <tt>true</tt> if the application is running on OS/2,
     *         <tt>false</tt> otherwise
     */
    public static boolean isOS2() {
        return _isOS2;
    }

    /** 
     * Returns whether or not the OS is Mac OS X.
     *
     * @return <tt>true</tt> if the application is running on Mac OS X, 
     *         <tt>false</tt> otherwise
     */
    public static boolean isMacOSX() {
    	return _isMacOSX;
    }

    /** 
     * Returns whether or not the OS is Solaris.
     *
     * @return <tt>true</tt> if the application is running on Solaris, 
     *         <tt>false</tt> otherwise
     */
    public static boolean isSolaris() {
    	return _isSolaris;
    }

    /** 
     * Returns whether or not the OS is Linux.
     *
     * @return <tt>true</tt> if the application is running on Linux, 
     *         <tt>false</tt> otherwise
     */
    public static boolean isLinux() {
    	return _isLinux;
    }

    /** 
     * Returns whether or not the OS is some version of
     * Unix, defined here as only Solaris or Linux.
     */
    public static boolean isUnix() {
    	return _isLinux || _isSolaris; 
    }

    /**
     * Returns whether the OS is POSIX-like. 
     */
    public static boolean isPOSIX() {
        return _isLinux || _isSolaris || _isMacOSX;
    }

    /**
     * Returns whether or not this operating system is considered
     * capable of meeting the requirements of a high load server.
     *
     * @return <tt>true</tt> if this OS meets high load server requirements,
     *         <tt>false</tt> otherwise
     */
    public static boolean isHighLoadOS() {
        return !(_isWindows98 || _isWindows95 || _isWindowsMe || _isWindowsNT);
    }

    /**
     * @return true if this is a well-supported version of windows.
     * (not 95, 98, nt or me)
     */
    public static boolean isGoodWindows() {
    	return isWindows() && isHighLoadOS();
    }

    /**
     * Return whether the current operating system supports moving files
     * to the trash. 
     */
    public static boolean supportsTrash() {
        return isWindows() || isMacOSX();
    }
    
    /**
     * Returns the maximum path system of file system of the current OS
     * or a conservative approximation.
     */
    public static int getMaxPathLength() { 
        if (isWindows()) {
            return Short.MAX_VALUE;
        }
        else if (isLinux()) {
            return 4096 - 1;
        }
        else {
            return 1024 - 1;
        }
    }
    
    public static boolean supportsTLS() {
        return true;
    }
    
    private static class OSInspecter implements Inspectable {
        @Override
        public Object inspect() {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("os_name", getOS());
            data.put("os_ver", getOSVersion());
            data.put("os_arch", getOSArch());
            data.put("num_cpus", Runtime.getRuntime().availableProcessors());
            return data;
        }
    }
}
