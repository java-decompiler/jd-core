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
package jd.core.process.analyzer.instruction.fast.visitor;

import org.apache.bcel.Const;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
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
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
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
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTestList;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;

public class CountDupLoadVisitor
{
    private DupStore dupStore;
    private int counter;

    public CountDupLoadVisitor()
    {
        init(null);
    }

    public void init(DupStore dupStore)
    {
        this.dupStore = dupStore;
        this.counter = 0;
    }

    public void visit(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            visit(((ArrayLength)instruction).getArrayref());
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                visit(asi.getArrayref());
                visit(asi.getIndexref());
                visit(asi.getValueref());
            }
            break;
        case Const.ATHROW:
            visit(((AThrow)instruction).getValue());
            break;
        case ByteCodeConstants.UNARYOP:
            visit(((UnaryOperatorInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
                visit(boi.getValue1());
                visit(boi.getValue2());
            }
            break;
        case Const.CHECKCAST:
            visit(((CheckCast)instruction).getObjectref());
            break;
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            visit(((StoreInstruction)instruction).getValueref());
            break;
        case ByteCodeConstants.DUPSTORE:
            visit(((DupStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            visit(((ConvertInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmp = (IfCmp)instruction;
                visit(ifCmp.getValue1());
                visit(ifCmp.getValue2());
            }
            break;
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            visit(((IfInstruction)instruction).getValue());
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                {
                    visit(branchList.get(i));
                }
            }
            break;
        case Const.INSTANCEOF:
            visit(((InstanceOf)instruction).getObjectref());
            break;
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            visit(((InvokeNoStaticInstruction)instruction).getObjectref());
            // intended fall through
        case Const.INVOKESTATIC:
            {
                List<Instruction> list = ((InvokeInstruction)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                {
                    visit(list.get(i));
                }
            }
            break;
        case ByteCodeConstants.INVOKENEW:
            {
                List<Instruction> list = ((InvokeNew)instruction).getArgs();
                for (int i=list.size()-1; i>=0; --i)
                {
                    visit(list.get(i));
                }
            }
            break;
        case Const.LOOKUPSWITCH:
            visit(((LookupSwitch)instruction).getKey());
            break;
        case Const.MONITORENTER:
            visit(((MonitorEnter)instruction).getObjectref());
            break;
        case Const.MONITOREXIT:
            visit(((MonitorExit)instruction).getObjectref());
            break;
        case Const.MULTIANEWARRAY:
            {
                Instruction[] dimensions = ((MultiANewArray)instruction).getDimensions();
                for (int i=dimensions.length-1; i>=0; --i)
                {
                    visit(dimensions[i]);
                }
            }
            break;
        case Const.NEWARRAY:
            visit(((NewArray)instruction).getDimension());
            break;
        case Const.ANEWARRAY:
            visit(((ANewArray)instruction).getDimension());
            break;
        case Const.POP:
            visit(((Pop)instruction).getObjectref());
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                visit(putField.getObjectref());
                visit(putField.getValueref());
            }
            break;
        case Const.PUTSTATIC:
            visit(((PutStatic)instruction).getValueref());
            break;
        case ByteCodeConstants.XRETURN:
            visit(((ReturnInstruction)instruction).getValueref());
            break;
        case Const.TABLESWITCH:
            visit(((TableSwitch)instruction).getKey());
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            visit(((TernaryOpStore)instruction).getObjectref());
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                visit(to.getValue1());
                visit(to.getValue2());
            }
            break;
        case ByteCodeConstants.ASSIGNMENT:
            {
                AssignmentInstruction ai = (AssignmentInstruction)instruction;
                visit(ai.getValue1());
                visit(ai.getValue2());
            }
            break;
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                visit(ali.getArrayref());
                visit(ali.getIndexref());
            }
            break;
        case ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC:
            visit(((IncInstruction)instruction).getValue());
            break;
        case Const.GETFIELD:
            visit(((GetField)instruction).getObjectref());
            break;
        case ByteCodeConstants.DUPLOAD:
            {
                if (((DupLoad)instruction).getDupStore() == this.dupStore) {
                    this.counter++;
                }
            }
            break;
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                visit(iai.getNewArray());
                if (iai.getValues() != null) {
                    visit(iai.getValues());
                }
            }
            break;
        case FastConstants.FOR:
            {
                FastFor ff = (FastFor)instruction;
                if (ff.getInit() != null) {
                    visit(ff.getInit());
                }
                if (ff.getInc() != null) {
                    visit(ff.getInc());
                }
            }
            // intended fall through
        case FastConstants.WHILE,
             FastConstants.DO_WHILE,
             FastConstants.IF_SIMPLE:
            {
                Instruction test = ((FastTestList)instruction).getTest();
                if (test != null) {
                    visit(test);
                }
            }
            // intended fall through
        case FastConstants.INFINITE_LOOP:
            {
                List<Instruction> instructions =
                    ((FastList)instruction).getInstructions();
                if (instructions != null) {
                    visit(instructions);
                }
            }
            break;
        case FastConstants.FOREACH:
            {
                FastForEach ffe = (FastForEach)instruction;
                visit(ffe.getVariable());
                visit(ffe.getValues());
                visit(ffe.getInstructions());
            }
            break;
        case FastConstants.IF_ELSE:
            {
                FastTest2Lists ft2l = (FastTest2Lists)instruction;
                visit(ft2l.getTest());
                visit(ft2l.getInstructions());
                visit(ft2l.getInstructions2());
            }
            break;
        case FastConstants.IF_CONTINUE,
             FastConstants.IF_BREAK,
             FastConstants.IF_LABELED_BREAK,
             FastConstants.GOTO_CONTINUE,
             FastConstants.GOTO_BREAK,
             FastConstants.GOTO_LABELED_BREAK:
            {
                FastInstruction fi = (FastInstruction)instruction;
                if (fi.getInstruction() != null) {
                    visit(fi.getInstruction());
                }
            }
            break;
        case FastConstants.SWITCH,
             FastConstants.SWITCH_ENUM,
             FastConstants.SWITCH_STRING:
            {
                FastSwitch fs = (FastSwitch)instruction;
                visit(fs.getTest());
                FastSwitch.Pair[] pairs = fs.getPairs();
                for (int i=pairs.length-1; i>=0; --i)
                {
                    List<Instruction> instructions = pairs[i].getInstructions();
                    if (instructions != null) {
                        visit(instructions);
                    }
                }
            }
            break;
        case FastConstants.TRY:
            {
                FastTry ft = (FastTry)instruction;
                visit(ft.getInstructions());
                if (ft.getFinallyInstructions() != null) {
                    visit(ft.getFinallyInstructions());
                }
                List<FastCatch> catchs = ft.getCatches();
                for (int i=catchs.size()-1; i>=0; --i) {
                    visit(catchs.get(i).instructions());
                }
            }
            break;
        case FastConstants.SYNCHRONIZED:
            {
                FastSynchronized fsd = (FastSynchronized)instruction;
                visit(fsd.getMonitor());
                visit(fsd.getInstructions());
            }
            break;
        case FastConstants.LABEL:
            {
                FastLabel fl = (FastLabel)instruction;
                if (fl.getInstruction() != null) {
                    visit(fl.getInstruction());
                }
            }
            break;
        case FastConstants.DECLARE:
            {
                FastDeclaration fd = (FastDeclaration)instruction;
                if (fd.getInstruction() != null) {
                    visit(fd.getInstruction());
                }
            }
            break;
        case Const.GETSTATIC,
             ByteCodeConstants.OUTERTHIS,
             Const.ACONST_NULL,
             ByteCodeConstants.LOAD,
             Const.ALOAD,
             Const.ILOAD,
             Const.BIPUSH,
             ByteCodeConstants.ICONST,
             ByteCodeConstants.LCONST,
             ByteCodeConstants.FCONST,
             ByteCodeConstants.DCONST,
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
                    "Can not count DupLoad in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }
    }

    private void visit(List<Instruction> instructions)
    {
        for (int i=instructions.size()-1; i>=0; --i) {
            visit(instructions.get(i));
        }
    }

    /**
     * @return le dernier parent sur lequel une substitution a été faite
     */
    public int getCounter()
    {
        return this.counter;
    }
}
