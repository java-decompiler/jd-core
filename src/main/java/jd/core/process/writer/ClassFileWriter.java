/**
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package jd.core.process.writer;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.Annotation;
import jd.core.model.classfile.attribute.AttributeSignature;
import jd.core.model.classfile.attribute.ElementValue;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;
import jd.core.model.layout.block.AnnotationsLayoutBlock;
import jd.core.model.layout.block.BlockLayoutBlock;
import jd.core.model.layout.block.ByteCodeLayoutBlock;
import jd.core.model.layout.block.CaseEnumLayoutBlock;
import jd.core.model.layout.block.CaseLayoutBlock;
import jd.core.model.layout.block.DeclareLayoutBlock;
import jd.core.model.layout.block.ExtendsSuperInterfacesLayoutBlock;
import jd.core.model.layout.block.ExtendsSuperTypeLayoutBlock;
import jd.core.model.layout.block.FastCatchLayoutBlock;
import jd.core.model.layout.block.FieldNameLayoutBlock;
import jd.core.model.layout.block.GenericExtendsSuperInterfacesLayoutBlock;
import jd.core.model.layout.block.GenericExtendsSuperTypeLayoutBlock;
import jd.core.model.layout.block.GenericImplementsInterfacesLayoutBlock;
import jd.core.model.layout.block.GenericTypeNameLayoutBlock;
import jd.core.model.layout.block.ImplementsInterfacesLayoutBlock;
import jd.core.model.layout.block.ImportsLayoutBlock;
import jd.core.model.layout.block.InstructionLayoutBlock;
import jd.core.model.layout.block.InstructionsLayoutBlock;
import jd.core.model.layout.block.LambdaMethodLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.model.layout.block.LayoutBlockConstants;
import jd.core.model.layout.block.MarkerLayoutBlock;
import jd.core.model.layout.block.MethodNameLayoutBlock;
import jd.core.model.layout.block.MethodStaticLayoutBlock;
import jd.core.model.layout.block.OffsetLayoutBlock;
import jd.core.model.layout.block.PackageLayoutBlock;
import jd.core.model.layout.block.ThrowsLayoutBlock;
import jd.core.model.layout.block.TypeNameLayoutBlock;
import jd.core.model.reference.Reference;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.InstructionPrinter;
import jd.core.printer.Printer;
import jd.core.process.writer.visitor.SourceWriterVisitor;
import jd.core.util.SignatureUtil;
import jd.core.util.StringUtil;
import jd.core.util.TypeNameUtil;

public final class ClassFileWriter
{
    private static final String BREAK = "break";

    private static final String DEFAULT = "default";

    private static final String EXTENDS = "extends";

    private static final String IMPLEMENTS = "implements";

    private static final String JAVADOC_DEPRECATED = "@deprecated";

    private static final String STRICTFP = "strictfp";

    private static final String ABSTRACT = "abstract";

    private static final String NATIVE = "native";

    private static final String SYNCHRONIZED = "synchronized";

    private static final String TRANSIENT = "transient";

    private static final String VOLATILE = "volatile";

    private static final String FINAL = "final";

    private static final String STATIC = "static";

    private static final String PROTECTED = "protected";

    private static final String PRIVATE = "private";

    private static final String PUBLIC = "public";

    private static final Set<String> keywords;

    private static final String[] ACCESS_FIELD_NAMES = {
        PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, null, VOLATILE,
        TRANSIENT
    };

    private static final String[] ACCESS_METHOD_NAMES = {
        PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, SYNCHRONIZED,
        null, null, NATIVE, null, ABSTRACT, STRICTFP
    };

    private static final String[] ACCESS_NESTED_CLASS_NAMES = {
        PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL
    };

    private static final String[] ACCESS_NESTED_ENUM_NAMES = {
        PUBLIC, PRIVATE, PROTECTED, STATIC
    };

    private final Loader loader;
    private final Printer printer;
    private final InstructionPrinter instructionPrinter;
    private final SourceWriterVisitor visitor;
    private final ReferenceMap referenceMap;
    private final List<LayoutBlock> layoutBlockList;
    private int index;
    private boolean addSpace;

    public static void write(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        int maxLineNumber, int majorVersion, int minorVersion,
        List<LayoutBlock> layoutBlockList)
    {
        ClassFileWriter cfw = new ClassFileWriter(
            loader, printer, referenceMap, layoutBlockList);

        cfw.write(maxLineNumber, majorVersion, minorVersion);
    }

    private ClassFileWriter(
        Loader loader, Printer printer, ReferenceMap referenceMap,
        List<LayoutBlock> layoutBlockList)
    {
        this.loader = loader;
        this.printer = printer;
        this.instructionPrinter = new InstructionPrinter(printer);
        this.visitor = new SourceWriterVisitor(
            loader, this.instructionPrinter, referenceMap, keywords);
        this.referenceMap = referenceMap;
        this.layoutBlockList = layoutBlockList;
        this.index = 0;
    }

    public void write(
        int maxLineNumber, int majorVersion, int minorVersion)
    {
        int length = layoutBlockList.size();

        this.printer.start(maxLineNumber, majorVersion, minorVersion);
        this.printer.startOfLine(searchFirstLineNumber());

        LayoutBlock lb;
        while (this.index < length)
        {
            lb = this.layoutBlockList.get(this.index++);

            switch (lb.getTag())
            {
            case LayoutBlockConstants.PACKAGE:
                writePackage((PackageLayoutBlock)lb);
                break;
            case LayoutBlockConstants.SEPARATOR_AT_BEGINING:
                writeSeparatorAtBegining(lb);
                break;
            case LayoutBlockConstants.SEPARATOR,
                 LayoutBlockConstants.SEPARATOR_OF_STATEMENTS,
                 LayoutBlockConstants.SEPARATOR_AFTER_IMPORTS:
                writeSeparator(lb);
                break;
            case LayoutBlockConstants.IMPORTS:
                writeImports((ImportsLayoutBlock)lb);
                break;
            case LayoutBlockConstants.TYPE_MARKER_START:
                writeTypeMarkerStart((MarkerLayoutBlock)lb);
                break;
            case LayoutBlockConstants.TYPE_MARKER_END:
                writeTypeMarkerEnd();
                break;
            case LayoutBlockConstants.FIELD_MARKER_START:
                writeFieldMarkerStart((MarkerLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FIELD_MARKER_END:
                writeFieldMarkerEnd();
                break;
            case LayoutBlockConstants.METHOD_MARKER_START:
                writeMethodMarkerStart((MarkerLayoutBlock)lb);
                break;
            case LayoutBlockConstants.METHOD_MARKER_END:
                writeMethodMarkerEnd();
                break;
            case LayoutBlockConstants.COMMENT_DEPRECATED:
                writeCommentDeprecated(lb);
                break;
            case LayoutBlockConstants.COMMENT_ERROR:
                writeCommentError(lb);
                break;
            case LayoutBlockConstants.ANNOTATIONS:
                writeAnnotations((AnnotationsLayoutBlock)lb);
                break;
            case LayoutBlockConstants.TYPE_NAME:
                writeType((TypeNameLayoutBlock)lb);
                break;
            case LayoutBlockConstants.EXTENDS_SUPER_TYPE:
                writeExtendsSuperType((ExtendsSuperTypeLayoutBlock)lb);
                break;
            case LayoutBlockConstants.EXTENDS_SUPER_INTERFACES:
                writeExtendsSuperInterfaces(
                    (ExtendsSuperInterfacesLayoutBlock)lb);
                break;
            case LayoutBlockConstants.IMPLEMENTS_INTERFACES:
                writeImplementsInterfaces((ImplementsInterfacesLayoutBlock)lb);
                break;
            case LayoutBlockConstants.GENERIC_TYPE_NAME:
                writeGenericType((GenericTypeNameLayoutBlock)lb);
                break;
            case LayoutBlockConstants.GENERIC_EXTENDS_SUPER_TYPE:
                writeGenericExtendsSuperType(
                    (GenericExtendsSuperTypeLayoutBlock)lb);
                break;
            case LayoutBlockConstants.GENERIC_EXTENDS_SUPER_INTERFACES:
                writeGenericExtendsSuperInterfaces(
                    (GenericExtendsSuperInterfacesLayoutBlock)lb);
                break;
            case LayoutBlockConstants.GENERIC_IMPLEMENTS_INTERFACES:
                writeGenericImplementsInterfaces(
                    (GenericImplementsInterfacesLayoutBlock)lb);
                break;
            case LayoutBlockConstants.TYPE_BODY_BLOCK_START,
                 LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START,
                 LayoutBlockConstants.METHOD_BODY_BLOCK_START,
                 LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START,
                 LayoutBlockConstants.STATEMENTS_BLOCK_START:
                writeStatementBlockStart(lb);
                break;
            case LayoutBlockConstants.TYPE_BODY_BLOCK_END,
                 LayoutBlockConstants.METHOD_BODY_BLOCK_END,
                 LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_END,
                 LayoutBlockConstants.STATEMENTS_BLOCK_END:
                writeStatementsBlockEnd(lb);
                break;
            case LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_END:
                writeStatementsInnerBodyBlockEnd(lb);
                break;
            case LayoutBlockConstants.TYPE_BODY_BLOCK_START_END,
                 LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START_END,
                 LayoutBlockConstants.METHOD_BODY_BLOCK_START_END,
                 LayoutBlockConstants.STATEMENTS_BLOCK_START_END:
                writeStatementsBlockStartEnd(lb);
                break;
            case LayoutBlockConstants.SWITCH_BLOCK_START:
                writeSwitchBlockStart(lb);
                break;
            case LayoutBlockConstants.SWITCH_BLOCK_END:
                writeSwitchBlockEnd(lb);
                break;
            case LayoutBlockConstants.CASE_BLOCK_START:
                writeCaseBlockStart(lb);
                break;
            case LayoutBlockConstants.CASE_BLOCK_END:
                writeCaseBlockEnd(lb);
                break;
            case LayoutBlockConstants.FOR_BLOCK_START:
                writeForBlockStart(lb);
                break;
            case LayoutBlockConstants.FOR_BLOCK_END:
                writeForBlockEnd();
                break;
            case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_START:
                writeSingleStatementsBlockStart(lb);
                break;
            case LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END:
                writeSingleStatementsBlockEnd(lb);
                break;
            case LayoutBlockConstants.SINGLE_STATEMENTS_BLOCK_START_END:
                writeSingleStatementsBlockStartEnd(lb);
                break;
            case LayoutBlockConstants.FIELD_NAME:
                writeField((FieldNameLayoutBlock)lb);
                break;
            case LayoutBlockConstants.METHOD_STATIC:
                writeMethodStatic((MethodStaticLayoutBlock)lb);
                break;
            case LayoutBlockConstants.METHOD_NAME:
                writeMethod((MethodNameLayoutBlock)lb);
                break;
            case LayoutBlockConstants.THROWS:
                writeThrows((ThrowsLayoutBlock)lb);
                break;
            case LayoutBlockConstants.INSTRUCTION:
                writeInstruction((InstructionLayoutBlock)lb);
                break;
            case LayoutBlockConstants.INSTRUCTIONS:
                writeInstructions((InstructionsLayoutBlock)lb);
                break;
            case LayoutBlockConstants.BYTE_CODE:
                writeByteCode((ByteCodeLayoutBlock)lb);
                break;
            case LayoutBlockConstants.DECLARE:
                writeDeclaration((DeclareLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FRAGMENT_IF:
                writeIf();
                break;
            case LayoutBlockConstants.FRAGMENT_WHILE:
                writeWhile();
                break;
            case LayoutBlockConstants.FRAGMENT_FOR:
                writeFor();
                break;
            case LayoutBlockConstants.FRAGMENT_SWITCH:
                writeSwitch();
                break;
            case LayoutBlockConstants.FRAGMENT_CASE:
                writeCase((CaseLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FRAGMENT_CASE_ENUM:
                writeCaseEnum((CaseEnumLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FRAGMENT_CASE_STRING:
                writeCaseString((CaseLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FRAGMENT_CATCH:
                writeCatch((FastCatchLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FRAGMENT_SYNCHRONIZED:
                writeSynchronized();
                break;
            case LayoutBlockConstants.STATEMENT_LABEL:
                writeLabel((OffsetLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FRAGMENT_ELSE:
                writeElse();
                break;
            case LayoutBlockConstants.FRAGMENT_ELSE_SPACE:
                writeElseSpace();
                break;
            case LayoutBlockConstants.FRAGMENT_DO:
                writeDo();
                break;
            case LayoutBlockConstants.FRAGMENT_INFINITE_LOOP:
                writeInfiniteLoop();
                break;
            case LayoutBlockConstants.FRAGMENT_TRY:
                writeTry();
                break;
            case LayoutBlockConstants.FRAGMENT_FINALLY:
                writeFinally();
                break;
            case LayoutBlockConstants.FRAGMENT_CONTINUE:
                writeContinue();
                break;
            case LayoutBlockConstants.FRAGMENT_BREAK:
                writeBreak();
                break;
            case LayoutBlockConstants.FRAGMENT_LABELED_BREAK:
                writeLabeledBreak((OffsetLayoutBlock)lb);
                break;
            case LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET:
                writeRightRoundBracket();
                break;
            case LayoutBlockConstants.FRAGMENT_RIGHT_ROUND_BRACKET_SEMICOLON:
                writeRightRoundBracketSemicolon();
                break;
            case LayoutBlockConstants.FRAGMENT_SEMICOLON:
                writeSemicolon();
                break;
            case LayoutBlockConstants.FRAGMENT_SEMICOLON_SPACE:
                writeSemicolonSpace();
                break;
            case LayoutBlockConstants.FRAGMENT_SPACE_COLON_SPACE:
                writeSpaceColonSpace();
                break;
            case LayoutBlockConstants.FRAGMENT_COMA_SPACE:
                writeComaSpace();
                break;
            case LayoutBlockConstants.FRAGMENT_ARROW:
                writeArrow();
                break;
            }
        }

        this.printer.endOfLine();
        this.printer.end();
    }

    private int searchFirstLineNumber()
    {
        int i = this.index;
        int length = this.layoutBlockList.size();

        LayoutBlock lb;
        while (i < length)
        {
            lb = this.layoutBlockList.get(i);
            i++;

            if (lb.getTag() == LayoutBlockConstants.INSTRUCTION
                    || lb.getTag() == LayoutBlockConstants.INSTRUCTIONS) {
                return lb.getFirstLineNumber();
            }
            if ((lb.getTag() == LayoutBlockConstants.SEPARATOR
                    || lb.getTag() == LayoutBlockConstants.SEPARATOR_AT_BEGINING
                    || lb.getTag() == LayoutBlockConstants.SEPARATOR_AFTER_IMPORTS
                    || lb.getTag() == LayoutBlockConstants.SEPARATOR_OF_STATEMENTS
                    || lb.getTag() == LayoutBlockConstants.IMPORTS
                    || lb.getTag() == LayoutBlockConstants.COMMENT_DEPRECATED
                    || lb.getTag() == LayoutBlockConstants.ANNOTATIONS
                    || lb.getTag() == LayoutBlockConstants.EXTENDS_SUPER_TYPE
                    || lb.getTag() == LayoutBlockConstants.EXTENDS_SUPER_INTERFACES
                    || lb.getTag() == LayoutBlockConstants.IMPLEMENTS_INTERFACES
                    || lb.getTag() == LayoutBlockConstants.GENERIC_EXTENDS_SUPER_TYPE
                    || lb.getTag() == LayoutBlockConstants.GENERIC_EXTENDS_SUPER_INTERFACES
                    || lb.getTag() == LayoutBlockConstants.GENERIC_IMPLEMENTS_INTERFACES
                    || lb.getTag() == LayoutBlockConstants.TYPE_BODY_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.TYPE_BODY_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.TYPE_BODY_BLOCK_START_END
                    || lb.getTag() == LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.INNER_TYPE_BODY_BLOCK_START_END
                    || lb.getTag() == LayoutBlockConstants.METHOD_BODY_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.METHOD_BODY_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.METHOD_BODY_BLOCK_START_END
                    || lb.getTag() == LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.METHOD_BODY_SINGLE_LINE_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.STATEMENTS_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.STATEMENTS_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.STATEMENTS_BLOCK_START_END
                    || lb.getTag() == LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.SINGLE_STATEMENT_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.SINGLE_STATEMENTS_BLOCK_START_END
                    || lb.getTag() == LayoutBlockConstants.SWITCH_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.SWITCH_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.CASE_BLOCK_START
                    || lb.getTag() == LayoutBlockConstants.CASE_BLOCK_END
                    || lb.getTag() == LayoutBlockConstants.FRAGMENT_CASE
                    || lb.getTag() == LayoutBlockConstants.FRAGMENT_CASE_ENUM
                    || lb.getTag() == LayoutBlockConstants.FRAGMENT_CASE_STRING) && lb.getLineCount() > 0) {
                return Printer.UNKNOWN_LINE_NUMBER;
            }
        }

        return Printer.UNKNOWN_LINE_NUMBER;
    }

    private void writePackage(PackageLayoutBlock plb)
    {
        this.printer.printKeyword("package");
        this.printer.print(' ');
        String internalPackageName = plb.getClassFile().getInternalPackageName();
        this.printer.print(
            internalPackageName.replace(
                StringConstants.INTERNAL_PACKAGE_SEPARATOR,
                StringConstants.PACKAGE_SEPARATOR));
        this.printer.print(';');
    }

    private void writeSeparatorAtBegining(LayoutBlock slb)
    {
        int lineCount = slb.getLineCount();

        this.printer.debugStartOfSeparatorLayoutBlock();
        //DEBUG this.printer.print('^');

        if (lineCount > 0)
        {
            endOfLine();

            if (lineCount > 1)
            {
                this.printer.extraLine(lineCount-1);
            }

            this.printer.startOfLine(
                searchFirstLineNumber());
        }

        this.printer.debugEndOfSeparatorLayoutBlock(
            slb.getMinimalLineCount(), slb.getLineCount(), slb.getMaximalLineCount());
    }

    private void writeSeparator(LayoutBlock slb)
    {
        int lineCount = slb.getLineCount();

        this.printer.debugStartOfSeparatorLayoutBlock();
        //DEBUG this.printer.print('^');

        if (lineCount > 0)
        {
            endOfLine();

            if (lineCount > 1)
            {
                this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                endOfLine();

                if (lineCount > 2)
                {
                    this.printer.extraLine(lineCount-2);
                }
            }

            this.printer.startOfLine(
                searchFirstLineNumber());
        }
        else
        {
            this.printer.print(' ');
            this.addSpace = false;
        }

        this.printer.debugEndOfSeparatorLayoutBlock(
            slb.getMinimalLineCount(), slb.getLineCount(), slb.getMaximalLineCount());
    }

    private void writeImports(ImportsLayoutBlock ilb)
    {
        Collection<Reference> collection = this.referenceMap.values();
        int length = collection.size();

        if (length > 0)
        {
            ClassFile classFile = ilb.getClassFile();
            String internalPackageName = classFile.getInternalPackageName();

            Iterator<Reference> iterator = collection.iterator();
            List<Reference> references =
                new ArrayList<>(length);

            String internalReferencePackageName;
            // Filtrage
            while (iterator.hasNext())
            {
                Reference reference = iterator.next();
                internalReferencePackageName = TypeNameUtil.internalTypeNameToInternalPackageName(
                    reference.getInternalName());

                // No import for same package classes
                // No import for 'java/lang' classes
                if (internalReferencePackageName.equals(internalPackageName) || StringConstants.INTERNAL_JAVA_LANG_PACKAGE_NAME.equals(
                        internalReferencePackageName))
                {
                    // TODO
                    continue;
                }

                references.add(reference);
            }

            // Reduction
            if (!references.isEmpty())
            {
                int delta = ilb.getPreferedLineCount() - ilb.getLineCount();

                if (delta > 0)
                {
                    Collections.sort(references, Comparator.comparing(Reference::getCounter).reversed());

                    int idx = references.size();

                    Reference reference;
                    while (delta-- > 0) {
                        idx--;
                        reference = references.remove(idx);
                        // Modification de 'ReferenceMap'
                        this.referenceMap.remove(reference.getInternalName());
                    }
                }

                // Affichage
                if (!references.isEmpty())
                {
                    Collections.sort(
                        references, Comparator.comparing(Reference::getInternalName));

                    this.printer.debugStartOfLayoutBlock();
                    this.printer.startOfImportStatements();
                    iterator = references.iterator();

                    if (iterator.hasNext())
                    {
                        writeImport(iterator.next());

                        while (iterator.hasNext())
                        {
                            endOfLine();
                            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                            writeImport(iterator.next());
                        }
                    }

                    this.printer.endOfImportStatements();
                    this.printer.debugEndOfLayoutBlock();
                }
            }
        }
    }

    private void writeImport(Reference reference)
    {
        this.printer.printKeyword("import");
        this.printer.print(' ');

        this.printer.printTypeImport(
            reference.getInternalName(),
            TypeNameUtil.internalTypeNameToQualifiedTypeName(
                reference.getInternalName()));

//        this.printer.print(':');
//        this.printer.print(reference.getCounter());
        this.printer.print(';');
    }

    private void writeTypeMarkerStart(MarkerLayoutBlock mlb)
    {
        String internalPath = mlb.getClassFile().getThisClassName() + StringConstants.CLASS_FILE_SUFFIX;
        this.printer.startOfTypeDeclaration(internalPath);
        this.printer.debugMarker("&lt;T&lt;");
    }
    private void writeTypeMarkerEnd()
    {
        this.printer.debugMarker("&gt;T&gt;");
        this.printer.endOfTypeDeclaration();
    }

    private void writeFieldMarkerStart(MarkerLayoutBlock mlb)
    {
        String internalPath = mlb.getClassFile().getThisClassName() + StringConstants.CLASS_FILE_SUFFIX;
        this.printer.startOfTypeDeclaration(internalPath);
        this.printer.debugMarker("&lt;F&lt;");
    }
    private void writeFieldMarkerEnd()
    {
        this.printer.debugMarker("&gt;F&gt;");
        this.printer.endOfTypeDeclaration();
    }

    private void writeMethodMarkerStart(MarkerLayoutBlock mlb)
    {
        String internalPath = mlb.getClassFile().getThisClassName() + StringConstants.CLASS_FILE_SUFFIX;
        this.printer.startOfTypeDeclaration(internalPath);
        this.printer.debugMarker("&lt;M&lt;");
    }
    private void writeMethodMarkerEnd()
    {
        this.printer.debugMarker("&gt;M&gt;");
        this.printer.endOfTypeDeclaration();
    }

    private void writeCommentDeprecated(LayoutBlock lb)
    {
        this.printer.debugStartOfCommentDeprecatedLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            this.printer.startOfJavadoc();
            this.printer.print("/** ");
            this.printer.startOfXdoclet();
            this.printer.print(JAVADOC_DEPRECATED);
            this.printer.endOfXdoclet();
            this.printer.print(" */");
            this.printer.endOfJavadoc();
            break;
        case 1:
            this.printer.startOfJavadoc();
            this.printer.print("/** ");
            this.printer.startOfXdoclet();
            this.printer.print(JAVADOC_DEPRECATED);
            this.printer.endOfXdoclet();
            this.printer.print(" */");
            this.printer.endOfJavadoc();
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        case 2:
            this.printer.startOfJavadoc();
            this.printer.print("/**");
            endOfLine();
            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print(" * ");
            this.printer.startOfXdoclet();
            this.printer.print(JAVADOC_DEPRECATED);
            this.printer.endOfXdoclet();
            this.printer.print(" */");
            this.printer.endOfJavadoc();
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        case 3:
            this.printer.startOfJavadoc();
            this.printer.print("/**");
            endOfLine();
            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print(" * ");
            this.printer.startOfXdoclet();
            this.printer.print(JAVADOC_DEPRECATED);
            this.printer.endOfXdoclet();
            endOfLine();
            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print(" */");
            this.printer.endOfJavadoc();
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        }

        this.printer.debugEndOfCommentDeprecatedLayoutBlock();
    }

    private void writeCommentError(LayoutBlock lb)
    {
        this.printer.debugStartOfCommentDeprecatedLayoutBlock();

        if (lb.getLineCount() == 0) {
            this.printer.startOfError();
            this.printer.print("/* Error */ ");
            this.printer.endOfError();
        } else if (lb.getLineCount() == 1) {
            this.printer.startOfError();
            this.printer.print("/* Error */");
            this.printer.endOfError();
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
        }

        this.printer.debugEndOfCommentDeprecatedLayoutBlock();
    }

    private void writeAnnotations(AnnotationsLayoutBlock alb)
    {
        List<Annotation> annotations = alb.getAnnotations();
        int length = annotations.size();

        if (length > 0)
        {
            this.printer.debugStartOfLayoutBlock();

            ClassFile classFile = alb.getClassFile();

            if (alb.getLineCount() == 0)
            {
                for (int i=0; i<length; i++)
                {
                    AnnotationWriter.writeAnnotation(
                        this.loader, this.printer, this.referenceMap,
                        classFile, annotations.get(i));
                }
            }
            else
            {
                int annotationsByLine = length / alb.getLineCount();

                if (annotationsByLine * alb.getLineCount() < length) {
                    annotationsByLine++;
                }

                int j = annotationsByLine;
                int k = alb.getLineCount();

                for (int i=0; i<length; i++)
                {
                    AnnotationWriter.writeAnnotation(
                        this.loader, this.printer, this.referenceMap,
                        classFile, annotations.get(i));

                    if (--j > 0)
                    {
                        this.printer.print(' ');
                    }
                    else
                    {
                        k--;
                        if (k > 0)
                        {
                            endOfLine();
                            this.printer.startOfLine(
                                Printer.UNKNOWN_LINE_NUMBER);
                        }
                        j = annotationsByLine;
                    }
                }

                endOfLine();
                this.printer.startOfLine(searchFirstLineNumber());
            }

            this.printer.debugEndOfLayoutBlock();
        }
    }

    private void writeType(TypeNameLayoutBlock tdlb)
    {
        this.printer.debugStartOfLayoutBlock();

        ClassFile classFile = tdlb.getClassFile();

        writeAccessAndType(classFile);

        this.printer.printTypeDeclaration(
            classFile.getThisClassName(), classFile.getClassName());

        if (tdlb.getLineCount() > 0)
        {
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
        }

        this.printer.debugEndOfLayoutBlock();
    }

    private void writeAccessAndType(ClassFile classFile)
    {
        // Affichage de la classe, de l'interface, de l'enum ou de l'annotation
         // Check annotation
        if ((classFile.getAccessFlags() & Const.ACC_ANNOTATION) != 0)
        {
            // Retrait du flags 'abstract'
            classFile.setAccessFlags(classFile.getAccessFlags() & ~Const.ACC_ABSTRACT);
        }

        // Access : public private static volatile ...
        if ((classFile.getAccessFlags() & Const.ACC_ENUM) == 0)
        {
            if (classFile.isAInnerClass()) {
                writeAccessNestedClass(classFile.getAccessFlags());
            } else {
                writeAccessClass(classFile.getAccessFlags());
            }
        } else if (classFile.isAInnerClass()) {
            writeAccessNestedEnum(classFile.getAccessFlags());
        } else {
            writeAccessEnum(classFile.getAccessFlags());
        }

        writeType(classFile.getAccessFlags());
        this.printer.print(' ');
    }

    private void writeAccessNestedClass(int accessFlags)
    {
        int acc;
        for(int i=0; i<ACCESS_NESTED_CLASS_NAMES.length; i++)
        {
            acc = 1 << i;

            if((accessFlags & acc) != 0 && acc != Const.ACC_SUPER && acc != Const.ACC_INTERFACE)
            {
                this.printer.printKeyword(ACCESS_NESTED_CLASS_NAMES[i]);
                this.printer.print(' ');
            }
        }

        if ((accessFlags & Const.ACC_ABSTRACT) != 0)
        {
            this.printer.printKeyword(ABSTRACT);
            this.printer.print(' ');
        }
    }

    private void writeAccessClass(int accessFlags)
    {
        if ((accessFlags & Const.ACC_PUBLIC) != 0)
        {
            this.printer.printKeyword(PUBLIC);
            this.printer.print(' ');
        }
        if ((accessFlags & Const.ACC_FINAL) != 0)
        {
            this.printer.printKeyword(FINAL);
            this.printer.print(' ');
        }
        if ((accessFlags & Const.ACC_ABSTRACT) != 0)
        {
            this.printer.printKeyword(ABSTRACT);
            this.printer.print(' ');
        }
    }

    private void writeAccessNestedEnum(int accessFlags)
    {
        int acc;
        for(int i=0; i<ACCESS_NESTED_ENUM_NAMES.length; i++)
        {
            acc = 1 << i;

            if((accessFlags & acc) != 0 && acc != Const.ACC_SUPER && acc != Const.ACC_INTERFACE)
            {
                this.printer.printKeyword(ACCESS_NESTED_ENUM_NAMES[i]);
                this.printer.print(' ');
            }
        }

        if ((accessFlags & Const.ACC_ABSTRACT) != 0)
        {
            this.printer.printKeyword(ABSTRACT);
            this.printer.print(' ');
        }
    }

    private void writeAccessEnum(int accessFlags)
    {
        if ((accessFlags & Const.ACC_PUBLIC) != 0) {
            this.printer.printKeyword(PUBLIC);
        }

        this.printer.print(' ');
    }

    private void writeType(int accessFlags)
    {
        if ((accessFlags & Const.ACC_ANNOTATION) != 0) {
            this.printer.printKeyword("@interface");
        } else if ((accessFlags & Const.ACC_ENUM) != 0) {
            this.printer.printKeyword("enum");
        } else if ((accessFlags & Const.ACC_INTERFACE) != 0) {
            this.printer.printKeyword("interface");
        } else {
            this.printer.printKeyword("class");
        }
    }

    private void writeExtendsSuperType(ExtendsSuperTypeLayoutBlock stelb)
    {
        this.printer.debugStartOfLayoutBlock();
        //DEBUG this.printer.print('^');

        if (stelb.getLineCount() > 0)
        {
            endOfLine();
            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.desindent();
        }
        else
        {
            this.printer.print(' ');
        }

        ClassFile classFile = stelb.getClassFile();

        this.printer.printKeyword(EXTENDS);
        this.printer.print(' ');

        String signature = SignatureUtil.createTypeName(classFile.getSuperClassName());
        SignatureWriter.writeSignature(
            this.loader, this.printer, this.referenceMap,
            classFile, signature);

        this.printer.debugEndOfLayoutBlock();
    }

    private void writeExtendsSuperInterfaces(
        ExtendsSuperInterfacesLayoutBlock sielb)
    {
        writeInterfaces(sielb, sielb.getClassFile(), true);
    }

    private void writeImplementsInterfaces(
        ImplementsInterfacesLayoutBlock iilb)
    {
        writeInterfaces(iilb, iilb.getClassFile(), false);
    }

    private void writeInterfaces(
        LayoutBlock lb, ClassFile classFile, boolean extendsKeyword)
    {
        this.printer.debugStartOfLayoutBlock();
        //DEBUG this.printer.print('^');

        if (lb.getLineCount() > 0)
        {
            endOfLine();
            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.desindent();
        }
        else
        {
            this.printer.print(' ');
        }

        int[] interfaceIndexes = classFile.getInterfaces();
        ConstantPool constants = classFile.getConstantPool();

        if (extendsKeyword)
        {
            this.printer.printKeyword(EXTENDS);
        }
        else
        {
            this.printer.printKeyword(IMPLEMENTS);
        }

        this.printer.print(' ');

        String signature = SignatureUtil.createTypeName(
            constants.getConstantClassName(interfaceIndexes[0]));
        SignatureWriter.writeSignature(
            this.loader, this.printer, this.referenceMap,
            classFile, signature);

        for(int i=1; i<interfaceIndexes.length; i++)
        {
            this.printer.print(", ");
            signature = SignatureUtil.createTypeName(
                constants.getConstantClassName(interfaceIndexes[i]));
            SignatureWriter.writeSignature(
                this.loader, this.printer, this.referenceMap,
                classFile, signature);
        }

        this.printer.debugEndOfLayoutBlock();
    }

    private void writeGenericType(GenericTypeNameLayoutBlock gtdlb)
    {
        writeAccessAndType(gtdlb.getClassFile());

        SignatureWriter.writeTypeDeclaration(
            this.loader, this.printer, this.referenceMap,
            gtdlb.getClassFile(), gtdlb.getSignature());
    }

    private void writeGenericExtendsSuperType(
        GenericExtendsSuperTypeLayoutBlock gstelb)
    {
        this.printer.debugStartOfLayoutBlock();
        //DEBUG this.printer.print('^');

        if (gstelb.getLineCount() > 0)
        {
            endOfLine();
            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.desindent();
        }
        else
        {
            this.printer.print(' ');
        }

        this.printer.printKeyword(EXTENDS);
        this.printer.print(' ');

        char[] caSignature = gstelb.getCaSignature();
        SignatureWriter.writeSignature(
            this.loader, this.printer, this.referenceMap, gstelb.getClassFile(),
            caSignature, caSignature.length, gstelb.getSignatureIndex());

        this.printer.debugEndOfLayoutBlock();
    }

    private void writeGenericExtendsSuperInterfaces(
        GenericExtendsSuperInterfacesLayoutBlock gsielb)
    {
        writeGenericInterfaces(
            gsielb, gsielb.getClassFile(), gsielb.getCaSignature(),
            gsielb.getSignatureIndex(), true);
    }

    private void writeGenericImplementsInterfaces(
        GenericImplementsInterfacesLayoutBlock giilb)
    {
        writeGenericInterfaces(
            giilb, giilb.getClassFile(), giilb.getCaSignature(),
            giilb.getSignatureIndex(), false);
    }

    private void writeGenericInterfaces(
        LayoutBlock lb, ClassFile classFile, char[] caSignature,
        int signatureIndex, boolean extendsKeyword)
    {
        this.printer.debugStartOfLayoutBlock();
        //DEBUG this.printer.print('^');

        if (lb.getLineCount() > 0)
        {
            endOfLine();
            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.desindent();
        }
        else
        {
            this.printer.print(' ');
        }

        if (extendsKeyword)
        {
            this.printer.printKeyword(EXTENDS);
        }
        else
        {
            this.printer.printKeyword(IMPLEMENTS);
        }

        this.printer.print(' ');

        int signatureLength = caSignature.length;
        signatureIndex = SignatureWriter.writeSignature(
            this.loader, this.printer, this.referenceMap, classFile,
            caSignature, signatureLength, signatureIndex);

        while (signatureIndex < signatureLength)
        {
            this.printer.print(", ");
            signatureIndex = SignatureWriter.writeSignature(
                this.loader, this.printer, this.referenceMap, classFile,
                caSignature, signatureLength, signatureIndex);
        }

        this.printer.debugEndOfLayoutBlock();
    }

    private void writeStatementBlockStart(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A { B */
            this.printer.print(" { ");
            this.printer.indent();
            break;
        case 1:
            /* A { B */
            this.printer.print(" {");
            endOfLine();
            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        default:
            /* A { ... B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print('{');
            endOfLine();

            this.printer.extraLine(lb.getLineCount()-2);

            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeSwitchBlockStart(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A { B */
            this.printer.print(" {");
            break;
        case 1:
            /* A { B */
            this.printer.print(" {");
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        default:
            /* A { ... B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print('{');
            endOfLine();

            this.printer.extraLine(lb.getLineCount()-2);

            this.printer.startOfLine(searchFirstLineNumber());
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeStatementsBlockEnd(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A } B */
            this.printer.print(" }");
            this.addSpace = true;
            this.printer.desindent();
            break;
        case 1:
            /* A } B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.desindent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.print('}');
            this.addSpace = true;
            break;
        default:
            /* A ... } B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.desindent();

            this.printer.extraLine(lb.getLineCount()-2);

            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print('}');
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            this.addSpace = false;
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeStatementsInnerBodyBlockEnd(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A }B */
            this.printer.print(" }");
            this.printer.desindent();
            break;
        case 1:
            /* A }B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.desindent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.print('}');
            break;
        default:
            /* A ... }B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.desindent();

            this.printer.extraLine(lb.getLineCount()-1);

            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.print('}');
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());

        this.addSpace = false;
    }

    private void writeSwitchBlockEnd(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A } B */
            this.printer.print('}');
            this.addSpace = true;
            break;
        case 1:
            /* A} B */
            this.printer.print('}');
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            this.addSpace = false;
            break;
        default:
            /* A ... } B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.desindent();

            this.printer.extraLine(lb.getLineCount()-1);

            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.print('}');
            this.addSpace = false;
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeCaseBlockStart(LayoutBlock lb)
    {
        this.printer.indent();
        this.printer.debugStartOfCaseBlockLayoutBlock();
        //writeSeparator(lb);
        int lineCount = lb.getLineCount();

        //DEBUG this.printer.print('^');

        if (lineCount > 0)
        {
            endOfLine();

            if (lineCount > 1)
            {
                this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                endOfLine();

                if (lineCount > 2)
                {
                    this.printer.extraLine(lineCount-2);
                }
            }

            this.printer.startOfLine(
                searchFirstLineNumber());
        }
        else
        {
            this.printer.print(' ');
        }
        this.printer.debugEndOfCaseBlockLayoutBlock();
    }

    private void writeCaseBlockEnd(LayoutBlock lb)
    {
        this.printer.desindent();
        this.printer.debugStartOfCaseBlockLayoutBlock();
        //writeSeparator(lb);
        int lineCount = lb.getLineCount();

        //DEBUG this.printer.print('^');

        if (lineCount > 0)
        {
            endOfLine();

            if (lineCount > 1)
            {
                this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                endOfLine();

                if (lineCount > 2)
                {
                    this.printer.extraLine(lineCount-2);
                }
            }

            this.printer.startOfLine(
                searchFirstLineNumber());
        }
        else
        {
            this.printer.print(' ');
        }
        this.printer.debugEndOfCaseBlockLayoutBlock();
    }

    private void writeForBlockStart(LayoutBlock lb)
    {
        this.printer.indent();
        this.printer.indent();
        this.printer.debugStartOfSeparatorLayoutBlock();
        int lineCount = lb.getLineCount();

        //DEBUG this.printer.print('^');

        if (lineCount > 0)
        {
            endOfLine();

            if (lineCount > 1)
            {
                this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                endOfLine();

                if (lineCount > 2)
                {
                    this.printer.extraLine(lineCount-2);
                }
            }

            this.printer.startOfLine(
                searchFirstLineNumber());
        }
        this.printer.debugEndOfSeparatorLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeForBlockEnd()
    {
        this.printer.desindent();
        this.printer.desindent();
    }

    private void writeStatementsBlockStartEnd(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A {} B */
            this.printer.print(" {}");
            break;
        case 1:
            /* A {} B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.print("{}");
            break;
        default:
            /* A {} ... B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print("{}");
            endOfLine();

            this.printer.extraLine(lb.getLineCount()-1);

            this.printer.startOfLine(searchFirstLineNumber());
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeSingleStatementsBlockStart(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A B */
            if (((BlockLayoutBlock)lb).getOther().getLineCount() > 0)
            {
                this.printer.print(" {");
            }

            this.printer.print(' ');
            this.printer.indent();
            break;
        case 1:
            /* A { B */
            //DEBUG this.printer.print('^');

            if (((BlockLayoutBlock)lb).getOther().getLineCount() > 0)
            {
                this.printer.print(" {");
            }

            endOfLine();
            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        default:
            /* A { ... B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print('{');
            endOfLine();

            this.printer.extraLine(lb.getLineCount()-2);

            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeSingleStatementsBlockEnd(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        switch (lb.getLineCount())
        {
        case 0:
            /* A B */
            if (((BlockLayoutBlock)lb).getOther().getLineCount() > 1)
            {
                this.printer.print(" }");
            }

            this.addSpace = true;
            this.printer.desindent();
            break;
        case 1:
            /* A } B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.desindent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.print('}');
            this.addSpace = true;
            break;
        default:
            /* A ... } B */
            //DEBUG this.printer.print('^');
            endOfLine();
            this.printer.desindent();

            this.printer.extraLine(lb.getLineCount()-2);

            this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
            this.printer.print('}');
            endOfLine();
            this.printer.startOfLine(searchFirstLineNumber());
            this.addSpace = false;
            break;
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeSingleStatementsBlockStartEnd(LayoutBlock lb)
    {
        this.printer.debugStartOfStatementsBlockLayoutBlock();

        if (lb.getLineCount() == 0) {
            /* A B */
            this.printer.print(" ;");
        } else {
            /* A ; ... B */
            this.printer.print(" ;");
            endOfLine();

            this.printer.extraLine(lb.getLineCount()-1);

            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
        }

        this.printer.debugEndOfStatementsBlockLayoutBlock(
            lb.getMinimalLineCount(), lb.getLineCount(), lb.getMaximalLineCount());
    }

    private void writeField(FieldNameLayoutBlock flb)
    {
        ClassFile classFile = flb.getClassFile();
        Field field = flb.getField();

        writeAccessField(field.getAccessFlags());

        ConstantPool constants = classFile.getConstantPool();

        AttributeSignature as = field.getAttributeSignature();
        int signatureIndex = as == null ?
                field.getDescriptorIndex() : as.getSignatureIndex();

        String signature = constants.getConstantUtf8(signatureIndex);

        SignatureWriter.writeSignature(
            this.loader, this.printer, this.referenceMap,
            classFile, signature);
        this.printer.print(' ');

        String fieldName = constants.getConstantUtf8(field.getNameIndex());
        if (keywords.contains(fieldName)) {
            fieldName = StringConstants.JD_FIELD_PREFIX + fieldName;
        }

        String internalClassName = classFile.getThisClassName();
        String descriptor = constants.getConstantUtf8(field.getDescriptorIndex());

        if ((field.getAccessFlags() & Const.ACC_STATIC) != 0) {
            this.printer.printStaticFieldDeclaration(
                internalClassName, fieldName, descriptor);
        } else {
            this.printer.printFieldDeclaration(
                internalClassName, fieldName, descriptor);
        }

        if (field.getValueAndMethod() != null)
        {
            this.printer.print(" = ");
            // La valeur du champ sera affichee par le bloc
            // 'InstructionsLayoutBlock' suivant.
        }
        else
        {
            Constant cv = field.getConstantValue(constants);

            if (cv != null)
            {
                this.printer.print(" = ");
                ConstantValueWriter.write(
                    this.loader, this.printer, this.referenceMap,
                    classFile, cv, (byte)signature.charAt(0));
            }
            this.printer.print(';');
        }
    }

    private void writeAccessField(int accessFlags)
    {
        int acc;
        for(int i=0; i<ACCESS_FIELD_NAMES.length; i++)
        {
            acc = 1 << i;

            if ((accessFlags & acc) != 0 && acc != Const.ACC_SUPER && acc != Const.ACC_INTERFACE && ACCESS_FIELD_NAMES[i] != null)
            {
                this.printer.printKeyword(ACCESS_FIELD_NAMES[i]);
                this.printer.print(' ');
            }
        }
    }

    private void writeMethodStatic(MethodStaticLayoutBlock mslb)
    {
        this.printer.printStaticConstructorDeclaration(
            mslb.getClassFile().getThisClassName(), STATIC);
    }

    private void writeMethod(MethodNameLayoutBlock mlb)
    {
        Method method = mlb.getMethod();

        if ((mlb.getClassFile().getAccessFlags() & Const.ACC_ANNOTATION) == 0)
        {
            if (mlb instanceof LambdaMethodLayoutBlock) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                LambdaMethodLayoutBlock lmlb = (LambdaMethodLayoutBlock) mlb;
                List<String> lambdaParameterNames = lmlb.getParameterNames();
                switch (lambdaParameterNames.size()) {
                    case 0:
                        this.printer.print("()");
                        break;
                    case 1:
                        this.printer.print(lambdaParameterNames.get(0));
                        break;
                    default:
                        this.printer.print('(');
                        for (Iterator<String> it = lambdaParameterNames.iterator(); it.hasNext();) {
                            this.printer.print(it.next());
                            if (it.hasNext()) {
                                this.printer.print(", ");
                            }
                        }
                        this.printer.print(')');
                        break;
                }
            } else {
                writeAccessMethod(method.getAccessFlags());
                SignatureWriter.writeMethodDeclaration(
                        keywords, this.loader, this.printer, this.referenceMap,
                        mlb.getClassFile(), method, mlb.getSignature(), mlb.hasDescriptorFlag(), mlb.isLambda());
            }
            if (mlb.hasNullCodeFlag()) {
                this.printer.print(';');
            }
        }
        else
        {
            writeAccessMethod(
                method.getAccessFlags() & ~(Const.ACC_PUBLIC|Const.ACC_ABSTRACT));

            SignatureWriter.writeMethodDeclaration(
                keywords, this.loader, this.printer, this.referenceMap,
                mlb.getClassFile(), method, mlb.getSignature(), mlb.hasDescriptorFlag(), mlb.isLambda());

            ElementValue defaultAnnotationValue =
                    method.getDefaultAnnotationValue();

            if (defaultAnnotationValue != null)
            {
                this.printer.print(' ');
                this.printer.printKeyword(DEFAULT);
                this.printer.print(' ');
                ElementValueWriter.writeElementValue(
                    this.loader, this.printer, this.referenceMap,
                    mlb.getClassFile(), defaultAnnotationValue);
            }

            this.printer.print(';');
        }
    }

    private void writeAccessMethod(int accessFlags)
    {
        int acc;
        for(int i=0; i<ACCESS_METHOD_NAMES.length; i++)
        {
            acc = 1 << i;

            if ((accessFlags & acc) != 0 && ACCESS_METHOD_NAMES[i] != null)
            {
                this.printer.printKeyword(ACCESS_METHOD_NAMES[i]);
                this.printer.print(' ');
            }
        }
    }

    private void writeThrows(ThrowsLayoutBlock tlb)
    {
        this.printer.debugStartOfLayoutBlock();
        //DEBUG this.printer.print('^');

        if (tlb.getLineCount() > 0)
        {
            endOfLine();
            this.printer.indent();
            this.printer.startOfLine(searchFirstLineNumber());
            this.printer.desindent();
        }
        else
        {
            this.printer.print(' ');
        }

        this.printer.printKeyword("throws");
        this.printer.print(' ');

        ClassFile classFile = tlb.getClassFile();
        ConstantPool constants = classFile.getConstantPool();
        int[] exceptionIndexes = tlb.getMethod().getExceptionIndexes();
        int exceptionIndexesLength = exceptionIndexes.length;

        if (exceptionIndexesLength > 0)
        {
            String firstInternalClassName =
                constants.getConstantClassName(exceptionIndexes[0]);
            this.printer.print(
                SignatureWriter.internalClassNameToShortClassName(
                    this.referenceMap, classFile, firstInternalClassName));

            String nextInternalClassName;
            for (int j=1; j<exceptionIndexesLength; j++)
            {
                this.printer.print(", ");
                nextInternalClassName = constants.getConstantClassName(exceptionIndexes[j]);
                this.printer.print(
                    SignatureWriter.internalClassNameToShortClassName(
                        this.referenceMap, classFile, nextInternalClassName));
            }
        }

        if (tlb.hasNullCodeFlag()) {
            this.printer.print(';');
        }

        this.printer.debugEndOfLayoutBlock();
    }

    private void writeInstruction(InstructionLayoutBlock ilb)
    {
        this.printer.debugStartOfInstructionBlockLayoutBlock();

        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.instructionPrinter.init(ilb.getFirstLineNumber());
        this.visitor.init(ilb.getClassFile(), ilb.getMethod(), ilb.getFirstOffset(), ilb.getLastOffset());
        this.instructionPrinter.startOfInstruction();
        this.visitor.visit(ilb.getInstruction());
        this.instructionPrinter.endOfInstruction();
        this.instructionPrinter.release();

        this.printer.debugEndOfInstructionBlockLayoutBlock();
    }

    private void writeInstructions(InstructionsLayoutBlock ilb)
    {
        this.printer.debugStartOfInstructionBlockLayoutBlock();

        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.instructionPrinter.init(ilb.getFirstLineNumber());
        this.visitor.init(
            ilb.getClassFile(), ilb.getMethod(), ilb.getFirstOffset(), ilb.getLastOffset());

        int idx = ilb.getFirstIndex();
        int lastIndex = ilb.getLastIndex();
        List<Instruction> instructions = ilb.getInstructions();

        Instruction instruction;
        while (idx <= lastIndex)
        {
            instruction = instructions.get(idx);

            if (idx > ilb.getFirstIndex() || ilb.getFirstOffset() == 0)
            {
                this.instructionPrinter.startOfInstruction();
            }

            this.visitor.visit(instruction);

            if (idx < lastIndex || ilb.getLastOffset() == instruction.getOffset())
            {
                // Ne pas afficher de ';' si une instruction n'a pas ete
                // entierement ecrite.
                this.instructionPrinter.endOfInstruction();
                this.printer.print(';');
            }

            idx++;
        }

        this.instructionPrinter.release();

        this.printer.debugEndOfInstructionBlockLayoutBlock();
    }

    private void writeByteCode(ByteCodeLayoutBlock bclb)
    {
//        this.printer.debugStartOfStatementsBlockLayoutBlock();
//        this.printer.startOfError();
//        this.printer.print("byte-code");
//        this.printer.endOfError();
//        endOfLine();
//        this.printer.startOfLine(searchFirstLineNumber());
//        this.printer.debugEndOfStatementsBlockLayoutBlock(
//            bclb.minimalLineCount, bclb.lineCount, bclb.maximalLineCount);

        ByteCodeWriter.write(
            this.loader, this.printer, this.referenceMap,
            bclb.getClassFile(), bclb.getMethod());
    }

    private void writeDeclaration(DeclareLayoutBlock dlb)
    {
        this.printer.debugStartOfInstructionBlockLayoutBlock();

        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.instructionPrinter.init(dlb.getFirstLineNumber());
        this.visitor.init(dlb.getClassFile(), dlb.getMethod(), 0, dlb.getInstruction().getOffset());
        this.instructionPrinter.startOfInstruction();
        this.visitor.visit(dlb.getInstruction());
        this.instructionPrinter.endOfInstruction();
        if (!this.printer.toString().trim().endsWith("{")) {
            this.printer.print(';');
        }
        this.instructionPrinter.release();

        this.printer.debugEndOfInstructionBlockLayoutBlock();
    }

    private void writeIf(/* InstructionLayoutBlock filb */)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("if");
        this.printer.print(" (");
    }

    private void writeWhile(/* InstructionLayoutBlock filb */)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("while");
        this.printer.print(" (");
    }

    private void writeFor()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("for");
        this.printer.print(" (");
    }

    private void writeLabeledBreak(OffsetLayoutBlock olb)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword(BREAK);
        this.printer.print(' ');
        this.printer.print(FastConstants.LABEL_PREFIX);
        this.printer.print(olb.getOffset());
        this.printer.print(';');
    }

    private void writeRightRoundBracket()
    {
        this.printer.print(')');
    }

    private void writeRightRoundBracketSemicolon()
    {
        this.printer.print(");");
    }

    private void writeSemicolon()
    {
        this.printer.print(';');
    }

    private void writeSemicolonSpace()
    {
        this.printer.print("; ");
    }

    private void writeSpaceColonSpace()
    {
        this.printer.print(" : ");
    }

    private void writeComaSpace()
    {
        this.printer.print(", ");
    }

    private void writeArrow()
    {
        this.printer.print(" -> ");
    }
    
    private void writeSwitch()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("switch");
        this.printer.print(" (");
    }

    private void writeCase(CaseLayoutBlock clb)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        String signature = clb.getFs().getTest().getReturnedSignature(
            clb.getClassFile().getConstantPool(), clb.getMethod().getLocalVariables());
        char type = signature == null ? 'X' : signature.charAt(0);

        FastSwitch.Pair[] pairs = clb.getFs().getPairs();
        int lineCount = clb.getLineCount() + 1;
        int lastIndex = clb.getLastIndex();
        int caseCount = lastIndex - clb.getFirstIndex() + 1;

        int caseByLine = caseCount / lineCount;
        int middleLineCount = caseCount - caseByLine*lineCount;
        int middleIndex = clb.getFirstIndex() + middleLineCount*(caseByLine + 1);
        int j = caseByLine + 1;

        for (int i=clb.getFirstIndex(); i<middleIndex; i++)
        {
            FastSwitch.Pair pair = pairs[i];

            if (pair.isDefault())
            {
                this.printer.printKeyword(DEFAULT);
            }
            else
            {
                this.printer.printKeyword("case");
                this.printer.print(' ');

                this.printer.debugStartOfInstructionBlockLayoutBlock();
                if (type == 'C')
                {
                    String escapedString =
                        StringUtil.escapeCharAndAppendApostrophe(
                            (char)pair.getKey());
                    this.printer.printString(
                        escapedString, clb.getClassFile().getThisClassName());
                }
                else
                {
                    this.printer.printNumeric(String.valueOf(pair.getKey()));
                }
                this.printer.debugEndOfInstructionBlockLayoutBlock();
            }
            this.printer.print(": ");

            if (lineCount > 0)
            {
                if (j == 1 && i < lastIndex)
                {
                    endOfLine();
                    this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                    j = caseByLine + 1;
                }
                else
                {
                    j--;
                }
            }
        }

        j = caseByLine;

        FastSwitch.Pair pair;
        for (int i=middleIndex; i<=lastIndex; i++)
        {
            pair = pairs[i];

            if (pair.isDefault())
            {
                this.printer.printKeyword(DEFAULT);
            }
            else
            {
                this.printer.printKeyword("case");
                this.printer.print(' ');

                this.printer.debugStartOfInstructionBlockLayoutBlock();
                if (type == 'C')
                {
                    String escapedString =
                        StringUtil.escapeCharAndAppendApostrophe(
                            (char)pair.getKey());
                    this.printer.printString(
                        escapedString, clb.getClassFile().getThisClassName());
                }
                else
                {
                    this.printer.printNumeric(String.valueOf(pair.getKey()));
                }
                this.printer.debugEndOfInstructionBlockLayoutBlock();
            }
            this.printer.print(": ");

            if (lineCount > 0)
            {
                if (j == 1 && i < lastIndex)
                {
                    endOfLine();
                    this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                    j = caseByLine;
                }
                else
                {
                    j--;
                }
            }
        }
    }

    private void writeCaseEnum(CaseEnumLayoutBlock celb)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        ClassFile classFile = celb.getClassFile();
        ConstantPool constants = classFile.getConstantPool();
        List<Integer> switchMap =
            classFile.getSwitchMaps().get(celb.getSwitchMapKeyIndex());

        ArrayLoadInstruction ali = (ArrayLoadInstruction)celb.getFs().getTest();
        Invokevirtual iv = (Invokevirtual)ali.getIndexref();
        ConstantMethodref cmr = constants.getConstantMethodref(iv.getIndex());
        String internalEnumName =
            constants.getConstantClassName(cmr.getClassIndex());

        String enumDescriptor = SignatureUtil.createTypeName(internalEnumName);

        FastSwitch.Pair[] pairs = celb.getFs().getPairs();
        int lineCount = celb.getLineCount() + 1;
        int lastIndex = celb.getLastIndex();
        int caseCount = lastIndex - celb.getFirstIndex() + 1;

        int caseByLine = caseCount / lineCount;
        int middleLineCount = caseCount - caseByLine*lineCount;
        int middleIndex = celb.getFirstIndex() + middleLineCount*(caseByLine + 1);
        int j = caseByLine + 1;

        for (int i=celb.getFirstIndex(); i<middleIndex; i++)
        {
            FastSwitch.Pair pair = pairs[i];

            if (pair.isDefault())
            {
                this.printer.printKeyword(DEFAULT);
            }
            else
            {
                this.printer.printKeyword("case");
                this.printer.print(' ');

                this.printer.debugStartOfInstructionBlockLayoutBlock();
                int key = pair.getKey();

                if (0 < key && key <= switchMap.size())
                {
                    String value = constants.getConstantUtf8(switchMap.get(key-1));
                    this.printer.printStaticField(
                        internalEnumName, value,
                        enumDescriptor, classFile.getThisClassName());
                }
                else
                {
                    this.printer.startOfError();
                    this.printer.print("???");
                    this.printer.endOfError();
                }
                this.printer.debugEndOfInstructionBlockLayoutBlock();
            }
            this.printer.print(": ");

            if (lineCount > 0)
            {
                if (j == 1 && i < lastIndex)
                {
                    endOfLine();
                    this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                    j = caseByLine + 1;
                }
                else
                {
                    j--;
                }
            }
        }

        j = caseByLine;

        FastSwitch.Pair pair;
        for (int i=middleIndex; i<=lastIndex; i++)
        {
            pair = pairs[i];

            if (pair.isDefault())
            {
                this.printer.printKeyword(DEFAULT);
            }
            else
            {
                this.printer.printKeyword("case");
                this.printer.print(' ');

                this.printer.debugStartOfInstructionBlockLayoutBlock();
                int key = pair.getKey();

                if (0 < key && key <= switchMap.size())
                {
                    String value = constants.getConstantUtf8(switchMap.get(key-1));
                    this.printer.printStaticField(
                        internalEnumName, value,
                        enumDescriptor, classFile.getThisClassName());
                }
                else
                {
                    this.printer.startOfError();
                    this.printer.print("???");
                    this.printer.endOfError();
                }
                this.printer.debugEndOfInstructionBlockLayoutBlock();
            }
            this.printer.print(": ");

            if (lineCount > 0)
            {
                if (j == 1 && i < lastIndex)
                {
                    endOfLine();
                    this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                    j = caseByLine;
                }
                else
                {
                    j--;
                }
            }
        }
    }

    private void writeCaseString(CaseLayoutBlock clb)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        ClassFile classFile = clb.getClassFile();
        ConstantPool constants = classFile.getConstantPool();

        FastSwitch.Pair[] pairs = clb.getFs().getPairs();
        int lineCount = clb.getLineCount() + 1;
        int lastIndex = clb.getLastIndex();
        int caseCount = lastIndex - clb.getFirstIndex() + 1;

        int caseByLine = caseCount / lineCount;
        int middleLineCount = caseCount - caseByLine*lineCount;
        int middleIndex = clb.getFirstIndex() + middleLineCount*(caseByLine + 1);
        int j = caseByLine + 1;

        for (int i=clb.getFirstIndex(); i<middleIndex; i++)
        {
            FastSwitch.Pair pair = pairs[i];

            if (pair.isDefault())
            {
                this.printer.printKeyword(DEFAULT);
            }
            else
            {
                this.printer.printKeyword("case");
                this.printer.print(' ');

                this.printer.debugStartOfInstructionBlockLayoutBlock();

                Constant cv = constants.getConstantValue(pair.getKey());
                ConstantValueWriter.write(
                    this.loader, this.printer, this.referenceMap,
                    classFile, cv);

                this.printer.debugEndOfInstructionBlockLayoutBlock();
            }
            this.printer.print(": ");

            if (lineCount > 0)
            {
                if (j == 1 && i < lastIndex)
                {
                    endOfLine();
                    this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                    j = caseByLine + 1;
                }
                else
                {
                    j--;
                }
            }
        }

        j = caseByLine;

        FastSwitch.Pair pair;
        for (int i=middleIndex; i<=lastIndex; i++)
        {
            pair = pairs[i];

            if (pair.isDefault())
            {
                this.printer.printKeyword(DEFAULT);
            }
            else
            {
                this.printer.printKeyword("case");
                this.printer.print(' ');

                this.printer.debugStartOfInstructionBlockLayoutBlock();

                Constant cv = constants.getConstantValue(pair.getKey());
                ConstantValueWriter.write(
                    this.loader, this.printer, this.referenceMap,
                    classFile, cv);

                this.printer.debugEndOfInstructionBlockLayoutBlock();
            }
            this.printer.print(": ");

            if (lineCount > 0)
            {
                if (j == 1 && i < lastIndex)
                {
                    endOfLine();
                    this.printer.startOfLine(Printer.UNKNOWN_LINE_NUMBER);
                    j = caseByLine;
                }
                else
                {
                    j--;
                }
            }
        }
    }

    private void writeCatch(FastCatchLayoutBlock fslb)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("catch");
        this.printer.print(" (");

        ClassFile classFile = fslb.getClassFile();
        ConstantPool constants = classFile.getConstantPool();
        Method method = fslb.getMethod();
        FastCatch fc = fslb.getFc();

        writeCatchType(classFile, constants, fc.exceptionTypeIndex());

        if (fc.otherExceptionTypeIndexes() != null)
        {
            int[] otherExceptionTypeIndexes = fc.otherExceptionTypeIndexes();
            int otherExceptionTypeIndexesLength =
                    otherExceptionTypeIndexes.length;

            for (int i=0; i<otherExceptionTypeIndexesLength; i++)
            {
                if (otherExceptionTypeIndexes[i] != 0)
                {
                    this.printer.print('|');
                    writeCatchType(
                        classFile, constants, otherExceptionTypeIndexes[i]);
                }
            }
        }

        this.printer.print(' ');

        LocalVariables localVariables = method == null ? null : method.getLocalVariables();
        if (localVariables == null) {
            localVariables = new LocalVariables();
        }
        LocalVariable lv = localVariables.searchLocalVariableWithIndexAndOffset(
                fc.localVarIndex(), fc.exceptionOffset());

        if (lv == null)
        {
            this.printer.startOfError();
            this.printer.print("???");
            this.printer.endOfError();
        }
        else
        {
            this.printer.print(constants.getConstantUtf8(lv.getNameIndex()));
        }

        this.printer.print(')');
    }

    private void writeCatchType(
        ClassFile classFile, ConstantPool constants, int exceptionTypeIndex)
    {
        String internalClassName =
            constants.getConstantClassName(exceptionTypeIndex);
        String className = SignatureWriter.internalClassNameToClassName(
            this.loader, this.referenceMap, classFile, internalClassName);
        this.printer.printType(
            internalClassName, className, classFile.getThisClassName());
    }

    private void writeSynchronized()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword(SYNCHRONIZED);
        this.printer.print(" (");
    }

    private void writeLabel(OffsetLayoutBlock olb)
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.print(FastConstants.LABEL_PREFIX);
        this.printer.print(olb.getOffset());
        this.printer.print(':');
    }

    private void writeElse()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("else");
    }

    private void writeElseSpace()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("else");
        this.printer.print(' ');
    }

    private void writeDo()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("do");
    }

    private void writeInfiniteLoop()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("for");
        this.printer.print(" (;;)");
    }

    private void writeTry()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("try");
    }

    private void writeFinally()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("finally");
    }

    private void writeContinue()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword("continue");
        this.printer.print(';');
    }

    private void writeBreak()
    {
        if (this.addSpace)
        {
            this.printer.print(" ");
            this.addSpace = false;
        }

        this.printer.printKeyword(BREAK);
        this.printer.print(';');
    }

    private void endOfLine()
    {
        this.printer.endOfLine();
        this.addSpace = false;
    }

    static
    {
        keywords = new HashSet<>();

        keywords.add("@interface");
        keywords.add(ABSTRACT);
        keywords.add("assert");
        keywords.add("boolean");
        keywords.add(BREAK);
        keywords.add("byte");
        keywords.add("case");
        keywords.add("catch");
        keywords.add("char");
        keywords.add("class");
        keywords.add("const");
        keywords.add("continue");
        keywords.add(DEFAULT);
        keywords.add("do");
        keywords.add("double");
        keywords.add("else");
        keywords.add("enum");
        keywords.add(EXTENDS);
        keywords.add("false");
        keywords.add(FINAL);
        keywords.add("finally");
        keywords.add("float");
        keywords.add("for");
        keywords.add("goto");
        keywords.add("if");
        keywords.add(IMPLEMENTS);
        keywords.add("import");
        keywords.add("instanceof");
        keywords.add("int");
        keywords.add("interface");
        keywords.add("long");
        keywords.add(NATIVE);
        keywords.add("new");
        keywords.add("null");
        keywords.add("package");
        keywords.add(PRIVATE);
        keywords.add(PROTECTED);
        keywords.add(PUBLIC);
        keywords.add("return");
        keywords.add("short");
        keywords.add(STATIC);
        keywords.add(STRICTFP);
        keywords.add("super");
        keywords.add("switch");
        keywords.add(SYNCHRONIZED);
        keywords.add("this");
        keywords.add("throw");
        keywords.add("throws");
        keywords.add(TRANSIENT);
        keywords.add("true");
        keywords.add("try");
        keywords.add("void");
        keywords.add(VOLATILE);
        keywords.add("while");
    }
}
