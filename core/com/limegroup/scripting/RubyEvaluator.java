package com.limegroup.scripting;
import javax.script.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.google.inject.Inject;
import com.google.inject.Injector;

public class RubyEvaluator {
    private final Injector injector;
    private final ScriptEngine engine;
    
    @Inject
    public RubyEvaluator(Injector injector) {
        this.injector = injector;
        
        // Create a JRuby engine.
        ScriptEngineManager factory = new ScriptEngineManager();
        this.engine = factory.getEngineByName("jruby");
        this.engine.setContext(new SimpleScriptContext());
        this.engine.getContext().setAttribute("injector", this.injector, ScriptContext.ENGINE_SCOPE);
    }
    
    public void eval(String file)
    throws FileNotFoundException, ScriptException {
        // Evaluate JRuby code from string.
        engine.eval(new BufferedReader(new FileReader(file)));
    }
}
