package com.limegroup.gnutella.gui;

import java.io.File;

/**
 * Constants used by gui classes.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class GUIConstants {

	/**
	 * Constant for the locale-specific resource key for the icon for the plug.
	 */
	public static final String LIMEWIRE_ICON = "limeicon";

	/**
	 * Constant for the the path to the LimeWire Windows launcher.
	 */
	public static final File LIMEWIRE_EXE_FILE = new File("LimeWire.exe").getAbsoluteFile();

	/**
	 * The number of pixels in the margin of a padded panel.
	 */
	public static final int OUTER_MARGIN = 6;

	/**
	 * Standard number of pixels that should separate many 
	 * different types of gui components.
	 */
    public static final int SEPARATOR = 6;

    /**
	 * Strings for different connection speeds.
	 */
    public static final String MODEM_SPEED = 
		I18n.tr("Dial Up");
    public static final String CABLE_SPEED = 
		I18n.tr("Broadband");
    public static final String T1_SPEED = 
		I18n.tr("T1");
    public static final String T3_SPEED = 
		I18n.tr("T3 or Higher");
    public static final String MULTICAST_SPEED =
        I18n.tr("Ethernet");
    
    public static final String THIRD_PARTY_RESULTS_SPEED = "--";    

	/** 
	 * the interval between statistics updates. 
	 */
	public static final int UPDATE_TIME = 2000;
}
