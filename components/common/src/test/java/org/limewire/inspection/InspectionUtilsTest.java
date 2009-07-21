package org.limewire.inspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

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
        
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(TestInterface.class).to(TestClass.class);
                bind(TestInterface2.class).to(TestClass2.class);
                bind(SyncListInterface.class).to(SyncList.class);
                bind(OutterI.class).to(Outter.class);
                bind(Parent.class).to(ConcreteChild.class);
                bind(IConcrete.class).to(Concrete.class);
            }
        };
        injector = Guice.createInjector(m);
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
            InspectionUtils.inspectValue(nameOf(NotGuiced.class) + ":,inspectableInt", injector);
        } catch (InspectionException expected){}
        
        // static traversal will work
        assertEquals("asdf",
                InspectionUtils.inspectValue(nameOf(NotGuiced.class) + ":inspectable", injector));
        assertEquals("1",
                InspectionUtils.inspectValue(nameOf(NotGuiced.class) + ":inspectableInt", injector));
    }
    
    public void testInspectablePrimitive() throws Exception {
        TestInterface t = injector.getInstance(TestInterface.class);
        t.setMemberString("a");
        t.setInspectableString("b");
        try {
            InspectionUtils.inspectValue(nameOf(TestClass.class) + ",reference1,memberString", injector);
            fail("should not be inspectable");
        } catch (InspectionException expcted){}
        
        String inspectable = (String)InspectionUtils.inspectValue(nameOf(TestClass.class) + ",inspectableString", injector);
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
            InspectionUtils.inspectValue(nameOf(TestClass.class) + ",memberList", injector);
            fail("should not be inspectable for size");
        } catch (InspectionException expcted){}
        
        String res = (String)InspectionUtils.inspectValue(nameOf(TestClass.class) + ",inspectableList", injector);
        assertEquals("2",res);
    }

    @SuppressWarnings("unchecked")
    public void testSyncCollection() throws Exception {
        SyncList syncList = injector.getInstance(SyncList.class);
        syncList.l = Collections.synchronizedList(new ArrayList());
        syncList.l.add(new Object());
        assertEquals(String.valueOf(syncList.l.size()),InspectionUtils.inspectValue(nameOf(SyncList.class) + ",l", injector));
    }
    
    public void testContainer() throws Exception {
        Object ret = InspectionUtils.inspectValue(nameOf(Outter.Inner.class) + ",inspectable", injector);
        assertEquals("asdf",ret);
    }
    
    public void testContainerInParent() throws Exception {
        Object ret = InspectionUtils.inspectValue(nameOf(Parent.class) + "|" + nameOf(AbstractParent.Inner.class) + ",inspectable", injector);
        assertEquals("abcd", ret);
    }
    
    public void testAbstractPoints() throws Exception {
        Object ret = InspectionUtils.inspectValue(nameOf(IConcrete.class) + "|" + nameOf(Abstract.class) + ",inspectableA", injector);
        assertEquals("qqqq", ret);
    }
    
    public void testRequirements() throws Exception {
        assertFalse(Requirements.created);
        if(OSUtils.isWindows()) {
            assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector));
            assertTrue(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_OSX] on field: int " + Requirements.class.getName() + ".y", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_LINUX] on field: int " + Requirements.class.getName() + ".z", x.getMessage());
            }
            assertFalse(Requirements.created);
        } else if(OSUtils.isMacOSX()) {
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector));                
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_WINDOWS] on field: int " + Requirements.class.getName() + ".x", x.getMessage());
            }
            assertFalse(Requirements.created);
            assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector));
            assertTrue(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_LINUX] on field: int " + Requirements.class.getName() + ".z", x.getMessage());
            }
            assertFalse(Requirements.created);
        } else if(OSUtils.isLinux()) {
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_WINDOWS] on field: int " + Requirements.class.getName() + ".x", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_OSX] on field: int " + Requirements.class.getName() + ".y", x.getMessage());
            }
            assertFalse(Requirements.created);
            assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector));
            assertTrue(Requirements.created);
        } else {
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",x", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_WINDOWS] on field: int " + Requirements.class.getName() + ".x", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",y", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_OSX] on field: int " + Requirements.class.getName() + ".y", x.getMessage());
            }
            assertFalse(Requirements.created);
            Requirements.created = false;
            try {
                assertEquals("5", InspectionUtils.inspectValue(nameOf(Requirements.class) + ",z", injector));
            } catch(InspectionException x) {
                assertEquals("invalid limitations: [OS_LINUX] on field: int " + Requirements.class.getName() + ".z", x.getMessage());
            }
            assertFalse(Requirements.created);
        }
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

}