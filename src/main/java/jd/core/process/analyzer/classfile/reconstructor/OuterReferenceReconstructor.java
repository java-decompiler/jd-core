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
package jd.core.process.analyzer.classfile.reconstructor;

import org.apache.bcel.Const;

import java.util.List;
import java.util.Map;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.process.analyzer.classfile.visitor.OuterGetFieldVisitor;
import jd.core.process.analyzer.classfile.visitor.OuterGetStaticVisitor;
import jd.core.process.analyzer.classfile.visitor.OuterInvokeMethodVisitor;
import jd.core.process.analyzer.classfile.visitor.OuterPutFieldVisitor;
import jd.core.process.analyzer.classfile.visitor.OuterPutStaticVisitor;
import jd.core.process.analyzer.classfile.visitor.ReplaceMultipleOuterReferenceVisitor;
import jd.core.process.analyzer.classfile.visitor.ReplaceOuterAccessorVisitor;
import jd.core.process.analyzer.classfile.visitor.ReplaceOuterReferenceVisitor;
import jd.core.util.SignatureUtil;

/*
 * Recontruction des references externes dans le corps des méthodes des classes
 * internes.
 */
public class OuterReferenceReconstructor
{
    private final ClassFile classFile;

    private final ReplaceOuterReferenceVisitor outerReferenceVisitor;
    private final ReplaceMultipleOuterReferenceVisitor multipleOuterReference;
    private final ReplaceOuterAccessorVisitor outerAccessorVisitor;

    private final OuterGetStaticVisitor outerGetStaticVisitor;
    private final OuterPutStaticVisitor outerPutStaticVisitor;
    private final OuterGetFieldVisitor outerGetFieldVisitor;
    private final OuterPutFieldVisitor outerPutFieldVisitor;
    private final OuterInvokeMethodVisitor outerMethodVisitor;

    public OuterReferenceReconstructor(
        Map<String, ClassFile> innerClassesMap, ClassFile classFile)
    {
        this.classFile = classFile;

        ConstantPool constants = classFile.getConstantPool();

        // Initialisation des visiteurs traitant les references des classes externes
        this.outerReferenceVisitor = new ReplaceOuterReferenceVisitor(
            Const.ALOAD, 1,
            createOuterThisInstructionIndex(classFile));
        this.multipleOuterReference =
            new ReplaceMultipleOuterReferenceVisitor(classFile);
        this.outerAccessorVisitor =
            new ReplaceOuterAccessorVisitor(classFile);
        // Initialisation des visiteurs traitant l'acces des champs externes
        this.outerGetFieldVisitor =
            new OuterGetFieldVisitor(innerClassesMap, constants);
        this.outerPutFieldVisitor =
            new OuterPutFieldVisitor(innerClassesMap, constants);
        // Initialisation du visiteur traitant l'acces des champs statics externes
        this.outerGetStaticVisitor =
            new OuterGetStaticVisitor(innerClassesMap, constants);
        this.outerPutStaticVisitor =
            new OuterPutStaticVisitor(innerClassesMap, constants);
        // Initialisation du visiteur traitant l'acces des méthodes externes
        this.outerMethodVisitor =
            new OuterInvokeMethodVisitor(innerClassesMap, constants);
    }

    public void reconstruct(
        Method method, List<Instruction> list)
    {
        // Inner no static class file
        if (classFile.getOuterThisField() != null)
        {
            // Replace outer reference parameter of constructors
            ConstantPool constants = classFile.getConstantPool();
            if (method.getNameIndex() == constants.getInstanceConstructorIndex()) {
                this.outerReferenceVisitor.visit(list);
            }
            // Replace multiple outer references
            this.multipleOuterReference.visit(list);
            // Replace static call to "OuterClass access$0(InnerClass)" methods.
            this.outerAccessorVisitor.visit(list);
        }

        // Replace outer field accessors
        this.outerGetFieldVisitor.visit(list);
        this.outerPutFieldVisitor.visit(list);
        // Replace outer static field accessors
        this.outerGetStaticVisitor.visit(list);
        this.outerPutStaticVisitor.visit(list);
        // Replace outer methods accessors
        this.outerMethodVisitor.visit(list);
    }

    // Creation d'une nouvelle constante de type 'Fieldref', dans le
    // pool, permettant l'affichage de 'OuterClass.this.'
    private static int createOuterThisInstructionIndex(ClassFile classFile)
    {
        if (classFile.getOuterClass() == null) {
            return 0;
        }

        String internalOuterClassName =
            classFile.getOuterClass().getInternalClassName();
        String outerClassName =
                SignatureUtil.getInnerName(internalOuterClassName);

        ConstantPool constants = classFile.getConstantPool();

        int signatureIndex = constants.addConstantUtf8(outerClassName);
        int classIndex = constants.addConstantClass(signatureIndex);
        int thisIndex = constants.getThisLocalVariableNameIndex();
        int descriptorIndex =
            constants.addConstantUtf8(internalOuterClassName);
        int nameAndTypeIndex = constants.addConstantNameAndType(
            thisIndex, descriptorIndex);

        return constants.addConstantFieldref(classIndex, nameAndTypeIndex);
    }
}
