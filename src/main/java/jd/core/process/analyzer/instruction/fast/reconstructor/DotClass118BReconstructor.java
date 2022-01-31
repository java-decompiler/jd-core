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
package jd.core.process.analyzer.instruction.fast.reconstructor;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.util.StringConstants;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetStatic;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.Invokestatic;
import jd.core.model.instruction.bytecode.instruction.Ldc;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.reference.ReferenceMap;
import jd.core.process.analyzer.classfile.visitor.ReplaceDupLoadVisitor;

/*
 * Recontruction du mot cle '.class' depuis les instructions generees par le
 * JDK 1.1.8 de IBM (?) :
 * ...
 * dupstore( getstatic( current class, 'class$...', Class ) )
 * ifnotnull( dupload )
 * {
 *   pop
 *   try
 *   {
 *     ternaryopstore( AssignmentInstruction(
 *             putstatic( dupload ),
 *             invokestatic( current class, 'class$...', nom de la classe ) ) )
 *   }
 *   catch (ClassNotFoundException localClassNotFoundException1)
 *   {
 *     throw new NoClassDefFoundError(localClassNotFoundException1.getMessage());
 *   }
 * }
 * ??? ( dupload )
 * ...
 */
public final class DotClass118BReconstructor
{
    private DotClass118BReconstructor() {
        super();
    }

    public static void reconstruct(
        ReferenceMap referenceMap, ClassFile classFile, List<Instruction> list)
    {
        int i = list.size();

        if  (i < 5) {
            return;
        }

        i -= 4;
        ConstantPool constants = classFile.getConstantPool();

        while (i-- > 0)
        {
            Instruction instruction = list.get(i);

            if (instruction.getOpcode() != ByteCodeConstants.DUPSTORE) {
                continue;
            }

            DupStore ds = (DupStore)instruction;

            if (ds.getObjectref().getOpcode() != Const.GETSTATIC) {
                continue;
            }

            GetStatic gs = (GetStatic)ds.getObjectref();

            ConstantFieldref cfr = constants.getConstantFieldref(gs.getIndex());

            if (cfr.getClassIndex() != classFile.getThisClassIndex()) {
                continue;
            }

            instruction = list.get(i+1);

            if (instruction.getOpcode() != ByteCodeConstants.IFXNULL) {
                continue;
            }

            IfInstruction ii = (IfInstruction)instruction;

            if (ii.getValue().getOpcode() != ByteCodeConstants.DUPLOAD ||
                ds.getOffset() != ii.getValue().getOffset()) {
                continue;
            }

            instruction = list.get(i+2);

            if (instruction.getOpcode() != Const.POP) {
                continue;
            }

            Pop pop = (Pop)instruction;

            if (pop.getObjectref().getOpcode() != ByteCodeConstants.DUPLOAD ||
                ds.getOffset() != pop.getObjectref().getOffset()) {
                continue;
            }

            instruction = list.get(i+3);

            if (instruction.getOpcode() != FastConstants.TRY) {
                continue;
            }

            FastTry ft = (FastTry)instruction;

            if (ft.getFinallyInstructions() != null ||
                ft.getInstructions().size() != 1 ||
                ft.getCatches().size() != 1) {
                continue;
            }

            List<Instruction> catchInstructions =
                ft.getCatches().get(0).instructions();

            if (catchInstructions.size() != 1 ||
                catchInstructions.get(0).getOpcode() != Const.ATHROW) {
                continue;
            }

            instruction = ft.getInstructions().get(0);

            if (instruction.getOpcode() != ByteCodeConstants.TERNARYOPSTORE) {
                continue;
            }

            TernaryOpStore tos = (TernaryOpStore)instruction;

            if (tos.getObjectref().getOpcode() != ByteCodeConstants.ASSIGNMENT) {
                continue;
            }

            AssignmentInstruction ai = (AssignmentInstruction)tos.getObjectref();

            if (ai.getValue2().getOpcode() != Const.INVOKESTATIC) {
                continue;
            }

            Invokestatic is = (Invokestatic)ai.getValue2();

            if (is.getArgs().size() != 1) {
                continue;
            }

            instruction = is.getArgs().get(0);

            if (instruction.getOpcode() != Const.LDC) {
                continue;
            }

            ConstantNameAndType cnatField = constants.getConstantNameAndType(
                cfr.getNameAndTypeIndex());

            String signature =
                constants.getConstantUtf8(cnatField.getSignatureIndex());

            if (! StringConstants.INTERNAL_CLASS_SIGNATURE.equals(signature)) {
                continue;
            }

            String nameField = constants.getConstantUtf8(cnatField.getNameIndex());

            if (! nameField.startsWith(StringConstants.CLASS_DOLLAR)) {
                continue;
            }

            ConstantMethodref cmr = constants.getConstantMethodref(is.getIndex());

            String className =
                constants.getConstantClassName(cmr.getClassIndex());

            if (! StringConstants.JAVA_LANG_CLASS.equals(className)) {
                continue;
            }

            ConstantNameAndType cnatMethod =
                constants.getConstantNameAndType(cmr.getNameAndTypeIndex());
            String nameMethod =
                constants.getConstantUtf8(cnatMethod.getNameIndex());

            if (! StringConstants.FORNAME_METHOD_NAME.equals(nameMethod)) {
                continue;
            }

            Ldc ldc = (Ldc)instruction;
            Constant cv = constants.getConstantValue(ldc.getIndex());

            if (cv.getTag() != Const.CONSTANT_String) {
                continue;
            }
            // Trouvé !
            ConstantString cs = (ConstantString)cv;
            String dotClassName = constants.getConstantUtf8(cs.getStringIndex());
            String internalName = dotClassName.replace(
                StringConstants.PACKAGE_SEPARATOR,
                StringConstants.INTERNAL_PACKAGE_SEPARATOR);

            referenceMap.add(internalName);

            // Ajout du nom interne
            int index = constants.addConstantUtf8(internalName);
            // Ajout d'une nouvelle classe
            index = constants.addConstantClass(index);
            ldc = new Ldc(
                Const.LDC, ii.getOffset(),
                ii.getLineNumber(), index);

            // Remplacement de l'intruction GetStatic par l'instruction Ldc
            ReplaceDupLoadVisitor visitor = new ReplaceDupLoadVisitor(ds, ldc);

            visitor.visit(list.get(i+4));

            // Retrait de l'intruction FastTry
            list.remove(i+3);
            // Retrait de l'intruction Pop
            list.remove(i+2);
            // Retrait de l'intruction IfNotNull
            list.remove(i+1);
            // Retrait de l'intruction DupStore
            list.remove(i);

            // Recherche de l'attribut statique et ajout de l'attribut SYNTHETIC
            Field[] fields = classFile.getFields();
            int j = fields.length;

            while (j-- > 0)
            {
                Field field = fields[j];

                if (field.getNameIndex() == cnatField.getNameIndex())
                {
                    field.setAccessFlags(field.getAccessFlags() | Const.ACC_SYNTHETIC);
                    break;
                }
            }
        }
    }
}
