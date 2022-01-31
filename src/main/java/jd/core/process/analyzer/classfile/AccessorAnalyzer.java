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
package jd.core.process.analyzer.classfile;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.accessor.AccessorConstants;
import jd.core.model.classfile.accessor.GetFieldAccessor;
import jd.core.model.classfile.accessor.GetStaticAccessor;
import jd.core.model.classfile.accessor.InvokeMethodAccessor;
import jd.core.model.classfile.accessor.PutFieldAccessor;
import jd.core.model.classfile.accessor.PutStaticAccessor;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ALoad;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.util.SignatureUtil;

/*
 * Recherche des accesseurs
 */
public final class AccessorAnalyzer
{
    private AccessorAnalyzer() {
        super();
    }

    public static void analyze(ClassFile classFile, Method method)
    {
        // Recherche des accesseurs de champs statiques
        //   static AuthenticatedSubject access$000()
        if (searchGetStaticAccessor(classFile, method)) {
            return;
        }

        // Recherche des accesseurs de champs statiques
        //   static void access$0(int)
        if (searchPutStaticAccessor(classFile, method)) {
            return;
        }

        // Recherche des accesseurs de champs
        //   static int access$1(TestInnerClass)
        if (searchGetFieldAccessor(classFile, method)) {
            return;
        }

        // Recherche des accesseurs de champs
        //   static void access$0(TestInnerClass, int)
        if (searchPutFieldAccessor(classFile, method)) {
            return;
        }

        // Recherche des accesseurs de méthodes
        //   static void access$100(EntitlementFunctionLibrary, EvaluationCtx, URI, Bag, Bag[])
        searchInvokeMethodAccessor(classFile, method);
    }

    /* Recherche des accesseurs de champs statiques:
     *   static AuthenticatedSubject access$000()
     *   {
     *     Byte code:
     *       getstatic 3    com/bea/security/providers/xacml/entitlement/function/EntitlementFunctionLibrary:kernelId    Lweblogic/security/acl/internal/AuthenticatedSubject;
     *       areturn
     *   }
     */
    private static boolean searchGetStaticAccessor(
        ClassFile classFile, Method method)
    {
        List<Instruction> list = method.getInstructions();
        if (list.size() != 1) {
            return false;
        }

        Instruction instruction = list.get(0);
        if (instruction.getOpcode() != ByteCodeConstants.XRETURN) {
            return false;
        }

        instruction = ((ReturnInstruction)instruction).getValueref();
        if (instruction.getOpcode() != Const.GETSTATIC) {
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();
        ConstantFieldref cfr = constants.getConstantFieldref(
            ((GetStatic)instruction).getIndex());

        if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
            return false;
        }

        String methodDescriptor =
            constants.getConstantUtf8(method.getDescriptorIndex());
        if (methodDescriptor.charAt(1) != ')') {
            return false;
        }

        String methodName = constants.getConstantUtf8(method.getNameIndex());

        ConstantNameAndType cnat = constants.getConstantNameAndType(
            cfr.getNameAndTypeIndex());

        String fieldDescriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
        String fieldName = constants.getConstantUtf8(cnat.getNameIndex());

        // Trouve ! Ajout de l'accesseur.
        classFile.addAccessor(methodName, methodDescriptor,
            new GetStaticAccessor(
                AccessorConstants.ACCESSOR_GETSTATIC,
                classFile.getThisClassName(), fieldName, fieldDescriptor));

        return true;
    }

