/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.RuntimeInvisibleAnnotations;
import org.apache.bcel.classfile.RuntimeVisibleAnnotations;
import org.jd.core.v1.loader.ZipLoader;
import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.AnnotationReferences;
import org.jd.core.v1.model.javasyntax.reference.BaseAnnotationReference;
import org.jd.core.v1.model.javasyntax.reference.ElementValuePairs;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.AnnotationConverter;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;
import org.jd.core.v1.service.deserializer.classfile.ClassFileDeserializer;
import org.junit.Test;

import java.io.InputStream;

import junit.framework.TestCase;

public class AnnotationConverterTest extends TestCase {

    @Test
    @SuppressWarnings("unchecked")
    public void test() throws Exception {
        try (InputStream is = this.getClass().getResourceAsStream("/zip/data-java-jdk-1.7.0.zip")) {
            ZipLoader loader = new ZipLoader(is);
            TypeMaker typeMaker = new TypeMaker(loader);
            ClassFileDeserializer deserializer = new ClassFileDeserializer();

            DecompileContext decompileContext = new DecompileContext();
            decompileContext.setMainInternalTypeName("org/jd/core/test/AnnotatedClass");
            decompileContext.setLoader(loader);

            ClassFile classFile = deserializer.loadClassFile(loader, decompileContext.getMainInternalTypeName());
            decompileContext.setClassFile(classFile);
            AnnotationConverter converter = new AnnotationConverter(typeMaker);

            // Check class
            assertNotNull(classFile);

            RuntimeVisibleAnnotations visibles = classFile.getAttribute(Const.ATTR_RUNTIME_VISIBLE_ANNOTATIONS);
            RuntimeInvisibleAnnotations invisibles = classFile.getAttribute(Const.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS);
            BaseAnnotationReference annotationReferences = converter.convert(visibles == null ? null : visibles.getAnnotationEntries(),
                                                                           invisibles == null ? null : invisibles.getAnnotationEntries());

            assertNotNull(annotationReferences);
            assertTrue(annotationReferences instanceof AnnotationReferences);

            AnnotationReferences<AnnotationReference> annotationReferenceList = (AnnotationReferences<AnnotationReference>)annotationReferences;

            assertEquals(2, annotationReferenceList.size());

            AnnotationReference annotationReference0 = annotationReferenceList.getFirst();
            
            assertEquals("org/jd/core/test/annotation/Quality", annotationReference0.getType().getInternalName());
            assertEquals("Quality", annotationReference0.getType().getName());
            assertNotNull(annotationReference0.getElementValue());
            assertNull(annotationReference0.getElementValuePairs());
            assertEquals(
                    "ExpressionElementValue{" +
                        "FieldReferenceExpression{" +
                        "type=InnerObjectType{ObjectType{org/jd/core/test/annotation/Quality}.Lorg/jd/core/test/annotation/Quality$Level;}, " +
                        "expression=ObjectTypeReferenceExpression{InnerObjectType{ObjectType{org/jd/core/test/annotation/Quality}.Lorg/jd/core/test/annotation/Quality$Level;}}, " +
                        "name=HIGH, " +
                        "descriptor=Lorg/jd/core/test/annotation/Quality$Level;}" +
                    "}",
                    annotationReference0.getElementValue().toString());

            AnnotationReference annotationReference1 = annotationReferenceList.get(1);

            assertEquals("org/jd/core/test/annotation/Author", annotationReference1.getType().getInternalName());
            assertEquals("Author", annotationReference1.getType().getName());
            assertNull(annotationReference1.getElementValue());
            assertNotNull(annotationReference1.getElementValuePairs());
            assertTrue(annotationReference1.getElementValuePairs() instanceof ElementValuePairs);

            ElementValuePairs elementValuePairArrayList = (ElementValuePairs)annotationReference1.getElementValuePairs();

            assertEquals(2, elementValuePairArrayList.size());
            assertEquals("value", elementValuePairArrayList.getFirst().name());
            assertEquals(
                    "AnnotationElementValue{" +
                        "type=ObjectType{org/jd/core/test/annotation/Name}, " +
                        "elementValue=null, " +
                        "elementValuePairs=ElementValuePairs{[" +
                            "ElementValuePair{name=salutation, elementValue=ExpressionElementValue{StringConstantExpression{\"Mr\"}}}, " +
                            "ElementValuePair{name=value, elementValue=ExpressionElementValue{StringConstantExpression{\"Donald\"}}}, " +
                            "ElementValuePair{name=last, elementValue=ExpressionElementValue{StringConstantExpression{\"Duck\"}}}" +
                        "]}" +
                    "}",
                    elementValuePairArrayList.get(0).elementValue().toString());
            assertEquals("contributors", elementValuePairArrayList.get(1).name());
            assertEquals(
                    "ElementValueArrayInitializerElementValue{" +
                        "ElementValues{[" +
                            "AnnotationElementValue{" +
                                "type=ObjectType{org/jd/core/test/annotation/Name}, " +
                                "elementValue=ExpressionElementValue{StringConstantExpression{\"Huey\"}}, " +
                                "elementValuePairs=null}, " +
                            "AnnotationElementValue{" +
                                "type=ObjectType{org/jd/core/test/annotation/Name}, " +
                                "elementValue=ExpressionElementValue{StringConstantExpression{\"Dewey\"}}, " +
                                "elementValuePairs=null}, " +
                            "AnnotationElementValue{" +
                                "type=ObjectType{org/jd/core/test/annotation/Name}, " +
                                "elementValue=ExpressionElementValue{StringConstantExpression{\"Louie\"}}, " +
                                "elementValuePairs=null}" +
                        "]}" +
                    "}",
                    elementValuePairArrayList.get(1).elementValue().toString());
        }
    }
}
