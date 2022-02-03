/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantUtf8;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Field;
import org.jd.core.v1.model.classfile.attribute.Annotations;
import org.jd.core.v1.model.classfile.attribute.ElementValueAnnotationValue;
import org.jd.core.v1.model.classfile.attribute.ElementValuePrimitiveType;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;
import org.jd.core.v1.util.StringConstants;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import junit.framework.TestCase;

public class ClassFileDeserializerTest extends TestCase {
    @Test
    public void testMissingClass() throws Exception {
        class NoOpLoader implements Loader {
            @Override
            public boolean canLoad(String internalName) {
                return false;
            }

            @Override
            public byte[] load(String internalName) throws IOException {
                fail("Loader cannot load anything");
                return null;
            }
        }

        ClassFileDeserializer deserializer = new ClassFileDeserializer();
        try {
            deserializer.loadClassFile(new NoOpLoader(), "DoesNotExist");
            fail("Expected exception");
        }
        // Expecting exception because class cannot be loaded
        catch (IllegalArgumentException expected) { }
    }

    @Test
    public void testAnnotatedClass() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            ZipLoader loader = new ZipLoader(is);
            ClassFileDeserializer deserializer = new ClassFileDeserializer();

            DecompileContext decompileContext = new DecompileContext();
            decompileContext.setMainInternalTypeName("org/jd/core/test/AnnotatedClass");
            decompileContext.setLoader(loader);

            ClassFile classFile = deserializer.loadClassFile(loader, decompileContext.getMainInternalTypeName());
            decompileContext.setClassFile(classFile);

            // Check class
            assertNotNull(classFile);
            assertEquals("org/jd/core/test/AnnotatedClass", classFile.getInternalTypeName());
            assertEquals("java/util/ArrayList", classFile.getSuperTypeName());

            Annotations invAttr = classFile.getAttribute(StringConstants.RUNTIMEINVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
            assertNotNull(invAttr.getAnnotations());
            assertEquals(2, invAttr.getAnnotations().length);
            assertNotNull(invAttr.getAnnotations()[0].elementValuePairs());
            assertEquals(1, invAttr.getAnnotations()[0].elementValuePairs().size());

            ElementValueAnnotationValue annotationValue = (ElementValueAnnotationValue) invAttr.getAnnotations()[1].elementValuePairs().get(0).getValue();
            assertEquals("Lorg/jd/core/test/annotation/Name;", annotationValue.annotationValue().descriptor());
            assertNotNull(annotationValue.annotationValue().elementValuePairs());
            assertEquals(3, annotationValue.annotationValue().elementValuePairs().size());
            assertEquals("salutation", annotationValue.annotationValue().elementValuePairs().get(0).getKey());

            ElementValuePrimitiveType primitiveType = (ElementValuePrimitiveType) annotationValue.annotationValue().elementValuePairs().get(1).getValue();
            ConstantUtf8 cu = primitiveType.getConstValue();
            assertEquals("Donald", cu.getBytes());

            // Check fields
            assertNotNull(classFile.getFields());
            assertEquals(10, classFile.getFields().length);

            // Check 1st field
            Field field = classFile.getFields()[1];
            assertEquals("b1", field.getName());
            assertEquals("B", field.getDescriptor());

            Annotations attr = field.getAttribute(StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
            assertNotNull(attr.getAnnotations());
            assertEquals(1, attr.getAnnotations().length);
            assertNotNull(attr.getAnnotations()[0].elementValuePairs());
            assertEquals(1, attr.getAnnotations()[0].elementValuePairs().size());
            assertEquals("b", attr.getAnnotations()[0].elementValuePairs().get(0).getKey());

            primitiveType = (ElementValuePrimitiveType) attr.getAnnotations()[0].elementValuePairs().get(0).getValue();
            ConstantInteger ci = primitiveType.getConstValue();
            assertEquals(-15, ci.getBytes());

            // Check 8th field
            field = classFile.getFields()[8];
            assertEquals("str2", field.getName());
            assertEquals("Ljava/lang/String;", field.getDescriptor());

            attr = field.getAttribute(StringConstants.RUNTIMEVISIBLEANNOTATIONS_ATTRIBUTE_NAME);
            assertNotNull(attr.getAnnotations());
            assertEquals(1, attr.getAnnotations().length);
            assertNotNull(attr.getAnnotations()[0].elementValuePairs());
            assertEquals(1, attr.getAnnotations()[0].elementValuePairs().size());
            assertEquals("str", attr.getAnnotations()[0].elementValuePairs().get(0).getKey());

            primitiveType = (ElementValuePrimitiveType) attr.getAnnotations()[0].elementValuePairs().get(0).getValue();
            cu = primitiveType.getConstValue();
            assertEquals("str \u0083 \u0909 \u1109", cu.getBytes());

            // Check getters
            assertNotNull(classFile.getMethods());
            assertEquals(3, classFile.getMethods().length);

            // Check constructor
            assertEquals(StringConstants.INSTANCE_CONSTRUCTOR, classFile.getMethods()[0].getName());
            assertEquals("()V", classFile.getMethods()[0].getDescriptor());
            assertNotNull(classFile.getMethods()[0].getAttribute("Code"));
        }
    }
}
