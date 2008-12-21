package com.limegroup.scripting;
import javax.script.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.limegroup.gnutella.LimeWireCore;
import com.google.inject.Inject;

public class RubyEvaluator {
    private final LimeWireCore core;
    private final ScriptEngine engine;
    
    @Inject
    public RubyEvaluator(LimeWireCore core) {
        this.core = core;
        
        // Create a JRuby engine.
        ScriptEngineManager factory = new ScriptEngineManager();
        this.engine = factory.getEngineByName("jruby");
        this.engine.setContext(new SimpleScriptContext());
        this.engine.getContext().setAttribute("core", this.core, ScriptContext.ENGINE_SCOPE);
    }
    
    public void eval(String file)
    throws FileNotFoundException, ScriptException {
        // Evaluate JRuby code from string.
        try {
            String path = ClassLoader.getSystemResource(file).toString();
            int i = path.indexOf("/");
            engine.eval(new BufferedReader(new FileReader(path.substring(i))));
        } catch (Exception exception) {
            exception.getCause().printStackTrace();
        }
    }
}
