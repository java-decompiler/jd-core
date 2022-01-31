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
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.List;

import jd.core.model.classfile.ConstantPool;
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
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Invokevirtual;
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

public class ReplaceStringBuxxxerVisitor
{
    private final ConstantPool constants;

    public ReplaceStringBuxxxerVisitor(ConstantPool constants)
    {
        this.constants = constants;
    }

    public void visit(Instruction instruction)
    {
        switch (instruction.getOpcode())
        {
        case Const.ARRAYLENGTH:
            {
                ArrayLength al = (ArrayLength)instruction;
                Instruction i = match(al.getArrayref());
                if (i == null) {
                    visit(al.getArrayref());
                } else {
                    al.setArrayref(i);
                }
            }
            break;
        case ByteCodeConstants.ARRAYLOAD:
            {
                ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
                Instruction i = match(ali.getArrayref());
                if (i == null) {
                    visit(ali.getArrayref());
                } else {
                    ali.setArrayref(i);
                }

                i = match(ali.getIndexref());
                if (i == null) {
                    visit(ali.getIndexref());
                } else {
                    ali.setIndexref(i);
                }
            }
            break;
        case Const.AASTORE,
             ByteCodeConstants.ARRAYSTORE:
            {
                ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
                Instruction i = match(asi.getArrayref());
                if (i == null) {
                    visit(asi.getArrayref());
                } else {
                    asi.setArrayref(i);
                }

                i = match(asi.getIndexref());
                if (i == null) {
                    visit(asi.getIndexref());
                } else {
                    asi.setIndexref(i);
                }

                i = match(asi.getValueref());
                if (i == null) {
                    visit(asi.getValueref());
                } else {
                    asi.setValueref(i);
                }
            }
            break;
        case ByteCodeConstants.ASSERT:
            {
                AssertInstruction ai = (AssertInstruction)instruction;
                Instruction i = match(ai.getTest());
                if (i == null) {
                    visit(ai.getTest());
                } else {
                    ai.setTest(i);
                }
                if (ai.getMsg() != null)
                {
                    i = match(ai.getMsg());
                    if (i == null) {
                        visit(ai.getMsg());
                    } else {
                        ai.setMsg(i);
                    }
                }
            }
            break;
        case ByteCodeConstants.ASSIGNMENT:
            {
                AssignmentInstruction ai = (AssignmentInstruction)instruction;
                Instruction i = match(ai.getValue1());
                if (i == null) {
                    visit(ai.getValue1());
                } else {
                    ai.setValue1(i);
                }

                i = match(ai.getValue2());
                if (i == null) {
                    visit(ai.getValue2());
                } else {
                    ai.setValue2(i);
                }
            }
            break;
        case Const.ATHROW:
            {
                AThrow aThrow = (AThrow)instruction;
                visit(aThrow.getValue());
            }
            break;
        case ByteCodeConstants.BINARYOP:
            {
                BinaryOperatorInstruction boi =
                    (BinaryOperatorInstruction)instruction;
                Instruction i = match(boi.getValue1());
                if (i == null) {
                    visit(boi.getValue1());
                } else {
                    boi.setValue1(i);
                }

                i = match(boi.getValue2());
                if (i == null) {
                    visit(boi.getValue2());
                } else {
                    boi.setValue2(i);
                }
            }
            break;
        case ByteCodeConstants.UNARYOP:
            {
                UnaryOperatorInstruction uoi =
                    (UnaryOperatorInstruction)instruction;
                Instruction i = match(uoi.getValue());
                if (i == null) {
                    visit(uoi.getValue());
                } else {
                    uoi.setValue(i);
                }
            }
            break;
        case ByteCodeConstants.DUPSTORE:
            {
                DupStore dupStore = (DupStore)instruction;
                Instruction i = match(dupStore.getObjectref());
                if (i == null) {
                    visit(dupStore.getObjectref());
                } else {
                    dupStore.setObjectref(i);
                }
            }
            break;
        case Const.CHECKCAST:
            {
                CheckCast cc = (CheckCast)instruction;
                Instruction i = match(cc.getObjectref());
                if (i == null) {
                    visit(cc.getObjectref());
                } else {
                    cc.setObjectref(i);
                }
            }
            break;
        case ByteCodeConstants.CONVERT,
             ByteCodeConstants.IMPLICITCONVERT:
            {
                ConvertInstruction ci = (ConvertInstruction)instruction;
                Instruction i = match(ci.getValue());
                if (i == null) {
                    visit(ci.getValue());
                } else {
                    ci.setValue(i);
                }
            }
            break;
        case ByteCodeConstants.IF,
             ByteCodeConstants.IFXNULL:
            {
                IfInstruction ifInstruction = (IfInstruction)instruction;
                Instruction i = match(ifInstruction.getValue());
                if (i == null) {
                    visit(ifInstruction.getValue());
                } else {
                    ifInstruction.setValue(i);
                }
            }
            break;
        case ByteCodeConstants.IFCMP:
            {
                IfCmp ifCmpInstruction = (IfCmp)instruction;
                Instruction i = match(ifCmpInstruction.getValue1());
                if (i == null) {
                    visit(ifCmpInstruction.getValue1());
                } else {
                    ifCmpInstruction.setValue1(i);
                }

                i = match(ifCmpInstruction.getValue2());
                if (i == null) {
                    visit(ifCmpInstruction.getValue2());
                } else {
                    ifCmpInstruction.setValue2(i);
                }
            }
            break;
        case Const.INSTANCEOF:
            {
                InstanceOf instanceOf = (InstanceOf)instruction;
                Instruction i = match(instanceOf.getObjectref());
                if (i == null) {
                    visit(instanceOf.getObjectref());
                } else {
                    instanceOf.setObjectref(i);
                }
            }
            break;
        case ByteCodeConstants.COMPLEXIF:
            {
                ComplexConditionalBranchInstruction complexIf = (ComplexConditionalBranchInstruction)instruction;
                List<Instruction> branchList = complexIf.getInstructions();
                for (int i=branchList.size()-1; i>=0; --i)
                {
                    visit(branchList.get(i));
                }
            }
            break;
        case Const.GETFIELD:
            {
                GetField getField = (GetField)instruction;
                Instruction i = match(getField.getObjectref());
                if (i == null) {
                    visit(getField.getObjectref());
                } else {
                    getField.setObjectref(i);
                }
            }
            break;
        case Const.INVOKEVIRTUAL,
             Const.INVOKEINTERFACE,
             Const.INVOKESPECIAL:
            {
                InvokeNoStaticInstruction insi =
                    (InvokeNoStaticInstruction)instruction;
                Instruction i = match(insi.getObjectref());
                if (i == null) {
                    visit(insi.getObjectref());
                } else {
                    insi.setObjectref(i);
                }
                replaceInArgs(insi.getArgs());
            }
            break;
        case Const.INVOKESTATIC,
             ByteCodeConstants.INVOKENEW:
            replaceInArgs(((InvokeInstruction)instruction).getArgs());
            break;
        case Const.LOOKUPSWITCH:
            {
                LookupSwitch lookupSwitch = (LookupSwitch)instruction;
                Instruction i = match(lookupSwitch.getKey());
                if (i == null) {
                    visit(lookupSwitch.getKey());
                } else {
                    lookupSwitch.setKey(i);
                }
            }
            break;
        case Const.MULTIANEWARRAY:
            {
                MultiANewArray multiANewArray = (MultiANewArray)instruction;
                Instruction[] dimensions = multiANewArray.getDimensions();
                Instruction ins;

                for (int i=dimensions.length-1; i>=0; i--)
                {
                    ins = match(dimensions[i]);
                    if (ins == null) {
                        visit(dimensions[i]);
                    } else {
                        dimensions[i] = ins;
                    }
                }
            }
            break;
        case Const.NEWARRAY:
            {
                NewArray newArray = (NewArray)instruction;
                Instruction i = match(newArray.getDimension());
                if (i == null) {
                    visit(newArray.getDimension());
                } else {
                    newArray.setDimension(i);
                }
            }
            break;
        case Const.ANEWARRAY:
            {
                ANewArray newArray = (ANewArray)instruction;
                Instruction i = match(newArray.getDimension());
                if (i == null) {
                    visit(newArray.getDimension());
                } else {
                    newArray.setDimension(i);
                }
            }
            break;
        case Const.POP:
            visit(((Pop)instruction).getObjectref());
            break;
        case Const.PUTFIELD:
            {
                PutField putField = (PutField)instruction;
                Instruction i = match(putField.getObjectref());
                if (i == null) {
                    visit(putField.getObjectref());
                } else {
                    putField.setObjectref(i);
                }

                i = match(putField.getValueref());
                if (i == null) {
                    visit(putField.getValueref());
                } else {
                    putField.setValueref(i);
                }
            }
            break;
        case Const.PUTSTATIC:
            {
                PutStatic putStatic = (PutStatic)instruction;
                Instruction i = match(putStatic.getValueref());
                if (i == null) {
                    visit(putStatic.getValueref());
                } else {
                    putStatic.setValueref(i);
                }
            }
            break;
        case ByteCodeConstants.XRETURN:
            {
                ReturnInstruction returnInstruction =
                    (ReturnInstruction)instruction;
                Instruction i = match(returnInstruction.getValueref());
                if (i == null) {
                    visit(returnInstruction.getValueref());
                } else {
                    returnInstruction.setValueref(i);
                }
            }
            break;
        case ByteCodeConstants.STORE,
             Const.ASTORE,
             Const.ISTORE:
            {
                StoreInstruction storeInstruction =
                    (StoreInstruction)instruction;
                Instruction i = match(storeInstruction.getValueref());
                if (i == null) {
                    visit(storeInstruction.getValueref());
                } else {
                    storeInstruction.setValueref(i);
                }
            }
            break;
        case Const.TABLESWITCH:
            {
                TableSwitch tableSwitch = (TableSwitch)instruction;
                Instruction i = match(tableSwitch.getKey());
                if (i == null) {
                    visit(tableSwitch.getKey());
                } else {
                    tableSwitch.setKey(i);
                }
            }
            break;
        case ByteCodeConstants.TERNARYOPSTORE:
            {
                TernaryOpStore tosInstruction = (TernaryOpStore)instruction;
                Instruction i = match(tosInstruction.getObjectref());
                if (i == null) {
                    visit(tosInstruction.getObjectref());
                } else {
                    tosInstruction.setObjectref(i);
                }
            }
            break;
        case ByteCodeConstants.TERNARYOP:
            {
                TernaryOperator to = (TernaryOperator)instruction;
                Instruction i = match(to.getValue1());
                if (i == null) {
                    visit(to.getValue1());
                } else {
                    to.setValue1(i);
                }

                i = match(to.getValue2());
                if (i == null) {
                    visit(to.getValue2());
                } else {
                    to.setValue2(i);
                }
            }
            break;
        case Const.MONITORENTER:
            {
                MonitorEnter meInstruction = (MonitorEnter)instruction;
                Instruction i = match(meInstruction.getObjectref());
                if (i == null) {
                    visit(meInstruction.getObjectref());
                } else {
                    meInstruction.setObjectref(i);
                }
            }
            break;
        case Const.MONITOREXIT:
            {
                MonitorExit meInstruction = (MonitorExit)instruction;
                Instruction i = match(meInstruction.getObjectref());
                if (i == null) {
                    visit(meInstruction.getObjectref());
                } else {
                    meInstruction.setObjectref(i);
                }
            }
            break;
        case ByteCodeConstants.INITARRAY,
             ByteCodeConstants.NEWANDINITARRAY:
            {
                InitArrayInstruction iaInstruction =
                    (InitArrayInstruction)instruction;
                Instruction i = match(iaInstruction.getNewArray());
                if (i == null) {
                    visit(iaInstruction.getNewArray());
                } else {
                    iaInstruction.setNewArray(i);
                }

                for (int index=iaInstruction.getValues().size()-1; index>=0; --index)
                {
                    i = match(iaInstruction.getValues().get(index));
                    if (i == null) {
                        visit(iaInstruction.getValues().get(index));
                    } else {
                        iaInstruction.getValues().set(index, i);
                    }
                }
            }
            break;
        case Const.ACONST_NULL,
             ByteCodeConstants.DUPLOAD,
             Const.LDC,
             Const.LDC2_W,
             Const.NEW,
             Const.RETURN,
             Const.BIPUSH,
             ByteCodeConstants.DCONST,
             ByteCodeConstants.FCONST,
             ByteCodeConstants.ICONST,
             ByteCodeConstants.LCONST,
             Const.IINC,
             ByteCodeConstants.PREINC,
             ByteCodeConstants.POSTINC,
             Const.JSR,
             Const.GETSTATIC,
             ByteCodeConstants.OUTERTHIS,
             Const.SIPUSH,
             ByteCodeConstants.LOAD,
             Const.ALOAD,
             Const.ILOAD,
             Const.GOTO,
             Const.INVOKEDYNAMIC,
             ByteCodeConstants.EXCEPTIONLOAD,
             Const.RET,
             ByteCodeConstants.RETURNADDRESSLOAD:
            break;
        default:
            System.err.println(
                    "Can not replace StringBuxxxer in " +
                    instruction.getClass().getName() + " " +
                    instruction.getOpcode());
        }
    }

