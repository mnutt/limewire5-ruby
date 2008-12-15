package org.limewire.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import junit.framework.Test;

public class I18NConvertTest extends BaseTestCase {

    private BufferedReader _buf;
    private boolean _last = false;
    private final String DELIM = ";";
    
    private final String CASE = "CASE";
    private final String ACCENTS = "ACCENTS";
    private final String SPLIT = "KEYWORD_SPLIT";
    private AbstractI18NConverter _instanceICU;
    
    private static final String fileName =
        "org/limewire/util/i18ntest.txt";

    public I18NConvertTest(String name) {
        super(name);
    }
    
    public static Test suite() {
        return buildTestSuite(I18NConvertTest.class);
    }

    @Override
    public void setUp() throws Exception {
        //read in a file and do the tests from there.
        FileInputStream fin = 
            new FileInputStream(TestUtils.getResourceFile(fileName));
        _buf = new BufferedReader(new InputStreamReader(fin, "UTF-8"));

        //_instance = I18NConvert.instance();

        _instanceICU = 
            (AbstractI18NConverter)PrivilegedAccessor.invokeConstructor(
               I18NConvertICU.class, new Object[]{});
    }
    
    public void testConversions() throws Exception {
        readLines(CASE);
        readLines(ACCENTS);
        _last = true;
        readLines(SPLIT);
        //System.out.println(System.currentTimeMillis() - l);
    }

    private void readLines(String what) {
        String line = "";
        try {
            while((line = _buf.readLine()) != null &&
                  !line.equals("# END " + what)) {
                doNorm(line, what);
            }
        }
        catch(IOException ioe) {
            fail("problem with i18ntest.txt file", ioe);
        }
        catch(Throwable t) {
            fail("problem at line : " + line, t);
        }

    }

    private void doNorm(String line, final String what) 
        throws Throwable {

        if(line.indexOf("# ") == -1) {
            String[] split = StringUtils.split(line, DELIM);
            String x2 = _instanceICU.getNorm(split[1]);

            assertEquals(what + " " + line + ":", 
                         split[0], 
                         x2);
        }
    }
    
    @Override
    public void tearDown() throws Exception {
       // System.out.println("tear down");
        if(_last && _buf != null)
            _buf.close();
    }

}






