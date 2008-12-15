package org.limewire.ui.swing.util;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Spawns the BrowseForFolder dialog for Windows.
 *
 */
public final class BrowseForFolder {
	
	private Shell32 lib;
	private Shell32.BROWSEINFO info;
	
	//see http://msdn.microsoft.com/en-us/library/bb773205(VS.85).aspx 
	//for documentation on these flags and the BROWSEINFO struct
	public final static int BIF_RETURNONLYFSDIRS = 1;
	public final static int BIF_DONTGOBELOWDOMAIN = 2;
	public final static int BIF_STATUSTEXT = 4;
	public final static int BIF_RETURNFSANCESTORS = 8;
	public final static int BIF_EDITBOX = 16;
	public final static int BIF_VALIDATE = 32;
	public final static int BIF_NEWDIALOGSTYLE = 64;
	public final static int BIF_BROWSEINCLUDEURLS = 128;
	public final static int BIF_USENEWUI = 256;
	public final static int BIF_UAHINT = 512;
	public final static int BIF_NONEWFOLDERBUTTON = 1024;
	public final static int BIF_NOTRANSLATETARGETS = 2048;
	public final static int BIF_BROWSEFORCOMPUTER = 4096;
	public final static int BIF_BROWSEFORPRINTER = 8192;
	public final static int BIF_BROWSEINCLUDEFILES = 16384;
	public final static int BIF_SHAREABLE = 32768;

	/**
	 * 
	 * @param msg the message to show the user. Appears above the tree.
	 * @param showEditBox true shows the edit box below the tree
	 * @param allowNewFolder true allows users to create a new folder in this dialog
	 */
	public BrowseForFolder(String msg, boolean showEditBox, boolean allowNewFolder){
		
		//initialize the struct
    	lib = Shell32.INSTANCE;
    	info = new Shell32.BROWSEINFO();
    	info.lpszTitle = msg;
    	
    	int flags = showEditBox ? BIF_EDITBOX : 0;
    	flags = flags | (allowNewFolder ? BIF_NEWDIALOGSTYLE : 0);
    	
    	//always hide recycle bin and see what other flags are set
    	info.ulFlags = BIF_RETURNONLYFSDIRS | flags;
	}
	
	/**
	 * Shows the widget
	 * @return a string containing the absolute path the user choose
	 */
	public String showWidget(){
		Pointer ptr = lib.SHBrowseForFolder(info);
		byte[] path = new byte[1024];
		lib.SHGetPathFromIDList(ptr, path);
		return Native.toString(path);
	}

	//Creates an interface loads the shell32.dll library
	private interface Shell32 extends Library  {
		Shell32 INSTANCE = (Shell32)
        Native.loadLibrary("shell32", Shell32.class);

		//A Java version of the C++ struct used for this dialog
	    public class BROWSEINFO extends Structure {
		   public Pointer hwndOwner;
		   public Pointer pidlRoot;
		   public Pointer pszDisplayName;
		   public String lpszTitle;
		   public int ulFlags;
		   public Pointer lpfn;
		   public Pointer lParam;
		   public int iImage;
	    }
    
	   //Method headers we will be calling from Java
	   Pointer SHBrowseForFolder(BROWSEINFO info);
	   Boolean SHGetPathFromIDList(Pointer idl, byte[] path);
	}
}

