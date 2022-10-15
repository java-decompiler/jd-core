/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1;

import org.apache.bcel.Const;
import org.jd.core.v1.loader.NopLoader;
import org.jd.core.v1.model.javasyntax.CompilationUnit;
import org.jd.core.v1.model.javasyntax.declaration.BodyDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ClassDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ConstructorDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.EnumDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.ExpressionVariableInitializer;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.FieldDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameter;
import org.jd.core.v1.model.javasyntax.declaration.FormalParameters;
import org.jd.core.v1.model.javasyntax.declaration.InterfaceDeclaration;
import org.jd.core.v1.model.javasyntax.declaration.LocalVariableDeclarator;
import org.jd.core.v1.model.javasyntax.declaration.MemberDeclarations;
import org.jd.core.v1.model.javasyntax.declaration.MethodDeclaration;
import org.jd.core.v1.model.javasyntax.expression.ArrayExpression;
import org.jd.core.v1.model.javasyntax.expression.BinaryOperatorExpression;
import org.jd.core.v1.model.javasyntax.expression.BooleanExpression;
import org.jd.core.v1.model.javasyntax.expression.DoubleConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.Expressions;
import org.jd.core.v1.model.javasyntax.expression.FieldReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.IntegerConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.LengthExpression;
import org.jd.core.v1.model.javasyntax.expression.LocalVariableReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.MethodInvocationExpression;
import org.jd.core.v1.model.javasyntax.expression.NewExpression;
import org.jd.core.v1.model.javasyntax.expression.NullExpression;
import org.jd.core.v1.model.javasyntax.expression.ObjectTypeReferenceExpression;
import org.jd.core.v1.model.javasyntax.expression.ParenthesesExpression;
import org.jd.core.v1.model.javasyntax.expression.StringConstantExpression;
import org.jd.core.v1.model.javasyntax.expression.ThisExpression;
import org.jd.core.v1.model.javasyntax.statement.ExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ForEachStatement;
import org.jd.core.v1.model.javasyntax.statement.IfStatement;
import org.jd.core.v1.model.javasyntax.statement.LocalVariableDeclarationStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnExpressionStatement;
import org.jd.core.v1.model.javasyntax.statement.ReturnStatement;
import org.jd.core.v1.model.javasyntax.statement.Statements;
import org.jd.core.v1.model.javasyntax.statement.SwitchStatement;
import org.jd.core.v1.model.javasyntax.type.ObjectType;
import org.jd.core.v1.model.javasyntax.type.PrimitiveType;
import org.jd.core.v1.model.javasyntax.type.Type;
import org.jd.core.v1.model.javasyntax.type.Types;
import org.jd.core.v1.model.message.DecompileContext;
import org.jd.core.v1.model.token.Token;
import org.jd.core.v1.printer.PlainTextMetaPrinter;
import org.jd.core.v1.service.fragmenter.javasyntaxtojavafragment.JavaSyntaxToJavaFragmentProcessor;
import org.jd.core.v1.service.layouter.LayoutFragmentProcessor;
import org.jd.core.v1.service.tokenizer.javafragmenttotoken.JavaFragmentToTokenProcessor;
import org.jd.core.v1.service.writer.WriteTokenProcessor;
import org.jd.core.v1.util.DefaultList;
import org.jd.core.v1.util.StringConstants;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.apache.bcel.Const.ACC_FINAL;
import static org.apache.bcel.Const.ACC_PRIVATE;
import static org.apache.bcel.Const.ACC_PUBLIC;
import static org.apache.bcel.Const.ACC_STATIC;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import junit.framework.TestCase;

public class JavaSyntaxToJavaSourceTest extends TestCase {

    protected JavaSyntaxToJavaFragmentProcessor fragmenter = new JavaSyntaxToJavaFragmentProcessor();
    protected LayoutFragmentProcessor layouter = new LayoutFragmentProcessor();
    //protected TestTokenizeJavaFragmentProcessor tokenizer = new TestTokenizeJavaFragmentProcessor();
    protected JavaFragmentToTokenProcessor tokenizer = new JavaFragmentToTokenProcessor();
    protected WriteTokenProcessor writer = new WriteTokenProcessor();