    private void replaceInArgs(List<Instruction> args)
    {
        if (!args.isEmpty())
        {
            Instruction ins;

            for (int i=args.size()-1; i>=0; --i)
            {
                ins = match(args.get(i));
                if (ins == null) {
                    visit(args.get(i));
                } else {
                    args.set(i, ins);
                }
            }
        }
    }

    private Instruction match(Instruction i)
    {
        if (i.getOpcode() == Const.INVOKEVIRTUAL)
        {
            Invokevirtual iv = (Invokevirtual)i;
            ConstantMethodref cmr = this.constants.getConstantMethodref(iv.getIndex());
            ConstantClass cc = this.constants.getConstantClass(cmr.getClassIndex());

            if (cc.getNameIndex() == constants.getStringBufferClassNameIndex() ||
                cc.getNameIndex() == constants.getStringBuilderClassNameIndex())
            {
                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                if (cnat.getNameIndex() == constants.getToStringIndex()) {
                    return match(iv.getObjectref(), cmr.getClassIndex());
                }
            }
        }

        return null;
    }

    private Instruction match(Instruction i, int classIndex)
    {
        if (i.getOpcode() == Const.INVOKEVIRTUAL)
        {
            InvokeNoStaticInstruction insi = (InvokeNoStaticInstruction)i;
            ConstantMethodref cmr =
                this.constants.getConstantMethodref(insi.getIndex());

            if (cmr.getClassIndex() == classIndex)
            {
                ConstantNameAndType cnat =
                    constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                if (cnat.getNameIndex() == this.constants.getAppendIndex() &&
                    insi.getArgs().size() == 1)
                {
                    Instruction result = match(insi.getObjectref(), cmr.getClassIndex());

                    if (result == null)
                    {
                        return insi.getArgs().get(0);
                    }
                    return new BinaryOperatorInstruction(
                        ByteCodeConstants.BINARYOP, i.getOffset(), i.getLineNumber(),
                        4,  StringConstants.INTERNAL_STRING_SIGNATURE, "+",
                        result, insi.getArgs().get(0));
                }
            }
        }
        else if (i.getOpcode() == ByteCodeConstants.INVOKENEW)
        {
            InvokeNew in = (InvokeNew)i;
            ConstantMethodref cmr =
                this.constants.getConstantMethodref(in.getIndex());

            if (cmr.getClassIndex() == classIndex && in.getArgs().size() == 1)
            {
                Instruction arg0 = in.getArgs().get(0);

                // Remove String.valueOf for String
                if (arg0.getOpcode() == Const.INVOKESTATIC)
                {
                    Invokestatic is = (Invokestatic)arg0;
                    cmr = this.constants.getConstantMethodref(is.getIndex());
                    ConstantClass cc = this.constants.getConstantClass(cmr.getClassIndex());

                    if (cc.getNameIndex() == this.constants.getStringClassNameIndex())
                    {
                        ConstantNameAndType cnat =
                            this.constants.getConstantNameAndType(cmr.getNameAndTypeIndex());

                        if (cnat.getNameIndex() == this.constants.getValueOfIndex() &&
                            is.getArgs().size() == 1) {
                            return is.getArgs().get(0);
                        }
                    }
                }

                return arg0;
            }
        }

        return null;
    }
}
