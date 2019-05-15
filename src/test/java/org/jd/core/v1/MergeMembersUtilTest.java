/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.jd.core.v1.model.classfile.ClassFile;
import org.jd.core.v1.model.classfile.Method;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.MergeMembersUtil;
import org.jd.core.v1.util.DefaultList;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings("unchecked")
public class MergeMembersUtilTest {

    @Test
    public void testEmptyResult() throws Exception {
        DefaultList<ClassFileMemberDeclaration> methods = newMethods();

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(null, methods, null);

        Assert.assertEquals(methods, result);
    }

    @Test
    public void testMergeFieldsAndMethods() throws Exception {
        DefaultList<ClassFileMemberDeclaration> fields = newFields();
        DefaultList<ClassFileMemberDeclaration> methods = newMethods();

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, methods, null);

        Assert.assertEquals(fields.size() + methods.size(), result.size());
        Assert.assertEquals(10, result.get(3).getFirstLineNumber());
        Assert.assertEquals(11, result.get(4).getFirstLineNumber());
        Assert.assertEquals(20, result.get(9).getFirstLineNumber());
        Assert.assertEquals(21, result.get(10).getFirstLineNumber());
        Assert.assertEquals(30, result.get(11).getFirstLineNumber());
    }

    @Test
    public void testMergeFieldsMethodsAndInnerTypes_0() throws Exception {
        DefaultList<ClassFileMemberDeclaration> fields = newFields();
        DefaultList<ClassFileMemberDeclaration> methods = newMethods();
        DefaultList<ClassFileMemberDeclaration> innerTypes = newInnerTypes(0);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, methods, innerTypes);

        Assert.assertEquals(fields.size() + methods.size() + innerTypes.size(), result.size());
        Assert.assertEquals(10, result.get(3).getFirstLineNumber());
        Assert.assertEquals(11, result.get(4).getFirstLineNumber());
        Assert.assertEquals(20, result.get(9).getFirstLineNumber());
        Assert.assertEquals(21, result.get(10).getFirstLineNumber());
        Assert.assertEquals(30, result.get(11).getFirstLineNumber());
        Assert.assertEquals( 0, result.get(12).getFirstLineNumber());
        Assert.assertEquals( 0, result.get(20).getFirstLineNumber());
    }

    @Test
    public void testMergeFieldsMethodsAndInnerTypes_25() throws Exception {
        DefaultList<ClassFileMemberDeclaration> fields = newFields();
        DefaultList<ClassFileMemberDeclaration> methods = newMethods();
        DefaultList<ClassFileMemberDeclaration> innerTypes = newInnerTypes(25);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, methods, innerTypes);

        Assert.assertEquals(fields.size() + methods.size() + innerTypes.size(), result.size());
        Assert.assertEquals(10, result.get(3).getFirstLineNumber());
        Assert.assertEquals(11, result.get(4).getFirstLineNumber());
        Assert.assertEquals(20, result.get(9).getFirstLineNumber());
        Assert.assertEquals(21, result.get(10).getFirstLineNumber());
        Assert.assertEquals(25, result.get(11).getFirstLineNumber());
        Assert.assertEquals(30, result.get(12).getFirstLineNumber());
    }

    @Test
    public void testMergeFields() throws Exception {
        ClassFileFieldDeclaration a = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a"));
        ClassFileFieldDeclaration b = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b"));
        ClassFileFieldDeclaration c = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c"));

        List<ClassFileFieldDeclaration> fields = Arrays.asList(a, b, c);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, null, null);

        Assert.assertEquals(fields.size(), result.size());
        Assert.assertEquals(a, result.get(0));
        Assert.assertEquals(b, result.get(1));
        Assert.assertEquals(c, result.get(2));
    }

    @Test
    public void testMergeAscendantSortedFields1() throws Exception {
        ClassFileFieldDeclaration a = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a"), 1);
        ClassFileFieldDeclaration b = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b"), 2);
        ClassFileFieldDeclaration c = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c"), 3);

        List<ClassFileFieldDeclaration> fields = Arrays.asList(a, b, c);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, null, null);

        Assert.assertEquals(fields.size(), result.size());
        Assert.assertEquals(a, result.get(0));
        Assert.assertEquals(b, result.get(1));
        Assert.assertEquals(c, result.get(2));
    }

    @Test
    public void testMergeAscendantSortedFields2() throws Exception {
        ClassFileFieldDeclaration a = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a"));
        ClassFileFieldDeclaration b = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b"), 1);
        ClassFileFieldDeclaration c = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c"));
        ClassFileFieldDeclaration d = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("d"), 2);
        ClassFileFieldDeclaration e = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("e"), 3);
        ClassFileFieldDeclaration f = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("f"));

        List<ClassFileFieldDeclaration> fields = Arrays.asList(a, b, c, d, e, f);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, null, null);

        Assert.assertEquals(fields.size(), result.size());
        Assert.assertEquals(a, result.get(0));
        Assert.assertEquals(b, result.get(1));
        Assert.assertEquals(c, result.get(2));
        Assert.assertEquals(d, result.get(3));
        Assert.assertEquals(e, result.get(4));
        Assert.assertEquals(f, result.get(5));
    }

    @Test
    public void testMergeDescendantSortedFields1() throws Exception {
        ClassFileFieldDeclaration a = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a"), 3);
        ClassFileFieldDeclaration b = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b"), 2);
        ClassFileFieldDeclaration c = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c"), 1);

        List<ClassFileFieldDeclaration> fields = Arrays.asList(a, b, c);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, null, null);

        Assert.assertEquals(fields.size(), result.size());
        Assert.assertEquals(c, result.get(0));
        Assert.assertEquals(b, result.get(1));
        Assert.assertEquals(a, result.get(2));
    }

    @Test
    public void testMergeDescendantSortedFields2() throws Exception {
        ClassFileFieldDeclaration a = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a"));
        ClassFileFieldDeclaration b = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b"), 3);
        ClassFileFieldDeclaration c = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c"));
        ClassFileFieldDeclaration d = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("d"), 2);
        ClassFileFieldDeclaration e = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("e"), 1);
        ClassFileFieldDeclaration f = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("f"));

        List<ClassFileFieldDeclaration> fields = Arrays.asList(a, b, c, d, e, f);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, null, null);

        Assert.assertEquals(fields.size(), result.size());
        Assert.assertEquals(f, result.get(0));
        Assert.assertEquals(e, result.get(1));
        Assert.assertEquals(d, result.get(2));
        Assert.assertEquals(c, result.get(3));
        Assert.assertEquals(b, result.get(4));
        Assert.assertEquals(a, result.get(5));
    }

    @Test
    public void testMergeUnsortedFields1() throws Exception {
        ClassFileFieldDeclaration a = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a"), 3);
        ClassFileFieldDeclaration b = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b"), 1);
        ClassFileFieldDeclaration c = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c"), 2);

        List<ClassFileFieldDeclaration> fields = Arrays.asList(a, b, c);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, null, null);

        Assert.assertEquals(fields.size(), result.size());
        Assert.assertEquals(b, result.get(0));
        Assert.assertEquals(c, result.get(1));
        Assert.assertEquals(a, result.get(2));
    }

    @Test
    public void testMergeUnsortedFields2() throws Exception {
        ClassFileFieldDeclaration a = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a"));
        ClassFileFieldDeclaration b = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b"), 3);
        ClassFileFieldDeclaration c = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c"));
        ClassFileFieldDeclaration d = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("d"), 1);
        ClassFileFieldDeclaration e = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("e"), 2);
        ClassFileFieldDeclaration f = new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("f"));

        List<ClassFileFieldDeclaration> fields = Arrays.asList(a, b, c, d, e, f);

        List<ClassFileMemberDeclaration> result = MergeMembersUtil.merge(fields, null, null);

        Assert.assertEquals(fields.size(), result.size());
        Assert.assertEquals(d, result.get(0));
        Assert.assertEquals(e, result.get(1));
        Assert.assertEquals(b, result.get(2));
        Assert.assertEquals(a, result.get(3));
        Assert.assertEquals(c, result.get(4));
        Assert.assertEquals(f, result.get(5));
    }

    protected DefaultList<ClassFileMemberDeclaration> newFields() {
        DefaultList<ClassFileMemberDeclaration> fields = new DefaultList<>();

        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("a")));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("b")));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("c")));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("d"), 10));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("e"), 11));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("f")));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("g")));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("h"), 30));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("i")));
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("j")));

        return fields;
    }

    protected DefaultList<ClassFileMemberDeclaration> newMethods() {
        DefaultList<ClassFileMemberDeclaration> methods = new DefaultList<>();
        ClassFile classFile = null;
        Method method = new Method(0, "method", "()V", null, null);

        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "a", PrimitiveType.TYPE_VOID, null));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "b", PrimitiveType.TYPE_VOID, null));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "c", PrimitiveType.TYPE_VOID, null, 20));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "d", PrimitiveType.TYPE_VOID, null, 21));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "e", PrimitiveType.TYPE_VOID, null));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "f", PrimitiveType.TYPE_VOID, null));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "g", PrimitiveType.TYPE_VOID, null, 40));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "h", PrimitiveType.TYPE_VOID, null));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "i", PrimitiveType.TYPE_VOID, null));
        methods.add(new ClassFileMethodDeclaration(null, classFile, method, "j", PrimitiveType.TYPE_VOID, null));

        return methods;
    }

    protected DefaultList<ClassFileMemberDeclaration> newInnerTypes(int lineNumber) {
        DefaultList<ClassFileMemberDeclaration> innerTypes = new DefaultList<>();

        DefaultList<ClassFileFieldDeclaration> fields = new DefaultList<>();
        fields.add(new ClassFileFieldDeclaration(0, PrimitiveType.TYPE_INT, new FieldDeclarator("d"), lineNumber));

        ClassFileBodyDeclaration bodyDeclaration = new ClassFileBodyDeclaration("A");
        bodyDeclaration.setFieldDeclarations(fields);

        innerTypes.add(new ClassFileClassDeclaration(null, 0, "A", "A", null, null, null, bodyDeclaration));

        return innerTypes;
    }
}