    /* Recherche des accesseurs de champs statiques:
     *   static void access$0(int)
     *   {
     *     Byte code:
     *       iload_0
     *       putstatic 11 basic/data/TestInnerClass:test0    I
     *       return
     *   }
     */
    private static boolean searchPutStaticAccessor(
        ClassFile classFile, Method method)
    {
        List<Instruction> list = method.getInstructions();
        if (list.size() != 2) {
            return false;
        }

        if (list.get(1).getOpcode() != Const.RETURN) {
            return false;
        }

        Instruction instruction = list.get(0);
        if (instruction.getOpcode() != Const.PUTSTATIC) {
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();
        ConstantFieldref cfr = constants.getConstantFieldref(
            ((PutStatic)instruction).getIndex());

        if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
            return false;
        }

        String methodDescriptor =
            constants.getConstantUtf8(method.getDescriptorIndex());
        if (methodDescriptor.charAt(1) == ')') {
            return false;
        }
        if (SignatureUtil.getParameterSignatureCount(methodDescriptor) != 1) {
            return false;
        }

        String methodName = constants.getConstantUtf8(method.getNameIndex());

        ConstantNameAndType cnat = constants.getConstantNameAndType(
            cfr.getNameAndTypeIndex());

        String fieldDescriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
        String fieldName = constants.getConstantUtf8(cnat.getNameIndex());

        // Trouve ! Ajout de l'accesseur.
        classFile.addAccessor(methodName, methodDescriptor,
            new PutStaticAccessor(
                AccessorConstants.ACCESSOR_PUTSTATIC,
                classFile.getThisClassName(), fieldName, fieldDescriptor));

        return true;
    }

    /* Recherche des accesseurs de champs:
     *   static int access$1(TestInnerClass)
     *   {
     *     Byte code:
     *       aload_0
     *       getfield 12 basic/data/TestInnerClass:test    I
     *       ireturn
     *   }
     */
    private static boolean searchGetFieldAccessor(
        ClassFile classFile, Method method)
    {
        List<Instruction> list = method.getInstructions();
        if (list.size() != 1) {
            return false;
        }

        Instruction instruction = list.get(0);
        if (instruction.getOpcode() != ByteCodeConstants.XRETURN) {
            return false;
        }

        instruction = ((ReturnInstruction)instruction).getValueref();
        if (instruction.getOpcode() != Const.GETFIELD) {
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();
        ConstantFieldref cfr = constants.getConstantFieldref(
            ((GetField)instruction).getIndex());

        if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
            return false;
        }

        String methodDescriptor =
            constants.getConstantUtf8(method.getDescriptorIndex());
        if (methodDescriptor.charAt(1) == ')') {
            return false;
        }
        if (SignatureUtil.getParameterSignatureCount(methodDescriptor) != 1) {
            return false;
        }

        String methodName = constants.getConstantUtf8(method.getNameIndex());

        ConstantNameAndType cnat = constants.getConstantNameAndType(
            cfr.getNameAndTypeIndex());

        String fieldDescriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
        String fieldName = constants.getConstantUtf8(cnat.getNameIndex());

        // Trouve ! Ajout de l'accesseur.
        classFile.addAccessor(methodName, methodDescriptor,
            new GetFieldAccessor(
                AccessorConstants.ACCESSOR_GETFIELD,
                classFile.getThisClassName(), fieldName, fieldDescriptor));

        return true;
    }

    /* Recherche des accesseurs de champs:
     *   static void access$0(TestInnerClass, int)
     *   {
     *     Byte code:
     *       aload_0
     *       iload_1
     *       putfield 13 basic/data/TestInnerClass:test    I
     *       return
     *   }
     */
    private static boolean searchPutFieldAccessor(
        ClassFile classFile, Method method)
    {
        List<Instruction> list = method.getInstructions();
        PutField pf;

        switch (list.size())
        {
        case 2:
            {
                if (list.get(1).getOpcode() != Const.RETURN) {
                    return false;
                }

                Instruction instruction = list.get(0);
                if (instruction.getOpcode() != Const.PUTFIELD) {
                    return false;
                }

                pf = (PutField)instruction;
            }
            break;
        case 3:
            {
                if (list.get(0).getOpcode() != ByteCodeConstants.DUPSTORE) {
                    return false;
                }

                if (list.get(2).getOpcode() != ByteCodeConstants.XRETURN) {
                    return false;
                }

                Instruction instruction = list.get(1);
                if (instruction.getOpcode() != Const.PUTFIELD) {
                    return false;
                }

                pf = (PutField)instruction;
            }
            break;
        default:
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();
        ConstantFieldref cfr = constants.getConstantFieldref(pf.getIndex());

        if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
            return false;
        }

        String methodDescriptor =
            constants.getConstantUtf8(method.getDescriptorIndex());
        if (methodDescriptor.charAt(1) == ')') {
            return false;
        }
        if (SignatureUtil.getParameterSignatureCount(methodDescriptor) != 2) {
            return false;
        }

        ConstantNameAndType cnat = constants.getConstantNameAndType(
                cfr.getNameAndTypeIndex());

        String methodName = constants.getConstantUtf8(method.getNameIndex());
        String fieldDescriptor = constants.getConstantUtf8(cnat.getSignatureIndex());
        String fieldName = constants.getConstantUtf8(cnat.getNameIndex());

        // Trouve ! Ajout de l'accesseur.
        classFile.addAccessor(methodName, methodDescriptor,
            new PutFieldAccessor(
                AccessorConstants.ACCESSOR_PUTFIELD,
                classFile.getThisClassName(), fieldName, fieldDescriptor));

        return true;
    }

