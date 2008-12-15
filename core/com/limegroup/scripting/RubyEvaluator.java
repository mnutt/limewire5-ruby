package com.limegroup.scripting;
import org.apache.bsf.*;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import com.limegroup.gnutella.LimeWireCore;
import org.apache.http.HttpRequest;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.nio.entity.NStringEntity;

public class RubyEvaluator {
    public AbstractHttpEntity eval(LimeWireCore core, HttpRequest request) throws FileNotFoundException, UnsupportedEncodingException, BSFException {
        BSFManager.registerScriptingEngine("ruby", 
                                           "org.jruby.javasupport.bsf.JRubyEngine", 
                                           new String[] { "rb" });
        BSFManager bsf = new BSFManager();

        // Create a JRuby engine.
        bsf.declareBean("core", core, core.getClass());
        bsf.declareBean("request", request, request.getClass());
        AbstractHttpEntity entity = null;

        // Evaluate JRuby code from string.
        try {
          this.getClass().getClassLoader();
          String path = ClassLoader.getSystemResource("myruby.rb").toString();
          int i = path.indexOf("/");
          BufferedReader source_buffer = new BufferedReader(new FileReader(path.substring(i)));
          String line;
          String source = "";
          while((line = source_buffer.readLine()) != null) {
            source += line;
            source += "\n";
          }
          entity = (AbstractHttpEntity) bsf.eval("ruby", "myruby.rb", 0, 0, source);
        } catch (Exception exception) {
            System.out.println("caught exception");
            exception.printStackTrace();
            entity = new NStringEntity(exception.getStackTrace().toString());
            entity.setContentType("text/html");
        }
        return entity; // result;
      }
}
