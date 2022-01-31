/*******************************************************************************
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
 ******************************************************************************/
package jd.core.process.analyzer.classfile.visitor;

import org.apache.bcel.Const;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
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
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IConst;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LoadInstruction;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;

public class CompareInstructionVisitor
{
    public boolean visit(Instruction i1, Instruction i2)
    {
        if (i1.getOpcode() != i2.getOpcode()) {
            return false;
        }

        switch (i1.getOpcode())
        {
        case Const.ARRAYLENGTH:
            return visit(
                ((ArrayLength)i1).getArrayref(), ((ArrayLength)i2).getArrayref());
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                if (Objects.compare(((ArrayStoreInstruction)i1).getSignature(),
                        ((ArrayStoreInstruction)i2).getSignature(), Comparator.naturalOrder()) != 0 || ! visit(
                        ((ArrayStoreInstruction)i1).getArrayref(),
                        ((ArrayStoreInstruction)i2).getArrayref()) || ! visit(
                        ((ArrayStoreInstruction)i1).getIndexref(),
                        ((ArrayStoreInstruction)i2).getIndexref())) {
                    return false;
                }

                return visit(
                        ((ArrayStoreInstruction)i1).getValueref(),
                        ((ArrayStoreInstruction)i2).getValueref());
            }
        case ByteCodeConstants.ASSERT:
            {
                if (! visit(
                        ((AssertInstruction)i1).getTest(),
                        ((AssertInstruction)i2).getTest())) {
                    return false;
                }

                Instruction msg1 = ((AssertInstruction)i1).getMsg();
                Instruction msg2 = ((AssertInstruction)i2).getMsg();

                if (msg1 == msg2) {
                    return true;
                }
                if (msg1 == null || msg2 == null) {
                    return false;
                }
                return visit(msg1, msg2);
            }
        case Const.ATHROW:
            return visit(((AThrow)i1).getValue(), ((AThrow)i2).getValue());
        case ByteCodeConstants.UNARYOP:
            {
                if (((UnaryOperatorInstruction)i1).getPriority() !=
                    ((UnaryOperatorInstruction)i2).getPriority() || ((UnaryOperatorInstruction)i1).getSignature().compareTo(
                        ((UnaryOperatorInstruction)i2).getSignature()) != 0 || ((UnaryOperatorInstruction)i1).getOperator().compareTo(
                        ((UnaryOperatorInstruction)i2).getOperator()) != 0) {
                    return false;
                }

                return visit(
                    ((UnaryOperatorInstruction)i1).getValue(),
                    ((UnaryOperatorInstruction)i2).getValue());
            }
        case ByteCodeConstants.BINARYOP:
            {
                if (((BinaryOperatorInstruction)i1).getPriority() !=
                    ((BinaryOperatorInstruction)i2).getPriority() || ((BinaryOperatorInstruction)i1).getSignature().compareTo(
                        ((BinaryOperatorInstruction)i2).getSignature()) != 0 || ((BinaryOperatorInstruction)i1).getOperator().compareTo(
                        ((BinaryOperatorInstruction)i2).getOperator()) != 0 || ! visit(
                        ((BinaryOperatorInstruction)i1).getValue1(),
                        ((BinaryOperatorInstruction)i2).getValue1())) {
                    return false;
                }

                return visit(
                    ((BinaryOperatorInstruction)i1).getValue2(),
                    ((BinaryOperatorInstruction)i2).getValue2());
            }
        case Const.CHECKCAST:
            {
                if (((CheckCast)i1).getIndex() != ((CheckCast)i2).getIndex()) {
                    return false;
                }

                return visit(
                    ((CheckCast)i1).getObjectref(), ((CheckCast)i2).getObjectref());
            }
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            {
                String rs1 = ((StoreInstruction)i1).getReturnedSignature(null, null);
                String rs2 = ((StoreInstruction)i2).getReturnedSignature(null, null);
                if (rs1 == null ? rs2 != null : rs1.compareTo(rs2) != 0) {
                    return false;
                }

                return visit(
                    ((StoreInstruction)i1).getValueref(),
                    ((StoreInstruction)i2).getValueref());
            }
        case ByteCodeConstants.DUPSTORE:
            return visit(
                ((DupStore)i1).getObjectref(), ((DupStore)i2).getObjectref());
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            {
                if (((ConvertInstruction)i1).getSignature().compareTo(
                        ((ConvertInstruction)i2).getSignature()) != 0) {
                    return false;
                }

                return visit(
                    ((ConvertInstruction)i1).getValue(),
                    ((ConvertInstruction)i2).getValue());
            }
        case ByteCodeConstants.IFCMP:
            {


                // Ce test perturbe le retrait des instructions 'finally' dans
                // les blocs 'try' et 'catch'.
                //  if (((IfCmp)i1).branch != ((IfCmp)i2).branch)
                //      return false;

                if (((IfCmp)i1).getCmp() != ((IfCmp)i2).getCmp() || ! visit(((IfCmp)i1).getValue1(), ((IfCmp)i2).getValue1())) {
                    return false;
                }

                return visit(((IfCmp)i1).getValue2(), ((IfCmp)i2).getValue2());
            }
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            {
                if (((IfInstruction)i1).getCmp() != ((IfInstruction)i2).getCmp()) {
                    return false;
                }

                return visit(
                    ((IfInstruction)i1).getValue(), ((IfInstruction)i2).getValue());
            }
        case ByteCodeConstants.COMPLEXIF:
            {
                if (((ComplexConditionalBranchInstruction)i1).getCmp() != ((ComplexConditionalBranchInstruction)i2).getCmp() || ((ComplexConditionalBranchInstruction)i1).getBranch() != ((ComplexConditionalBranchInstruction)i2).getBranch()) {
                    return false;
                }

                return visit(
                        ((ComplexConditionalBranchInstruction)i1).getInstructions(),
                        ((ComplexConditionalBranchInstruction)i2).getInstructions());
            }
        case Const.INSTANCEOF:
            {
                if (((InstanceOf)i1).getIndex() != ((InstanceOf)i2).getIndex()) {
                    return false;
                }

                return visit(
                    ((InstanceOf)i1).getObjectref(), ((InstanceOf)i2).getObjectref());
            }
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            {
                if (! visit(
                        ((InvokeNoStaticInstruction)i1).getObjectref(),
                        ((InvokeNoStaticInstruction)i2).getObjectref())) {
                    return false;
                }
            }
            // intended fall through
        case Const.INVOKESTATIC:
            return visit(
                ((InvokeInstruction)i1).getArgs(), ((InvokeInstruction)i2).getArgs());
        case ByteCodeConstants.INVOKENEW:
            {
                if (((InvokeNew)i1).getIndex() != ((InvokeNew)i2).getIndex()) {
                    return false;
                }

                return visit(
                    ((InvokeNew)i1).getArgs(), ((InvokeNew)i2).getArgs());
            }
        case Const.MULTIANEWARRAY:
            {
                if (((MultiANewArray)i1).getIndex() != ((MultiANewArray)i2).getIndex()) {
                    return false;
                }

                Instruction[] dimensions1 = ((MultiANewArray)i1).getDimensions();
                Instruction[] dimensions2 = ((MultiANewArray)i2).getDimensions();

                if (dimensions1.length != dimensions2.length) {
                    return false;
                }

                for (int i=dimensions1.length-1; i>=0; --i)
                {
                    if (! visit(dimensions1[i], dimensions2[i])) {
                        return false;
                    }
                }

                return true;
            }
        case Const.NEWARRAY:
            {
                if (((NewArray)i1).getType() != ((NewArray)i2).getType()) {
                    return false;
                }

                return visit(
                    ((NewArray)i1).getDimension(), ((NewArray)i2).getDimension());
            }
        case Const.ANEWARRAY:
            {
                if (((ANewArray)i1).getIndex() != ((ANewArray)i2).getIndex()) {
                    return false;
                }

                return visit(
                    ((ANewArray)i1).getDimension(), ((ANewArray)i2).getDimension());
            }
        case Const.PUTFIELD:
            {
                if (! visit(
                        ((PutField)i1).getObjectref(),
                        ((PutField)i2).getObjectref())) {
                    return false;
                }

                return visit(
                        ((PutField)i1).getValueref(), ((PutField)i2).getValueref());
            }
        case ByteCodeConstants.TERNARYOPSTORE:
            {
                if (((TernaryOpStore)i1).getTernaryOp2ndValueOffset()-i1.getOffset() !=
                    ((TernaryOpStore)i2).getTernaryOp2ndValueOffset()-i2.getOffset()) {
                    return false;
                }

                return visit(
                    ((TernaryOpStore)i1).getObjectref(),
                    ((TernaryOpStore)i2).getObjectref());
            }
        case ByteCodeConstants.TERNARYOP:
            {
                if (! visit(
                        ((TernaryOperator)i1).getTest(),
                        ((TernaryOperator)i2).getTest()) || ! visit(
                        ((TernaryOperator)i1).getValue1(),
                        ((TernaryOperator)i2).getValue1())) {
                    return false;
                }

                return visit(
                        ((TernaryOperator)i1).getValue2(),
                        ((TernaryOperator)i2).getValue2());
            }
        case ByteCodeConstants.ASSIGNMENT:
            {
                if (((AssignmentInstruction)i1).getPriority() !=
                    ((AssignmentInstruction)i2).getPriority() || ((AssignmentInstruction)i1).getOperator().compareTo(
                        ((AssignmentInstruction)i2).getOperator()) != 0 || ! visit(
                        ((AssignmentInstruction)i1).getValue1(),
                        ((AssignmentInstruction)i2).getValue1())) {
                    return false;
                }

                return visit(
                    ((AssignmentInstruction)i1).getValue2(),
                    ((AssignmentInstruction)i2).getValue2());
            }
        case ByteCodeConstants.ARRAYLOAD:
            {
                String s1 = ((ArrayLoadInstruction)i1).getReturnedSignature(null, null);
                String s2 = ((ArrayLoadInstruction)i2).getReturnedSignature(null, null);

                if (s1 == null)
                {
                    if (s2 != null) {
                        return false;
                    }
                } else if (s1.compareTo(s2) != 0) {
                    return false;
                }

                if (! visit(
                        ((ArrayLoadInstruction)i1).getArrayref(),
                        ((ArrayLoadInstruction)i2).getArrayref())) {
                    return false;
                }

                return visit(
                    ((ArrayLoadInstruction)i1).getIndexref(),
                    ((ArrayLoadInstruction)i2).getIndexref());
            }
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            {
                if (((IncInstruction)i1).getCount() != ((IncInstruction)i2).getCount()) {
                    return false;
                }

                return visit(
                        ((IncInstruction)i1).getValue(),
                        ((IncInstruction)i2).getValue());
            }
        case Const.GETFIELD:
            {
                if (((GetField)i1).getIndex() != ((GetField)i2).getIndex()) {
                    return false;
                }

                return visit(
                    ((GetField)i1).getObjectref(), ((GetField)i2).getObjectref());
            }
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                if (! visit(
                        ((InitArrayInstruction)i1).getNewArray(),
                        ((InitArrayInstruction)i2).getNewArray())) {
                    return false;
                }

                return visit(((InitArrayInstruction)i1).getValues(),
                        ((InitArrayInstruction)i2).getValues());
            }
        case Const.ALOAD,
             Const.ILOAD:
            {
                String rs1 = ((LoadInstruction)i1).getReturnedSignature(null, null);
                String rs2 = ((LoadInstruction)i2).getReturnedSignature(null, null);
                return rs1 == null ? rs2 == null : rs1.compareTo(rs2) == 0;
            }
        case ByteCodeConstants.ICONST,
             Const.BIPUSH,
             Const.SIPUSH:
            {
                if (((IConst)i1).getValue() != ((IConst)i2).getValue()) {
                    return false;
                }

                return
                    ((IConst)i1).getSignature().compareTo(
                        ((IConst)i2).getSignature()) == 0;
            }
        case ByteCodeConstants.DCONST,
             ByteCodeConstants.LCONST,
             ByteCodeConstants.FCONST:
            return ((ConstInstruction)i1).getValue() == ((ConstInstruction)i2).getValue();
        case ByteCodeConstants.DUPLOAD:
            return ((DupLoad)i1).getDupStore() == ((DupLoad)i2).getDupStore();
        case Const.TABLESWITCH,
             ByteCodeConstants.XRETURN,
             Const.PUTSTATIC,
             Const.LOOKUPSWITCH,
             Const.MONITORENTER,
             Const.MONITOREXIT,
             Const.POP,
             Const.ACONST_NULL,
             ByteCodeConstants.LOAD,
             Const.GETSTATIC,
             ByteCodeConstants.OUTERTHIS,
             Const.GOTO,
             Const.IINC,
             Const.JSR,
             Const.LDC,
             Const.LDC2_W,
             Const.NEW,
             Const.NOP,
             Const.RET,
             Const.RETURN,
             Const.INVOKEDYNAMIC,
             ByteCodeConstants.EXCEPTIONLOAD,
             ByteCodeConstants.RETURNADDRESSLOAD:
            return true;
        default:
            System.err.println(
                "Can not compare instruction " +
                i1.getClass().getName() + " and " + i2.getClass().getName());
            return false;
        }
    }

    protected boolean visit(
        List<Instruction> l1, List<Instruction> l2)
    {
        int i = l1.size();

        if (i != l2.size()) {
            return false;
        }

        while (i-- > 0)
        {
            if (! visit(l1.get(i), l2.get(i))) {
                return false;
            }
        }

        return true;
    }
}
