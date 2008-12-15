package org.limewire.core.impl.updates;

import org.limewire.core.api.updates.UpdateInformation;

public class UpdateInformationImpl implements UpdateInformation {

    private final String button1Text;
    private final String button2Text;
    private final String command;
    private final String text;
    private final String title;
    private final String url;
    
    public UpdateInformationImpl(String button1Text, String button2Text, String command, 
            String text, String title, String url) {
        this.button1Text = button1Text;
        this.button2Text = button2Text;
        this.command = command;
        this.text = text;
        this.title = title;
        this.url = url;
    }
    
    @Override
    public String getButton1Text() {
        return button1Text;
    }

    @Override
    public String getButton2Text() {
        return button2Text;
    }

    @Override
    public String getUpdateCommand() {
        return command;
    }

    @Override
    public String getUpdateText() {
        return text;
    }

    @Override
    public String getUpdateTitle() {
        return title;
    }

    @Override
    public String getUpdateURL() {
        return url;
    }
    
    @Override
    public String toString() {
        return "\n{" + 
            "button 1: " + button1Text + ", button 2: " + button2Text + ", url: " + url + 
            ", text: " + text + ", title: " + title + ", updateCommand: " + command + "}";
    }
}
