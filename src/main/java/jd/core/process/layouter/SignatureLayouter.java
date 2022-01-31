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
package jd.core.process.layouter;

import org.apache.bcel.Const;
import org.jd.core.v1.util.StringConstants;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.layout.block.GenericExtendsSuperInterfacesLayoutBlock;
import jd.core.model.layout.block.GenericExtendsSuperTypeLayoutBlock;
import jd.core.model.layout.block.GenericImplementsInterfacesLayoutBlock;
import jd.core.model.layout.block.GenericTypeNameLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.util.CharArrayUtil;
import jd.core.util.SignatureUtil;

public final class SignatureLayouter
{
    private SignatureLayouter() {
        super();
    }

    public static boolean createLayoutBlocksForClassSignature(
        ClassFile classFile,
        String signature,
        List<LayoutBlock> layoutBlockList)
    {
        boolean displayExtendsOrImplementsFlag = false;

        char[] caSignature = signature.toCharArray();
        int length = caSignature.length;
        int index = 0;
        int newIndex;

        layoutBlockList.add(new GenericTypeNameLayoutBlock(classFile, signature));

        // Affichage des generics
        index = skipGenerics(caSignature, length, index);

        // Affichage de la classe mere
        newIndex = SignatureUtil.skipSignature(caSignature, length, index);

        if ((classFile.getAccessFlags() &
                (Const.ACC_INTERFACE|Const.ACC_ENUM)) == 0 &&
            !isObjectClass(caSignature, index, newIndex))
        {
            displayExtendsOrImplementsFlag = true;
            layoutBlockList.add(
                new GenericExtendsSuperTypeLayoutBlock(
                    classFile, caSignature, index));
        }

        // Affichage des interfaces ou des super interfaces
        if (newIndex < length)
        {
            displayExtendsOrImplementsFlag = true;

            if ((classFile.getAccessFlags() & Const.ACC_INTERFACE) != 0)
            {
                layoutBlockList.add(
                    new GenericExtendsSuperInterfacesLayoutBlock(
                        classFile, caSignature, newIndex));
            }
            else
            {
                layoutBlockList.add(
                    new GenericImplementsInterfacesLayoutBlock(
                        classFile, caSignature, newIndex));
            }
        }

        return displayExtendsOrImplementsFlag;
    }

    private static int skipGenerics(char[] caSignature, int length, int index)
    {
        if (caSignature[index] == '<')
        {
            int depth = 1;

            while (index < length)
            {
                char c = caSignature[++index];

                if (c == '<')
                {
                    depth++;
                }
                else if (c == '>')
                {
                    if (depth <= 1) {
                        index++;
                        break;
                    }
                    depth--;
                }
            }
        }

        return index;
    }

    private static boolean isObjectClass(
        char[] caSignature, int beginIndex, int endIndex)
    {
        return StringConstants.INTERNAL_OBJECT_SIGNATURE
                    .equals(CharArrayUtil.substring(caSignature, beginIndex, endIndex));
    }
}
