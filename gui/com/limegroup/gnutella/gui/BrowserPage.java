package com.limegroup.gnutella.gui;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Rectangle;


public class BrowserPage extends Canvas
{
    /** Hold the active URL */
    private String m_strURL = null;

    /** Hold the canvas windows handle */
    private int m_hWnd = 0;

    /** native entry point for getting canvas handle */
    public native int getHWND();

    /** native entry point for initializing the IE control. */
    public native void initialize(int hWnd, String strURL);

    /** native entry point for resizing */
    public native void resizeControl(int hWnd, int nWidth, int nHeight);


	/**
	 *  Construct the canvas and set the url of the page to display
	 */
    public BrowserPage(String url) {
    	m_strURL = url;
	}

    
	/**
     *  Intercept the standard addNotify and pick up the true window handle
     */ 
    @Override
    public void addNotify() {
        super.addNotify();
        m_hWnd = getHWND();
        initialize(m_hWnd, m_strURL);
    }

	
//	/**
//     *  Test the component in a standalone frame
//	 */
//    public static void main( String[] argv ) {
//        // Load the library manually that contains the JNI code.
//        System.loadLibrary("BrowserPage");
//
//        Frame f = new Frame();
//        f.setLayout(new BorderLayout());
//        f.setTitle("Internet Explorer inside Java Canvas");
//		
//        BrowserPage w = new BrowserPage("http://www.limewire.org");
//        if(argv.length>0)
//            w.m_strURL = argv[0];
//
//        f.add(w,BorderLayout.CENTER);
//        //String strText = "URL:" + w.m_strURL;
//        //f.add(new Label(strText),BorderLayout.NORTH);
//        f.setBounds(200,200,700,500);
//        f.setVisible(true);
//
//		// Need this kludge to wake up the internal component.
//		try {
//			Thread.sleep(100);
//		} catch(Exception e) {};	
//        Rectangle r = w.getBounds();
//        w.setSize(r.width,r.height);
//    }

	/**
     *  Intercede on the setSize method to resize the native browser
	 */
    @Override
    public void setSize( int width, int height ) {
        super.setSize(width,height);
        if(m_hWnd != 0) {
            resizeControl(m_hWnd, width, height);
		}
    }

	/**
     *  Intercede on the setSize method to resize the native browser
	 */
    @Override
    public void setSize( Dimension d ) {
        super.setSize(d);
        if(m_hWnd != 0) {
            resizeControl(m_hWnd, d.width, d.height);
		}
    }

	/**
     *  Intercede on the setBounds method to resize the native browser
	 */
    @Override
    public void setBounds( int x, int y, int width, int height ) {
        super.setBounds(x,y,width,height);
        if(m_hWnd != 0) {
            resizeControl(m_hWnd, width, height);
		}
    }

	/**
     *  Intercede on the setBounds method to resize the native browser
	 */
    @Override
    public void setBounds( Rectangle r ) {
        super.setBounds(r);
        if(m_hWnd != 0) {
            resizeControl(m_hWnd, r.width, r.height);
		}
    }
}
