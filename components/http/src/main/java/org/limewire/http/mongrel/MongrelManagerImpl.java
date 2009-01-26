package org.limewire.http.mongrel;

import java.io.File;
import java.io.FileNotFoundException;

import javax.script.ScriptException;

import com.google.inject.Inject;

import com.limegroup.scripting.RubyEvaluator;

public class MongrelManagerImpl implements MongrelManager {

    private RubyEvaluator rubyEvaluator;
    
    @Inject
    public MongrelManagerImpl(RubyEvaluator rubyEvaluator) {
        this.rubyEvaluator = rubyEvaluator;
    }
    
    @Override
    public String getServiceName() {
        return org.limewire.i18n.I18nMarker.marktr("Mongrel Manager");
    }
    
    @Override
    public void start() {
        System.out.println("Starting mongrel...");
        try {
            String usablePath = null;
            String[] loadPaths = {
                "../../../../../script/start_rails",
                "rails/script/start_rails"
            };
            
            // Look through the paths to find one 
            for(String path : loadPaths) {
                File file = new File(path);
                if(file.exists()) {
                    usablePath = path;
                }
            };
            if(usablePath != null) {
                this.rubyEvaluator.eval(usablePath);
            } else {
                throw new FileNotFoundException();
            }
        } catch(FileNotFoundException exception) {
            System.out.println("couldn't find mongrel start script.");
        } catch(ScriptException exception) {
            exception.getCause().printStackTrace();
        }
    }
    @Override
    public void stop() {
    }
    
    @Override
    public boolean isAsyncStop() {
        return true;
    }

}
