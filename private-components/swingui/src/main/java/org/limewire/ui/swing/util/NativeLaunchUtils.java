package org.limewire.ui.swing.util;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Locale;

import javax.swing.JOptionPane;

import org.limewire.concurrent.ManagedThread;
import org.limewire.core.api.Category;
import org.limewire.logging.Log;
import org.limewire.logging.LogFactory;
import org.limewire.ui.swing.components.FocusJOptionPane;
import org.limewire.util.OSUtils;
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
public final class NativeLaunchUtils {
    private static final Log LOG = LogFactory.getLog(NativeLaunchUtils.class);
            
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
    private NativeLaunchUtils() {}
    
    /**
     * Opens the specified url in a browser. 
     *
     * <p>A browser will only be opened if the underlying operating system 
     * recognizes the url as one that should be opened in a browser, 
     * namely a url that ends in .htm or .html.
     *
     * @param url the url to open
     */
    public static void openURL(final String url) {
        ManagedThread managedThread = new ManagedThread( new Runnable() {
            @Override
            public void run() {
                try {
                    Desktop.getDesktop().browse(new URI(url));
                } catch (Throwable t) {
                    try {
                        if (OSUtils.isWindows()) {
                            openURLWindows(url);
                        } else if (OSUtils.isMacOSX()) {
                            openURLMac(url);
                        } else {
                            openURLLinux(url);
                        }
                    } catch (IOException iox) {
                        logException(I18n.tr("Unable to open URL"), I18n.tr("Open URL"), iox);
                    }
                } 
            }
        });
        managedThread.start();
    }

    /**
     * Trys to open the url with the default browser on linux, passing it the specified
     * url.
     * 
     * @param url the url to open in the browser
     */
    private static Process openURLLinux(String url) throws IOException {
        return exec("xdg-open", url);
    }

    /**
     * Opens the default web browser on windows, passing it the specified
     * url.
     *
     * @param url the url to open in the browser
     */
    private static void openURLWindows(String url) throws IOException {
        SystemUtils.openURL(url);
    }

    /**
     * Opens the specified url in the default browser on the Mac.
     * This makes use of the dynamically-loaded MRJ classes.
     *
     * @param url the url to load
     *
     * @throws <tt>IOException</tt> if the necessary Mac classes were not
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
     * Launches the specified file.  If the file's Category is PROGRAM or OTHER, this delegates to 
     * <code>launchExplorer(file)</code>
     */
    public static void safeLaunchFile(File file){
        Category category = CategoryUtils.getCategory(file);
        if(category == Category.PROGRAM || category == Category.OTHER){
            launchExplorer(file);
        } else {
            launchFile(file);
        }
    }

    /**
     * Launches the file whose abstract path is specified in the <tt>File</tt>
     * parameter. This method will not launch any file with .exe, .vbs, .lnk,
     * .bat, .sys, or .com extensions, displaying an error if one of the file is
     * of one of these types.
     * 
     * This is run on its own thread to prevent ui calls from blocking.
     *
     * @param file the file to launch
     * @return an object for accessing the launch process; null, if the process
     *         can be represented (e.g. the file was launched through a native
     *         call)
     */
    private static void launchFile(final File file) {
        ManagedThread managedThread = new ManagedThread( new Runnable() {
            @Override
            public void run() {
                try {
                    launchFileImpl(file);
                } catch (LaunchException lex) {
                    logException(I18n.tr("Unable to open file: {0}", file.getName()),
                            I18n.tr("Open File"), lex);
                } catch (IOException iox) {
                    logException(I18n.tr("Unable to open file: {0}", file.getName()),
                            I18n.tr("Open File"), iox);
                } catch (SecurityException ex) {
                    logException(I18n.tr("Unable to open file: {0}", file.getName()),
                            I18n.tr("Open File"), ex);
                }
            }
        });
        managedThread.start();
        
    }
    
    private static void launchFileImpl(File file) throws IOException, SecurityException {
        String path = file.getCanonicalPath();
        String extCheckString = path.toLowerCase(Locale.US);

        if(!extCheckString.endsWith(".exe") &&
                    !extCheckString.endsWith(".vbs") &&
                    !extCheckString.endsWith(".lnk") &&
                    !extCheckString.endsWith(".bat") &&
                    !extCheckString.endsWith(".sys") &&
                    !extCheckString.endsWith(".com")) {
            
             if(OSUtils.isLinux()) {
                 //Desktop.open is not working well under linux
                 //it converts the path to a uri and many programs are not supporting it properly
                 int exitCode = -1;
                 try {
                     Process process = launchFileLinux(path);
                     exitCode = process.waitFor();
                 } catch(Exception e) {
                     //exceptions can be thrown when launcher does not exist
                     exitCode = -1;
                 }
                 
                 if(exitCode != 0) {
                     //a non-zero exit value means there was an error opening the file
                     //failing back to Desktop.open
                     openFile(file);
                 }
             } else {
                 //Using Desktop.open for windows and mac
                 openFile(file);
             }
        } else {
             throw new SecurityException();
         }
    }

