package com.limegroup.gnutella.lws.server;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Test;
import junit.textui.TestRunner;

import org.limewire.lws.server.LWSServerUtil;
import org.limewire.lws.server.LWSDispatcherSupport.Responses;

/**
 * Makes sure
 * {@link LWSManager#registerHandler(String, LWSManagerCommandResponseHandler)}
 * is working.
 */
public class HandlersTest extends AbstractCommunicationSupportWithNoLocalServer {

    public HandlersTest(String s) {
        super(s);
    }

    public static Test suite() {
        return buildTestSuite(HandlersTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run(suite());
    }

    private class Handler extends LWSManager.AbstractHandler {
        
        public Handler(String command) {
            super(command);
        }

        private boolean handled = false;
        
        public boolean getHandled() {
            return handled;
        }
        
        public void resetHandled() {
            handled = false;
        }
        
        public String handle(Map<String, String> args) {
            assertEquals("a1 should map to A1", args.get("a1"), "A1");
            assertEquals("b1 should map to B1", args.get("b1"), "B1");
            assertEquals("c1 should map to C1", args.get("c1"), "C1");

            handled = true;
            return Responses.OK;
        }
    }

    private void doCommand(String command, Handler handler, boolean handlingExpected) {
        Map<String, String> args = new HashMap<String, String>() {{
            put("a1", "A1");
            put("b1", "B1");
            put("c1", "C1");
        }};

        String response = sendCommandToClient(command, args, true);

        String responseSubstring = LWSServerUtil.removeCallback(response);
        String expectedResponse = handlingExpected ? Responses.OK : "";
        assertEquals("unexpected response",
                expectedResponse, responseSubstring);        

        assertEquals("unexpected handled state",
                handlingExpected, handler.getHandled());
    }

    private void testHandlers(int count) {

        String [] commands = new String[count];
        Handler [] handlers = new Handler[count];

        // Create and register handlers
        for (int i=0; i < count; i++) {
            commands[i] = "Foo" + i;
            handlers[i] = new Handler(commands[i]);
            assertTrue("registration failed",
                    getLWSManager().registerHandler(commands[i], handlers[i]));            
        }            

        // Send messages and expect handling
        for (int i=0; i < count; i++) {
            doCommand(commands[i], handlers[i], true);
        }            

        // Unregister handlers
        for (int i=0; i < count; i++) {
            assertTrue("unregistration failed",
                    getLWSManager().unregisterHandler(commands[i]));
        }            

        // Send messages again and expect no handling
        for (int i=0; i < count; i++) {
            handlers[i].resetHandled();
            doCommand(commands[i], handlers[i], false);
        }            
    }
    
    public void testAddTwoHandlers() {
        testHandlers(2);
    }

    public void testAddThreeHandlers() {
        testHandlers(3);
    }

    /**
     * Registering more than one handler for the same command overwrites
     * the old registration with the new one.
     */
    public void testWithTowHandlersAndSameName() {

        String command = "Foo";

        Handler handler1 = new Handler(command);
        assertTrue("first registration failed", 
                getLWSManager().registerHandler(command, handler1));

        Handler handler2 = new Handler(command);
        assertTrue("duplicate registration failed",
                getLWSManager().registerHandler(command, handler2));

    }

}
