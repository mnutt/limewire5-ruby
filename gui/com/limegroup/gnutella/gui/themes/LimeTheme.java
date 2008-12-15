package com.limegroup.gnutella.gui.themes;

import java.awt.Font;

import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

/**
 * This class defines the colors used in the application.
 */
//2345678|012345678|012345678|012345678|012345678|012345678|012345678|012345678|
public final class LimeTheme extends DefaultMetalTheme {

	@Override
    public String getName() { return "LimeTheme"; }

	// 0, 51, 102, 153, 204, 255 -- the web safe rgb values

	// BASIC METAL COLORS (PLUS WHITE AND BLACK)

	// Primary1: titles & highlight
	private final ColorUIResource prim1 = 
		new ColorUIResource(ThemeFileHandler.PRIMARY1_COLOR.getValue());

	// Primary2: depressed menu
	private final ColorUIResource prim2 = 
		new ColorUIResource(ThemeFileHandler.PRIMARY2_COLOR.getValue());

	// Primary3: tooltip & scrollbar highlight
	private final ColorUIResource prim3 = 
		new ColorUIResource(ThemeFileHandler.PRIMARY3_COLOR.getValue());

	// Secondary1: base outline
	private final ColorUIResource sec1 = 
		new ColorUIResource(ThemeFileHandler.SECONDARY1_COLOR.getValue());

	// Secondary2: inactive tabs, table cols & section outline
	// old "darker" color (135, 145, 170)
	private final ColorUIResource sec2 = 
		new ColorUIResource(ThemeFileHandler.SECONDARY2_COLOR.getValue());

	// Secondary3: background color
	private final ColorUIResource sec3 = 
		new ColorUIResource(ThemeFileHandler.SECONDARY3_COLOR.getValue());

	//SystemText: playoptions mp3list library sharing prompt everything in options
	private final ColorUIResource win1 = 
		new ColorUIResource(ThemeFileHandler.WINDOW1_COLOR.getValue());


	// CUSTOMIZED METAL COMPONENT COLORS (NEEDED FOR THOSE MAPPED TO WHITE/BLACK)

	//Control: background color
	//(initially mapped to Secondary2)
	private final ColorUIResource win2 = 
		new ColorUIResource(ThemeFileHandler.WINDOW2_COLOR.getValue());

	//ControlHighlight: highlite around radiobuttons inputfields and whole windows
	//(initially mapped to Secondary3)
	private final ColorUIResource win3 = 
		new ColorUIResource(ThemeFileHandler.WINDOW3_COLOR.getValue());

	//ControlShadow: Missing
	//(mapped to Secondary1)
	//ControlDarkShadow: Missing
	//(mapped to Black)

	//ControlTextColor: TABtext - liblist - buttons
	//(initially mapped to Primary1)
	private final ColorUIResource win4 = 
		new ColorUIResource(ThemeFileHandler.WINDOW4_COLOR.getValue());

	//ControlInfo: checks and little arrows on drop downs
	//(initially mapped to Secondary1)
	private final ColorUIResource win5 = 
		new ColorUIResource(ThemeFileHandler.WINDOW5_COLOR.getValue());

	//WindowBackground: unspecified
	//(initially mapped to Primary3)
	private final ColorUIResource win6 = 
		new ColorUIResource(ThemeFileHandler.WINDOW6_COLOR.getValue());

	//WindowTitleInactiveForeground: unspecified
	//(initially mapped to White)
	private final ColorUIResource win7 =  
		new ColorUIResource(ThemeFileHandler.WINDOW7_COLOR.getValue());

	//UserTextColor: results list and blinking cursor
	//(initially mapped to Black)
	private final ColorUIResource win8 = 
		new ColorUIResource(ThemeFileHandler.WINDOW8_COLOR.getValue());

	//MenuForeground:
	//(initially mapped to Primary1)
	private final ColorUIResource win9 = 
		new ColorUIResource(ThemeFileHandler.WINDOW9_COLOR.getValue());

	//MenuSelectedForeground:
	//(initially mapped to Primary1)
	private final ColorUIResource win10 = 
		new ColorUIResource(ThemeFileHandler.WINDOW10_COLOR.getValue());

	//DesktopColor: MDI container or desktop background
	//(initially mapped to Secondary3)
	private final ColorUIResource win11 = 
		new ColorUIResource(ThemeFileHandler.WINDOW11_COLOR.getValue());

	//MenuBackground:
	//(initially mapped to Secondary3)
	private final ColorUIResource win12 = 
		new ColorUIResource(ThemeFileHandler.WINDOW12_COLOR.getValue());

	private FontUIResource controlFont;	//dialog, BOLD_STYLE, 11/12
	private FontUIResource captionFont;	//dialog, BOLD, 11/12
	private FontUIResource systemFont;	//dialog, PLAIN/BOLD_STYLE, 11/12
	private FontUIResource userFont;	//dialog(input), PLAIN, 11/12
	private FontUIResource smallFont;	//dialog, PLAIN, 10