    private static void openFile(File file) throws IOException {
        String path = file.getCanonicalPath();
         try {
             Desktop.getDesktop().open(file);
         } catch(Throwable t) {
             //failing over to native implementations when Desktop.open fails.
             if (OSUtils.isWindows()) {
                 launchFileWindows(path);
             } else if (OSUtils.isMacOSX()) {
                 launchFileMacOSX(path);
             } else {
                 launchFileLinux(path);
             }
         }
    }

    /**
     * Launches the Explorer/Finder and highlights the file.
     * 
     * @param file the file to show in explorer
     * @return null, if not supported by platform; the launched process otherwise
     * @see #safeLaunchFile(File)
     */
    public static Process launchExplorer(File file) {
        try {
            return launchExplorerImpl(file);
        } catch (LaunchException lex) {
            logException(I18n.tr("Unable to locate file: {0}", file.getName()),
                    I18n.tr("Locate File"), lex);
            return null;
        } catch (SecurityException ex) {
            logException(I18n.tr("Unable to locate file: {0}", file.getName()),
                    I18n.tr("Locate File"), ex);
            return null;
        } catch (IOException iox) {
            logException(I18n.tr("Unable to locate file: {0}", file.getName()),
                    I18n.tr("Locate File"), iox);
            return null;
        }
    }
    
    private static Process launchExplorerImpl(File file) throws IOException, SecurityException {
        if (OSUtils.isWindows()) {
            String explorePath = file.getPath(); 
            try { 
                explorePath = file.getCanonicalPath(); 
            } catch (IOException ignored) {
            } 
            
            if(file.isDirectory()) {
                return exec(new String[] { "explorer", explorePath });
            } else {
                // launches explorer and highlights the file
                return exec(new String[] { "explorer", "/select,", explorePath });
            }
            
        } else if (OSUtils.isMacOSX()) {
            // launches the Finder and highlights the file
            return exec(selectFileCommand(file));
        } else if (OSUtils.isLinux()) {
            // launches the Finder and highlights the file
            return exec(selectFileCommandLinux(file));
        }
        return null;
    }
    
    private static String[] selectFileCommandLinux(File file) {
        String path = null;
        File parentDir = file.isDirectory() ? file : file.getParentFile();
        try {
            path = parentDir.getCanonicalPath();
        } catch (IOException err) {
            path = parentDir.getAbsolutePath();
        }
        return new String[] {"xdg-open", path};        
    }

    /**
     * Launches the given file on Linux.
     *
     * @param path the path of the file to launch
     *
     * @return Process which was used to open the file. In this case the xdg-open. 
     * The actual file will be opened in another process.
     */
    private static Process launchFileLinux(String path) throws IOException {
        return exec("xdg-open", path);
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
     * @throws IOException if an I/O error occurs in making the runtime.exec()
     *  call or in getting the canonical path of the file
     */
    private static Process launchFileMacOSX(final String file) throws IOException {
        return exec(new String[]{"open", file});
    }
    
    /**
     * Launches the Finder and selects the given File.
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
    
    private static Process exec(String... commands) throws LaunchException {
        ProcessBuilder pb = new ProcessBuilder(commands);
        try {
            return pb.start();
        } catch (IOException e) {
            throw new LaunchException(e, commands);
        }
    }
    
    /**
     * Logs the specified exception, and displays the specified user message
     * if the current thread is the UI thread.
     */
    private static void logException(final String userMessage, final String title, Exception ex) {
        // Report exception to logger.
        LOG.error(userMessage, ex);
        
        // Display user message
        SwingUtils.invokeLater( new Runnable() {
            @Override
            public void run() {
                FocusJOptionPane.showMessageDialog(GuiUtils.getMainFrame(),
                        userMessage, title, JOptionPane.INFORMATION_MESSAGE);
            }
        });
    }
    
    public static class LaunchException extends IOException {
        
        private final String[] command;

        /**
         * @param cause the exception that occurred during execution of command
         * @param command the executed command
         */
        public LaunchException(IOException cause, String... command) {
            this.command = command;
            
            initCause(cause);
        }

        /**
         * @param command the executed command.
         */
        public LaunchException(String... command) {
            this.command = command;
        }

        public String[] getCommand() {
            return command;
        }
    }
    
}
