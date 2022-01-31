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
import org.apache.bcel.classfile.LineNumber;
import org.jd.core.v1.model.classfile.attribute.CodeException;

import java.util.List;

import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeAnnotationDefault;
import jd.core.model.classfile.attribute.AttributeCode;
import jd.core.model.classfile.attribute.AttributeExceptions;
import jd.core.model.classfile.attribute.AttributeLocalVariableTable;
import jd.core.model.classfile.attribute.AttributeNumberTable;
import jd.core.model.classfile.attribute.AttributeRuntimeParameterAnnotations;
import jd.core.model.classfile.attribute.ElementValue;
import jd.core.model.classfile.attribute.ParameterAnnotations;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class Method extends FieldOrMethod
{
    private boolean containsError;
    private int[] exceptionIndexes; // NO_UCD (use final)
    private byte[] code; // NO_UCD (use final)
    private LineNumber[] lineNumbers; // NO_UCD (use final)
    private CodeException[] codeExceptions;
    private ParameterAnnotations[] visibleParameterAnnotations; // NO_UCD (use final)
    private ParameterAnnotations[] invisibleParameterAnnotations; // NO_UCD (use final)
    private ElementValue defaultAnnotationValue; // NO_UCD (use final)
    private List<Instruction> instructions;
    private List<Instruction> fastNodes;
    private LocalVariables localVariables;
    /**
     * Champs permettant l'affichage des parametres des instanciations des
     * classes anonymes.
     */
    private int superConstructorParameterCount;

    public Method(int accessFlags, int nameIndex, int descriptorIndex,
                  Attribute[] attributes)
    {
        super(accessFlags, nameIndex, descriptorIndex, attributes);

        this.containsError = false;
        this.exceptionIndexes = null;
        this.code = null;
        this.localVariables = null;
        this.lineNumbers = null;
        this.codeExceptions = null;
        this.visibleParameterAnnotations = null;
        this.invisibleParameterAnnotations = null;
        this.defaultAnnotationValue = null;
        this.superConstructorParameterCount = 0;

        if (attributes != null)
        {
            AttributeCode ac = null;

            for (int i=this.getAttributes().length-1; i>=0; --i)
            {
                Attribute attribute =  this.getAttribute(i);
                switch (attribute.getTag())
                {
                case Const.ATTR_EXCEPTIONS:
                    this.exceptionIndexes =
                        ((AttributeExceptions)attribute).getExceptionIndexTable();
                    break;
                case Const.ATTR_CODE:
                    ac = (AttributeCode)attributes[i];
                    break;
                case Const.ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS:
                    this.visibleParameterAnnotations =
                        ((AttributeRuntimeParameterAnnotations)attribute).getParameterAnnotations();
                    break;
                case Const.ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS:
                    this.invisibleParameterAnnotations =
                        ((AttributeRuntimeParameterAnnotations)attribute).getParameterAnnotations();
                    break;
                case Const.ATTR_ANNOTATION_DEFAULT:
                    this.defaultAnnotationValue =
                        ((AttributeAnnotationDefault)attribute).getDefaultValue();
                    break;
                }
            }

            if (ac != null)
            {
                this.code = ac.getCode();

                // localVariables
                AttributeLocalVariableTable alvt = ac.getAttributeLocalVariableTable();
                if (alvt != null && alvt.getLocalVariableTable() != null)
                {
                    AttributeLocalVariableTable alvtt = ac.getAttributeLocalVariableTypeTable();
                    LocalVariable[] localVariableTypeTable =
                        alvtt == null ? null : alvtt.getLocalVariableTable();
                    this.localVariables = new LocalVariables(
                        alvt.getLocalVariableTable(), localVariableTypeTable);
                }

                // lineNumbers
                AttributeNumberTable ant = ac.getAttributeLineNumberTable();
                this.lineNumbers = ant == null ? null : ant.getLineNumberTable();

                // codeExceptions
                this.codeExceptions = ac.getExceptionTable();
            }
        }
    }

    public boolean containsError()
    {
        return containsError;
    }

    public void setContainsError(boolean containsError)
    {
        this.containsError = containsError;
    }

    public int[] getExceptionIndexes()
    {
        return this.exceptionIndexes;
    }

    public LocalVariables getLocalVariables()
    {
        return this.localVariables;
    }

    public void setLocalVariables(LocalVariables llv)
    {
        this.localVariables = llv;
    }

    public List<Instruction> getInstructions()
    {
        return instructions;
    }
    public void setInstructions(List<Instruction> instructions)
    {
        this.instructions = instructions;
    }

    public List<Instruction> getFastNodes()
    {
        return fastNodes;
    }

    public void setFastNodes(List<Instruction> fastNodes)
    {
        this.fastNodes = fastNodes;
    }

    public byte[] getCode()
    {
        return this.code;
    }

    public LineNumber[] getLineNumbers()
    {
        return lineNumbers;
    }

    public CodeException[] getCodeExceptions()
    {
        return this.codeExceptions;
    }

    public ParameterAnnotations[] getVisibleParameterAnnotations()
    {
        return this.visibleParameterAnnotations;
    }

    public ParameterAnnotations[] getInvisibleParameterAnnotations()
    {
        return this.invisibleParameterAnnotations;
    }

    public ElementValue getDefaultAnnotationValue()
    {
        return this.defaultAnnotationValue;
    }

    public int getSuperConstructorParameterCount()
    {
        return superConstructorParameterCount;
    }

    public void setSuperConstructorParameterCount(int count)
    {
        this.superConstructorParameterCount = count;
    }
}
