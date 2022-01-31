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
package jd.core.process.writer.visitor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantUtf8;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.LocalVariable;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConstInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.ExceptionLoad;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Goto;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.IInc;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Jsr;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.model.reference.ReferenceMap;
import jd.core.printer.InstructionPrinter;
import jd.core.process.writer.ConstantValueWriter;
import jd.core.process.writer.SignatureWriter;
import jd.core.process.writer.SourceWriteable;
import jd.core.util.SignatureUtil;
import jd.core.util.StringUtil;
import jd.core.util.UtilConstants;

public class SourceWriterVisitor
{
    private static final String[] CMP_NAMES = {
            "==", "<", ">", "", "!", "<=", ">=", "!=" };
    private static final char[] BIN_OPS = {'&', '^', '|'};

    private final Loader loader;
    private final InstructionPrinter printer;
    private final ReferenceMap referenceMap;
    private final Set<String> keywordSet;
    private ConstantPool constants;
    private LocalVariables localVariables;

    private ClassFile classFile;
    private int methodAccessFlags;
    private int firstOffset;
    private int lastOffset;
    private int previousOffset;

    public SourceWriterVisitor(
        Loader loader,
        InstructionPrinter printer,
        ReferenceMap referenceMap,
        Set<String> keywordSet)
    {
        this.loader = loader;
        this.printer = printer;
        this.referenceMap = referenceMap;
        this.keywordSet = keywordSet;
    }

    /**
     * Affichage de toutes les instructions avec
     *  - firstOffset <= offset
     *  - offset <= lastOffset.
     */
    public void init(
        ClassFile classFile, Method method, int firstOffset, int lastOffset)
    {
        this.classFile = classFile;
        this.firstOffset = firstOffset;
        this.lastOffset = lastOffset;
        this.previousOffset = 0;

        if (classFile == null || method == null)
        {
            this.constants = null;
            this.methodAccessFlags = 0;
            this.localVariables = null;
        }
        else
        {
            this.constants = classFile.getConstantPool();
            this.methodAccessFlags = method.getAccessFlags();
            this.localVariables = method.getLocalVariables();
        }
    }

