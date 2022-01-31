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

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;

/*
 * utilisé par TernaryOpReconstructor
 */
public final class SearchInstructionByOffsetVisitor
{
    private SearchInstructionByOffsetVisitor() {
        super();
    }

    public static Instruction visit(Instruction instruction, int offset)
    {
        if (instruction.getOffset() == offset) {
            return instruction;
        }

        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            return visit(((ArrayLength)instruction).getArrayref(), offset);
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            return visit(((ArrayStoreInstruction)instruction).getArrayref(), offset);
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                instruction = visit(ai.getTest(), offset);
                if (instruction != null) {
                    return instruction;
                }
                if (ai.getMsg() == null) {
                    return null;
                }
                return visit(ai.getMsg(), offset);
            }
        case Const.ATHROW:
            return visit(((AThrow)instruction).getValue(), offset);
        case ByteCodeConstants.UNARYOP:
            return visit(((UnaryOperatorInstruction)instruction).getValue(), offset);
        case ByteCodeConstants.BINARYOP,
             ByteCodeConstants.ASSIGNMENT:
            {
                BinaryOperatorInstruction boi =
                    (BinaryOperatorInstruction)instruction;
                instruction = visit(boi.getValue1(), offset);
                if (instruction != null) {
                    return instruction;
                }
                return visit(boi.getValue2(), offset);
            }
        case Const.CHECKCAST:
            return visit(((CheckCast)instruction).getObjectref(), offset);
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            return visit(((StoreInstruction)instruction).getValueref(), offset);
        case ByteCodeConstants.DUPSTORE:
            return visit(((DupStore)instruction).getObjectref(), offset);
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            return visit(((ConvertInstruction)instruction).getValue(), offset);
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                instruction = visit(ifCmp.getValue1(), offset);
                if (instruction != null) {
                    return instruction;
                }
                return visit(ifCmp.getValue2(), offset);
            }
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            return visit(((IfInstruction)instruction).getValue(), offset);
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                {
                    instruction = visit(branchList.get(i), offset);
                    if (instruction != null) {
                        return instruction;
                    }
                }
            }
            break;
        case Const.INSTANCEOF:
            return visit(((InstanceOf)instruction).getObjectref(), offset);
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            {
                Instruction result = visit(
                    ((InvokeNoStaticInstruction)instruction).getObjectref(), offset);
                if (result != null) {
                    return result;
                }
            }
            // intended fall through
        case Const.INVOKESTATIC,
             ByteCodeConstants.INVOKENEW:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                {
                    instruction = visit(list.get(i), offset);
                    if (instruction != null) {
                        return instruction;
                    }
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            return visit(((LookupSwitch)instruction).getKey(), offset);
        case Const.MONITORENTER:
            return visit(((MonitorEnter)instruction).getObjectref(), offset);
        case Const.MONITOREXIT:
            return visit(((MonitorExit)instruction).getObjectref(), offset);
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i)
                {
                    instruction = visit(dimensions[i], offset);
                    if (instruction != null) {
                        return instruction;
                    }
                }
            }
            break;
        case Const.NEWARRAY:
            return visit(((NewArray)instruction).getDimension(), offset);
        case Const.ANEWARRAY:
            return visit(((ANewArray)instruction).getDimension(), offset);
        case Const.POP:
            return visit(((Pop)instruction).getObjectref(), offset);
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                instruction = visit(putField.getObjectref(), offset);
                if (instruction != null) {
                    return instruction;
                }
                return visit(putField.getValueref(), offset);
            }
        case Const.PUTSTATIC:
            return visit(((PutStatic)instruction).getValueref(), offset);
        case ByteCodeConstants.XRETURN:
            return visit(((ReturnInstruction)instruction).getValueref(), offset);
        case Const.TABLESWITCH:
            return visit(((TableSwitch)instruction).getKey(), offset);
        case ByteCodeConstants.TERNARYOPSTORE:
            return visit(((TernaryOpStore)instruction).getObjectref(), offset);
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            return visit(((IncInstruction)instruction).getValue(), offset);
        case Const.GETFIELD:
            return visit(((GetField)instruction).getObjectref(), offset);
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                instruction = visit(iai.getNewArray(), offset);
                if (instruction != null) {
                    return instruction;
                }
                if (iai.getValues() != null) {
                    return visit(iai.getValues(), offset);
                }
            }
            break;
        case Const.ACONST_NULL,
             ByteCodeConstants.ARRAYLOAD,
             ByteCodeConstants.LOAD,
             Const.ALOAD,
             Const.ILOAD,
             Const.BIPUSH,
             ByteCodeConstants.ICONST,
             ByteCodeConstants.LCONST,
             ByteCodeConstants.FCONST,
             ByteCodeConstants.DCONST,
             ByteCodeConstants.DUPLOAD,
             Const.GETSTATIC,
             ByteCodeConstants.OUTERTHIS,
             Const.GOTO,
             Const.IINC,
             Const.JSR,
             Const.LDC,
             Const.LDC2_W,
             Const.NEW,
             Const.NOP,
             Const.SIPUSH,
             Const.RET,
             Const.RETURN,
             Const.INVOKEDYNAMIC,
             ByteCodeConstants.EXCEPTIONLOAD,
             ByteCodeConstants.RETURNADDRESSLOAD:
            break;
        default:
            System.err.println(
                    "Can not search instruction in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }

        return null;
    }

    private static Instruction visit(List<Instruction> instructions, int offset)
    {
        for (int i=instructions.size()-1; i>=0; --i)
        {
            Instruction instruction = visit(instructions.get(i), offset);
            if (instruction != null) {
                return instruction;
            }
        }

        return null;
    }
}
