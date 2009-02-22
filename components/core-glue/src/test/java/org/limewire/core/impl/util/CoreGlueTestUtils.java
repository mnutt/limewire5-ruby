package org.limewire.core.impl.util;


public class CoreGlueTestUtils {

    /**
     * Creates the Guice injector with the limewire default modules and the 
     * test module that can override bindings in the former modules.
     * 
     * @param module the test modules that can override bindings
     * @param callbackClass the class that is used as a callback
     * @return the injector
     */
//    public static Injector createInjector(Module...modules) {
//        return createInjector(Stage.DEVELOPMENT, modules);
//    }
//    
//    public static Injector createInjector(Stage stage, Module...modules) {
//        Module combinedReplacements = Modules.combine(modules);
//        Module combinedOriginals = Modules.combine(new LimeWireCoreModule(null), new CoreGlueModule(), new BlockingConnectionFactoryModule());
//        Module replaced = Modules.override(combinedOriginals).with(combinedReplacements);
//        return Guice.createInjector(stage, replaced);
//    }
//
//    /**
//     * Creates the Guice injector with the limewire default modules and the 
//     * test module that can override bindings in the former modules.
//     * 
//     * Also starts the {@link LifecycleManager}.
//     * 
//     * @param module the test modules that can override bindings
//     * @param callbackClass the class that is used as a callback
//     * @return the injector
//     */
//    public static Injector createInjectorAndStart(Module...modules) {
//        // Use PRODUCTION to ensure all Services are created.
//        Injector injector = createInjector(Stage.PRODUCTION, modules);
//        LifecycleManager lifecycleManager = injector.getInstance(LifecycleManager.class);
//        lifecycleManager.start();
//        return injector;
//    }
    
}
