package org.limewire.ui.swing;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import junit.framework.TestCase;

import org.limewire.util.TestUtils;

public class ResourceTest extends TestCase {
    
    public static void testMissingAndAdditionalResources() throws Exception {
        File appFrame = TestUtils.getResourceFile("org/limewire/ui/swing/mainframe/resources/AppFrame.properties");
        File iconFolder = new File(appFrame.getParentFile(), "icons");
        List<File> icons = getContents(iconFolder);
        
        String parent = appFrame.getParentFile().getPath().replace('\\', '/');
        List<String> sanitized = new ArrayList<String>();
        for(File icon : icons) {
            String path = icon.getPath().replace('\\', '/');
            path = path.substring(parent.length()+1);
            sanitized.add(path.trim());
        }
        
        Properties props = new Properties();
        props.load(new FileInputStream(appFrame));
        
        List<String> leftover = new ArrayList<String>(sanitized);
        
        List<String> validUnused = new ArrayList<String>();
        validUnused.add("icons/lime.ico");
        validUnused.add("icons/friends/friends_icon.png");
        
        for(String valid : validUnused) {
            assertTrue("Missing: " + valid + " in list of icons.", leftover.remove(valid));
        }
        
        for(Object value : props.values()) {
            String name = value.toString().trim();
            leftover.remove(name);
        }        
        
        assertEquals("unused icons!: " + leftover, 0, leftover.size());
        
        Map<String, String> iconResources = new TreeMap<String, String>();
        for(Map.Entry<Object, Object> value : props.entrySet()) {
            if(value.getValue().toString().trim().startsWith("icons/")) {
                iconResources.put(value.getKey().toString().trim(), value.getValue().toString().trim());
            }
        }
        for(String icon : sanitized) {
            while(iconResources.values().remove(icon));
        }
        
        assertEquals("resources referring to icons that don't exist!: " + iconResources, 0, iconResources.size());
        
    }
    
    private static List<File> getContents(File folder) {
        final List<File> contents = new ArrayList<File>();
        folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String name = pathname.getName();
                if(name.equals(".cvsignore") ||
                   name.equals(".directory") ||
                   name.equals("CVS") ||
                   name.equals("Thumbs.db")) {
                    return false;
                }
                
                if(pathname.isDirectory()) {
                    contents.addAll(getContents(pathname));
                } else {
                    contents.add(pathname);
                }
                return false;
            }
        });
        return contents;
    }

}