    @Test
    public void testClassDeclaration() throws Exception {
        ObjectType stringArrayType = new ObjectType(StringConstants.JAVA_LANG_STRING, "java.lang.String", "String", 1);
        ObjectType printStreamType = new ObjectType("java/io/PrintStream", "java.io.PrintStream", "PrintStream");

        CompilationUnit compilationUnit = new CompilationUnit(
            new ClassDeclaration(
                Const.ACC_PUBLIC,
                "org/jd/core/v1/service/test/TokenWriterTest",
                "TokenWriterTest",
                new BodyDeclaration(
                    "org/jd/core/v1/service/test/TokenWriterTest",
                    new MethodDeclaration(
                        Const.ACC_PUBLIC | Const.ACC_STATIC,
                        "main",
                        PrimitiveType.TYPE_VOID,
                        new FormalParameter(ObjectType.TYPE_STRING.createType(1), "args"),
                        "([Ljava/lang/String;)V",
                        new Statements(
                            new IfStatement(
                                new BinaryOperatorExpression(
                                    8,
                                    PrimitiveType.TYPE_BOOLEAN,
                                    new LocalVariableReferenceExpression(8, stringArrayType, "args"),
                                    "==",
                                    new NullExpression(8, stringArrayType),
                                    9
                                ),
                                ReturnStatement.RETURN
                            ),
                            new LocalVariableDeclarationStatement(
                                PrimitiveType.TYPE_INT,
                                new LocalVariableDeclarator(0,
                                    "i",
                                    new ExpressionVariableInitializer(
                                        new MethodInvocationExpression(
                                            10,
                                            PrimitiveType.TYPE_INT,
                                            new ThisExpression(10, new ObjectType("org/jd/core/v1/service/test/TokenWriterTest", "org.jd.core.v1.service.writer.TokenWriterTest", "TokenWriterTest")),
                                            "org/jd/core/v1/service/test/TokenWriterTest",
                                            "call",
                                            "(Ljava/lang/String;ILjava/util/Enumeration;C)I",
                                            new Expressions(
                                                new StringConstantExpression(11, "aaaa"),
                                                new LocalVariableReferenceExpression(12, PrimitiveType.TYPE_INT, "b"),
                                                new NewExpression(
                                                    13,
                                                    new ObjectType("java/util/Enumeration", "java.util.Enumeration", "Enumeration"),
                                                    "()V",
                                                    new BodyDeclaration(
                                                        "java/util/Enumeration",
                                                        new MemberDeclarations(
                                                            new MethodDeclaration(
                                                                Const.ACC_PUBLIC,
                                                                "hasMoreElements",
                                                                PrimitiveType.TYPE_BOOLEAN,
                                                                "()Z",
                                                                new ReturnExpressionStatement(new BooleanExpression(15, false))
                                                            ),
                                                            new MethodDeclaration(
                                                                Const.ACC_PUBLIC,
                                                                "nextElement",
                                                                ObjectType.TYPE_OBJECT,
                                                                "()Ljava/lang/Object;",
                                                                new ReturnExpressionStatement(new NullExpression(18, ObjectType.TYPE_OBJECT))
                                                            )
                                                        )
                                                    ),
                                                    false,
                                                    false
                                                ),
                                                new LocalVariableReferenceExpression(21, PrimitiveType.TYPE_CHAR, "c")
                                            ),
                                            null
                                        )
                                    )
                                )
                            ),
                            new ExpressionStatement(
                                new MethodInvocationExpression(
                                    22,
                                    PrimitiveType.TYPE_VOID,
                                    new FieldReferenceExpression(
                                        22,
                                        printStreamType,
                                        new ObjectTypeReferenceExpression(22, new ObjectType(StringConstants.JAVA_LANG_SYSTEM, "java.lang.System", "System")),
                                        StringConstants.JAVA_LANG_SYSTEM,
                                        "out",
                                        "Ljava/io/PrintStream;"
                                    ),
                                    "java/io/PrintStream",
                                    "println",
                                    "(I)V",
                                    new LocalVariableReferenceExpression(22, PrimitiveType.TYPE_INT, "i"),
                                    null
                                )
                            )
                        )
                    )
                )
            )
        );

        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        Map<String, Object> configuration = Collections.singletonMap("realignLineNumbers", Boolean.TRUE);
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setCompilationUnit(compilationUnit);

        decompileContext.setMainInternalTypeName("org/jd/core/v1/service/test/TokenWriterTest");
        decompileContext.setLoader(new NopLoader());
        decompileContext.setPrinter(printer);
        decompileContext.setConfiguration(configuration);
        decompileContext.setMaxLineNumber(22);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        fragmenter.process(compilationUnit, decompileContext);
        layouter.process(decompileContext);
        DefaultList<Token> tokens = tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);
        writer.process(decompileContext);

