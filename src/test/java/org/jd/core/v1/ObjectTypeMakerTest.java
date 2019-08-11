/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import junit.framework.TestCase;
import org.jd.core.v1.loader.ClassPathLoader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ObjectTypeMaker;
import org.junit.Test;

import java.io.InputStream;

public class ObjectTypeMakerTest extends TestCase {

    @Test
    public void testOuterClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass");

        assertEquals("org/jd/core/test/OuterClass", ot.getInternalName());
        assertEquals("org.jd.core.test.OuterClass", ot.getQualifiedName());
        assertEquals("OuterClass", ot.getName());
    }

    @Test
    public void testOuterClass$InnerClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$InnerClass");

        assertEquals("org/jd/core/test/OuterClass$InnerClass", ot.getInternalName());
        assertEquals("org.jd.core.test.OuterClass.InnerClass", ot.getQualifiedName());
        assertEquals("InnerClass", ot.getName());
    }

    @Test
    public void testOuterClass$StaticInnerClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$StaticInnerClass");

        assertEquals("org/jd/core/test/OuterClass$StaticInnerClass", ot.getInternalName());
        assertEquals("org.jd.core.test.OuterClass.StaticInnerClass", ot.getQualifiedName());
        assertEquals("StaticInnerClass", ot.getName());
    }

    @Test
    public void testOuterClass$1() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$1");

        assertEquals("org/jd/core/test/OuterClass$1", ot.getInternalName());
        assertNull(ot.getQualifiedName());
        assertNull(ot.getName());
    }

    @Test
    public void testOuterClass$1LocalClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$1LocalClass");

        assertEquals("org/jd/core/test/OuterClass$1LocalClass", ot.getInternalName());
        assertNull(ot.getQualifiedName());
        assertEquals("LocalClass", ot.getName());
    }

    @Test
    public void testThread() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("java/lang/Thread");

        assertEquals("java/lang/Thread", ot.getInternalName());
        assertEquals("java.lang.Thread", ot.getQualifiedName());
        assertEquals("Thread", ot.getName());
    }

    @Test
    public void testThreadState() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("java/lang/Thread$State");

        assertEquals("java/lang/Thread$State", ot.getInternalName());
        assertEquals("java.lang.Thread.State", ot.getQualifiedName());
        assertEquals("State", ot.getName());
    }

    @Test
    public void testUnknownClass() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("org/unknown/Class");

        assertEquals("org/unknown/Class", ot.getInternalName());
        assertEquals("org.unknown.Class", ot.getQualifiedName());
        assertEquals("Class", ot.getName());
    }

    @Test
    public void testUnknownInnerClass() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType ot = maker.makeFromInternalTypeName("org/unknown/Class$InnerClass");

        assertEquals("org/unknown/Class$InnerClass", ot.getInternalName());
        assertEquals("org.unknown.Class.InnerClass", ot.getQualifiedName());
        assertEquals("InnerClass", ot.getName());
    }

    @Test
    public void testListIsAssignableFromArrayList() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType parent = maker.makeFromInternalTypeName("java/util/List");
        ObjectType child = maker.makeFromInternalTypeName("java/util/ArrayList");

        assertNotNull(parent);
        assertNotNull(child);
        assertTrue(maker.isAssignable(parent, child));
    }

    @Test
    public void testClassIsAssignableFromObject() throws Exception {
        ClassPathLoader loader = new ClassPathLoader();
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType parent = maker.makeFromInternalTypeName("java/lang/Class");
        ObjectType child = maker.makeFromInternalTypeName("java/lang/Object");

        assertNotNull(parent);
        assertNotNull(child);
        assertFalse(maker.isAssignable(parent, child));
    }

    @Test
    public void testObjectIsAssignableFromSafeNumberComparator() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType parent = maker.makeFromInternalTypeName("java/lang/Object");
        ObjectType child = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$SafeNumberComparator");

        assertNotNull(parent);
        assertNotNull(child);
        assertTrue(maker.isAssignable(parent, child));
    }

    @Test
    public void testComparatorIsAssignableFromSafeNumberComparator() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType parent = maker.makeFromInternalTypeName("java/util/Comparator");
        ObjectType child = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$SafeNumberComparator");

        assertNotNull(parent);
        assertNotNull(child);
        assertTrue(maker.isAssignable(parent, child));
    }

    @Test
    public void testNumberComparatorIsAssignableFromSafeNumberComparator() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType parent = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$NumberComparator");
        ObjectType child = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass$SafeNumberComparator");

        assertNotNull(parent);
        assertNotNull(child);
        assertTrue(maker.isAssignable(parent, child));
    }

    @Test
    public void testOuterClassIsAssignableFromSimpleClass() throws Exception {
        InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip");
        ZipLoader loader = new ZipLoader(is);
        ObjectTypeMaker maker = new ObjectTypeMaker(loader);
        ObjectType parent = maker.makeFromInternalTypeName("org/jd/core/test/OuterClass");
        ObjectType child = maker.makeFromInternalTypeName("org/jd/core/test/SimpleClass");

        assertNotNull(parent);
        assertNotNull(child);
        assertFalse(maker.isAssignable(parent, child));
    }
}
