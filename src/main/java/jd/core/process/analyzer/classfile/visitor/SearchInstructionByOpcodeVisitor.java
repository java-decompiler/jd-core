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
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.model.instruction.fast.instruction.FastFor;
import jd.core.model.instruction.fast.instruction.FastForEach;
import jd.core.model.instruction.fast.instruction.FastInstruction;
import jd.core.model.instruction.fast.instruction.FastLabel;
import jd.core.model.instruction.fast.instruction.FastList;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastSwitch.Pair;
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTestList;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;

/*
 * utilisé par TernaryOpReconstructor
 */
public final class SearchInstructionByOpcodeVisitor
{
    private SearchInstructionByOpcodeVisitor() {
        super();
    }

    public static Instruction visit(Instruction instruction, int opcode)
        throws RuntimeException
    {
        if (instruction == null) {
            throw new IllegalStateException("Null instruction");
        }

        if (instruction.getOpcode() == opcode) {
            return instruction;
        }

        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            return visit(((ArrayLength)instruction).getArrayref(), opcode);
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            return visit(((ArrayStoreInstruction)instruction).getArrayref(), opcode);
        case Const.ATHROW:
            return visit(((AThrow)instruction).getValue(), opcode);
        case ByteCodeConstants.UNARYOP:
            return visit(((UnaryOperatorInstruction)instruction).getValue(), opcode);
        case ByteCodeConstants.BINARYOP,
             ByteCodeConstants.ASSIGNMENT:
            {
                BinaryOperatorInstruction boi =
                    (BinaryOperatorInstruction)instruction;
                Instruction tmp = visit(boi.getValue1(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                return visit(boi.getValue2(), opcode);
            }
        case Const.CHECKCAST:
            return visit(((CheckCast)instruction).getObjectref(), opcode);
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            return visit(((StoreInstruction)instruction).getValueref(), opcode);
        case ByteCodeConstants.DUPSTORE:
            return visit(((DupStore)instruction).getObjectref(), opcode);
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            return visit(((ConvertInstruction)instruction).getValue(), opcode);
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                Instruction tmp = visit(ifCmp.getValue1(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                return visit(ifCmp.getValue2(), opcode);
            }
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            return visit(((IfInstruction)instruction).getValue(), opcode);
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                {
                    Instruction tmp = visit(branchList.get(i), opcode);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
            break;
        case Const.INSTANCEOF:
            return visit(((InstanceOf)instruction).getObjectref(), opcode);
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            {
                Instruction result = visit(
                    ((InvokeNoStaticInstruction)instruction).getObjectref(), opcode);
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
                    Instruction tmp = visit(list.get(i), opcode);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            return visit(((LookupSwitch)instruction).getKey(), opcode);
        case Const.MONITORENTER:
            return visit(((MonitorEnter)instruction).getObjectref(), opcode);
        case Const.MONITOREXIT:
            return visit(((MonitorExit)instruction).getObjectref(), opcode);
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i)
                {
                    Instruction tmp = visit(dimensions[i], opcode);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
            break;
        case Const.NEWARRAY:
            return visit(((NewArray)instruction).getDimension(), opcode);
        case Const.ANEWARRAY:
            return visit(((ANewArray)instruction).getDimension(), opcode);
        case Const.POP:
            return visit(((Pop)instruction).getObjectref(), opcode);
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                Instruction tmp = visit(putField.getObjectref(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                return visit(putField.getValueref(), opcode);
            }
        case Const.PUTSTATIC:
            return visit(((PutStatic)instruction).getValueref(), opcode);
        case ByteCodeConstants.XRETURN:
            return visit(((ReturnInstruction)instruction).getValueref(), opcode);
        case Const.TABLESWITCH:
            return visit(((TableSwitch)instruction).getKey(), opcode);
        case ByteCodeConstants.TERNARYOPSTORE:
            return visit(((TernaryOpStore)instruction).getObjectref(), opcode);
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            return visit(((IncInstruction)instruction).getValue(), opcode);
        case Const.GETFIELD:
            return visit(((GetField)instruction).getObjectref(), opcode);
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                Instruction tmp = visit(iai.getNewArray(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                if (iai.getValues() != null) {
                    return visit(iai.getValues(), opcode);
                }
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                Instruction tmp = visit(to.getValue1(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                return visit(to.getValue2(), opcode);
            }
        case FastConstants.TRY:
            {
                FastTry ft = (FastTry)instruction;
                Instruction tmp = visit(ft.getInstructions(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                List<FastCatch> catches = ft.getCatches();
                for (int i=catches.size()-1; i>=0; --i)
                {
                    tmp = visit(catches.get(i).instructions(), opcode);
                    if (tmp != null) {
                        return tmp;
                    }
                }
                if (ft.getFinallyInstructions() != null) {
                    return visit(ft.getFinallyInstructions(), opcode);
                }
            }
            break;
        case FastConstants.SYNCHRONIZED:
            {
                FastSynchronized fsy = (FastSynchronized)instruction;
                Instruction tmp = visit(fsy.getMonitor(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                return visit(fsy.getInstructions(), opcode);
            }
        case FastConstants.FOR:
            {
                FastFor ff = (FastFor)instruction;
                if (ff.getInit() != null)
                {
                    Instruction tmp = visit(ff.getInit(), opcode);
                    if (tmp != null) {
                        return tmp;
                    }
                }
                if (ff.getInc() != null)
                {
                    Instruction tmp = visit(ff.getInc(), opcode);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
            // intended fall through
        case FastConstants.WHILE,
             FastConstants.DO_WHILE,
             FastConstants.IF_SIMPLE:
            {
                FastTestList ftl = (FastTestList)instruction;
                if (ftl.getTest() != null)
                {
                    Instruction tmp = visit(ftl.getTest(), opcode);
                    if (tmp != null) {
                        return tmp;
                    }
                }
            }
            // intended fall through
        case FastConstants.INFINITE_LOOP:
            {
                List<Instruction> instructions =
                        ((FastList)instruction).getInstructions();
                if (instructions != null) {
                    return visit(instructions, opcode);
                }
            }
            break;
        case FastConstants.FOREACH:
            {
                FastForEach ffe = (FastForEach)instruction;
                Instruction tmp = visit(ffe.getVariable(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                tmp = visit(ffe.getValues(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                return visit(ffe.getInstructions(), opcode);
            }
        case FastConstants.IF_ELSE:
            {
                FastTest2Lists ft2l = (FastTest2Lists)instruction;
                Instruction tmp = visit(ft2l.getTest(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                tmp = visit(ft2l.getInstructions(), opcode);
                if (tmp != null) {
                    return tmp;
                }
                return visit(ft2l.getInstructions2(), opcode);
            }
        case FastConstants.IF_CONTINUE,
             FastConstants.IF_BREAK,
             FastConstants.IF_LABELED_BREAK,
             FastConstants.GOTO_CONTINUE,
             FastConstants.GOTO_BREAK,
             FastConstants.GOTO_LABELED_BREAK:
            {
                FastInstruction fi = (FastInstruction)instruction;
                if (fi.getInstruction() != null) {
                    return visit(fi.getInstruction(), opcode);
                }
            }
            break;
        case FastConstants.DECLARE:
            {
                FastDeclaration fd = (FastDeclaration)instruction;
                if (fd.getInstruction() != null) {
                    return visit(fd.getInstruction(), opcode);
                }
            }
            break;
        case FastConstants.SWITCH,
             FastConstants.SWITCH_ENUM,
             FastConstants.SWITCH_STRING:
            {
                FastSwitch fs = (FastSwitch)instruction;
                Instruction tmp = visit(fs.getTest(), opcode);
                if (tmp != null) {
                    return tmp;
                }

                Pair[] pairs = fs.getPairs();
                for (int i=pairs.length-1; i>=0; --i)
                {
                    List<Instruction> instructions = pairs[i].getInstructions();
                    if (instructions != null)
                    {
                        tmp = visit(instructions, opcode);
                        if (tmp != null) {
                            return tmp;
                        }
                    }
                }
            }
            break;
        case FastConstants.LABEL:
            {
                FastLabel fla = (FastLabel)instruction;
                if (fla.getInstruction() != null) {
                    return visit(fla.getInstruction(), opcode);
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
             ByteCodeConstants.EXCEPTIONLOAD,
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
             ByteCodeConstants.RETURNADDRESSLOAD,
             Const.SIPUSH:
            break;
        default:
            System.err.println(
                    "Can not search instruction in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }

        return null;
    }

    private static Instruction visit(List<Instruction> instructions, int opcode)
        throws RuntimeException
    {
        for (int i=instructions.size()-1; i>=0; --i)
        {
            Instruction instruction = visit(instructions.get(i), opcode);
            if (instruction != null) {
                return instruction;
            }
        }

        return null;
    }
}
