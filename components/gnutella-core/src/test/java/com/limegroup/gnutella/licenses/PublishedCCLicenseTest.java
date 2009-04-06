package com.limegroup.gnutella.licenses;

import java.io.File;
import java.io.StringReader;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTMLEditorKit.Parser;

import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cyberneko.relaxng.parsers.SAXParser;
import org.limewire.gnutella.tests.LimeTestCase;
import org.limewire.util.TestUtils;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


public class PublishedCCLicenseTest extends LimeTestCase {
    
    private static final Log LOG = LogFactory.getLog(PublishedCCLicenseTest.class);
    
    /** Standard constructors */
    public PublishedCCLicenseTest(String name) {
        super(name);
    }

    public static Test suite() {
        return buildTestSuite(PublishedCCLicenseTest.class);
    }

    /**
     * Runs this test individually.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    /**
     * HTML parser may run in a separate thread
     */
    
    private String rdf;
    private Object rdfLock = new Object();
    
    /**
     * Tests that the verification RDF validates
     * against a RELAX-NG Schema
     */
    
    public void testVerifyRDFValidates() throws Exception {

		String rdfEmbeddedComment = PublishedCCLicense.getRDFRepresentation(
				"TIMMAY", "TIMMAY SLEEPAY", "2005", "Timmay Drink Espresso",
				"urn:sha1:TIMMAYESPRESSOH32VV4A2LJUDNLPAJ6",
				CCConstants.ATTRIBUTION_SHARE_NON_COMMERCIAL);

		// first extract the RDF out of the html comment

		StringReader reader = new StringReader(rdfEmbeddedComment);
		
        HTMLEditorKit.ParserCallback callback = new CommentExtractor();
        getHTMLEditorKitParser().parse(reader, callback, true);
        
		synchronized (rdfLock) {
			// wait for parser to finish
			while ( rdf == null ) {
				try {
					rdfLock.wait();
				} catch (InterruptedException e) {
				}
			}		
		}
		

		// now let's parse the RDF and validate it against the schema

		// set up ManekiNeko parser
		SAXParser parser = new SAXParser();
        parser.setErrorHandler(new ErrorHandler() {
            public void error(SAXParseException exception) throws SAXException {
                LOG.error("error in sax", exception);
                
            }

            public void fatalError(SAXParseException exception) throws SAXException {
                LOG.fatal("error in sax", exception);
                throw exception;
            }

            public void warning(SAXParseException exception) throws SAXException {
                LOG.warn("error in sax", exception);
            }
            
        });

		parser.setFeature("http://xml.org/sax/features/namespaces", true);
		parser.setFeature("http://xml.org/sax/features/validation", true);

        // bug in MinekiNeko prevents use of compact schema.  use regular schema instead
        //		parser.setFeature("http://cyberneko.org/xml/features/relaxng/compact-syntax", true);

        File f = TestUtils.getResourceFile("com/limegroup/gnutella/licenses/ccverificationrdf-schema.rng");
        assertTrue(f.exists()); // must have rng to validate.
		parser.setProperty(
				"http://cyberneko.org/xml/properties/relaxng/schema-location",
				f.getAbsoluteFile().toURI());
		parser.parse(new InputSource(new StringReader(rdf)));

	}
    
    /** 
     * Returns the HTML parser as used by HTMLEditorKit. We could use
     * HTMLEditorKit to get an instance of it but HTMLEditorKit instantiates
     * some java.awt.Cursor objects internally that causes some problems 
     * on OSX even in java.awt.headless mode!
     */
    private static Parser getHTMLEditorKitParser() {
        try {
            Class c = Class.forName("javax.swing.text.html.parser.ParserDelegator");
            return (Parser) c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private class CommentExtractor extends HTMLEditorKit.ParserCallback {
		@Override
        public void handleComment(char[] data, int pos) {
//			System.err.println("about to parse Comment");
			synchronized (rdfLock) {
				rdf = new String(data);
				rdfLock.notify();
			}
//			System.err.println("finished parsing Comment");
		}

//		public void flush() throws BadLocationException {
//			System.err.println("flush called");
//			synchronized (rdfLock) {
//				rdfLock.notify();
//			}
//		}
	}
 
}
