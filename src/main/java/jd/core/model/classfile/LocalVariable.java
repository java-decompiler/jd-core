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

import java.util.Objects;

public class LocalVariable
    implements Comparable<LocalVariable>
{
    private int startPc;
    private int length;
    private int nameIndex;
    private int signatureIndex;
    private final int index;
    private boolean exceptionOrReturnAddress;
    // Champ de bits utilisé pour determiner le type de la variable (byte, char,
    // short, int).
    private int typesBitField;
    // Champs utilisé lors de la génération des déclarations de variables
    // locales (FastDeclarationAnalyzer.Analyze).
    private boolean declarationFlag;

    private boolean finalFlag;

    private boolean toBeRemoved;

    public LocalVariable(
            int startPc, int length, int nameIndex, int signatureIndex,
            int index)
    {
        this(startPc, length, nameIndex, signatureIndex, index, false, 0);
    }

    public LocalVariable(
            int startPc, int length, int nameIndex, int signatureIndex,
            int index, int typesBitSet)
    {
        this(startPc, length, nameIndex, signatureIndex, index, false,
             typesBitSet);
    }

    public LocalVariable(
            int startPc, int length, int nameIndex, int signatureIndex,
            int index, boolean exception)
    {
        this(startPc, length, nameIndex, signatureIndex, index, exception, 0);
    }

    protected LocalVariable(
        int startPc, int length, int nameIndex, int signatureIndex,
        int index, boolean exceptionOrReturnAddress, int typesBitField)
    {
        this.setStartPc(startPc);
        this.setLength(length);
        this.setNameIndex(nameIndex);
        this.setSignatureIndex(signatureIndex);
        this.index = index;
        this.setExceptionOrReturnAddress(exceptionOrReturnAddress);
        this.setDeclarationFlag(exceptionOrReturnAddress);
        this.setTypesBitField(typesBitField);
    }

    public void updateRange(int offset)
    {
        if (offset < this.getStartPc())
        {
            this.setLength(this.getLength() + this.getStartPc() - offset);
            this.setStartPc(offset);
        }

        if (offset >= this.getStartPc()+this.getLength())
        {
            this.setLength(offset - this.getStartPc() + 1);
        }
    }

    @Override
    public String toString()
    {
        return
            "LocalVariable{startPc=" + getStartPc() +
            ", length=" + getLength() +
            ", nameIndex=" + getNameIndex() +
            ", signatureIndex=" + getSignatureIndex() +
            ", index=" + getIndex() +
            "}";
    }

    @Override
    public int compareTo(LocalVariable other)
    {
        if (other == null) {
            return -1;
        }

        if (this.getNameIndex() != other.getNameIndex()) {
            return this.getNameIndex() - other.getNameIndex();
        }

        if (this.getLength() != other.getLength()) {
            return this.getLength() - other.getLength();
        }

        if (this.getStartPc() != other.getStartPc()) {
            return this.getStartPc() - other.getStartPc();
        }

        return this.getIndex() - other.getIndex();
    }

    @Override
    public int hashCode() {
        return Objects.hash(nameIndex, length, startPc, index);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        return compareTo((LocalVariable) obj) == 0;
    }

    public boolean isToBeRemoved() {
        return toBeRemoved;
    }

    public void setToBeRemoved(boolean toBeRemoved) {
        this.toBeRemoved = toBeRemoved;
    }

    public int getIndex() {
        return index;
    }

    public int getSignatureIndex() {
        return signatureIndex;
    }

    public void setSignatureIndex(int signatureIndex) {
        this.signatureIndex = signatureIndex;
    }

    public boolean isExceptionOrReturnAddress() {
        return exceptionOrReturnAddress;
    }

    public void setExceptionOrReturnAddress(boolean exceptionOrReturnAddress) {
        this.exceptionOrReturnAddress = exceptionOrReturnAddress;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getStartPc() {
        return startPc;
    }

    private void setStartPc(int startPc) {
        this.startPc = startPc;
    }

    public int getNameIndex() {
        return nameIndex;
    }

    public void setNameIndex(int nameIndex) {
        this.nameIndex = nameIndex;
    }

    public int getTypesBitField() {
        return typesBitField;
    }

    public void setTypesBitField(int typesBitField) {
        this.typesBitField = typesBitField;
    }

    public boolean hasDeclarationFlag() {
        return declarationFlag;
    }

    public void setDeclarationFlag(boolean declarationFlag) {
        this.declarationFlag = declarationFlag;
    }

    public boolean hasFinalFlag() {
        return finalFlag;
    }

    public void setFinalFlag(boolean finalFlag) {
        this.finalFlag = finalFlag;
    }
}
