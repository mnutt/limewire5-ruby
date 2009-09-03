package org.limewire.inspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

import org.limewire.inject.EagerSingleton;
import org.limewire.inject.GuiceUtils;
import org.limewire.inject.LimeWireInjectModule;
import org.limewire.util.BaseTestCase;
import org.limewire.util.OSUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;

public class InspectionUtilsTest extends BaseTestCase {
    public InspectionUtilsTest(String name) {
        super(name);
    }
    
    static Injector injector;
    public static Test suite() {
        return buildTestSuite(InspectionUtilsTest.class);
    }
    
    @Override
    public void setUp() throws Exception {
        Requirements.created = false;
        
        injector = Guice.createInjector(getModules());
        GuiceUtils.loadEagerSingletons(injector);
    }
    
    private Module[] getModules() {
        List<Module> modules = new ArrayList<Module>();
        modules.add(new LimeWireInjectModule());
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(TestInterface.class).to(TestClass.class);
                bind(TestInterface2.class).to(TestClass2.class);
                bind(SyncListInterface.class).to(SyncList.class);
                bind(SyncListInterface2.class).to(SyncList2.class);
                bind(OutterI.class).to(Outter.class);
                bind(Parent.class).to(ConcreteChild.class);
                bind(IConcrete.class).to(Concrete.class);
                bind(UsageData.class).to(UsageDataImpl.class);
                bind(NetworkData.class).to(NetworkDataImpl.class);
            }
        };
        modules.add(m);
        return modules.toArray(new Module[modules.size()]);
    }
    
    
    public void testOldStyleTraversal() throws Exception {
        NotGuiced.inspectableInt = 1;
        NotGuiced.inspectable = new Inspectable() {
            @Override
            public Object inspect() {
                return "asdf";
            }
        };
        
        // injector-based traversal will fail
        try {
            InspectionUtils.inspectValue(nameOf(NotGuiced.class) + ":,inspectableInt", injector, true);
        } catch (InspectionException expected){}
        
        // static traversal will work
        assertEquals("asdf",
                InspectionUtils.inspectValue(nameOf(NotGuiced.class) + ":inspectable", injector, true));
        assertEquals("1",
                InspectionUtils.inspectValue(nameOf(NotGuiced.class) + ":inspectableInt", injector, true));
    }
    
    public void testInspectablePrimitive() throws Exception {
        TestInterface t = injector.getInstance(TestInterface.class);
        t.setMemberString("a");
        t.setInspectableString("b");
        try {
            InspectionUtils.inspectValue(nameOf(TestClass.class) + ",reference1,memberString", injector, true);
            fail("should not be inspectable");
        } catch (InspectionException expcted){}
        
        String inspectable = (String)InspectionUtils.inspectValue(nameOf(TestClass.class) + ",inspectableString", injector, true);
        assertEquals("b", inspectable);
    }
    
    public void testInspectableForSize() throws Exception {
        TestInterface t = injector.getInstance(TestInterface.class);
        List<Object> member = new ArrayList<Object>();
        List<Object> inspectable= new ArrayList<Object>();
        member.add(new Object());
        inspectable.add(new Object());
        inspectable.add(new Object());
        
        t.setMemeberList(member);
        t.setInspectableList(inspectable);
        
        try {
            InspectionUtils.inspectValue(nameOf(TestClass.class) + ",memberList", injector, true);
            fail("should not be inspectable for size");
        } catch (InspectionException expcted){}
        
        String res = (String)InspectionUtils.inspectValue(nameOf(TestClass.class) + ",inspectableList", injector, true);
        assertEquals("2",res);
    }

    @SuppressWarnings("unchecked")
    public void testSyncCollection() throws Exception {
        SyncList syncList = injector.getInstance(SyncList.class);
        syncList.l = Collections.synchronizedList(new ArrayList());
        syncList.l.add(new Object());
        assertEquals(String.valueOf(syncList.l.size()),InspectionUtils.inspectValue(nameOf(SyncList.class) + ",l", injector, true));
    
        SyncList2 syncList2 = injector.getInstance(SyncList2.class);
        syncList2.l = Collections.synchronizedList(new ArrayList());
        syncList2.l.add(new Object());
        assertEquals(String.valueOf(syncList2.l.size()),InspectionUtils.inspectValue(nameOf(SyncList2.class) + ",l", injector, true));
    }
    
    public void testContainer() throws Exception {
        Object ret = InspectionUtils.inspectValue(nameOf(Outter.Inner.class) + ",inspectable", injector, true);
        assertEquals("asdf",ret);
    }
    
    public void testContainerInParent() throws Exception {
        Object ret = InspectionUtils.inspectValue(nameOf(Parent.class) + "|" + nameOf(AbstractParent.Inner.class) + ",inspectable", injector, true);
        assertEquals("abcd", ret);
    }
    
    public void testAbstractPoints() throws Exception {
        Object ret = InspectionUtils.inspectValue(nameOf(IConcrete.class) + "|" + nameOf(Abstract.class) + ",inspectableA", injector, true);
        assertEquals("qqqq", ret);
    }
    
    public void testRequirements() throws Exception {
        assertFalse(Requirements.created);
        if(OSUtils.isWindows()) {
            assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector, true));
            assertTrue(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_OSX] on field: int " + Requirements.class.getName() + ".y", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_LINUX] on field: int " + Requirements.class.getName() + ".z", x.getMessage());
            }
            assertFalse(Requirements.created);
        } else if(OSUtils.isMacOSX()) {
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector, true));                
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_WINDOWS] on field: int " + Requirements.class.getName() + ".x", x.getMessage());
            }
            assertFalse(Requirements.created);
            assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector, true));
            assertTrue(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_LINUX] on field: int " + Requirements.class.getName() + ".z", x.getMessage());
            }
            assertFalse(Requirements.created);
        } else if(OSUtils.isLinux()) {
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_WINDOWS] on field: int " + Requirements.class.getName() + ".x", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_OSX] on field: int " + Requirements.class.getName() + ".y", x.getMessage());
            }
            assertFalse(Requirements.created);
            assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector, true));
            assertTrue(Requirements.created);
        } else {
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_WINDOWS] on field: int " + Requirements.class.getName() + ".x", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_OSX] on field: int " + Requirements.class.getName() + ".y", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector, true));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_LINUX] on field: int " + Requirements.class.getName() + ".z", x.getMessage());
            }
            assertFalse(Requirements.created);
        }
    }
    
    public void testUsageData() throws InspectionException {
        UsageData usageData = injector.getInstance(UsageData.class);
        usageData.setUsageData("foo");
        try {
            InspectionUtils.inspectValue(nameOf(UsageDataImpl.class) + ",data", injector, false);
            fail();
        } catch (InspectionException e) {            
        }
        assertEquals("foo", InspectionUtils.inspectValue(nameOf(UsageDataImpl.class) + ",data", injector, true));
    }
    
    public void testNetworkData() throws InspectionException {
        NetworkData networkData = injector.getInstance(NetworkData.class);
        networkData.setNetworkData("foo");
        networkData.setOtherNetworkData("bar");
        assertEquals("foo", InspectionUtils.inspectValue(nameOf(NetworkDataImpl.class) + ",data", injector, false));
        assertEquals("foo", InspectionUtils.inspectValue(nameOf(NetworkDataImpl.class) + ",data", injector, true));
        assertEquals("bar", InspectionUtils.inspectValue(nameOf(NetworkDataImpl.class) + ",otherData", injector, false));
        assertEquals("bar", InspectionUtils.inspectValue(nameOf(NetworkDataImpl.class) + ",otherData", injector, true));
    }
    
    private static String nameOf(Class clazz) {
        return clazz.getName();
    }
    
    @Singleton
    private static class Requirements {
        static boolean created = false;
        public Requirements() {
            created = true;
        }
        @InspectablePrimitive(value="win", requires = InspectionRequirement.OS_WINDOWS)
        int x = 5;
        
        @InspectablePrimitive(value="mac", requires = InspectionRequirement.OS_OSX)
        int y = 5;
        
        @InspectablePrimitive(value="lin", requires = InspectionRequirement.OS_LINUX)
        int z = 5;
    }

    private static interface TestInterface {
        void setReference1(TestInterface ti);

        void setReference2(TestInterface2 ti);

        void setObjectReference(Object reference);

        void setMemberString(String s);

        void setInspectableString(String s);

        void setMemeberList(List l);

        void setInspectableList(List l);
    }

    @SuppressWarnings("unused")
    @Singleton
    private static class TestClass implements TestInterface {

        private static TestInterface reference1;

        private static TestInterface2 reference2;

        public void setReference1(TestInterface ti) {
            reference1 = ti;
        }

        public void setReference2(TestInterface2 ti) {
            reference2 = ti;
        }

        private String memberString;

        @InspectablePrimitive("")
        private String inspectableString;

        public void setMemberString(String memberString) {
            this.memberString = memberString;
        }

        public void setInspectableString(String inspectableString) {
            this.inspectableString = inspectableString;
        }

        private List memberList;

        @InspectableForSize("")
        private List inspectableList;

        public void setMemeberList(List memberList) {
            this.memberList = memberList;
        }

        public void setInspectableList(List inspectableList) {
            this.inspectableList = inspectableList;
        }

        private Object reference;

        public void setObjectReference(Object reference) {
            this.reference = reference;
        }

        @Override
        public String toString() {
            return "testclass";
        }
    }

    private static interface TestInterface2 {
        void setReference1(TestInterface ti);

        void setObjectReference(Object reference);
    }

    @Singleton
    @SuppressWarnings( { "unused", "FieldCanBeLocal", "UnusedDeclaration" })
    private static class TestClass2 implements TestInterface2 {
        static TestInterface reference1;

        public void setReference1(TestInterface ti) {
            reference1 = ti;
        }

        private Object reference;

        public void setObjectReference(Object reference) {
            this.reference = reference;
        }

        @Override
        public String toString() {
            return "testclass2";
        }
    }

    private static interface SyncListInterface {
        void setList(List l);
    }

    @Singleton
    private static class SyncList implements SyncListInterface {
        @InspectableForSize("")
        List l;

        public void setList(List l) {
            this.l = l;
        }
    }
    
    private static interface SyncListInterface2 {
        void setList(List l);
    }
    
    @EagerSingleton
    private static class SyncList2 implements SyncListInterface2 {
        @InspectableForSize("")
        List l;

        public void setList(List l) {
            this.l = l;
        }
    }

    private static interface OutterI {
    }

    @Singleton
    @SuppressWarnings( { "unused", "UnusedDeclaration" })
    private static class Outter implements OutterI {
        @InspectableContainer
        private class Inner {
            @InspectionPoint("i")
            private final Inspectable inspectable = new Inspectable() {
                @Override
                public Object inspect() {
                    return "asdf";
                }
            };
        }
    }

    private static class NotGuiced {
        @InspectablePrimitive("")
        static int inspectableInt;

        @InspectionPoint("insp")
        static Inspectable inspectable;
    }
    
    private static interface IConcrete {}
    
    private static abstract class Abstract implements IConcrete {
        @InspectablePrimitive("point") String inspectableA = "qqqq";
    }
    
    @Singleton private static class Concrete extends Abstract {}

    private static interface Parent {
    }

    @SuppressWarnings( { "unused", "UnusedDeclaration" })
    private static abstract class AbstractParent implements Parent {
        @InspectableContainer
        private class Inner {
            @InspectionPoint("i")
            private final Inspectable inspectable = new Inspectable() {
                @Override
                public Object inspect() {
                    return "abcd";
                }
            };
        }
    }

    @Singleton
    private static class ConcreteChild extends AbstractParent {
    }
    
    private static interface UsageData {
        void setUsageData(String s);
        String getUsageData();
    }
    
    @Singleton
    private static class UsageDataImpl implements UsageData {
        
        @InspectablePrimitive(value = "", category = DataCategory.USAGE)
        private String data;
        
        @Override
        public void setUsageData(String s) {
            this.data = s;
        }

        @Override
        public String getUsageData() {
            return data;
        }
    }
    
    private static interface NetworkData {
        void setNetworkData(String s);
        String getNetworkData();
        void setOtherNetworkData(String s);
        String getOtherNetworkData();
    }
    
    @Singleton
    private static class NetworkDataImpl implements NetworkData {
        
        @InspectablePrimitive(value = "", category = DataCategory.NETWORK)
        private String data;
        
        @InspectablePrimitive(value = "")
        private String otherData;
        
        @Override
        public void setNetworkData(String s) {
            this.data = s;
        }

        @Override
        public String getNetworkData() {
            return data;
        }
        
        @Override
        public void setOtherNetworkData(String s) {
            this.otherData = s;
        }

        @Override
        public String getOtherNetworkData() {
            return otherData;
        }
    }

}