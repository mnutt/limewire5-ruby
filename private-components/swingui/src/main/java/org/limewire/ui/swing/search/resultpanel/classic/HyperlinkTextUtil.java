package org.limewire.ui.swing.search.resultpanel.classic;

public class HyperlinkTextUtil {

    public static String hyperlinkText(Object...pieces ) {
        StringBuilder bldr = new StringBuilder();
        bldr.append("<html><u>");
        for(Object s : pieces) {
            bldr.append(s);
        }
        bldr.append("</u></html>");
        return bldr.toString();
    }

}
