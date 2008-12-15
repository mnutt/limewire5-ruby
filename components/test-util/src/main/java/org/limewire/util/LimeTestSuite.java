package org.limewire.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;


/**
 * A modified TestSuite that allows the backends
 * to always be shutdown after tests finished,
 * regardless of halt-on-failure or halt-on-error.
 */
public class LimeTestSuite extends TestSuite {   
    
    private TestResult _testResult = null;
    private boolean _beforeTests = false;
    private final Class _testClass;
    private static final String preTestName = "globalSetUp";
    private static final String postTestName = "globalTearDown";
    
    /**
     * Constructors...
     */
    LimeTestSuite(boolean singleTest, Class testClass) {
        super();
        _testClass = testClass;
    }
    
    LimeTestSuite(Class a, String b) {
        super(a, b);
        _testClass = a;
    }
    LimeTestSuite(Class a) {
        super(a);
        _testClass = a;
    }
         
    /**
     * Modified run.
     * Before & after tests are run, it sets the error callback
     * to itself to catch any stray errors.
     */
    @Override
    public void run(TestResult result) {
        _beforeTests = true;
        _testResult = result;
        ErrorUtils.setCallback(this);
        
        // First try doing the before-tests-setup
        try {
            runStaticMethod("beforeAllTestsSetUp");
        } catch(Throwable t) {
            // If there is an error here, report it,
            // run the after all tests tear down, and exit.
            error(t);
            try {
                runStaticMethod("afterAllTestsTearDown");
            } catch(Throwable t2) {
                error(t2);
            }
            ///CLOVER:FLUSH 
            return;
        }
        
        // Then try running the preTest method
        try {
            runStaticMethod(preTestName);
        } catch(TestFailedException tfe) {
            // If it fails, run the post test and exit.
            try {
                runStaticMethod(postTestName);
            } catch(TestFailedException tfe2) {
                // oh well.
            }
            ///CLOVER:FLUSH
            return;
        }        
        
        // Try running all the tests.
        try {
            super.run(result);
        } finally {
            // Regardless of if any fail or not, 
            // always run the last post test & after all tests methods.
            _beforeTests = false;
            ErrorUtils.setCallback(this);
            try {
                runStaticMethod(postTestName);
            } catch(TestFailedException tfe) {
                // oh well.
            }
            try {
                runStaticMethod("afterAllTestsTearDown");
            } catch(Throwable t) {
                error(t);
            }                
        }
        ///CLOVER:FLUSH
    }
    
    public void runStaticMethod(String name) throws TestFailedException {
        List methods = null;
        try {
            methods = getAllStaticMethods(_testClass, name);
            if(methods.isEmpty()) return;
        } catch(NoSuchMethodException e) {
            return;
        }

        for(Iterator i = methods.iterator(); i.hasNext(); ) {
            Method m = (Method)i.next();
            if ( !Modifier.isStatic(m.getModifiers()) ) {
                runTest(warning("Method "+name+" must be declared static."),
                        _testResult);
                throw new TestFailedException();
            } else if ( !Modifier.isPublic(m.getModifiers()) ) {
                runTest(warning("Method "+name+" must be declared public."),
                        _testResult);
                throw new TestFailedException();
            } else {
                try {
                    // If this method takes the class parameter, send it
                    if(m.getParameterTypes().length == 1)
                        m.invoke(null, new Object[] { _testClass });
                    // Otherwise use the no-arg invocation
                    else
                        m.invoke(null, new Object[] {});
                } catch (InvocationTargetException e) {
                    runTest(reportError(e.getCause(), _testResult), _testResult);
                    throw new TestFailedException();
                } catch (IllegalAccessException e) {
                    runTest(warning("Cannot access method: "+name, e),
                            _testResult);
                    throw new TestFailedException();
                }                
            }
        }
    }
    
    /**
     * Retrieves all static methods of the specified class (and all subclasses)
     * that are the specified name and either take no parameters or a Class
     * parameter.  The method that takes a Class parameter takes priority
     * if both are found.
     * 
     * Throws NoSuchMethodException if none are found.
     */
    @SuppressWarnings("unchecked")
	public List getAllStaticMethods(Class entryClass, String methodName)
      throws NoSuchMethodException {
        List<Method> methods = new LinkedList<Method>();
        Class clazz = entryClass;
        Class[] classes = new Class[] { Class.class };
        while(clazz != null) {
            Method add = null;
            try {
                add = clazz.getDeclaredMethod(methodName, classes);
            } catch(NoSuchMethodException tryAgain) {
                // If nothing with the class parameter, try with none
                try {
                    add = clazz.getDeclaredMethod(methodName, (Class[])null);
                } catch(NoSuchMethodException ignored) {}
            }
            
            // If we found a method, add it to the beginning of the list
            if(add != null)
                methods.add(0, add);

            // Try again with a superclass.
            clazz = clazz.getSuperclass();
        }
        
        if(methods.isEmpty())
            throw new NoSuchMethodException("Invalid method: " + methodName);

        return methods;
    }    
    
    /**
     * Stub for error(Throwable, String)
     */
    public void error(Throwable ex) {
        error(ex, null);
    }    
    
    /**
     * The error service callback.
     *
     * This does a somewhat unusual thing to report the error.
     * Instead of just appending it to the _testResult, it creates
     * a new TestCase and has that fail.
     * This has the advantage of reporting the error in a different
     * 'test', ensuring that people aren't confused as to how the error
     * was caused.
     */    
    public void error(Throwable ex, String detail) {
        if ( _testResult != null )
            runTest(reportError(ex, _testResult), _testResult);
        else
            ex.printStackTrace();
    }
    
    /**
     * Returns a test which will fail and log a warning message.
     * Modified from JUnit's TestSuite.java's warning method.
     * Note that it does not have to extend BaseTestCase, just TestCase.
     * BaseTestCase would add needless complexity to an otherwise
     * simple error report.
     */
    private Test reportError(final Throwable thrown, final TestResult result) {
        String testName = _beforeTests ?
            "LimeTestSuite - Before Test Errors" :
            "LimeTestSuite - After Test Errors";
    	return new TestCase(testName) {
    		@Override
            protected void runTest() {
    		    if ( thrown instanceof AssertionFailedError )
    		        result.addFailure(this, (AssertionFailedError)thrown);
    		    else
    			    result.addError(this, thrown);
    		}
    	};
    }
    
	/**
	 * Returns a test which will fail and log a warning message.
	 */
	public static Test warning(final String message) {
		return new TestCase("warning") {
			@Override
            protected void runTest() {
				fail(message);
			}
		};
	}
	
	/**
	 * Returns a test which will fail and log a warning message.
	 */
    public static Test warning(final String message, final Throwable thrown) {
		return new BaseTestCase("warning") {
		    @Override
            public void preSetUp() {}
		    @Override
            public void postTearDown() {}
			@Override
            protected void runTest() {			    
				fail(message, thrown);
			}
		};
	}
	
	private class TestFailedException extends Exception {
	    TestFailedException() { super(); }
	}
}