    /* Recherche des accesseurs de méthodes:
     *     static void access$100(EntitlementFunctionLibrary, EvaluationCtx, URI, Bag, Bag[])
     *     {
     *       Byte code:
     *         aload_0
     *         aload_1
     *         aload_2
     *         aload_3
     *         aload 4
     *         invokevirtual 2    com/bea/security/providers/xacml/entitlement/function/EntitlementFunctionLibrary:debugEval    (Lcom/bea/security/xacml/EvaluationCtx;Ljava/net/URI;Lcom/bea/common/security/xacml/attr/Bag;[Lcom/bea/common/security/xacml/attr/Bag;)V
     *         return
     *     }
     */
    private static boolean searchInvokeMethodAccessor(
        ClassFile classFile, Method method)
    {
        List<Instruction> list = method.getInstructions();
        Instruction instruction;

        switch(list.size())
        {
        case 1:
            instruction = list.get(0);
            if (instruction.getOpcode() != ByteCodeConstants.XRETURN) {
                return false;
            }
            instruction = ((ReturnInstruction)instruction).getValueref();
            break;
        case 2:
            instruction = list.get(1);
            if (instruction.getOpcode() != Const.RETURN) {
                return false;
            }
            instruction = list.get(0);
            break;
        default:
            return false;
        }

        InvokeInstruction ii;

        switch (instruction.getOpcode())
        {
        case Const.INVOKEVIRTUAL,
             Const.INVOKESPECIAL,
             Const.INVOKEINTERFACE:
            InvokeNoStaticInstruction insi =
                (InvokeNoStaticInstruction)instruction;

            if (insi.getObjectref().getOpcode() != Const.ALOAD ||
                ((ALoad)insi.getObjectref()).getIndex() != 0) {
                return false;
            }

            ii = insi;
            break;
        case Const.INVOKESTATIC:
            ii = (InvokeInstruction)instruction;
            break;
        default:
            return false;
        }

        ConstantPool constants = classFile.getConstantPool();

        String methodName = constants.getConstantUtf8(method.getNameIndex());
        String methodDescriptor =
            constants.getConstantUtf8(method.getDescriptorIndex());

        ConstantMethodref cmr = constants.getConstantMethodref(ii.getIndex());
        ConstantNameAndType cnat = constants.getConstantNameAndType(
            cmr.getNameAndTypeIndex());

        String targetMethodName = constants.getConstantUtf8(cnat.getNameIndex());
        String targetMethodDescriptor =
            constants.getConstantUtf8(cnat.getSignatureIndex());

        // Trouve ! Ajout de l'accesseur.
        classFile.addAccessor(methodName, methodDescriptor,
            new InvokeMethodAccessor(
                AccessorConstants.ACCESSOR_INVOKEMETHOD, classFile.getThisClassName(),
                ii.getOpcode(), targetMethodName, targetMethodDescriptor,
                cmr.getListOfParameterSignatures(),
                cmr.getReturnedSignature()));

        return true;
    }
}
