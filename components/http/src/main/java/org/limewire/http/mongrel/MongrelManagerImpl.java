package org.limewire.http.mongrel;

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
            this.rubyEvaluator.eval("scripting/rails_start.rb");
        } catch(FileNotFoundException exception) {
            System.out.println("couldn't find mongrel start script.");
            exception.printStackTrace();
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
