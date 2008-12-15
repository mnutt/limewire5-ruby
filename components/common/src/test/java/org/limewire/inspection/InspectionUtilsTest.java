package org.limewire.inspection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import junit.framework.Test;

import org.limewire.util.BaseTestCase;

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
        Module m = new AbstractModule() {
            @Override
            public void configure() {
                bind(TestInterface.class).to(TestClass.class);
                bind(TestInterface2.class).to(TestClass2.class);
                bind(SyncListInterface.class).to(SyncList.class);
                bind(OutterI.class).to(Outter.class);
                bind(Parent.class).to(ConcreteChild.class);
            }
        };
        injector = Guice.createInjector(m);
    }
    
    public void testTraverse() throws Exception {
        TestInterface t1 = injector.getInstance(TestInterface.class);
        TestInterface2 t2 = injector.getInstance(TestInterface2.class);
        
        /*
         * Loop:
         * t1 --> t2
         * t2 --> t1
         */
        t1.setReference2(t2);
        t2.setReference1(t1);
        t1.setObjectReference(new InspectableClass("inspectable1"));
        t2.setObjectReference(new InspectableClass("inspectable2"));
        
        // notice that the class name can be the impl or the interface - works both ways
        assertEquals("inspectable1",
                InspectionUtils.inspectValue(
                        "org.limewire.inspection.TestClass,reference2,reference1,reference", injector));

        assertEquals("inspectable2",
                InspectionUtils.inspectValue(
                        "org.limewire.inspection.TestClass2,reference1,reference2," +
                        "reference1,reference2,reference1,reference2,reference", injector));
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass", injector);
            fail("invalid value");
        } catch (InspectionException expcted){}
        
        try {
            InspectionUtils.inspectValue("org.limewire.not.existing.class,a", injector);
            fail("class should not exist");
        } catch (InspectionException expcted){}
        
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass,wrongField", injector);
            fail("field should not be found");
        } catch (InspectionException expcted){}
    }
    
    public void testStaticMembersTraversed() throws Exception {
        TestInterface t1 = injector.getInstance(TestInterface.class);
        TestInterface2 t2 = injector.getInstance(TestInterface2.class);
        t1.setReference2(t2);
        t1.setReference1(t1);
        t2.setReference1(t1);
        t1.setObjectReference(t2);
        t2.setObjectReference(new InspectableClass("test"));
        
        /*
         * traverse
         * t1 -> self reference(static) -> t2 -> t1(static) -> t2 (static) -> t1 ->t2 -> Inspectable
         */
        assertEquals("test",
                InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,reference,reference1,reference,reference", injector));
    }
    
    
    public void testOldStyleTraversal() throws Exception {
        NotGuiced.inspectableInt = 1;
        NotGuiced.inspectable = new Inspectable() {
            public Object inspect() {
                return "asdf";
            }
        };
        
        // injector-based traversal will fail
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.NotGuiced,inspectableInt", injector);
        } catch (InspectionException expected){}
        
        // static traversal will work
        assertEquals("asdf",
                InspectionUtils.inspectValue("org.limewire.inspection.NotGuiced:inspectable", injector));
        assertEquals("1",
                InspectionUtils.inspectValue("org.limewire.inspection.NotGuiced:inspectableInt", injector));
    }
    
    public void testInspectablePrimitive() throws Exception {
        TestInterface t = injector.getInstance(TestInterface.class);
        t.setMemberString("a");
        t.setInspectableString("b");
        try {
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass,reference1,memberString", injector);
            fail("should not be inspectable");
        } catch (InspectionException expcted){}
        
        String inspectable = (String)InspectionUtils.inspectValue("org.limewire.inspection.TestClass,inspectableString", injector);
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
            InspectionUtils.inspectValue("org.limewire.inspection.TestClass,memberList", injector);
            fail("should not be inspectable for size");
        } catch (InspectionException expcted){}
        
        String res = (String)InspectionUtils.inspectValue("org.limewire.inspection.TestClass,inspectableList", injector);
        assertEquals("2",res);
    }
    
    

    @SuppressWarnings("unchecked")
    public void testSyncCollection() throws Exception {
        SyncList syncList = injector.getInstance(SyncList.class);
        syncList.l = Collections.synchronizedList(new ArrayList());
        syncList.l.add(new Object());
        assertEquals(String.valueOf(syncList.l.size()),InspectionUtils.inspectValue("org.limewire.inspection.SyncList,l", injector));
    }
    
    public void testContainer() throws Exception {
        Object ret = InspectionUtils.inspectValue("org.limewire.inspection.Outter$Inner,inspectable", injector);
        assertEquals("asdf",ret);
    }
    
    public void testContainerInParent() throws Exception {
        Object ret = InspectionUtils.inspectValue("org.limewire.inspection.Parent|org.limewire.inspection.AbstractParent$Inner,inspectable", injector);
        assertEquals("abcd", ret);
    }
}

interface TestInterface {
    void setReference1(TestInterface ti);
    void setReference2(TestInterface2 ti);
    
    void setObjectReference(Object reference);
    
    void setMemberString(String s);
    void setInspectableString(String s);
    
    void setMemeberList(List l);
    void setInspectableList(List l);
}

@SuppressWarnings({"unused", "FieldCanBeLocal", "UnusedDeclaration"})
@Singleton
class TestClass implements TestInterface{
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

interface TestInterface2 {
    void setReference1(TestInterface ti);
    void setObjectReference(Object reference);
}
@Singleton
@SuppressWarnings({"unused", "FieldCanBeLocal", "UnusedDeclaration"})
class TestClass2 implements TestInterface2 {
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

class InspectableClass implements Inspectable {
    String s;
    InspectableClass(String s) {
        this.s = s;
    }
    public Object inspect() {
        return s;
    }
}

interface SyncListInterface {
    void setList(List l);
}

@Singleton
class SyncList implements SyncListInterface{
    @InspectableForSize("")
    List l;
    public void setList(List l) {
        this.l = l;
    }
}

interface OutterI {}

@Singleton
@SuppressWarnings({"unused", "UnusedDeclaration"})
class Outter implements OutterI {
    @InspectableContainer
    private class Inner {
        private final Inspectable inspectable = new Inspectable() {
            public Object inspect() {
                return "asdf";
            }
        };
    }
}

class NotGuiced {
    @InspectablePrimitive("")
    static int inspectableInt;
    
    static Inspectable inspectable;
}

interface Parent {}

@SuppressWarnings({"unused", "UnusedDeclaration"})
abstract class AbstractParent implements Parent {
    @InspectableContainer
    private class Inner {
        private final Inspectable inspectable = new Inspectable() {
            public Object inspect() {
                return "abcd";
            }
        };
    }
}

@Singleton
class ConcreteChild extends AbstractParent {}