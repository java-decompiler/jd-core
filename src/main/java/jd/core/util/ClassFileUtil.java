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
package jd.core.util;

import org.apache.bcel.Const;
import org.jd.core.v1.util.StringConstants;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Method;

/*
 * internalPath:        basic/data/Test.class
 * internalClassName:    basic/data/Test
 * qualifiedClassName:    basic.data.Test
 * internalPackageName:    basic/data
 * packageName:            basic.data
 */
public final class ClassFileUtil
{
    private ClassFileUtil() {
        super();
    }

    public static boolean containsMultipleConstructor(ClassFile classFile)
    {
        ConstantPool constants = classFile.getConstantPool();
        Method[] methods = classFile.getMethods();
        boolean flag = false;

        for (int i=0; i<methods.length; i++)
        {
            Method method = methods[i];

            if ((method.getAccessFlags() &
                 (Const.ACC_SYNTHETIC|Const.ACC_BRIDGE)) != 0) {
                continue;
            }

            if (method.getNameIndex() == constants.getInstanceConstructorIndex())
            {
                if (flag) {
                    // A other constructor has been found
                    return true;
                }
                // A first constructor has been found
                flag = true;
            }
        }

        return false;
    }

    public static boolean isAMethodOfEnum(
        ClassFile classFile, Method method, String signature)
    {
        ConstantPool constants = classFile.getConstantPool();

        if ((method.getAccessFlags() & (Const.ACC_PUBLIC|Const.ACC_STATIC)) ==
            (Const.ACC_PUBLIC|Const.ACC_STATIC))
        {
            String methodName = constants.getConstantUtf8(method.getNameIndex());

            if (StringConstants.ENUM_VALUEOF_METHOD_NAME.equals(methodName))
            {
                String s = "(Ljava/lang/String;)" + classFile.getInternalClassName();
                if (s.equals(signature))
                {
                    // Ne pas afficher la méthode
                    // "public static enumXXX valueOf(String paramString)".
                    return true;
                }
            }

            if (StringConstants.ENUM_VALUES_METHOD_NAME.equals(methodName))
            {
                String s = "()[" + classFile.getInternalClassName();
                if (s.equals(signature))
                {
                    // Ne pas afficher la méthode
                    // "public static enumXXX[] values()".
                    return true;
                }
            }
        }

        return false;
    }
}
