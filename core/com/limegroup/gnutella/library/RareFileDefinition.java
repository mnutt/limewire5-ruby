package com.limegroup.gnutella.library;

import org.limewire.core.settings.DHTSettings;
import org.limewire.setting.evt.SettingEvent;
import org.limewire.setting.evt.SettingListener;
import org.limewire.util.RPNParser;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class RareFileDefinition { 
    
    private volatile RPNParser parser;
    
    @Inject
    RareFileDefinition() {
        parser = new RPNParser(DHTSettings.RARE_FILE_DEFINITION.getValue());      
        DHTSettings.RARE_FILE_DEFINITION.addSettingListener(new SettingListener() {
            @Override
            public void settingChanged(SettingEvent evt) {
                parser = new RPNParser(DHTSettings.RARE_FILE_DEFINITION.getValue());
            }
        });
    }
    
    public boolean isRareFile(FileDesc fd) {
        try {
            return parser.evaluate(fd);
        } catch (IllegalArgumentException badSimpp) {
            return false;
        }
    }
}