        String source = printer.toString();

        System.out.println("- - - - - - - - ");
        System.out.print(source);
        System.out.println("- - - - - - - - ");

        Assert.assertNotEquals(-1, source.indexOf("/* 22: 22 */"));
        Assert.assertEquals(-1, source.indexOf("java.lang.System"));
    }

    @Test
    public void testInterfaceDeclaration() throws Exception {
        ObjectType cloneableType = new ObjectType(StringConstants.JAVA_LANG_CLONEABLE, "java.lang.Cloneable", "Cloneable");
        ObjectType stringType = new ObjectType(StringConstants.JAVA_LANG_STRING, "java.lang.String", "String");
        ObjectType listType = new ObjectType("java/util/List", "java.util.List", "List", stringType);

        CompilationUnit compilationUnit = new CompilationUnit(
            new InterfaceDeclaration(
                Const.ACC_PUBLIC,
                "org/jd/core/v1/service/test/InterfaceTest",
                "InterfaceTest",
                new Types(listType, cloneableType)
            )
        );

        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setCompilationUnit(compilationUnit);

        decompileContext.setMainInternalTypeName("org/jd/core/v1/service/test/InterfaceTest");
        decompileContext.setLoader(new NopLoader());
        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(49);
        decompileContext.setMinorVersion(0);

        fragmenter.process(compilationUnit, decompileContext);
        layouter.process(decompileContext);
        DefaultList<Token> tokens = tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);

        writer.process(decompileContext);

        String source = printer.toString();

        System.out.println("- - - - - - - - ");
        System.out.print(source);
        System.out.println("- - - - - - - - ");

        Assert.assertNotEquals(-1, source.indexOf("/*   1:   0 */ package org.jd.core.v1.service.test;"));
        Assert.assertNotEquals(-1, source.indexOf("interface InterfaceTest"));
        Assert.assertNotEquals(-1, source.indexOf("extends List"));
        Assert.assertNotEquals(-1, source.indexOf("<String"));
        Assert.assertNotEquals(-1, source.indexOf(", Cloneable"));
    }

    @Test
    public void testEnumDayDeclaration() throws Exception {
        CompilationUnit compilationUnit = new CompilationUnit(
            new EnumDeclaration(
                null,
                Const.ACC_PUBLIC,
                "org/jd/core/v1/service/test/Day",
                "Day",
                null,
                Arrays.asList(
                    new EnumDeclaration.Constant("SUNDAY"),
                    new EnumDeclaration.Constant("MONDAY"),
                    new EnumDeclaration.Constant("TUESDAY"),
                    new EnumDeclaration.Constant("WEDNESDAY"),
                    new EnumDeclaration.Constant("THURSDAY"),
                    new EnumDeclaration.Constant("FRIDAY"),
                    new EnumDeclaration.Constant("SATURDAY")
                ),
                null
            )
        );

        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setCompilationUnit(compilationUnit);

        decompileContext.setMainInternalTypeName("org/jd/core/v1/service/test/Day");
        decompileContext.setLoader(new NopLoader());
        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        fragmenter.process(compilationUnit, decompileContext);
        layouter.process(decompileContext);
        DefaultList<Token> tokens = tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);

        writer.process(decompileContext);

        String source = printer.toString();

        System.out.println("- - - - - - - - ");
        System.out.print(source);
        System.out.println("- - - - - - - - ");

        Assert.assertTrue(source.indexOf(", MONDAY") != -1 || source.indexOf("-->MONDAY") != -1);
    }

    @Test
    @SuppressFBWarnings
    @SuppressWarnings("all")
    public void testEnumPlanetDeclaration() throws Exception {
        Type cloneableType = new ObjectType(StringConstants.JAVA_LANG_CLONEABLE, "java.lang.Cloneable", "Cloneable");
        Type stringType = ObjectType.TYPE_STRING;
        Type arrayOfStringType = stringType.createType(1);
        Type listType = new ObjectType("java/util/List", "java.util.List", "List", stringType);
        Type printStreamType = new ObjectType("java/io/PrintStream", "java.io.PrintStream", "PrintStream");
        Type planetType = new ObjectType("org/jd/core/v1/service/test/Planet", "org.jd.core.v1.service.test.Planet", "Planet");
        ThisExpression thisExpression = new ThisExpression(new ObjectType("org/jd/core/v1/service/test/Planet", "org.jd.core.v1.service.test.Planet", "Planet"));

        CompilationUnit compilationUnit = new CompilationUnit(
            new EnumDeclaration(
                ACC_PUBLIC,
                "org/jd/core/v1/service/test/Planet",
                "Planet",
                Arrays.asList(
                    new EnumDeclaration.Constant(
                        "MERCURY",
                        new Expressions(
                            new DoubleConstantExpression(3.303e+23),
                            new DoubleConstantExpression(2.4397e6)
                        )
                    ),
                    new EnumDeclaration.Constant(
                        "VENUS",
                        new Expressions(
                            new DoubleConstantExpression(4.869e+24),
                            new DoubleConstantExpression(6.0518e6)
                        )
                    ),
                    new EnumDeclaration.Constant(
                        "EARTH",
                        new Expressions(
                            new DoubleConstantExpression(5.976e+24),
                            new DoubleConstantExpression(6.37814e6)
                        )
                    )
                ),
                new BodyDeclaration(
                    "org/jd/core/v1/service/test/Planet",
                    new MemberDeclarations(
                        new FieldDeclaration(ACC_PRIVATE | ACC_FINAL, PrimitiveType.TYPE_DOUBLE, new FieldDeclarator("mass")),
                        new FieldDeclaration(ACC_PRIVATE | ACC_FINAL, PrimitiveType.TYPE_DOUBLE, new FieldDeclarator("radius")),
                        new ConstructorDeclaration(
                            0,
                            new FormalParameters(
                                new FormalParameter(PrimitiveType.TYPE_DOUBLE, "mass"),
                                new FormalParameter(PrimitiveType.TYPE_DOUBLE, "radius")
                            ),
                            "(DD)V",
                            new Statements(
                                new ExpressionStatement(new BinaryOperatorExpression(
                                    0,
                                    PrimitiveType.TYPE_DOUBLE,
                                    new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "mass", "D"),
                                    "=",
                                    new LocalVariableReferenceExpression(PrimitiveType.TYPE_DOUBLE, "mass"),
                                    16
                                )),
                                new ExpressionStatement(new BinaryOperatorExpression(
                                    0,
                                    PrimitiveType.TYPE_DOUBLE,
                                    new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "radius", "D"),
                                    "=",
                                    new LocalVariableReferenceExpression(PrimitiveType.TYPE_DOUBLE, "radius"),
                                    16
                                ))
                            )
                        ),
                        new MethodDeclaration(
                            ACC_PRIVATE,
                            "mass",
                            PrimitiveType.TYPE_DOUBLE,
                            "()D",
                            new ReturnExpressionStatement(new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "mass", "D"))
                        ),
                        new MethodDeclaration(
                            ACC_PRIVATE,
                            "radius",
                            PrimitiveType.TYPE_DOUBLE,
                            "()D",
                            new ReturnExpressionStatement(new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "radius", "D"))
                        ),
                        new FieldDeclaration(
                            ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                            PrimitiveType.TYPE_DOUBLE,
                            new FieldDeclarator("G", new ExpressionVariableInitializer(new DoubleConstantExpression(6.67300E-11)))
                        ),
                        new MethodDeclaration(
                            0,
                            "surfaceGravity",
                            PrimitiveType.TYPE_DOUBLE,
                            "()D",
                            new ReturnExpressionStatement(
                                new BinaryOperatorExpression(
                                    0,
                                    PrimitiveType.TYPE_DOUBLE,
                                    new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "G", "D"),
                                    "*",
                                    new BinaryOperatorExpression(
                                        0,
                                        PrimitiveType.TYPE_DOUBLE,
                                        new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "mass", "D"),
                                        "/",
                                        new ParenthesesExpression(new BinaryOperatorExpression(
                                            0,
                                            PrimitiveType.TYPE_DOUBLE,
                                            new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "radius", "D"),
                                            "*",
                                            new FieldReferenceExpression(PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "radius", "D"),
                                            4
                                        )),
                                        4
                                    ),
                                    4
                                )
                            )
                        ),
                        new MethodDeclaration(
                            0,
                            "surfaceWeight",
                            PrimitiveType.TYPE_DOUBLE,
                            new FormalParameter(PrimitiveType.TYPE_DOUBLE, "otherMass"),
                            "(D)D",
                            new ReturnExpressionStatement(
                                new BinaryOperatorExpression(
                                    0,
                                    PrimitiveType.TYPE_DOUBLE,
                                    new LocalVariableReferenceExpression(PrimitiveType.TYPE_DOUBLE, "otherMass"),
                                    "*",
                                    new MethodInvocationExpression(
                                        PrimitiveType.TYPE_DOUBLE, thisExpression, "org/jd/core/v1/service/test/Planet", "surfaceGravity", "()D", null
                                    ),
                                    4
                                )
                            )
                        ),
                        new MethodDeclaration(
                            ACC_PUBLIC | ACC_STATIC,
                            "surfaceWeight",
                            PrimitiveType.TYPE_VOID,
                            new FormalParameter(arrayOfStringType, "args"),
                            "([Ljava/lan/String;)V",
                            new Statements(
                                new IfStatement(
                                    new BinaryOperatorExpression(
                                        0,
                                        PrimitiveType.TYPE_BOOLEAN,
                                        new LengthExpression(new LocalVariableReferenceExpression(arrayOfStringType, "args")),
                                        "!=",
                                        new IntegerConstantExpression(PrimitiveType.TYPE_INT, 1),
                                        9
                                    ),
                                    new Statements(
                                        new ExpressionStatement(new MethodInvocationExpression(
                                            PrimitiveType.TYPE_VOID,
                                            new FieldReferenceExpression(
                                                printStreamType,
                                                new ObjectTypeReferenceExpression(new ObjectType(StringConstants.JAVA_LANG_SYSTEM, "java.lang.System", "System")),
                                                StringConstants.JAVA_LANG_SYSTEM,
                                                "out",
                                                "Ljava/io/PrintStream;"
                                            ),
                                            "java/io/PrintStream",
                                            "println",
                                            "(Ljava/lang/String;)V",
                                            new StringConstantExpression("Usage: java Planet <earth_weight>"),
                                            null
                                        )),
                                        new ExpressionStatement(new MethodInvocationExpression(
                                            PrimitiveType.TYPE_VOID,
                                            new ObjectTypeReferenceExpression(new ObjectType(StringConstants.JAVA_LANG_SYSTEM, "java.lang.System", "System")),
                                            StringConstants.JAVA_LANG_SYSTEM,
                                            "exit",
                                            "(I)V",
                                            new IntegerConstantExpression(PrimitiveType.TYPE_INT, -1),
                                            null
                                        ))
                                    )
                                ),
                                new LocalVariableDeclarationStatement(
                                    PrimitiveType.TYPE_DOUBLE,
                                    new LocalVariableDeclarator(0, "earthWeight", new ExpressionVariableInitializer(
                                        new MethodInvocationExpression(
                                            PrimitiveType.TYPE_DOUBLE,
                                            new ObjectTypeReferenceExpression(new ObjectType(StringConstants.JAVA_LANG_DOUBLE, "java.lang.Double", "Double")),
                                            StringConstants.JAVA_LANG_DOUBLE,
                                            "parseDouble",
                                            "(Ljava/lang/String;)D",
                                            new ArrayExpression(
                                                new LocalVariableReferenceExpression(arrayOfStringType, "args"),
                                                new IntegerConstantExpression(PrimitiveType.TYPE_INT, 0)
                                            ),
                                            null
                                        )
                                    ))
                                ),
                                new LocalVariableDeclarationStatement(
                                    PrimitiveType.TYPE_DOUBLE,
                                    new LocalVariableDeclarator(0, "mass", new ExpressionVariableInitializer(
                                        new BinaryOperatorExpression(
                                            0,
                                            PrimitiveType.TYPE_DOUBLE,
                                            new LocalVariableReferenceExpression(PrimitiveType.TYPE_DOUBLE, "earthWeight"),
                                            "/",
                                            new MethodInvocationExpression(
                                                PrimitiveType.TYPE_DOUBLE,
                                                new FieldReferenceExpression(
                                                    planetType,
                                                    new ObjectTypeReferenceExpression(new ObjectType("org/jd/core/v1/service/test/Planet", "org.jd.core.v1.service.test.Planet", "Planet")),
                                                    "org/jd/core/v1/service/test/Planet",
                                                    "EARTH",
                                                    "org/jd/core/v1/service/test/Planet"),
                                                "org/jd/core/v1/service/test/Planet",
                                                "surfaceGravity",
                                                "()D",
                                                null
                                            ),
                                            4
                                        )
                                    ))
                                ),
                                new ForEachStatement(
                                    planetType,
                                    "p",
                                    new MethodInvocationExpression(
                                        planetType,
                                        new ObjectTypeReferenceExpression(new ObjectType("org/jd/core/v1/service/test/Planet", "org.jd.core.v1.service.test.Planet", "Planet")),
                                        "org/jd/core/v1/service/test/Planet",
                                        "values",
                                        "()[Lorg/jd/core/v1/service/test/Planet;", 
                                        null),
                                    new ExpressionStatement(new MethodInvocationExpression(
                                        PrimitiveType.TYPE_VOID,
                                        new FieldReferenceExpression(
                                            printStreamType,
                                            new ObjectTypeReferenceExpression(new ObjectType(StringConstants.JAVA_LANG_SYSTEM, "java.lang.System", "System")),
                                            StringConstants.JAVA_LANG_SYSTEM,
                                            "out",
                                            "Ljava/io/PrintStream;"
                                        ),
                                        "java/io/PrintStream",
                                        "printf",
                                        "(Ljava/lang/String;[Ljava/lang/Object;)V",
                                        new Expressions(
                                            new StringConstantExpression("Your weight on %s is %f%n"),
                                            new LocalVariableReferenceExpression(planetType, "p"),
                                            new MethodInvocationExpression(
                                                PrimitiveType.TYPE_DOUBLE,
                                                new LocalVariableReferenceExpression(planetType, "p"),
                                                "org/jd/core/v1/service/test/Planet",
                                                "surfaceWeight",
                                                "(D)D",
                                                new LocalVariableReferenceExpression(PrimitiveType.TYPE_DOUBLE, "mass"),
                                                null
                                            )
                                        ),
                                        null
                                    ))
                                )
                            )
                        )
                    )
                )
            )
        );

        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setCompilationUnit(compilationUnit);

        decompileContext.setMainInternalTypeName("org/jd/core/v1/service/test/Planet");
        decompileContext.setLoader(new NopLoader());
        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        fragmenter.process(compilationUnit, decompileContext);
        DefaultList<Token> tokens = tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);

        //tokenizer.process(message);
        tokens = new JavaFragmentToTokenProcessor().process(decompileContext.getBody());
        writer.process(decompileContext);

        String source = printer.toString();

        System.out.println("- - - - - - - - ");
        System.out.print(source);
        System.out.println("- - - - - - - - ");
    }

    @Test
    public void testSwitch() throws Exception {
        CompilationUnit compilationUnit = new CompilationUnit(
            new ClassDeclaration(
                Const.ACC_PUBLIC,
                "org/jd/core/v1/service/test/SwitchTest",
                "SwitchTest",
                new BodyDeclaration(
                    "org/jd/core/v1/service/test/SwitchTest",
                    new MethodDeclaration(
                        Const.ACC_PUBLIC | Const.ACC_STATIC,
                        "translate",
                        PrimitiveType.TYPE_INT,
                        new FormalParameter(PrimitiveType.TYPE_INT, "i"),
                        "(I)Ljava/lang/String;",
                        new SwitchStatement(
                            new LocalVariableReferenceExpression(PrimitiveType.TYPE_INT, "i"),
                            Arrays.asList(
                                (SwitchStatement.Block)new SwitchStatement.LabelBlock(
                                    new SwitchStatement.ExpressionLabel(new IntegerConstantExpression(PrimitiveType.TYPE_INT, 0)),
                                    new Statements(
                                        new LocalVariableDeclarationStatement(
                                            ObjectType.TYPE_STRING,
                                            new LocalVariableDeclarator(0, "zero", new ExpressionVariableInitializer(new StringConstantExpression("zero")))
                                        ),
                                        new ReturnExpressionStatement(new LocalVariableReferenceExpression(ObjectType.TYPE_STRING, "zero"))
                                    )
                                ),
                                new SwitchStatement.MultiLabelsBlock(
                                    Arrays.asList(
                                        (SwitchStatement.Label)new SwitchStatement.ExpressionLabel(new IntegerConstantExpression(PrimitiveType.TYPE_INT, 1)),
                                        new SwitchStatement.ExpressionLabel(new IntegerConstantExpression(PrimitiveType.TYPE_INT, 2))
                                    ),
                                    new ReturnExpressionStatement(new StringConstantExpression("one or two"))
                                ),
                                new SwitchStatement.LabelBlock(
                                    SwitchStatement.DEFAULT_LABEL,
                                    new ReturnExpressionStatement(new StringConstantExpression("other"))
                                )
                            )
                        )
                    )
                )
            )
        );

        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setCompilationUnit(compilationUnit);

        decompileContext.setMainInternalTypeName("org/jd/core/v1/service/test/SwitchTest");
        decompileContext.setLoader(new NopLoader());
        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        fragmenter.process(compilationUnit, decompileContext);
        layouter.process(decompileContext);
        DefaultList<Token> tokens = tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);

        writer.process(decompileContext);

        String source = printer.toString();

        System.out.println("- - - - - - - - ");
        System.out.print(source);
        System.out.println("- - - - - - - - ");

        Assert.assertNotEquals(-1, source.indexOf("switch (i)"));
    }

    @Test
    public void testBridgeAndSyntheticAttributes() throws Exception {
        CompilationUnit compilationUnit = new CompilationUnit(
            new ClassDeclaration(
                Const.ACC_PUBLIC,
                "org/jd/core/v1/service/test/SyntheticAttributeTest",
                "SyntheticAttributeTest",
                new BodyDeclaration(
                    "org/jd/core/v1/service/test/SyntheticAttributeTest",
                    new MemberDeclarations(
                        new FieldDeclaration(
                            Const.ACC_PUBLIC|Const.ACC_BRIDGE,
                            PrimitiveType.TYPE_INT,
                            new FieldDeclarator("i")
                        ),
                        new MethodDeclaration(
                            Const.ACC_PUBLIC|Const.ACC_BRIDGE,
                            "testBridgeAttribute",
                            PrimitiveType.TYPE_VOID,
                            "()V"
                        ),
                        new MethodDeclaration(
                            Const.ACC_PUBLIC|Const.ACC_SYNTHETIC,
                            "testSyntheticAttribute",
                            PrimitiveType.TYPE_VOID,
                            "()V"
                        )
                    )
                )
            )
        );

        PlainTextMetaPrinter printer = new PlainTextMetaPrinter();
        DecompileContext decompileContext = new DecompileContext();
        decompileContext.setCompilationUnit(compilationUnit);

        decompileContext.setMainInternalTypeName("org/jd/core/v1/service/test/SyntheticAttributeTest");
        decompileContext.setLoader(new NopLoader());
        decompileContext.setPrinter(printer);
        decompileContext.setMaxLineNumber(0);
        decompileContext.setMajorVersion(0);
        decompileContext.setMinorVersion(0);

        fragmenter.process(compilationUnit, decompileContext);
        layouter.process(decompileContext);
        DefaultList<Token> tokens = tokenizer.process(decompileContext.getBody());
        decompileContext.setTokens(tokens);

        writer.process(decompileContext);

        String source = printer.toString();

        System.out.println("- - - - - - - - ");
        System.out.print(source);
        System.out.println("- - - - - - - - ");

        Assert.assertEquals(-1, source.indexOf("/* bridge */"));
        Assert.assertEquals(-1, source.indexOf("/* synthetic */"));
    }
}
