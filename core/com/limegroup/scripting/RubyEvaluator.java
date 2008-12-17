package com.limegroup.scripting;
import javax.script.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;

import com.limegroup.gnutella.LimeWireCore;
import org.apache.http.HttpRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.entity.NStringEntity;

public class RubyEvaluator {
    public AbstractHttpEntity eval(LimeWireCore core, HttpRequest request)
	throws FileNotFoundException, UnsupportedEncodingException {
	ScriptEngineManager factory = new ScriptEngineManager();
	// Create a JRuby engine.
	ScriptEngine engine = factory.getEngineByName("jruby");
	engine.setContext(new SimpleScriptContext());
	engine.getContext().setAttribute("core", core, ScriptContext.ENGINE_SCOPE);
	engine.getContext().setAttribute("request", request, ScriptContext.ENGINE_SCOPE);
	AbstractHttpEntity entity;

	// Evaluate JRuby code from string.
	try {	    
	  String path = ClassLoader.getSystemResource("myruby.rb").toString();
	  int i = path.indexOf("/");
	  entity = (AbstractHttpEntity) engine.eval(new BufferedReader(new FileReader(path.substring(i))));
	} catch (Exception exception) {
	  System.out.println("caught exception");
	  StringWriter sw = new StringWriter();
	  PrintWriter pw = new PrintWriter(sw);

	  pw.print("<pre>");
	  exception.printStackTrace(pw);
	  pw.print("</pre>");

	  entity = new NStringEntity(sw.toString());
	  entity.setContentType("text/html");
	}
	return entity; // result;
      }
}
