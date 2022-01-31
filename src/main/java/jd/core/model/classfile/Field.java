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
package jd.core.model.classfile;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;

import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeConstantValue;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.util.UtilConstants;

public class Field extends FieldOrMethod
{
    private ValueAndMethod valueAndMethod;
    /*
     * Attributs pour l'affichage des champs synthetique des classes anonymes.
     * Champs modifié par:
     * 1) ClassFileAnalyzer.AnalyseAndModifyConstructors(...) pour y placer le
     *    numéro (position) du parametre du constructeur initialisant le champs.
     */
    private int anonymousClassConstructorParameterIndex;
    /*
     * 2) NewInstructionReconstructorBase.InitAnonymousClassConstructorParameterName(...)
     *    pour y placer l'index du nom du parametre.
     * 3) SourceWriterVisitor.writeGetField(...) pour afficher le nom du
     *    parameter de la classe englobante.
     */
    private int outerMethodLocalVariableNameIndex;

    public Field(
        int accessFlags, int nameIndex,
        int descriptorIndex, Attribute[] attributes)
    {
        super(accessFlags, nameIndex, descriptorIndex, attributes);
        this.setAnonymousClassConstructorParameterIndex(UtilConstants.INVALID_INDEX);
        this.setOuterMethodLocalVariableNameIndex(UtilConstants.INVALID_INDEX);
    }

    public Constant getConstantValue(ConstantPool constants)
    {
        if (this.getAttributes() != null) {
            for (Attribute attribute : this.getAttributes()) {
                if (attribute.getTag() == Const.ATTR_CONSTANT_VALUE)
                {
                    AttributeConstantValue acv = (AttributeConstantValue)attribute;
                    return constants.getConstantValue(acv.getConstantvalueIndex());
                }
            }
        }

        return null;
    }

    public ValueAndMethod getValueAndMethod()
    {
        return valueAndMethod;
    }

    public void setValueAndMethod(Instruction value, Method method)
    {
        this.valueAndMethod = new ValueAndMethod(value, method);
    }

    public int getAnonymousClassConstructorParameterIndex() {
        return anonymousClassConstructorParameterIndex;
    }

    public void setAnonymousClassConstructorParameterIndex(int anonymousClassConstructorParameterIndex) {
        this.anonymousClassConstructorParameterIndex = anonymousClassConstructorParameterIndex;
    }

    public int getOuterMethodLocalVariableNameIndex() {
        return outerMethodLocalVariableNameIndex;
    }

    public void setOuterMethodLocalVariableNameIndex(int outerMethodLocalVariableNameIndex) {
        this.outerMethodLocalVariableNameIndex = outerMethodLocalVariableNameIndex;
    }

    public record ValueAndMethod(Instruction value, Method method) {
    }
}