	@Override
    protected ColorUIResource getPrimary1()   { return prim1; }
	@Override
    protected ColorUIResource getPrimary2()   { return prim2; }
	@Override
    protected ColorUIResource getPrimary3()   { return prim3; }
	@Override
    protected ColorUIResource getSecondary1() { return sec1; }
	@Override
    protected ColorUIResource getSecondary2() { return sec2; }
	@Override
    protected ColorUIResource getSecondary3() { return sec3; }

	@Override
    public ColorUIResource getSystemTextColor()               { return win1; }
	@Override
    public ColorUIResource getControl()                       { return win2; }
	@Override
    public ColorUIResource getControlHighlight()              { return win3; }
	@Override
    public ColorUIResource getControlTextColor()              { return win4; }
	@Override
    public ColorUIResource getControlInfo()                   { return win5; }
	@Override
    public ColorUIResource getWindowBackground()              { return win6; }
	@Override
    public ColorUIResource getWindowTitleInactiveForeground() { return win7; }
	@Override
    public ColorUIResource getUserTextColor()                 { return win8; }
	@Override
    public ColorUIResource getMenuForeground()                { return win9; }
	@Override
    public ColorUIResource getMenuSelectedForeground()        { return win10; }
	@Override
    public ColorUIResource getDesktopColor()                  { return win11; }
	@Override
    public ColorUIResource getMenuBackground()                { return win12; }

	// inherit doc comment
	@Override
    public FontUIResource getControlTextFont() {
		if (controlFont == null) {
			Font font = new Font(
				ThemeFileHandler.CONTROL_TEXT_FONT_NAME.getValue(), 
				ThemeFileHandler.CONTROL_TEXT_FONT_STYLE.getValue(),
				ThemeFileHandler.CONTROL_TEXT_FONT_SIZE.getValue());
			try {
				controlFont =  new FontUIResource(Font.getFont(
					"swing.plaf.metal.controlFont",
					font));
			} catch (Exception e) {
				controlFont =  new FontUIResource(font);
			}
		}
		return controlFont;
	}

	// inherit doc comment
	@Override
    public FontUIResource getSystemTextFont() {
		if (systemFont == null) {
			Font font = new Font(
				ThemeFileHandler.SYSTEM_TEXT_FONT_NAME.getValue(), 
				ThemeFileHandler.SYSTEM_TEXT_FONT_STYLE.getValue(),
				ThemeFileHandler.SYSTEM_TEXT_FONT_SIZE.getValue());
			try {
				systemFont = new FontUIResource(Font.getFont(
					"swing.plaf.metal.systemFont",
					font));
			} catch (Exception e) {
				systemFont = new FontUIResource(font);
			}
		}
		return systemFont;
	}

	// inherit doc comment
	@Override
    public FontUIResource getUserTextFont() {
		if (userFont == null) {
			Font font = new Font(
				ThemeFileHandler.USER_TEXT_FONT_NAME.getValue(), 
				ThemeFileHandler.USER_TEXT_FONT_STYLE.getValue(),
				ThemeFileHandler.USER_TEXT_FONT_SIZE.getValue());
			try {
				userFont = new FontUIResource(Font.getFont(
					"swing.plaf.metal.userFont",
					font));
			} catch (Exception e) {
				userFont = new FontUIResource(font);
			}
		}
		return userFont;
	}

	// inherit doc comment
	@Override
    public FontUIResource getMenuTextFont() {
		if (controlFont == null) {
			Font font = new Font(
				ThemeFileHandler.MENU_TEXT_FONT_NAME.getValue(), 
				ThemeFileHandler.MENU_TEXT_FONT_STYLE.getValue(),
				ThemeFileHandler.MENU_TEXT_FONT_SIZE.getValue());
			try {
				controlFont = new FontUIResource(Font.getFont(
					"swing.plaf.metal.controlFont",
					font));
			} catch (Exception e) {
				controlFont = new FontUIResource(font);
			}
		}
		return controlFont;
	}

	// inherit doc comment
	@Override
    public FontUIResource getWindowTitleFont() {
		if (captionFont == null) {
			Font font = new Font(
				ThemeFileHandler.WINDOW_TITLE_FONT_NAME.getValue(), 
				ThemeFileHandler.WINDOW_TITLE_FONT_STYLE.getValue(),
				ThemeFileHandler.WINDOW_TITLE_FONT_SIZE.getValue());
			try {
				captionFont = new FontUIResource(Font.getFont(
					"swing.plaf.metal.controlFont",
					font));
			} catch (Exception e) {
				captionFont = new FontUIResource(font);
			}
		}
		return captionFont;
	}

	// inherit doc comment
	@Override
    public FontUIResource getSubTextFont() {
		if (smallFont == null) {
			Font font = new Font(
				ThemeFileHandler.SUB_TEXT_FONT_NAME.getValue(), 
				ThemeFileHandler.SUB_TEXT_FONT_STYLE.getValue(),
				ThemeFileHandler.SUB_TEXT_FONT_SIZE.getValue());
			try {
				smallFont = new FontUIResource(Font.getFont(
					"swing.plaf.metal.smallFont",
					font));
			} catch (Exception e) {
				smallFont = new FontUIResource(font);
			}
		}
		return smallFont;
    }

}
