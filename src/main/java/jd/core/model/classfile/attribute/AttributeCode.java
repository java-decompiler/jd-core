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
package jd.core.model.classfile.attribute;

import org.apache.bcel.Const;
import org.jd.core.v1.model.classfile.attribute.CodeException;

public class AttributeCode extends Attribute
{
    private final byte[] code;
    private final CodeException[] exceptionTable;
    private final Attribute[] attributes;

    public AttributeCode(byte tag,
                         byte[] code,
                         CodeException[] exceptionTable,
                         Attribute[] attributes)
    {
        super(tag);
        this.code = code;
        this.exceptionTable = exceptionTable;
        this.attributes = attributes;
    }

    public AttributeNumberTable getAttributeLineNumberTable()
    {
        if (this.attributes != null) {
            for (int i=this.attributes.length-1; i>=0; --i) {
                if (this.attributes[i].getTag() == Const.ATTR_LINE_NUMBER_TABLE) {
                    return (AttributeNumberTable)this.attributes[i];
                }
            }
        }

        return null;
    }

    public AttributeLocalVariableTable getAttributeLocalVariableTable()
    {
        if (this.attributes != null) {
            for (int i=this.attributes.length-1; i>=0; --i) {
                if (this.attributes[i].getTag() == Const.ATTR_LOCAL_VARIABLE_TABLE) {
                    return (AttributeLocalVariableTable)this.attributes[i];
                }
            }
        }

        return null;
    }

    public AttributeLocalVariableTable getAttributeLocalVariableTypeTable()
    {
        if (this.attributes != null) {
            for (int i=this.attributes.length-1; i>=0; --i) {
                if (this.attributes[i].getTag() == Const.ATTR_LOCAL_VARIABLE_TYPE_TABLE) {
                    return (AttributeLocalVariableTable)this.attributes[i];
                }
            }
        }

        return null;
    }

    public byte[] getCode() {
        return code;
    }

    public CodeException[] getExceptionTable() {
        return exceptionTable;
    }
}
