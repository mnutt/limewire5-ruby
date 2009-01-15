package com.limegroup.gnutella;

import com.limegroup.gnutella.ConsoleInitializer;

/**
 * The command-line UI for the Gnutella servent.
 */

public class Main {
    public static void main(String[] args) {
        ConsoleInitializer console = new ConsoleInitializer();
        console.initialize(args);
    }
}
