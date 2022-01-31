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
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssertInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
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
public final class SearchDupLoadInstructionVisitor
{
    private SearchDupLoadInstructionVisitor() {
        super();
    }

    public static DupLoad visit(Instruction instruction, DupStore dupStore)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            return visit(((ArrayLength)instruction).getArrayref(), dupStore);
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                DupLoad dupLoad = visit(ali.getArrayref(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                return visit(ali.getIndexref(), dupStore);
            }
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                DupLoad dupLoad = visit(asi.getArrayref(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                dupLoad = visit(asi.getIndexref(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                return visit(asi.getValueref(), dupStore);
            }
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                DupLoad dupLoad = visit(ai.getTest(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                if (ai.getMsg() == null) {
                    return null;
                }
                return visit(ai.getMsg(), dupStore);
            }
        case Const.ATHROW:
            return visit(((AThrow)instruction).getValue(), dupStore);
        case ByteCodeConstants.UNARYOP:
            return visit(((UnaryOperatorInstruction)instruction).getValue(), dupStore);
        case ByteCodeConstants.BINARYOP,
             ByteCodeConstants.ASSIGNMENT:
            {
                BinaryOperatorInstruction boi =
                    (BinaryOperatorInstruction)instruction;
                DupLoad dupLoad = visit(boi.getValue1(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                return visit(boi.getValue2(), dupStore);
            }
        case Const.CHECKCAST:
            return visit(((CheckCast)instruction).getObjectref(), dupStore);
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            return visit(((StoreInstruction)instruction).getValueref(), dupStore);
        case ByteCodeConstants.DUPLOAD:
            if (((DupLoad)instruction).getDupStore() == dupStore) {
                return (DupLoad)instruction;
            }
            break;
        case ByteCodeConstants.DUPSTORE:
            return visit(((DupStore)instruction).getObjectref(), dupStore);
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            return visit(((ConvertInstruction)instruction).getValue(), dupStore);
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                DupLoad dupLoad = visit(ifCmp.getValue1(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                return visit(ifCmp.getValue2(), dupStore);
            }
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            return visit(((IfInstruction)instruction).getValue(), dupStore);
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                {
                    DupLoad dupLoad = visit(branchList.get(i), dupStore);
                    if (dupLoad != null) {
                        return dupLoad;
                    }
                }
            }
            break;
        case Const.INSTANCEOF:
            return visit(((InstanceOf)instruction).getObjectref(), dupStore);
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            {
                DupLoad dupLoad = visit(
                    ((InvokeNoStaticInstruction)instruction).getObjectref(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
            }
            // intended fall through
        case Const.INVOKESTATIC,
             ByteCodeConstants.INVOKENEW:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                {
                    DupLoad dupLoad = visit(list.get(i), dupStore);
                    if (dupLoad != null) {
                        return dupLoad;
                    }
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            return visit(((LookupSwitch)instruction).getKey(), dupStore);
        case Const.MONITORENTER:
            return visit(((MonitorEnter)instruction).getObjectref(), dupStore);
        case Const.MONITOREXIT:
            return visit(((MonitorExit)instruction).getObjectref(), dupStore);
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i)
                {
                    DupLoad dupLoad = visit(dimensions[i], dupStore);
                    if (dupLoad != null) {
                        return dupLoad;
                    }
                }
            }
            break;
        case Const.NEWARRAY:
            return visit(((NewArray)instruction).getDimension(), dupStore);
        case Const.ANEWARRAY:
            return visit(((ANewArray)instruction).getDimension(), dupStore);
        case Const.POP:
            return visit(((Pop)instruction).getObjectref(), dupStore);
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                DupLoad dupLoad = visit(putField.getObjectref(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                return visit(putField.getValueref(), dupStore);
            }
        case Const.PUTSTATIC:
            return visit(((PutStatic)instruction).getValueref(), dupStore);
        case ByteCodeConstants.XRETURN:
            return visit(((ReturnInstruction)instruction).getValueref(), dupStore);
        case Const.TABLESWITCH:
            return visit(((TableSwitch)instruction).getKey(), dupStore);
        case ByteCodeConstants.TERNARYOPSTORE:
            return visit(((TernaryOpStore)instruction).getObjectref(), dupStore);
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            return visit(((IncInstruction)instruction).getValue(), dupStore);
        case Const.GETFIELD:
            return visit(((GetField)instruction).getObjectref(), dupStore);
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                DupLoad dupLoad = visit(iai.getNewArray(), dupStore);
                if (dupLoad != null) {
                    return dupLoad;
                }
                if (iai.getValues() != null) {
                    return visit(iai.getValues(), dupStore);
                }
            }
            break;
        case Const.ACONST_NULL,
             ByteCodeConstants.LOAD,
             Const.ALOAD,
             Const.ILOAD,
             Const.BIPUSH,
             ByteCodeConstants.ICONST,
             ByteCodeConstants.LCONST,
             ByteCodeConstants.FCONST,
             ByteCodeConstants.DCONST,
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
                    "Can not search DupLoad instruction in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }

        return null;
    }

    private static DupLoad visit(
        List<Instruction> instructions, DupStore dupStore)
    {
        for (int i=instructions.size()-1; i>=0; --i)
        {
            DupLoad dupLoad = visit(instructions.get(i), dupStore);
            if (dupLoad != null) {
                return dupLoad;
            }
        }

        return null;
    }
}
