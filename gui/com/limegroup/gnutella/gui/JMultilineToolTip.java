package com.limegroup.gnutella.gui;

import javax.swing.JToolTip;


public class JMultilineToolTip extends JToolTip {
    
    private String[] tipArray;
    private static JMultilineToolTip instance = new JMultilineToolTip();
    
    public static JMultilineToolTip instance(){
        instance.initialize();//re-initialize
        return instance;
    }
    
    private void initialize(){
        //System.out.println("Sumeet: JMultilineToolTip:initialize");
        tipArray = new String[0];
        setUI(MultilineToolTipUI.instance());
    }

    //private constructor
    private JMultilineToolTip() {
        initialize();
    }
    
    public void setToolTipArray(String[] s){
        this.tipArray = s;
    }

    public String[] getTipArray(){
        return tipArray;
    }
}