    public int visit(Instruction instruction)
    {
        int lineNumber = instruction.getLineNumber();

        if (instruction.getOffset() < this.firstOffset ||
            this.previousOffset > this.lastOffset) {
            return lineNumber;
        }

        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            {
                lineNumber = visit(instruction, ((ArrayLength)instruction).getArrayref());

                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.print(lineNumber, '.');
                    this.printer.printJavaWord("length");
                }
            }
            break;
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                lineNumber = writeArray(ali, ali.getArrayref(), ali.getIndexref());
            }
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                lineNumber = writeArray(asi, asi.getArrayref(), asi.getIndexref());

                int nextOffset = this.previousOffset + 1;
                if (this.firstOffset <= nextOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(lineNumber, " = ");
                }

                lineNumber = visit(asi, asi.getValueref());
            }
            break;
        case Const.ANEWARRAY:
            {
                ANewArray newArray = (ANewArray)instruction;
                Instruction dimension = newArray.getDimension();

                String signature = constants.getConstantClassName(newArray.getIndex());

                if (signature.charAt(0) != '[') {
                    signature = SignatureUtil.createTypeName(signature);
                }

                String signatureWithoutArray =
                    SignatureUtil.cutArrayDimensionPrefix(signature);

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "new");
                    this.printer.print(' ');

                    SignatureWriter.writeSignature(
                        this.loader, this.printer, this.referenceMap,
                        this.classFile, signatureWithoutArray);

                    this.printer.print(lineNumber, '[');
                }

                lineNumber = visit(dimension);

                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.print(lineNumber, ']');

                    int dimensionCount =
                        signature.length() - signatureWithoutArray.length();

                    for (int i=dimensionCount; i>0; --i) {
                        this.printer.print(lineNumber, "[]");
                    }
                }
            }
            break;
        case Const.ACONST_NULL:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset) {
                    this.printer.printKeyword(lineNumber, "null");
                }
            }
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "assert");
                    this.printer.print(' ');
                }

                lineNumber = visit(ai, ai.getTest());

                if (ai.getMsg() != null)
                {

                    if (this.firstOffset <= this.previousOffset &&
                        ai.getMsg().getOffset() <= this.lastOffset) {
                        this.printer.print(lineNumber, " : ");
                    }

                    lineNumber = visit(ai, ai.getMsg());
                }
            }
            break;
        case ByteCodeConstants.ASSIGNMENT:
            lineNumber = writeAssignmentInstruction(
                (AssignmentInstruction)instruction);
            break;
        case Const.ATHROW:
            {
                AThrow athrow = (AThrow)instruction;
                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "throw");
                    this.printer.print(' ');
                }

                lineNumber = visit(athrow, athrow.getValue());
            }
            break;
        case ByteCodeConstants.UNARYOP:
            {
                UnaryOperatorInstruction ioi =
                    (UnaryOperatorInstruction)instruction;

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(lineNumber, ioi.getOperator());
                }

                lineNumber = visit(ioi, ioi.getValue());
            }
            break;
        case ByteCodeConstants.BINARYOP:
            lineNumber = writeBinaryOperatorInstruction(
                (BinaryOperatorInstruction)instruction);
            break;
        case Const.BIPUSH,
             Const.SIPUSH,
             ByteCodeConstants.ICONST:
            lineNumber = writeBIPushSIPushIConst((IConst)instruction);
            break;
        case ByteCodeConstants.LCONST:
            if (this.firstOffset <= this.previousOffset &&
                instruction.getOffset() <= this.lastOffset)
            {
                this.printer.printNumeric(lineNumber,
                    String.valueOf(((ConstInstruction)instruction).getValue()) + 'L');
            }
            break;
        case ByteCodeConstants.FCONST:
            if (this.firstOffset <= this.previousOffset &&
                instruction.getOffset() <= this.lastOffset)
            {
                String value =
                    String.valueOf(((ConstInstruction)instruction).getValue());
                if (value.indexOf('.') == -1) {
                    value += ".0";
                }
                this.printer.printNumeric(lineNumber, value + 'F');
            }
            break;
        case ByteCodeConstants.DCONST:
            if (this.firstOffset <= this.previousOffset &&
                instruction.getOffset() <= this.lastOffset)
            {
                String value =
                    String.valueOf(((ConstInstruction)instruction).getValue());
                if (value.indexOf('.') == -1) {
                    value += ".0";
                }
                this.printer.printNumeric(lineNumber, value + 'D');
            }
            break;
        case ByteCodeConstants.CONVERT:
            lineNumber = writeConvertInstruction(
                (ConvertInstruction)instruction);
            break;
        case ByteCodeConstants.IMPLICITCONVERT:
            lineNumber = visit(((ConvertInstruction)instruction).getValue());
            break;
        case Const.CHECKCAST:
            {
                CheckCast checkCast = (CheckCast)instruction;

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.print(lineNumber, '(');

                    String signature;
                    Constant c = constants.get(checkCast.getIndex());

                    if (c instanceof ConstantUtf8)
                    {
                        // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                        ConstantUtf8 cutf8 = (ConstantUtf8) c;
                        signature = cutf8.getBytes();
                    }
                    else
                    {
                        ConstantClass cc = (ConstantClass)c;
                        signature = constants.getConstantUtf8(cc.getNameIndex());
                        if (signature.charAt(0) != '[') {
                            signature = SignatureUtil.createTypeName(signature);
                        }
                    }

                    SignatureWriter.writeSignature(
                        this.loader, this.printer, this.referenceMap,
                        this.classFile, signature);

                    this.printer.print(')');
                }

                lineNumber = visit(checkCast, checkCast.getObjectref());
            }
            break;
        case FastConstants.DECLARE:
            lineNumber = writeDeclaration((FastDeclaration)instruction);
            break;
        case ByteCodeConstants.DUPSTORE:
            {
                DupStore dupStore = (DupStore)instruction;

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.print(
                        lineNumber, StringConstants.TMP_LOCAL_VARIABLE_NAME);
                    this.printer.print(instruction.getOffset());
                    this.printer.print('_');
                    this.printer.print(
                        ((DupStore)instruction).getObjectref().getOffset());
                    this.printer.print(" = ");
                }

                lineNumber = visit(instruction, dupStore.getObjectref());
            }
            break;
        case ByteCodeConstants.DUPLOAD:
            {
                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.print(
                        lineNumber, StringConstants.TMP_LOCAL_VARIABLE_NAME);
                    this.printer.print(instruction.getOffset());
                    this.printer.print('_');
                    this.printer.print(
                        ((DupLoad)instruction).getDupStore().getObjectref().getOffset());
                }
            }
            break;
        case FastConstants.ENUMVALUE:
            lineNumber = writeEnumValueInstruction((InvokeNew)instruction);
            break;
        case Const.GETFIELD:
            writeGetField((GetField)instruction);
            break;
        case Const.GETSTATIC:
            lineNumber = writeGetStatic((GetStatic)instruction);
            break;
        case ByteCodeConstants.OUTERTHIS:
            lineNumber = writeOuterThis((GetStatic)instruction);
            break;
        case Const.GOTO:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    Goto gotoInstruction = (Goto)instruction;
                    this.printer.printKeyword(lineNumber, "goto");
                    this.printer.print(' ');
                    this.printer.print(
                        lineNumber, gotoInstruction.getJumpOffset());
                }
            }
            break;
        case FastConstants.GOTO_CONTINUE:
            if (this.firstOffset <= this.previousOffset &&
                instruction.getOffset() <= this.lastOffset)
            {
                this.printer.printKeyword(lineNumber, "continue");
            }
            break;
        case FastConstants.GOTO_BREAK:
            if (this.firstOffset <= this.previousOffset &&
                instruction.getOffset() <= this.lastOffset)
            {
                this.printer.printKeyword(lineNumber, "break");
            }
            break;
        case ByteCodeConstants.IF:
            lineNumber = writeIfTest((IfInstruction)instruction);
            break;
        case ByteCodeConstants.IFCMP:
            lineNumber = writeIfCmpTest((IfCmp)instruction);
            break;
        case ByteCodeConstants.IFXNULL:
            lineNumber = writeIfXNullTest((IfInstruction)instruction);
            break;
        case ByteCodeConstants.COMPLEXIF:
            lineNumber = writeComplexConditionalBranchInstructionTest(
                (ComplexConditionalBranchInstruction)instruction);
            break;
        case Const.IINC:
            lineNumber = writeIInc((IInc)instruction);
            break;
        case ByteCodeConstants.PREINC:
            lineNumber = writePreInc((IncInstruction)instruction);
            break;
        case ByteCodeConstants.POSTINC:
            lineNumber = writePostInc((IncInstruction)instruction);
            break;
        case ByteCodeConstants.INVOKENEW:
            lineNumber = writeInvokeNewInstruction((InvokeNew)instruction);
            break;
        case Const.INSTANCEOF:
            {
                InstanceOf instanceOf = (InstanceOf)instruction;

                lineNumber = visit(instanceOf, instanceOf.getObjectref());

                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.print(lineNumber, ' ');
                    this.printer.printKeyword("instanceof");
                    this.printer.print(' ');

                    // reference to a class, array, or interface
                    String signature =
                        constants.getConstantClassName(instanceOf.getIndex());

                    if (signature.charAt(0) != '[') {
                        signature = SignatureUtil.createTypeName(signature);
                    }

                    SignatureWriter.writeSignature(
                        this.loader, this.printer, this.referenceMap,
                        this.classFile, signature);
                }
            }
            break;
        case Const.INVOKEINTERFACE,
             Const.INVOKEVIRTUAL:
            lineNumber = writeInvokeNoStaticInstruction(
                (InvokeNoStaticInstruction)instruction);
            break;
        case Const.INVOKESPECIAL:
            lineNumber = writeInvokespecial(
                (InvokeNoStaticInstruction)instruction);
            break;
        case Const.INVOKESTATIC:
            lineNumber = writeInvokestatic((Invokestatic)instruction);
            break;
        case Const.JSR:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "jsr");
                    this.printer.print(' ');
                    this.printer.print((short)((Jsr)instruction).getBranch());
                }
            }
            break;
        case Const.LDC,
             Const.LDC2_W:
            lineNumber = writeLcdInstruction((IndexInstruction)instruction);
            break;
        case ByteCodeConstants.LOAD,
             Const.ALOAD,
             Const.ILOAD:
            lineNumber = writeLoadInstruction((LoadInstruction)instruction);
            break;
        case Const.LOOKUPSWITCH:
            {
                LookupSwitch lookupSwitch = (LookupSwitch)instruction;

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "switch");
                    this.printer.print(" (");
                }

                lineNumber = visit(lookupSwitch.getKey());

                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.print(lineNumber, ')');
                }
            }
            break;
        case Const.TABLESWITCH:
            {
                TableSwitch tableSwitch = (TableSwitch)instruction;

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "switch");
                    this.printer.print(" (");
                }

                lineNumber = visit(tableSwitch.getKey());

                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.print(lineNumber, ')');
                }
            }
            break;
        case Const.MONITORENTER:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.startOfError();
                    this.printer.printKeyword(lineNumber, "monitorenter");
                    this.printer.endOfError();
                }
            }
            break;
        case Const.MONITOREXIT:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.startOfError();
                    this.printer.printKeyword(lineNumber, "monitorexit");
                    this.printer.endOfError();
                }
            }
            break;
        case Const.MULTIANEWARRAY:
            lineNumber = writeMultiANewArray((MultiANewArray)instruction);
            break;
        case Const.NEW:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "new");
                    this.printer.print(' ');
                    this.printer.print(
                        lineNumber, constants.getConstantClassName(
                            ((IndexInstruction)instruction).getIndex()));
                }
            }
            break;
        case Const.NEWARRAY:
            {
                NewArray newArray = (NewArray)instruction;
                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.printKeyword(lineNumber, "new");
                    this.printer.print(' ');
                    SignatureWriter.writeSignature(
                        this.loader, this.printer,
                        this.referenceMap, this.classFile,
                        SignatureUtil.getSignatureFromType(newArray.getType()));
                    this.printer.print(lineNumber, '[');
                }

                lineNumber = visit(newArray.getDimension());

                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset) {
                    this.printer.print(lineNumber, ']');
                }
            }
            break;
        case Const.POP:
            lineNumber = visit(instruction, ((Pop)instruction).getObjectref());
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;

                ConstantFieldref cfr = constants.getConstantFieldref(putField.getIndex());
                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cfr.getNameAndTypeIndex());

                boolean displayPrefix = false;

                if (this.localVariables.containsLocalVariableWithNameIndex(cnat.getNameIndex()))
                {
                    if (putField.getObjectref().getOpcode() == Const.ALOAD) {
                        if (((ALoad)putField.getObjectref()).getIndex() == 0) {
                            displayPrefix = true;
                        }
                    } else if (putField.getObjectref().getOpcode() == ByteCodeConstants.OUTERTHIS && !needAPrefixForThisField(
                            cnat.getNameIndex(), cnat.getSignatureIndex(),
                            (GetStatic)putField.getObjectref())) {
                        displayPrefix = true;
                    }
                }

                if (this.firstOffset <= this.previousOffset &&
                    putField.getObjectref().getOffset() <= this.lastOffset)
                {
                    if (!displayPrefix)
                    {
                        this.printer.addNewLinesAndPrefix(lineNumber);
                        this.printer.startOfOptionalPrefix();
                    }

                    lineNumber = visit(putField, putField.getObjectref());
                    this.printer.print(lineNumber, '.');

                    if (!displayPrefix)
                    {
                        this.printer.endOfOptionalPrefix();
                    }
                }

                String fieldName =
                    constants.getConstantUtf8(cnat.getNameIndex());
                if (this.keywordSet.contains(fieldName)) {
                    fieldName = StringConstants.JD_FIELD_PREFIX + fieldName;
                }

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    String internalClassName =
                        this.constants.getConstantClassName(cfr.getClassIndex());
                    String descriptor =
                        this.constants.getConstantUtf8(cnat.getSignatureIndex());
                    this.printer.printField(
                        lineNumber, internalClassName, fieldName,
                        descriptor, this.classFile.getThisClassName());
                    this.printer.print(" = ");
                }

                lineNumber = visit(putField, putField.getValueref());
            }
            break;
        case Const.PUTSTATIC:
            lineNumber = writePutStatic((PutStatic)instruction);
            break;
        case Const.RET:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.startOfError();
                    this.printer.printKeyword(lineNumber, "ret");
                    this.printer.endOfError();
                }
            }
            break;
        case Const.RETURN:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset) {
                    this.printer.printKeyword(lineNumber, "return");
                }
            }
            break;
        case ByteCodeConstants.XRETURN:
            {
                ReturnInstruction ri = (ReturnInstruction)instruction;

                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.printKeyword(ri.getLineNumber(), "return");
                    this.printer.print(' ');
                }

                lineNumber = visit(ri.getValueref());
            }
            break;
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            lineNumber = writeStoreInstruction((StoreInstruction)instruction);
            break;
        case ByteCodeConstants.EXCEPTIONLOAD:
            lineNumber = writeExceptionLoad((ExceptionLoad)instruction);
            break;
        case ByteCodeConstants.RETURNADDRESSLOAD:
            {
                if (this.firstOffset <= this.previousOffset &&
                    instruction.getOffset() <= this.lastOffset)
                {
                    this.printer.startOfError();
                    this.printer.printKeyword(lineNumber, "returnAddress");
                    this.printer.endOfError();
                }
            }
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            {
                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.startOfError();
                    this.printer.print(lineNumber, "tmpTernaryOp");
                    this.printer.print(lineNumber, " = ");
                    this.printer.endOfError();
                }

                lineNumber = visit(
                    instruction, ((TernaryOpStore)instruction).getObjectref());
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator tp = (TernaryOperator)instruction;

                lineNumber = visit(tp.getTest());

                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(lineNumber, " ? ");
                }

                lineNumber = visit(tp, tp.getValue1());

                nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(lineNumber, " : ");
                }

                lineNumber = visit(tp, tp.getValue2());
            }
            break;
        case ByteCodeConstants.INITARRAY:
            lineNumber = writeInitArrayInstruction(
                (InitArrayInstruction)instruction);
            break;
        case ByteCodeConstants.NEWANDINITARRAY:
            lineNumber = writeNewAndInitArrayInstruction(
                (InitArrayInstruction)instruction);
            break;
        case Const.NOP:
            break;
        case Const.INVOKEDYNAMIC:
            if (instruction instanceof SourceWriteable) { // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                SourceWriteable sw = (SourceWriteable) instruction;
                sw.write(printer, this);
            }
            break;
        default:
            System.err.println(
                    "Can not write code for " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }

        this.previousOffset = instruction.getOffset();

        return lineNumber;
    }

    protected int visit(Instruction parent, Instruction child)
    {
        return visit(parent.getPriority(), child);
    }

    protected int visit(int parentPriority, Instruction child)
    {
        if (parentPriority >= child.getPriority()) {
            return visit(child);
        }
        int nextOffset = this.previousOffset + 1;
        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(child.getLineNumber(), '(');
        }
        int lineNumber = visit(child);
        nextOffset = this.previousOffset + 1;
        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(lineNumber, ')');
        }
        return lineNumber;
    }

    private boolean needAPrefixForThisField(
        int fieldNameIndex, int fieldDescriptorIndex, GetStatic getStatic)
    {
        if (this.classFile.getField(fieldNameIndex, fieldDescriptorIndex) != null)
        {
            // La classe courante contient un champ ayant le même nom et la
            // même signature
            return true;
        }

        ConstantFieldref cfr =
            this.constants.getConstantFieldref(getStatic.getIndex());
        String getStaticOuterClassName =
            this.constants.getConstantClassName(cfr.getClassIndex());
        String fieldName = this.constants.getConstantUtf8(fieldNameIndex);
        String fieldDescriptor =
            this.constants.getConstantUtf8(fieldDescriptorIndex);

        ClassFile outerClassFile = this.classFile.getOuterClass();

        String outerClassName;
        while (outerClassFile != null)
        {
            outerClassName = outerClassFile.getThisClassName();
            if (outerClassName.equals(getStaticOuterClassName)) {
                break;
            }

            if (outerClassFile.getField(fieldName, fieldDescriptor) != null)
            {
                // La classe englobante courante contient un champ ayant le
                // même nom et la même signature
                return true;
            }

            outerClassFile = outerClassFile.getOuterClass();
        }

        return false;
    }

    private int writeBIPushSIPushIConst(IConst iconst)
    {
        int lineNumber = iconst.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            iconst.getOffset() <= this.lastOffset)
        {
            int value = iconst.getValue();
            String signature = iconst.getSignature();

            if ("S".equals(signature))
            {
                if ((short)value == Short.MIN_VALUE)
                {
                    writeBIPushSIPushIConst(
                        lineNumber, StringConstants.JAVA_LANG_SHORT, StringConstants.MIN_VALUE, "S");
                }
                else if ((short)value == Short.MAX_VALUE)
                {
                    writeBIPushSIPushIConst(
                        lineNumber, StringConstants.JAVA_LANG_SHORT, StringConstants.MAX_VALUE, "S");
                }
                else
                {
                    this.printer.printNumeric(lineNumber, String.valueOf(value));
                }
            }
            else if ("B".equals(signature))
            {
                if (value == Byte.MIN_VALUE)
                {
                    writeBIPushSIPushIConst(
                        lineNumber, StringConstants.JAVA_LANG_BYTE, StringConstants.MIN_VALUE, "B");
                }
                else if (value == Byte.MAX_VALUE)
                {
                    writeBIPushSIPushIConst(
                        lineNumber, StringConstants.JAVA_LANG_BYTE, StringConstants.MAX_VALUE, "B");
                }
                else
                {
                    this.printer.printNumeric(lineNumber, String.valueOf(value));
                }
            }
            else if ("C".equals(signature))
            {
                String escapedString =
                    StringUtil.escapeCharAndAppendApostrophe((char)value);
                String scopeInternalName =  this.classFile.getThisClassName();
                this.printer.printString(
                    lineNumber, escapedString, scopeInternalName);
            }
            else if ("Z".equals(signature))
            {
                this.printer.printKeyword(
                    lineNumber, value == 0 ? "false" : "true");
            }
            else
            {
                this.printer.printNumeric(lineNumber, String.valueOf(value));
            }
        }

        return lineNumber;
    }

    private void writeBIPushSIPushIConst(
        int lineNumber, String internalTypeName, String name, String descriptor)
    {
        String className = SignatureWriter.internalClassNameToClassName(
            this.loader, this.referenceMap, this.classFile, internalTypeName);
        String scopeInternalName = this.classFile.getThisClassName();
        this.printer.printType(
            lineNumber, internalTypeName, className, scopeInternalName);
        this.printer.print(lineNumber, '.');
        this.printer.printStaticField(
            lineNumber, internalTypeName, name, descriptor, scopeInternalName);
    }

    private int writeArray(
        Instruction parent, Instruction arrayref, Instruction indexref)
    {
        int lineNumber = visit(parent, arrayref);

        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(lineNumber, '[');
        }

        lineNumber = visit(parent, indexref);

        if (this.firstOffset <= this.previousOffset &&
            parent.getOffset() <= this.lastOffset) {
            this.printer.print(lineNumber, ']');
        }

        return lineNumber;
    }

    /** +, -, *, /, %, <<, >>, >>>, &, |, ^. */
    private int writeBinaryOperatorInstruction(BinaryOperatorInstruction boi)
    {
        int lineNumber;

        String op = boi.getOperator();
        if (op.length() == 1 && Arrays.binarySearch(BIN_OPS, op.charAt(0)) >= 0)
        {
            // Binary operators
            lineNumber =
                writeBinaryOperatorParameterInHexaOrBoolean(boi, boi.getValue1());

            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset)
            {
                this.printer.print(lineNumber, ' ');
                this.printer.print(lineNumber, op);
                this.printer.print(lineNumber, ' ');
            }

            return
                writeBinaryOperatorParameterInHexaOrBoolean(boi, boi.getValue2());
        }

        // Other operators
        lineNumber = visit(boi, boi.getValue1());

        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            this.printer.print(lineNumber, ' ');
            this.printer.print(lineNumber, op);
            this.printer.print(lineNumber, ' ');
        }

        if (boi.getPriority() > boi.getValue2().getPriority()) {
            return visit(boi.getValue2());
        }
        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(lineNumber, '(');
        }
        lineNumber = visit(boi.getValue2());
        if (this.firstOffset <= this.previousOffset &&
            boi.getOffset() <= this.lastOffset) {
            this.printer.print(lineNumber, ')');
        }
        return lineNumber;
    }

    protected int writeBinaryOperatorParameterInHexaOrBoolean(
        Instruction parent, Instruction child)
    {
        if (parent.getPriority() >= child.getPriority()) {
            return writeBinaryOperatorParameterInHexaOrBoolean(child);
        }
        int nextOffset = this.previousOffset + 1;
        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(child.getLineNumber(), '(');
        }
        int lineNumber = writeBinaryOperatorParameterInHexaOrBoolean(child);
        nextOffset = this.previousOffset + 1;
        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(lineNumber, ')');
        }
        return lineNumber;
    }

    private int writeBinaryOperatorParameterInHexaOrBoolean(Instruction value)
    {
        int lineNumber = value.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            value.getOffset() <= this.lastOffset)
        {
            switch (value.getOpcode())
            {
            case Const.BIPUSH,
                 ByteCodeConstants.ICONST,
                 Const.SIPUSH:
                {
                    IConst iconst = (IConst)value;
                    if ("Z".equals(iconst.getSignature()))
                    {
                        if (iconst.getValue() == 0) {
                            this.printer.printKeyword(lineNumber, "false");
                        } else {
                            this.printer.printKeyword(lineNumber, "true");
                        }
                    }
                    else
                    {
                        this.printer.printNumeric(
                            lineNumber,
                            "0x" + Integer.toHexString(iconst.getValue()).toUpperCase());
                    }
                }
                break;
            case Const.LDC,
                 Const.LDC2_W:
                this.printer.addNewLinesAndPrefix(lineNumber);
                Constant cst = constants.get( ((IndexInstruction)value).getIndex() );
                ConstantValueWriter.writeHexa(
                    this.loader, this.printer, this.referenceMap,
                    this.classFile, cst);
                break;
            default:
                lineNumber = visit(value);
                break;
            }
        }

        return lineNumber;
    }

    protected int writeIfTest(IfInstruction ifInstruction)
    {
        String signature =
            ifInstruction.getValue().getReturnedSignature(constants, localVariables);

        if (signature != null && signature.charAt(0) == 'Z')
        {
            if (ifInstruction.getCmp() == ByteCodeConstants.CMP_EQ
             || ifInstruction.getCmp() == ByteCodeConstants.CMP_LE
             || ifInstruction.getCmp() == ByteCodeConstants.CMP_GE) {
                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(ifInstruction.getLineNumber(), "!");
                }
            }

            return visit(2, ifInstruction.getValue());

//            visit(ifInstruction, ifInstruction.value);
//            switch (ifInstruction.cmp)
//            {
//            case ByteCodeConstants.CMP_EQ:
//            case ByteCodeConstants.CMP_LE:
//            case ByteCodeConstants.CMP_GE:
//                spw.print(" == false");
//                break;
//            default:
//                spw.print(" == true");
//            }
        }
        int lineNumber = visit(6, ifInstruction.getValue());
        if (this.firstOffset <= this.previousOffset &&
            ifInstruction.getOffset() <= this.lastOffset)
        {
            this.printer.print(' ');
            this.printer.print(CMP_NAMES[ifInstruction.getCmp()]);
            this.printer.print(' ');
            this.printer.printNumeric("0");
        }
        return lineNumber;
    }

    protected int writeIfCmpTest(IfCmp ifCmpInstruction)
    {
        int lineNumber = visit(6, ifCmpInstruction.getValue1());

        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            this.printer.print(lineNumber, ' ');
            this.printer.print(CMP_NAMES[ifCmpInstruction.getCmp()]);
            this.printer.print(' ');
        }

        return visit(6, ifCmpInstruction.getValue2());
    }

    protected int writeIfXNullTest(IfInstruction ifXNull)
    {
        int lineNumber = visit(6, ifXNull.getValue());

        if (this.firstOffset <= this.previousOffset &&
            ifXNull.getOffset() <= this.lastOffset)
        {
            this.printer.print(lineNumber, ' ');
            this.printer.print(CMP_NAMES[ifXNull.getCmp()]);
            this.printer.print(' ');
            this.printer.printKeyword("null");
        }

        return lineNumber;
    }

    protected int writeComplexConditionalBranchInstructionTest(
        ComplexConditionalBranchInstruction ccbi)
    {
        List<Instruction> branchList = ccbi.getInstructions();
        int length = branchList.size();

        if (length > 1)
        {
            String operator =
                ccbi.getCmp()==ByteCodeConstants.CMP_AND ? " && " : " || ";
            Instruction instruction = branchList.get(0);
            int lineNumber = instruction.getLineNumber();

            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset) {
                this.printer.print(lineNumber, '(');
            }

            lineNumber = visit(instruction);

            nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset) {
                this.printer.print(lineNumber, ')');
            }

            for (int i=1; i<length; i++)
            {
                instruction = branchList.get(i);

                nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.print(lineNumber, operator);
                    this.printer.print(instruction.getLineNumber(), '(');
                }

                lineNumber = visit(instruction);

                if (this.firstOffset <= this.previousOffset &&
                    ccbi.getOffset() <= this.lastOffset) {
                    this.printer.print(lineNumber, ')');
                }
            }

            return lineNumber;
        }
        if (length > 0)
        {
            return visit(branchList.get(0));
        }
        return Instruction.UNKNOWN_LINE_NUMBER;
    }

    private int writeIInc(IInc iinc)
    {
        int lineNumber = iinc.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            iinc.getOffset() <= this.lastOffset)
        {
            String lvName = null;

            LocalVariable lv =
                this.localVariables.getLocalVariableWithIndexAndOffset(
                        iinc.getIndex(), iinc.getOffset());

            if (lv != null)
            {
                int lvNameIndex = lv.getNameIndex();
                if (lvNameIndex > 0) {
                    lvName = constants.getConstantUtf8(lvNameIndex);
                }
            }

            if (lvName == null)
            {
                //new RuntimeException("local variable not found")
                //    .printStackTrace();
                this.printer.startOfError();
                this.printer.print(lineNumber, "???");
                this.printer.endOfError();
            }
            else
            {
                this.printer.print(lineNumber, lvName);
            }

            switch (iinc.getCount())
            {
            case -1:
                this.printer.print(lineNumber, "--");
                break;
            case 1:
                this.printer.print(lineNumber, "++");
                break;
            default:
                if (iinc.getCount() >= 0)
                {
                    this.printer.print(lineNumber, " += ");
                    this.printer.printNumeric(lineNumber, String.valueOf(iinc.getCount()));
                }
                else
                {
                    this.printer.print(lineNumber, " -= ");
                    this.printer.printNumeric(lineNumber, String.valueOf(-iinc.getCount()));
                }
            }
        }

        return lineNumber;
    }

    private int writePreInc(IncInstruction ii)
    {
        int lineNumber = ii.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            ii.getOffset() <= this.lastOffset)
        {
            switch (ii.getCount())
            {
            case -1:
                this.printer.print(lineNumber, "--");
                lineNumber = visit(ii.getValue());
                break;
            case 1:
                this.printer.print(lineNumber, "++");
                lineNumber = visit(ii.getValue());
                break;
            default:
                lineNumber = visit(ii.getValue());

                if (ii.getCount() >= 0)
                {
                    this.printer.print(lineNumber, " += ");
                    this.printer.printNumeric(lineNumber, String.valueOf(ii.getCount()));
                }
                else
                {
                    this.printer.print(lineNumber, " -= ");
                    this.printer.printNumeric(lineNumber, String.valueOf(-ii.getCount()));
                }
                break;
            }
        }

        return lineNumber;
    }

    private int writePostInc(IncInstruction ii)
    {
        int lineNumber = ii.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            ii.getOffset() <= this.lastOffset)
        {
            switch (ii.getCount())
            {
            case -1:
                lineNumber = visit(ii.getValue());
                this.printer.print(lineNumber, "--");
                break;
            case 1:
                lineNumber = visit(ii.getValue());
                this.printer.print(lineNumber, "++");
                break;
            default:
                new RuntimeException("PostInc with value=" + ii.getCount())
                    .printStackTrace();
            }
        }

        return lineNumber;
    }

    private int writeInvokeNewInstruction(InvokeNew in)
    {
        ConstantMethodref cmr = this.constants.getConstantMethodref(in.getIndex());
        String internalClassName =
            this.constants.getConstantClassName(cmr.getClassIndex());
        String prefix =
            this.classFile.getThisClassName() +
            StringConstants.INTERNAL_INNER_SEPARATOR;
        ClassFile innerClassFile;

        if (internalClassName.startsWith(prefix)) {
            innerClassFile = this.classFile.getInnerClassFile(internalClassName);
        } else {
            innerClassFile = null;
        }

        int lineNumber = in.getLineNumber();
        int firstIndex;
        int length = in.getArgs().size();

        ConstantNameAndType cnat =
            this.constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        String constructorDescriptor =
            this.constants.getConstantUtf8(cnat.getSignatureIndex());

        if (innerClassFile == null)
        {
            // Normal new invoke
            firstIndex = 0;
        }
        else if (innerClassFile.getInternalAnonymousClassName() == null)
        {
            // Inner class new invoke
            firstIndex = computeFirstIndex(innerClassFile.getAccessFlags(), in);
        }
        else
        {
            // Anonymous new invoke
            firstIndex = computeFirstIndex(this.methodAccessFlags, in);
            // Search parameter count of super constructor
            String constructorName =
                this.constants.getConstantUtf8(cnat.getNameIndex());
            Method constructor =
                innerClassFile.getMethod(constructorName, constructorDescriptor);
            if (constructor != null)
            {
                length =
                    firstIndex + constructor.getSuperConstructorParameterCount();
//                if (length > in.args.size()) {
//                    throw new IllegalStateException();
//                }
            }
        }

        if (this.firstOffset <= this.previousOffset)
        {
            this.printer.printKeyword(lineNumber, "new");
            this.printer.print(' ');

            if (innerClassFile == null || innerClassFile.getInternalAnonymousClassName() == null)
            {
                // Normal or inner class new invoke
                SignatureWriter.writeConstructor(
                    this.loader, this.printer, this.referenceMap,
                    this.classFile,
                    SignatureUtil.createTypeName(internalClassName),
                    constructorDescriptor);
                //writeArgs(in.lineNumber, 0, in.args);
            } else {
                // Anonymous new invoke
                SignatureWriter.writeConstructor(
                    this.loader, this.printer, this.referenceMap, this.classFile,
                    SignatureUtil.createTypeName(innerClassFile.getInternalAnonymousClassName()),
                    constructorDescriptor);
            }
        }

        return writeArgs(in.getLineNumber(), firstIndex, length, in.getArgs());
    }

    private static int computeFirstIndex(int accessFlags, InvokeNew in)
    {
        if ((accessFlags & Const.ACC_STATIC) != 0 || in.getArgs().isEmpty()) {
            return 0;
        }
        Instruction arg0 = in.getArgs().get(0);
        if (arg0.getOpcode() == Const.ALOAD &&
            ((ALoad)arg0).getIndex() == 0)
        {
            return 1;
        }
        return 0;
    }

    private int writeEnumValueInstruction(InvokeNew in)
    {
        int lineNumber = in.getLineNumber();

        ConstantFieldref cfr = constants.getConstantFieldref(in.getEnumValueFieldRefIndex());
        ConstantNameAndType cnat = constants.getConstantNameAndType(
            cfr.getNameAndTypeIndex());

        String internalClassName = classFile.getThisClassName();
        String name = constants.getConstantUtf8(cnat.getNameIndex());
        String descriptor = constants.getConstantUtf8(cnat.getSignatureIndex());

        this.printer.addNewLinesAndPrefix(lineNumber);
        this.printer.printStaticFieldDeclaration(
            internalClassName, name, descriptor);

        if (in.getArgs().size() > 2) {
            lineNumber = writeArgs(lineNumber, 2, in.getArgs().size(), in.getArgs());
        }

        return lineNumber;
    }

    private int writeGetField(GetField getField)
    {
        int lineNumber = getField.getLineNumber();
        ConstantFieldref cfr = constants.getConstantFieldref(getField.getIndex());
        ConstantNameAndType cnat =
            constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
        Field field =
            this.classFile.getField(cnat.getNameIndex(), cnat.getSignatureIndex());

        if (field != null &&
            field.getOuterMethodLocalVariableNameIndex() != UtilConstants.INVALID_INDEX)
        {
            // Specificite des classes anonymes : affichage du nom du champs de
            // la méthode englobante plutot que le nom du champs
            if (this.firstOffset <= this.previousOffset &&
                getField.getOffset() <= this.lastOffset)
            {
                String internalClassName =
                    this.constants.getConstantClassName(cfr.getClassIndex());
                String fieldName = this.constants.getConstantUtf8(
                    field.getOuterMethodLocalVariableNameIndex());
                if (this.keywordSet.contains(fieldName)) {
                    fieldName = StringConstants.JD_FIELD_PREFIX + fieldName;
                }
                String descriptor =
                    this.constants.getConstantUtf8(cnat.getSignatureIndex());
                this.printer.printField(
                    lineNumber, internalClassName, fieldName,
                    descriptor, this.classFile.getThisClassName());
            }
        }
        else
        {
            // Cas normal
            boolean displayPrefix = false;

            if (this.localVariables.containsLocalVariableWithNameIndex(cnat.getNameIndex()))
            {
                if (getField.getObjectref().getOpcode() == Const.ALOAD) {
                    if (((ALoad)getField.getObjectref()).getIndex() == 0) {
                        displayPrefix = true;
                    }
                } else if (getField.getObjectref().getOpcode() == ByteCodeConstants.OUTERTHIS && !needAPrefixForThisField(
                        cnat.getNameIndex(), cnat.getSignatureIndex(),
                        (GetStatic)getField.getObjectref())) {
                    displayPrefix = true;
                }
            }

            if (this.firstOffset <= this.previousOffset &&
                getField.getObjectref().getOffset() <= this.lastOffset)
            {
                if (!displayPrefix)
                {
                    this.printer.addNewLinesAndPrefix(lineNumber);
                    this.printer.startOfOptionalPrefix();
                }

                lineNumber = visit(getField, getField.getObjectref());
                this.printer.print(lineNumber, '.');

                if (!displayPrefix)
                {
                    this.printer.endOfOptionalPrefix();
                }
            }

            if (this.firstOffset <= this.previousOffset &&
                getField.getOffset() <= this.lastOffset)
            {
                String internalClassName =
                    this.constants.getConstantClassName(cfr.getClassIndex());
                String fieldName =
                    this.constants.getConstantUtf8(cnat.getNameIndex());
                if (this.keywordSet.contains(fieldName)) {
                    fieldName = StringConstants.JD_FIELD_PREFIX + fieldName;
                }
                String descriptor =
                    this.constants.getConstantUtf8(cnat.getSignatureIndex());
                this.printer.printField(
                    lineNumber, internalClassName, fieldName,
                    descriptor, this.classFile.getThisClassName());
            }
        }

        return lineNumber;
    }

    private int writeInvokeNoStaticInstruction(InvokeNoStaticInstruction insi)
    {
        ConstantMethodref cmr = constants.getConstantMethodref(insi.getIndex());
        ConstantNameAndType cnat =
            constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        boolean thisInvoke = false;

        if (insi.getObjectref().getOpcode() == Const.ALOAD &&
            ((ALoad)insi.getObjectref()).getIndex() == 0)
        {
            ALoad aload = (ALoad)insi.getObjectref();
            LocalVariable lv =
                this.localVariables.getLocalVariableWithIndexAndOffset(
                        aload.getIndex(), aload.getOffset());

            if (lv != null)
            {
                String name = this.constants.getConstantUtf8(lv.getNameIndex());
                if (StringConstants.THIS_LOCAL_VARIABLE_NAME.equals(name)) {
                    thisInvoke = true;
                }
            }
        }

        if (thisInvoke)
        {
            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset)
            {
                String internalClassName =
                    this.constants.getConstantClassName(cmr.getClassIndex());
                String methodName = constants.getConstantUtf8(cnat.getNameIndex());
                if (this.keywordSet.contains(methodName)) {
                    methodName = StringConstants.JD_METHOD_PREFIX + methodName;
                }
                String descriptor =
                    this.constants.getConstantUtf8(cnat.getSignatureIndex());
                // Methode de la classe courante : elimination du prefix 'this.'
                this.printer.printMethod(
                    insi.getLineNumber(), internalClassName, methodName,
                    descriptor, this.classFile.getThisClassName());
            }
        }
        else
        {
            boolean displayPrefix =
                insi.getObjectref().getOpcode() != ByteCodeConstants.OUTERTHIS ||
            needAPrefixForThisMethod(
                cnat.getNameIndex(), cnat.getSignatureIndex(),
                (GetStatic)insi.getObjectref());

            int lineNumber = insi.getObjectref().getLineNumber();

            if (!displayPrefix)
            {
                this.printer.addNewLinesAndPrefix(lineNumber);
                this.printer.startOfOptionalPrefix();
            }

            visit(insi, insi.getObjectref());

            int nextOffset = this.previousOffset + 1;
            lineNumber = insi.getLineNumber();

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset)
            {
                this.printer.print(lineNumber, '.');
            }

            if (!displayPrefix)
            {
                this.printer.endOfOptionalPrefix();
            }

            nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset)
            {
                String internalClassName =
                    this.constants.getConstantClassName(cmr.getClassIndex());
                String methodName = constants.getConstantUtf8(cnat.getNameIndex());
                if (this.keywordSet.contains(methodName)) {
                    methodName = StringConstants.JD_METHOD_PREFIX + methodName;
                }
                String descriptor =
                    this.constants.getConstantUtf8(cnat.getSignatureIndex());
                this.printer.printMethod(
                    lineNumber, internalClassName, methodName,
                    descriptor, this.classFile.getThisClassName());
            }
        }

        return writeArgs(insi.getLineNumber(), 0, insi.getArgs().size(), insi.getArgs());
    }

    private boolean needAPrefixForThisMethod(
        int methodNameIndex, int methodDescriptorIndex, GetStatic getStatic)
    {
        if (this.classFile.getMethod(methodNameIndex, methodDescriptorIndex) != null)
        {
            // La classe courante contient une method ayant le même nom et la
            // même signature
            return true;
        }

        ConstantFieldref cfr =
            this.constants.getConstantFieldref(getStatic.getIndex());
        String getStaticOuterClassName =
            this.constants.getConstantClassName(cfr.getClassIndex());
        String methodName = this.constants.getConstantUtf8(methodNameIndex);
        String methodDescriptor =
            this.constants.getConstantUtf8(methodDescriptorIndex);

        ClassFile outerClassFile = this.classFile.getOuterClass();

        String outerClassName;
        while (outerClassFile != null)
        {
            outerClassName = outerClassFile.getThisClassName();
            if (outerClassName.equals(getStaticOuterClassName)) {
                break;
            }

            if (outerClassFile.getMethod(methodName, methodDescriptor) != null)
            {
                // La classe englobante courante contient une method ayant le
                // même nom et la même signature
                return true;
            }

            outerClassFile = outerClassFile.getOuterClass();
        }

        return false;
    }

    private int writeInvokespecial(InvokeNoStaticInstruction insi)
    {
        ConstantMethodref cmr = constants.getConstantMethodref(insi.getIndex());
        ConstantNameAndType cnat =
            constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
        boolean thisInvoke = false;
        int firstIndex;

        if (insi.getObjectref().getOpcode() == Const.ALOAD &&
            ((ALoad)insi.getObjectref()).getIndex() == 0)
        {
            ALoad aload = (ALoad)insi.getObjectref();
            LocalVariable lv =
                this.localVariables.getLocalVariableWithIndexAndOffset(
                        aload.getIndex(), aload.getOffset());

            if (lv != null &&
                lv.getNameIndex() == this.constants.getThisLocalVariableNameIndex())
            {
                thisInvoke = true;
            }
        }

        // Appel d'un constructeur?
        if (thisInvoke && cnat.getNameIndex() == constants.getInstanceConstructorIndex())
        {
            if (cmr.getClassIndex() == classFile.getThisClassIndex())
            {
                // Appel d'un constructeur de la classe courante
                if ((this.classFile.getAccessFlags() & Const.ACC_ENUM) == 0)
                {
                    if (this.classFile.isAInnerClass() &&
                        (this.classFile.getAccessFlags() & Const.ACC_STATIC) == 0)
                    {
                        // inner class: firstIndex=1
                        firstIndex = 1;
                    }
                    else
                    {
                        // class: firstIndex=0
                        // static inner class: firstIndex=0
                        firstIndex = 0;
                    }
                }
                else
                {
                    // enum: firstIndex=2
                    // static inner enum: firstIndex=2
                    firstIndex = 2;
                }
            } else // Appel d'un constructeur de la classe mere
            if (this.classFile.isAInnerClass())
            {
                // inner class: firstIndex=1
                firstIndex = 1;
            }
            else
            {
                // class: firstIndex=0
                firstIndex = 0;
            }
        }
        else
        {
            // Appel a une méthode privee?
            firstIndex = 0;
        }

        if (thisInvoke)
        {
            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset)
            {
                int lineNumber = insi.getLineNumber();

                // Appel d'un constructeur?
                if (cnat.getNameIndex() == constants.getInstanceConstructorIndex())
                {
                    if (cmr.getClassIndex() == classFile.getThisClassIndex())
                    {
                        // Appel d'un constructeur de la classe courante
                        this.printer.printKeyword(lineNumber, "this");
                    }
                    else
                    {
                        // Appel d'un constructeur de la classe mere
                        this.printer.printKeyword(lineNumber, "super");
                    }
                }
                else
                {
                    // Appel a une méthode privee?
                    Method method = this.classFile.getMethod(
                        cnat.getNameIndex(), cnat.getSignatureIndex());

                    if (method == null ||
                        (method.getAccessFlags() & Const.ACC_PRIVATE) == 0)
                    {
                        // Methode de la classe mere
                        this.printer.printKeyword(lineNumber, "super");
                        this.printer.print(lineNumber, '.');
                    }
                    //else
                    //{
                    //    // Methode de la classe courante : elimination du prefix 'this.'
                    //}

                    String internalClassName =
                        this.constants.getConstantClassName(cmr.getClassIndex());
                    String methodName = constants.getConstantUtf8(cnat.getNameIndex());
                    if (this.keywordSet.contains(methodName)) {
                        methodName = StringConstants.JD_METHOD_PREFIX + methodName;
                    }
                    String descriptor =
                        this.constants.getConstantUtf8(cnat.getSignatureIndex());
                    this.printer.printMethod(
                        lineNumber, internalClassName, methodName,
                        descriptor, this.classFile.getThisClassName());
                }
            }
        }
        else
        {
            int lineNumber = insi.getLineNumber();

            visit(insi, insi.getObjectref());

            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset)
            {
                this.printer.print(lineNumber, '.');

                String internalClassName =
                    this.constants.getConstantClassName(cmr.getClassIndex());
                String methodName = constants.getConstantUtf8(cnat.getNameIndex());
                if (this.keywordSet.contains(methodName)) {
                    methodName = StringConstants.JD_METHOD_PREFIX + methodName;
                }
                String descriptor =
                    this.constants.getConstantUtf8(cnat.getSignatureIndex());

                this.printer.printMethod(
                    internalClassName, methodName,
                    descriptor, this.classFile.getThisClassName());
            }
        }

        return writeArgs(
            insi.getLineNumber(), firstIndex, insi.getArgs().size(), insi.getArgs());
    }

    private int writeInvokestatic(Invokestatic invokestatic)
    {
        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            int lineNumber = invokestatic.getLineNumber();

            ConstantMethodref cmr = constants.getConstantMethodref(invokestatic.getIndex());

            String internalClassName =
                this.constants.getConstantClassName(cmr.getClassIndex());

            if (classFile.getThisClassIndex() != cmr.getClassIndex())
            {
                this.printer.addNewLinesAndPrefix(lineNumber);
                int length = SignatureWriter.writeSignature(
                    this.loader, this.printer,
                    this.referenceMap, this.classFile,
                    SignatureUtil.createTypeName(constants.getConstantClassName(cmr.getClassIndex())));

                if (length > 0)
                {
                    this.printer.print('.');
                }
            }

            ConstantNameAndType cnat =
                constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

            String methodName = constants.getConstantUtf8(cnat.getNameIndex());
            if (this.keywordSet.contains(methodName)) {
                methodName = StringConstants.JD_METHOD_PREFIX + methodName;
            }
            String descriptor =
                    this.constants.getConstantUtf8(cnat.getSignatureIndex());

            this.printer.printStaticMethod(
                lineNumber, internalClassName, methodName, descriptor,
                this.classFile.getThisClassName());
        }

        return writeArgs(
            invokestatic.getLineNumber(), 0,
            invokestatic.getArgs().size(), invokestatic.getArgs());
    }

    private int writeArgs(
        int lineNumber, int firstIndex, int length, List<Instruction> args)
    {
        if (length > firstIndex)
        {
            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset) {
                this.printer.print(lineNumber, '(');
            }

            if (firstIndex < args.size()) {
                lineNumber = visit(args.get(firstIndex));
            }
            for (int i=firstIndex+1; i<length && i<args.size(); i++)
            {
                nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(lineNumber, ", ");
                }

                lineNumber = visit(args.get(i));
            }

            nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset) {
                this.printer.print(lineNumber, ')');
            }
        }
        else
        {
            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset) {
                this.printer.print(lineNumber, "()");
            }
        }

        return lineNumber;
    }

    private int writeGetStatic(GetStatic getStatic)
    {
        int lineNumber = getStatic.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            getStatic.getOffset() <= this.lastOffset)
        {
            ConstantFieldref cfr = constants.getConstantFieldref(getStatic.getIndex());
            String internalClassName =
                constants.getConstantClassName(cfr.getClassIndex());

            if (cfr.getClassIndex() != classFile.getThisClassIndex())
            {
                this.printer.addNewLinesAndPrefix(lineNumber);

                String className = SignatureUtil.createTypeName(internalClassName);

                int length = SignatureWriter.writeSignature(
                    this.loader, this.printer,
                    this.referenceMap, this.classFile, className);

                if (length > 0)
                {
                    this.printer.print(lineNumber, '.');
                }
            }

            ConstantNameAndType cnat = constants.getConstantNameAndType(
                cfr.getNameAndTypeIndex());
            String descriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
            String constName = constants.getConstantUtf8(cnat.getNameIndex());

            this.printer.printStaticField(
                lineNumber, internalClassName, constName,
                descriptor, this.classFile.getThisClassName());
        }

        return lineNumber;
    }

    private int writeOuterThis(GetStatic getStatic)
    {
        int lineNumber = getStatic.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            getStatic.getOffset() <= this.lastOffset)
        {
            ConstantFieldref cfr = constants.getConstantFieldref(getStatic.getIndex());

            if (cfr.getClassIndex() != classFile.getThisClassIndex())
            {
                this.printer.addNewLinesAndPrefix(lineNumber);
                int length = SignatureWriter.writeSignature(
                    this.loader, this.printer,
                    this.referenceMap, this.classFile,
                    SignatureUtil.createTypeName(constants.getConstantClassName(cfr.getClassIndex())));

                if (length > 0)
                {
                    this.printer.print(lineNumber, '.');
                }
            }

            ConstantNameAndType cnat = constants.getConstantNameAndType(
                cfr.getNameAndTypeIndex());
            this.printer.printKeyword(
                lineNumber, constants.getConstantUtf8(cnat.getNameIndex()));
        }

        return lineNumber;
    }

    private int writeLcdInstruction(IndexInstruction ii)
    {
        int lineNumber = ii.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            ii.getOffset() <= this.lastOffset)
        {
            // Dans les specs, LDC pointe vers une constante du pool. Lors de la
            // declaration d'enumeration, le byte code de la méthode
            // 'Enum.valueOf(Class<T> enumType, String name)' contient une
            // instruction LDC pointant un objet de type 'ConstantClass'.
            Constant cst = constants.get(ii.getIndex());

            if (cst instanceof ConstantClass)
            {
                // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                ConstantClass cc = (ConstantClass) cst;
                // Exception a la regle
                String signature = SignatureUtil.createTypeName(
                    constants.getConstantUtf8(cc.getNameIndex()));

                this.printer.addNewLinesAndPrefix(lineNumber);
                SignatureWriter.writeSignature(
                    this.loader, this.printer, this.referenceMap,
                    this.classFile, signature);
                this.printer.print('.');
                this.printer.printKeyword("class");
            }
            else
            {
                // Cas général
                this.printer.addNewLinesAndPrefix(lineNumber);
                ConstantValueWriter.write(
                    this.loader, this.printer, this.referenceMap,
                    this.classFile, cst);
            }
        }

        return lineNumber;
    }

    private int writeLoadInstruction(LoadInstruction loadInstruction)
    {
        int lineNumber = loadInstruction.getLineNumber();

        if (this.firstOffset <= this.previousOffset &&
            loadInstruction.getOffset() <= this.lastOffset)
        {
            LocalVariable lv =
                this.localVariables.getLocalVariableWithIndexAndOffset(
                    loadInstruction.getIndex(), loadInstruction.getOffset());

            if (lv == null || lv.getNameIndex() <= 0)
            {
                // Error
                this.printer.startOfError();
                this.printer.print(lineNumber, "???");
                this.printer.endOfError();
            }
            else
            {
                int nameIndex = lv.getNameIndex();

                if (nameIndex == this.constants.getThisLocalVariableNameIndex())
                {
                    this.printer.printKeyword(
                        lineNumber, constants.getConstantUtf8(lv.getNameIndex()));
                }
                else
                {
                    this.printer.print(
                        lineNumber, constants.getConstantUtf8(lv.getNameIndex()));
                }
            }
        }

        return lineNumber;
    }

    private int writeMultiANewArray(MultiANewArray multiANewArray)
    {
        int lineNumber = multiANewArray.getLineNumber();

        String signature = constants.getConstantClassName(multiANewArray.getIndex());

        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            this.printer.printKeyword(lineNumber, "new");
            this.printer.print(' ');

            SignatureWriter.writeSignature(
                this.loader, this.printer, this.referenceMap, this.classFile,
                SignatureUtil.cutArrayDimensionPrefix(signature));
        }

        Instruction[] dimensions = multiANewArray.getDimensions();

        for (int i=dimensions.length-1; i>=0; i--)
        {
            nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset) {
                this.printer.print(lineNumber, '[');
            }

            lineNumber = visit(dimensions[i]);

            nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset) {
                this.printer.print(lineNumber, ']');
            }
        }

        // Affichage des dimensions sans taille
        nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            int dimensionCount = SignatureUtil.getArrayDimensionCount(signature);
            for (int i=dimensions.length; i<dimensionCount; i++) {
                this.printer.print(lineNumber, "[]");
            }
        }

        return lineNumber;
    }

    private int writePutStatic(PutStatic putStatic)
    {
        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            int lineNumber = putStatic.getLineNumber();

            ConstantFieldref cfr = constants.getConstantFieldref(putStatic.getIndex());

            if (cfr.getClassIndex() != classFile.getThisClassIndex())
            {
                this.printer.addNewLinesAndPrefix(lineNumber);

                String signature = SignatureUtil.createTypeName(
                    this.constants.getConstantClassName(cfr.getClassIndex()));

                int length = SignatureWriter.writeSignature(
                    this.loader, this.printer,
                    this.referenceMap, this.classFile, signature);

                if (length > 0)
                {
                    this.printer.print(lineNumber, '.');
                }
            }

            ConstantNameAndType cnat =
                constants.getConstantNameAndType(cfr.getNameAndTypeIndex());
            String descriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
            String internalClassName = SignatureUtil.getInternalName(descriptor);
            String constName = constants.getConstantUtf8(cnat.getNameIndex());

            this.printer.printStaticField(
                lineNumber, internalClassName, constName,
                descriptor, this.classFile.getThisClassName());

            this.printer.print(lineNumber, " = ");
        }

        // Est-il necessaire de parenthéser l'expression ?
        // visit(putStatic, putStatic.valueref);
        return visit(putStatic.getValueref());
    }

    private int writeStoreInstruction(StoreInstruction storeInstruction)
    {
        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            int lineNumber = storeInstruction.getLineNumber();

            LocalVariable lv =
                this.localVariables.getLocalVariableWithIndexAndOffset(
                    storeInstruction.getIndex(), storeInstruction.getOffset());

            if (lv == null || lv.getNameIndex() <= 0)
            {
                this.printer.startOfError();
                this.printer.print(lineNumber, "???");
                this.printer.endOfError();
            }
            else
            {
                this.printer.print(
                    lineNumber, constants.getConstantUtf8(lv.getNameIndex()));
            }

            this.printer.print(lineNumber, " = ");
        }

        // Est-il necessaire de parenthéser l'expression ?
        // visit(storeInstruction, storeInstruction.valueref);
        return visit(storeInstruction.getValueref());
    }

    private int writeExceptionLoad(ExceptionLoad exceptionLoad)
    {
        int lineNumber = exceptionLoad.getLineNumber();
        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            if (exceptionLoad.getExceptionNameIndex() == 0)
            {
                this.printer.printKeyword(lineNumber, "finally");
            }
            else
            {
                LocalVariable lv =
                    this.localVariables.getLocalVariableWithIndexAndOffset(
                        exceptionLoad.getIndex(), exceptionLoad.getOffset());

                if (lv == null || lv.getNameIndex() == 0)
                {
                    this.printer.startOfError();
                    this.printer.print(lineNumber, "???");
                    this.printer.endOfError();
                }
                else
                {
                    this.printer.print(
                        lineNumber, constants.getConstantUtf8(lv.getNameIndex()));
                }
            }
        }

        return lineNumber;
    }

    private int writeAssignmentInstruction(AssignmentInstruction ai)
    {
        int lineNumber = ai.getLineNumber();
        int previousOffsetBackup = this.previousOffset;

        visit(ai.getValue1());

        this.previousOffset = previousOffsetBackup;
        int nextOffset = this.previousOffset + 1;

        String op = ai.getOperator();
        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            this.printer.print(lineNumber, ' ');
            this.printer.print(lineNumber, op);
            this.printer.print(lineNumber, ' ');
        }

        /* +=, -=, *=, /=, %=, <<=, >>=, >>>=, &=, |=, ^= */
        if (!op.isEmpty() && Arrays.binarySearch(BIN_OPS, op.charAt(0)) >= 0)
        {
            // Binary operators
            return writeBinaryOperatorParameterInHexaOrBoolean(ai, ai.getValue2());
        }

        return visit(ai, ai.getValue2());
    }

    private int writeConvertInstruction(ConvertInstruction instruction)
    {
        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            int lineNumber = instruction.getLineNumber();

            switch (instruction.getSignature().charAt(0))
            {
            case 'C':
                this.printer.print(lineNumber, '(');
                this.printer.printKeyword("char");
                this.printer.print(')');
                break;
            case 'B':
                this.printer.print(lineNumber, '(');
                this.printer.printKeyword("byte");
                this.printer.print(')');
                break;
            case 'S':
                this.printer.print(lineNumber, '(');
                this.printer.printKeyword("short");
                this.printer.print(')');
                break;
            case 'I':
                this.printer.print(lineNumber, '(');
                this.printer.printKeyword("int");
                this.printer.print(')');
                break;
            case 'L':
                this.printer.print(lineNumber, '(');
                this.printer.printKeyword("long");
                this.printer.print(')');
                break;
            case 'F':
                this.printer.print(lineNumber, '(');
                this.printer.printKeyword("float");
                this.printer.print(')');
                break;
            case 'D':
                this.printer.print(lineNumber, '(');
                this.printer.printKeyword("double");
                this.printer.print(')');
                break;
            }
        }

        return visit(instruction, instruction.getValue());
    }

    private int writeDeclaration(FastDeclaration fd)
    {
        int lineNumber = fd.getLineNumber();

        LocalVariable lv =
            localVariables.getLocalVariableWithIndexAndOffset(
                fd.getLv().getIndex(), fd.getOffset());

        if (lv == null)
        {
            if (fd.getInstruction() == null)
            {
                int nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset)
                {
                    this.printer.startOfError();
                    this.printer.print(lineNumber, "???");
                    this.printer.endOfError();
                }
            }
            else
            {
                lineNumber = visit(fd.getInstruction());
            }
        }
        else
        {
            int nextOffset = this.previousOffset + 1;

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset)
            {
                this.printer.addNewLinesAndPrefix(lineNumber);
                String signature =
                    this.constants.getConstantUtf8(lv.getSignatureIndex());
                String internalName =
                        SignatureUtil.getInternalName(signature);
                ClassFile innerClassFile =
                    this.classFile.getInnerClassFile(internalName);

                if (lv.hasFinalFlag())
                {
                    this.printer.printKeyword("final");
                    this.printer.print(' ');
                }

                if (innerClassFile != null &&
                    innerClassFile.getInternalAnonymousClassName() != null)
                {
                    String internalAnonymousClassSignature =
                            SignatureUtil.createTypeName(innerClassFile.getInternalAnonymousClassName());
                    SignatureWriter.writeSignature(
                        this.loader, this.printer, this.referenceMap,
                        this.classFile, internalAnonymousClassSignature);
                }
                else
                {
                    SignatureWriter.writeSignature(
                        this.loader, this.printer, this.referenceMap,
                        this.classFile, signature);
                }

                this.printer.print(' ');
            }

            if (fd.getInstruction() == null)
            {
                nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(
                        lineNumber, constants.getConstantUtf8(lv.getNameIndex()));
                }
            }
            else
            {
                lineNumber = visit(fd.getInstruction());
            }
        }

        return lineNumber;
    }

    /**
     * Affichage des initialisations de tableaux associees aux instructions
     * 'NEWARRAY' et 'ANEWARRAY' dans les affectations '?Store' et passees
     * en parametres.
     */
    private int writeInitArrayInstruction(InitArrayInstruction iai)
    {
        int lineNumber = iai.getLineNumber();

        // Affichage des valeurs
        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(lineNumber, "{");
        }

        List<Instruction> values = iai.getValues();
        final int length = values.size();

        if (length > 0)
        {
            Instruction instruction = values.get(0);

            if (this.firstOffset <= this.previousOffset &&
                nextOffset <= this.lastOffset && lineNumber == instruction.getLineNumber()) {
                this.printer.print(" ");
            }

            lineNumber = visit(instruction);

            for (int i=1; i<length; i++)
            {
                nextOffset = this.previousOffset + 1;

                if (this.firstOffset <= this.previousOffset &&
                    nextOffset <= this.lastOffset) {
                    this.printer.print(lineNumber, ", ");
                }

                lineNumber = visit(values.get(i));
            }
        }

        nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset) {
            this.printer.print(lineNumber, " }");
        }

        return lineNumber;
    }

    /**
     * Affichage des initialisations de tableaux associees aux instructions
     * 'NEWARRAY' et 'ANEWARRAY' dans les affectations '?Store' et passees
     * en parametres.
     */
    private int writeNewAndInitArrayInstruction(InitArrayInstruction iai)
    {
        int nextOffset = this.previousOffset + 1;

        if (this.firstOffset <= this.previousOffset &&
            nextOffset <= this.lastOffset)
        {
            int lineNumber = iai.getLineNumber();

            // Affichage de l'instruction 'new'
            this.printer.printKeyword(lineNumber, "new");
            this.printer.print(' ');

            if (iai.getNewArray().getOpcode() == Const.NEWARRAY) {
                NewArray na = (NewArray)iai.getNewArray();
                SignatureWriter.writeSignature(
                    this.loader, this.printer,
                    this.referenceMap, this.classFile,
                    SignatureUtil.getSignatureFromType(na.getType()));
            } else if (iai.getNewArray().getOpcode() == Const.ANEWARRAY) {
                ANewArray ana = (ANewArray)iai.getNewArray();
                String signature = constants.getConstantClassName(ana.getIndex());

                if (signature.charAt(0) != '[') {
                    signature = SignatureUtil.createTypeName(signature);
                }

                SignatureWriter.writeSignature(
                    this.loader, this.printer, this.referenceMap,
                    this.classFile, signature);
            }

            this.printer.print(lineNumber, "[] ");
        }

        return writeInitArrayInstruction(iai);
    }
}
