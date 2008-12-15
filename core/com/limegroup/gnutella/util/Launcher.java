package com.limegroup.gnutella.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Locale;

import org.limewire.core.settings.URLHandlerSettings;
import org.limewire.util.MediaType;
import org.limewire.util.OSUtils;
import org.limewire.util.QuotedStringTokenizer;
import org.limewire.util.StringUtils;
import org.limewire.util.SystemUtils;



/**
 * This class launches files in their associated applications and opens 
 * urls in the default browser for different operating systems.  This
 * really only works meaningfully for the Mac and Windows.<p>
 *
 * Acknowledgement goes to Eric Albert for demonstrating the general 
 * technique for loading the MRJ classes in his frequently-used
 * "BrowserLauncher" code.
 * <p>
 * This code is Copyright 1999-2001 by Eric Albert (ejalbert@cs.stanford.edu) 
 * and may be redistributed or modified in any form without restrictions as
 * long as the portion of this comment from this paragraph through the end of  
 * the comment is not removed.  The author requests that he be notified of any 
 * application, applet, or other binary that makes use of this code, but that's 
 * more out of curiosity than anything and is not required.  This software
 * includes no warranty.  The author is not repsonsible for any loss of data 
 * or functionality or any adverse or unexpected effects of using this software.
 * <p>
 * Credits:
 * <br>Steven Spencer, JavaWorld magazine 
 * (<a href="http://www.javaworld.com/javaworld/javatips/jw-javatip66.html">Java Tip 66</a>)
 * <br>Thanks also to Ron B. Yeh, Eric Shapiro, Ben Engber, Paul Teitlebaum, 
 * Andrea Cantatore, Larry Barowski, Trevor Bedzek, Frank Miedrich, and Ron 
 * Rabakukk
 *
 * @author Eric Albert 
 *  (<a href="mailto:ejalbert@cs.stanford.edu">ejalbert@cs.stanford.edu</a>)
 * @version 1.4b1 (Released June 20, 2001)
 */
 //2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class Launcher {

	/**
	 * <tt>boolean</tt> specifying whether or not the necessary Mac
	 * classes were loaded successfully.
	 */
	private static boolean _macClassesLoadedSuccessfully = true;

	/**
	 * The openURL method of com.apple.mrj.MRJFileUtils.
	 */
	private static Method _openURL;

	/** 
	 * Loads the necessary Mac classes if running on Mac.
	 */
	static {
	    if(OSUtils.isMacOSX()) {
			try {
				loadMacClasses();		
			} catch(IOException ioe) {
				_macClassesLoadedSuccessfully = false;
			}
		}
	}

	/** 
	 * This class should be never be instantiated; this just ensures so. 
	 */
	private Launcher() {}
	
	/**
	 * Opens the specified url in a browser. 
	 *
	 * <p>A browser will only be opened if the underlying operating system 
	 * recognizes the url as one that should be opened in a browser, 
	 * namely a url that ends in .htm or .html.
	 *
	 * @param url  The url to open
	 *
	 * @return  An int indicating the success of the browser launch
	 *
	 * @throws IOException if the url cannot be loaded do to an IO problem
	 */
	public static int openURL(String url) throws IOException {	   
		if(OSUtils.isWindows()) {
			return openURLWindows(url);
		}	   
		else if(OSUtils.isMacOSX()) {
			openURLMac(url);
		}
		else {
		    // Other OS
			launchFileOther(url);
		}
		return -1;
	}

	/**
	 * Opens the default web browser on windows, passing it the specified
	 * url.
	 *
	 * @param url the url to open in the browser
	 * @return the error code of the native call, -1 if the call failed
	 *  for any reason
	 */
	private static int openURLWindows(String url) throws IOException {
		return SystemUtils.openURL(url);
	}
	
	/**
	 * Opens the specified url in the default browser on the Mac.
	 * This makes use of the dynamically-loaded MRJ classes.
	 *
	 * @param url the url to load
	 *
	 * @throws <tt>IOException</tt> if the necessary mac classes were not
	 *         loaded successfully or if another exception was
	 *         throws -- it wraps these exceptions in an <tt>IOException</tt>
	 */
	private static void openURLMac(String url) throws IOException {
		if(!_macClassesLoadedSuccessfully) throw new IOException();
		try {
			Object[] params = new Object[] {url};
			_openURL.invoke(null, params);
		} 
		catch (NoSuchMethodError err) {
			throw new IOException();
			// this can occur when earlier versions of MRJ are used which
			// do not support the openURL method.
		} catch (NoClassDefFoundError err) {
			throw new IOException();
			// this can occur under runtime environments other than MRJ.
		} catch (IllegalAccessException iae) {
			throw new IOException();
		} catch (InvocationTargetException ite) {
			throw new IOException();
		}
	}

	/**
	 * Launches the file whose abstract path is specified in the <tt>File</tt>
	 * parameter. This method will not launch any file with .exe, .vbs, .lnk,
	 * .bat, .sys, or .com extensions, diplaying an error if one of the file is
	 * of one of these types.
	 * 
	 * @param path
	 *            The path of the file to launch
	 * @return an object for accessing the launch process; null, if the process
	 *         can be represented (e.g. the file was launched through a native
	 *         call)
	 * @throws IOException
	 *             if the file cannot be launched
	 * @throws SecurityException
	 *             if the file has an extension that is not allowed
	 */
	public static LimeProcess launchFile(File file) throws IOException, SecurityException {
		String path = file.getCanonicalPath();
		String extCheckString = path.toLowerCase(Locale.US);

        // expand pmf files before display
        if (extCheckString.endsWith(".pmf")) {
            file = PackagedMediaFileUtils.preparePMFFile(file.toString());
            // don't launch an invalid file
            if (file == null) {
            	throw new IOException("Invalid file");
            }

            path = file.getCanonicalPath();
            extCheckString = path.toLowerCase(Locale.US);
        }

		if(!extCheckString.endsWith(".exe") &&
		   !extCheckString.endsWith(".vbs") &&
		   !extCheckString.endsWith(".lnk") &&
		   !extCheckString.endsWith(".bat") &&
		   !extCheckString.endsWith(".sys") &&
		   !extCheckString.endsWith(".com")) {
			if(OSUtils.isWindows()) {
				launchFileWindows(path);
				return null;
			}
			else if(OSUtils.isMacOSX()) {
				return launchFileMacOSX(path);
			}
			else {
			    // Other OS, use helper apps
				return launchFileOther(path);
			}
		} else {
			throw new SecurityException();
		}	
	}

    /**
     * Launches the Explorer/Finder and highlights the file.
     * 
     * @param file the file to show in explorer
     * @return null, if not supported by platform; the launched process otherwise
     * @see #launchFile(File)
     */
    public static LimeProcess launchExplorer(File file) throws IOException, SecurityException {
        if (OSUtils.isWindows()) {
            String explorePath = file.getPath(); 
            try { 
                explorePath = file.getCanonicalPath(); 
            } catch (IOException ignored) {
            } 
            
            if(file.isDirectory()) {
                // launches explorer in the directory
                LimeProcess.exec(new String[] { "explorer", explorePath });
            } else {
                // launches explorer and highlights the file
                return LimeProcess.exec(new String[] { "explorer", "/select,", explorePath });
            }
            
        } else if (OSUtils.isMacOSX()) {
            // launches the Finder and highlights the file
            return LimeProcess.exec(selectFileCommand(file));
        }
        return null;
    }
    
	/**
	 * Launches the given file on Windows.
	 *
	 * @param path the path of the file to launch
	 *
	 * @return an int for the exit code of the native method
	 */
	private static int launchFileWindows(String path) throws IOException {
        try {
            return SystemUtils.openFile(path);
        } catch(IOException iox) {
            throw new LaunchException(iox, path);
        }
	}

	/**
	 * Launches a file on OSX, appending the full path of the file to the
	 * "open" command that opens files in their associated applications
	 * on OSX.
	 *
	 * @param file the <tt>File</tt> instance denoting the abstract pathname
	 *  of the file to launch
	 * @return 
	 * @throws IOException if an I/O error occurs in making the runtime.exec()
	 *  call or in getting the canonical path of the file
	 */
	private static LimeProcess launchFileMacOSX(final String file) throws IOException {
	    return LimeProcess.exec(new String[]{"open", file});
	}
    
    /**
     * Launches the Finder and selects the given File
     */
    private static String[] selectFileCommand(File file) {
        String path = null;
        try {
            path = file.getCanonicalPath();
        } catch (IOException err) {
            path = file.getAbsolutePath();
        }
        
        String[] command = new String[] { 
                "osascript", 
                "-e", "set unixPath to \"" + path + "\"",
                "-e", "set hfsPath to POSIX file unixPath",
                "-e", "tell application \"Finder\"", 
                "-e",    "activate", 
                "-e",    "select hfsPath",
                "-e", "end tell" 
        };
        
        return command;
    }
    
	/** 
	 * Loads specialized classes for the Mac needed to launch files.
	 *
	 * @return <tt>true</tt>  if initialization succeeded,
	 *	   	   <tt>false</tt> if initialization failed
	 *
	 * @throws <tt>IOException</tt> if an exception occurs loading the
	 *         necessary classes
	 */
	@SuppressWarnings("unchecked")
    private static void loadMacClasses() throws IOException {
		try {
			Class mrjAdapter = Class.forName("net.roydesign.mac.MRJAdapter");
			_openURL = mrjAdapter.getDeclaredMethod("openURL", new Class[]{String.class});
		} catch (ClassNotFoundException cnfe) {
			throw new IOException();
		} catch (NoSuchMethodException nsme) {
			throw new IOException();
		} catch (SecurityException se) {
			throw new IOException();
		} 
	}
    
    
	/**
	 * Attempts to launch the given file.
	 *
	 * @throws IOException  if the call to Runtime.exec throws an IOException
	 *                      or if the Process created by the Runtime.exec call
	 *                      throws an InterruptedException
	 */
	private static LimeProcess launchFileOther(String path) throws IOException {
	    String handler;
	    if (MediaType.getAudioMediaType().matches(path)) {
	    	handler = URLHandlerSettings.AUDIO_PLAYER.getValue();
	    } else if (MediaType.getVideoMediaType().matches(path)) {
	    	handler = URLHandlerSettings.VIDEO_PLAYER.getValue();
	    } else if (MediaType.getImageMediaType().matches(path)) {
	    	handler = URLHandlerSettings.IMAGE_VIEWER.getValue();
	    } else {
	    	handler = URLHandlerSettings.BROWSER.getValue();
	    }

	    QuotedStringTokenizer tok = new QuotedStringTokenizer(handler);
	    String[] strs = new String[tok.countTokens()];
	    for (int i = 0; i < strs.length; i++) {
	    	strs[i] = StringUtils.replace(tok.nextToken(), "$URL$", path);
	    }

	    return LimeProcess.exec(strs);
    }
}
