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
package jd.core.model.layout.block;

import jd.core.model.classfile.ClassFile;
import jd.core.model.instruction.bytecode.instruction.Instruction;

public class GenericImplementsInterfacesLayoutBlock
    extends ImplementsInterfacesLayoutBlock
{
    private final char[] caSignature;
    private final int signatureIndex;

    public GenericImplementsInterfacesLayoutBlock(
        ClassFile classFile, char[] caSignature, int signatureIndex)
    {
        super(
            LayoutBlockConstants.GENERIC_IMPLEMENTS_INTERFACES,
            Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER,
            0, 1, 1, classFile);
        this.caSignature = caSignature;
        this.signatureIndex = signatureIndex;
    }

    public char[] getCaSignature() {
        return caSignature;
    }

    public int getSignatureIndex() {
        return signatureIndex;
    }
}
