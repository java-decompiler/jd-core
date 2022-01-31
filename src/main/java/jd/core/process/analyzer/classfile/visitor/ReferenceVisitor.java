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
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantFieldref;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
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
import jd.core.model.instruction.bytecode.instruction.IndexInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.New;
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
import jd.core.model.reference.ReferenceMap;
import jd.core.util.SignatureUtil;

public class ReferenceVisitor
{
    private final ConstantPool constants;
    private final ReferenceMap referenceMap;

    public ReferenceVisitor(ConstantPool constants, ReferenceMap referenceMap)
    {
        this.constants = constants;
        this.referenceMap = referenceMap;
    }

    public void visit(Instruction instruction)
    {
        if (instruction == null) {
            return;
        }

        String internalName;

        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            {
                ArrayLength al = (ArrayLength)instruction;
                visit(al.getArrayref());
            }
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                visit(asi.getValueref());
            }
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                visit(ai.getTest());
                visit(ai.getMsg());
            }
            break;
        case Const.ATHROW:
            {
                AThrow aThrow = (AThrow)instruction;
                visit(aThrow.getValue());
            }
            break;
        case ByteCodeConstants.UNARYOP:
            {
                UnaryOperatorInstruction uoi = (UnaryOperatorInstruction)instruction;
                visit(uoi.getValue());
            }
            break;
        case ByteCodeConstants.BINARYOP,
             ByteCodeConstants.ASSIGNMENT:
            {
                BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
                visit(boi.getValue1());
                visit(boi.getValue2());
            }
            break;
        case Const.CHECKCAST:
            {
                CheckCast checkCast = (CheckCast)instruction;
                visitCheckCastAndMultiANewArray(checkCast.getIndex());
                visit(checkCast.getObjectref());
            }
            break;
        case ByteCodeConstants.STORE,
             Const.ISTORE:
            {
                StoreInstruction storeInstruction = (StoreInstruction)instruction;
                visit(storeInstruction.getValueref());
            }
            break;
        case Const.ASTORE:
            {
                StoreInstruction storeInstruction = (StoreInstruction)instruction;
                visit(storeInstruction.getValueref());
            }
            break;
        case ByteCodeConstants.DUPSTORE:
            {
                DupStore dupStore = (DupStore)instruction;
                visit(dupStore.getObjectref());
            }
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            {
                ConvertInstruction ci = (ConvertInstruction)instruction;
                visit(ci.getValue());
            }
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
            {
                IfInstruction iff = (IfInstruction)instruction;
                visit(iff.getValue());
            }
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                List<Instruction> branchList =
                    ((ComplexConditionalBranchInstruction)instruction).getInstructions();
                for (int i=branchList.size()-1; i>=0; --i) {
                    visit(branchList.get(i));
                }
            }
            break;
        case Const.INSTANCEOF:
            {
                InstanceOf instanceOf = (InstanceOf)instruction;
                visitCheckCastAndMultiANewArray(instanceOf.getIndex());
                visit(instanceOf.getObjectref());
            }
            break;
        case Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL,
             Const.INVOKEVIRTUAL:
            InvokeNoStaticInstruction insi =
                (InvokeNoStaticInstruction)instruction;
                visit(insi.getObjectref());
                // intended fall through
        case Const.INVOKESTATIC,
             ByteCodeConstants.INVOKENEW:
            {
                InvokeInstruction ii = (InvokeInstruction)instruction;
                ConstantMethodref cmr = constants.getConstantMethodref(ii.getIndex());
                internalName = constants.getConstantClassName(cmr.getClassIndex());
                addReference(internalName);
                visit(ii.getArgs());
            }
            break;
        case Const.LOOKUPSWITCH:
            {
                LookupSwitch ls = (LookupSwitch)instruction;
                visit(ls.getKey());
            }
            break;
        case Const.MONITORENTER:
            {
                MonitorEnter monitorEnter = (MonitorEnter)instruction;
                visit(monitorEnter.getObjectref());
            }
            break;
        case Const.MONITOREXIT:
            {
                MonitorExit monitorExit = (MonitorExit)instruction;
                visit(monitorExit.getObjectref());
            }
            break;
        case Const.MULTIANEWARRAY:
            {
                MultiANewArray multiANewArray = (MultiANewArray)instruction;
                visitCheckCastAndMultiANewArray(multiANewArray.getIndex());
                Instruction[] dimensions = multiANewArray.getDimensions();
                for (int i=dimensions.length-1; i>=0; --i) {
                    visit(dimensions[i]);
                }
            }
            break;
        case Const.NEWARRAY:
            {
                NewArray newArray = (NewArray)instruction;
                visit(newArray.getDimension());
            }
            break;
        case Const.NEW:
            {
                New aNew = (New)instruction;
                addReference(this.constants.getConstantClassName(aNew.getIndex()));
            }
            break;
        case Const.ANEWARRAY:
            {
                ANewArray aNewArray = (ANewArray)instruction;
                addReference(
                    this.constants.getConstantClassName(aNewArray.getIndex()));
                visit(aNewArray.getDimension());
            }
            break;
        case Const.POP:
            {
                Pop pop = (Pop)instruction;
                visit(pop.getObjectref());
            }
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                visit(putField.getObjectref());
                visit(putField.getValueref());
            }
            break;
        case Const.PUTSTATIC:
            {
                PutStatic putStatic = (PutStatic)instruction;
                visit(putStatic.getValueref());
            }
            break;
        case ByteCodeConstants.XRETURN:
            {
                ReturnInstruction ri = (ReturnInstruction)instruction;
                visit(ri.getValueref());
            }
            break;
        case Const.TABLESWITCH:
            {
                TableSwitch ts = (TableSwitch)instruction;
                visit(ts.getKey());
            }
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            {
                TernaryOpStore tos = (TernaryOpStore)instruction;
                visit(tos.getObjectref());
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                visit(to.getTest());
                visit(to.getValue1());
                visit(to.getValue2());
            }
            break;
        case Const.GETFIELD:
            GetField getField = (GetField)instruction;
            visit(getField.getObjectref());
            // intended fall through
        case Const.GETSTATIC,
             ByteCodeConstants.OUTERTHIS:
            {
                IndexInstruction indexInstruction = (IndexInstruction)instruction;
                ConstantFieldref cfr = constants.getConstantFieldref(indexInstruction.getIndex());
                internalName = constants.getConstantClassName(cfr.getClassIndex());
                addReference(internalName);
            }
            break;
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iai = (InitArrayInstruction)instruction;
                visit(iai.getNewArray());
                for (int index=iai.getValues().size()-1; index>=0; --index) {
                    visit(iai.getValues().get(index));
                }
            }
            break;
        case FastConstants.FOR:
            FastFor ff = (FastFor)instruction;
            visit(ff.getInit());
            visit(ff.getInc());
            // intended fall through
        case FastConstants.WHILE,
             FastConstants.DO_WHILE,
             FastConstants.IF_SIMPLE:
            FastTestList ftl = (FastTestList)instruction;
            visit(ftl.getTest());
            // intended fall through
        case FastConstants.INFINITE_LOOP:
            {
                List<Instruction> instructions =
                        ((FastList)instruction).getInstructions();
                visit(instructions);
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
                visit(fi.getInstruction());
            }
            break;
        case FastConstants.SWITCH,
             FastConstants.SWITCH_ENUM,
             FastConstants.SWITCH_STRING:
            {
                FastSwitch fs = (FastSwitch)instruction;
                visit(fs.getTest());
                Pair[] pairs = fs.getPairs();
                for (int i=pairs.length-1; i>=0; --i)
                {
                    List<Instruction> instructions = pairs[i].getInstructions();
                    visit(instructions);
                }
            }
            break;
        case FastConstants.DECLARE:
            {
                FastDeclaration fd = (FastDeclaration)instruction;
                visit(fd.getInstruction());
            }
            break;
        case FastConstants.TRY:
            {
                FastTry ft = (FastTry)instruction;
                visit(ft.getInstructions());
                List<FastCatch> catches = ft.getCatches();
                for (int i=catches.size()-1; i>=0; --i) {
                    visit(catches.get(i).instructions());
                }
                visit(ft.getFinallyInstructions());
            }
            break;
        case FastConstants.SYNCHRONIZED:
            {
                FastSynchronized fsy = (FastSynchronized)instruction;
                visit(fsy.getMonitor());
                visit(fsy.getInstructions());
            }
            break;
        case FastConstants.LABEL:
            {
                FastLabel fla = (FastLabel)instruction;
                visit(fla.getInstruction());
            }
            break;
        case Const.LDC:
            {
                IndexInstruction indexInstruction = (IndexInstruction)instruction;
                Constant cst = constants.get(indexInstruction.getIndex());
                if (cst instanceof ConstantClass)
                {
                    // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
                    ConstantClass cc = (ConstantClass) cst;
                    internalName = constants.getConstantUtf8(cc.getNameIndex());
                    addReference(internalName);
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
             Const.GOTO,
             Const.IINC,
             ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC,
             Const.JSR,
             Const.LDC2_W,
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
                    "Can not count reference in " +
                    instruction.getClass().getName() +
                    ", opcode=" + instruction.getOpcode());
        }
    }

    private void visit(List<Instruction> instructions)
    {
        if (instructions != null)
        {
            for (int i=instructions.size()-1; i>=0; --i) {
                visit(instructions.get(i));
            }
        }
    }

    private void visitCheckCastAndMultiANewArray(int index)
    {
        Constant c = constants.get(index);

        if (c instanceof ConstantClass)
        {
            // to convert to jdk16 pattern matching only when spotbugs #1617 and eclipse #577987 are solved
            ConstantClass cc = (ConstantClass) c;
            addReference(constants.getConstantUtf8(cc.getNameIndex()));
        }
    }

    private void addReference(String signature)
    {
        if (signature.charAt(0) == '[')
        {
            signature = SignatureUtil.cutArrayDimensionPrefix(signature);

            if (signature.charAt(0) == 'L') {
                referenceMap.add(SignatureUtil.getInnerName(signature));
            }
        }
        else
        {
            referenceMap.add(signature);
        }
    }
}
