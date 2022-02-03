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
package jd.core.process.deserializer;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.Constant;
import org.apache.bcel.classfile.ConstantClass;
import org.apache.bcel.classfile.ConstantDouble;
import org.apache.bcel.classfile.ConstantFieldref;
import org.apache.bcel.classfile.ConstantFloat;
import org.apache.bcel.classfile.ConstantInteger;
import org.apache.bcel.classfile.ConstantLong;
import org.apache.bcel.classfile.ConstantMethodHandle;
import org.apache.bcel.classfile.ConstantMethodType;
import org.apache.bcel.classfile.ConstantNameAndType;
import org.apache.bcel.classfile.ConstantString;
import org.apache.bcel.classfile.ConstantUtf8;
import org.apache.bcel.classfile.InnerClass;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.model.classfile.constant.ConstantInterfaceMethodref;
import org.jd.core.v1.model.classfile.constant.ConstantMethodref;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.ExceptionUtil;
import org.jd.core.v1.util.StringConstants;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jd.core.CoreConstants;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeInnerClasses;

public final class ClassFileDeserializer
{
    private ClassFileDeserializer() {
        super();
    }

    public static ClassFile deserialize(Loader loader, String internalClassPath)
        throws IOException
    {
        ClassFile classFile = loadSingleClass(loader, internalClassPath);
        if (classFile == null) {
            return null;
        }

        AttributeInnerClasses aics = classFile.getAttributeInnerClasses();
        if (aics == null) {
            return classFile;
        }

        String internalClassPathPrefix =
            internalClassPath.substring(
                0, internalClassPath.length() - StringConstants.CLASS_FILE_SUFFIX.length());
        String innerInternalClassNamePrefix =
            internalClassPathPrefix + StringConstants.INTERNAL_INNER_SEPARATOR;
        ConstantPool constants = classFile.getConstantPool();

        InnerClass[] cs = aics.getClasses();
        int length = cs.length;
        List<ClassFile> innerClassFiles = new ArrayList<>(length);

        for (int i=0; i<length; i++)
        {
            String innerInternalClassPath =
                constants.getConstantClassName(cs[i].getInnerClassIndex());

            if (! innerInternalClassPath.startsWith(innerInternalClassNamePrefix)) {
                continue;
            }
            int offsetInternalInnerSeparator = innerInternalClassPath.indexOf(
                StringConstants.INTERNAL_INNER_SEPARATOR,
                innerInternalClassNamePrefix.length());
            if (offsetInternalInnerSeparator != -1)
            {
                String tmpInnerInternalClassPath =
                    innerInternalClassPath.substring(0, offsetInternalInnerSeparator) +
                    StringConstants.CLASS_FILE_SUFFIX;
                if (loader.canLoad(tmpInnerInternalClassPath)) {
                    // 'innerInternalClassName' is not a direct inner class.
                    continue;
                }
            }

            try
            {
                ClassFile innerClassFile =
                    deserialize(loader, innerInternalClassPath +
                    StringConstants.CLASS_FILE_SUFFIX);

                if (innerClassFile != null)
                {
                    // Alter inner class access flag
                    innerClassFile.setAccessFlags(cs[i].getInnerAccessFlags());
                    // Setup outer class reference
                    innerClassFile.setOuterClass(classFile);
                    // Add inner classes
                    innerClassFiles.add(innerClassFile);
                }
            }
            catch (IOException e)
            {
                assert ExceptionUtil.printStackTrace(e);
            }
        }

        // Add inner classes
        classFile.setInnerClassFiles(innerClassFiles);

        return classFile;
    }

    private static ClassFile loadSingleClass(
            Loader loader, String internalClassPath)
        throws IOException
    {
        ClassFile classFile = null;
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(loader.load(internalClassPath))))
        {
            classFile = deserialize(dis, loader);
        }
        catch (IOException e)
        {
            assert ExceptionUtil.printStackTrace(e);
        }
        return classFile;
    }

    private static ClassFile deserialize(DataInput di, Loader loader)
        throws IOException
    {
        checkMagic(di);

        int minorVersion = di.readUnsignedShort();
        int majorVersion = di.readUnsignedShort();

        Constant[] constants = deserializeConstants(di);
        ConstantPool constantPool = new ConstantPool(constants);

        int accessFlags = di.readUnsignedShort();
        int thisClass = di.readUnsignedShort();
        int superClass = di.readUnsignedShort();

        int[] interfaces = deserializeInterfaces(di);
        Field[] fieldInfos = deserializeFields(di, constantPool);
        Method[] methodInfos = deserializeMethods(di, constantPool);

        Attribute[] attributeInfos =
            AttributeDeserializer.deserialize(di, constantPool);

        return new ClassFile(
                minorVersion, majorVersion,
                constantPool,
                accessFlags, thisClass, superClass,
                interfaces,
                fieldInfos,
                methodInfos,
                attributeInfos,
                loader
        );
    }

    private static Constant[] deserializeConstants(DataInput di)
        throws IOException
    {
        int count = di.readUnsignedShort();

        if (count == 0) {
            return null;
        }

        Constant[] constants = new Constant[count];

        for (int i=1; i<count; i++) {
            byte tag = di.readByte();
            switch (tag) {
                case Const.CONSTANT_Module, Const.CONSTANT_Package, Const.CONSTANT_Class:
                    constants[i] = new ConstantClass(di.readUnsignedShort());
                    break;
                case Const.CONSTANT_Fieldref:
                    constants[i] = new ConstantFieldref(di.readUnsignedShort(),
                                                        di.readUnsignedShort());
                    break;
                case Const.CONSTANT_Methodref:
                    constants[i] = new ConstantMethodref(di.readUnsignedShort(),
                                                         di.readUnsignedShort());
                    break;
                case Const.CONSTANT_InterfaceMethodref:
                    constants[i] = new ConstantInterfaceMethodref(di.readUnsignedShort(),
                                                                  di.readUnsignedShort());
                    break;
                case Const.CONSTANT_String:
                    constants[i] = new ConstantString(di.readUnsignedShort());
                    break;
                case Const.CONSTANT_Integer:
                    constants[i] = new ConstantInteger(di.readInt());
                    break;
                case Const.CONSTANT_Float:
                    constants[i] = new ConstantFloat(di.readFloat());
                    break;
                case Const.CONSTANT_Long:
                    constants[i] = new ConstantLong(di.readLong());
                    i++;
                    break;
                case Const.CONSTANT_Double:
                    constants[i] = new ConstantDouble(di.readDouble());
                    i++;
                    break;
                case Const.CONSTANT_NameAndType:
                    constants[i] = new ConstantNameAndType(di.readUnsignedShort(),
                                                           di.readUnsignedShort());
                    break;
                case Const.CONSTANT_Utf8:
                    constants[i] = new ConstantUtf8(di.readUTF());
                    break;
                case Const.CONSTANT_InvokeDynamic, Const.CONSTANT_Dynamic:
                    constants[i] = new ConstantMethodref(di.readUnsignedShort(),
                                                         di.readUnsignedShort());
                    break;
                case Const.CONSTANT_MethodHandle:
                    constants[i] = new ConstantMethodHandle(di.readByte(),
                                                            di.readUnsignedShort());
                    break;
                case Const.CONSTANT_MethodType:
                    constants[i] = new ConstantMethodType(di.readUnsignedShort());
                    break;
                default:
                    throw new ClassFormatException("Invalid constant pool entry");
            }
        }

        return constants;
    }

    private static int[] deserializeInterfaces(DataInput di)
        throws IOException
    {
        int count = di.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        int[] interfaces = new int[count];

        for (int i=0; i<count; i++) {
            interfaces[i] = di.readUnsignedShort();
        }

        return interfaces;
    }

    private static Field[] deserializeFields(
            DataInput di, ConstantPool constantPool)
        throws IOException
    {
        int count = di.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        Field[] fieldInfos = new Field[count];

        for (int i=0; i<count; i++) {
            fieldInfos[i] = new Field(
                        di.readUnsignedShort(),
                        di.readUnsignedShort(),
                        di.readUnsignedShort(),
                        AttributeDeserializer.deserialize(di, constantPool));
        }

        return fieldInfos;
    }

    private static Method[] deserializeMethods(DataInput di,
            ConstantPool constants)
        throws IOException
    {
        int count = di.readUnsignedShort();
        if (count == 0) {
            return null;
        }

        Method[] methodInfos = new Method[count];

        for (int i=0; i<count; i++) {
            methodInfos[i] = new Method(
                        di.readUnsignedShort(),
                        di.readUnsignedShort(),
                        di.readUnsignedShort(),
                        AttributeDeserializer.deserialize(di, constants));
        }

        return methodInfos;
    }

    private static void checkMagic(DataInput di)
        throws IOException
    {
        int magic = di.readInt();

        if(magic != CoreConstants.JAVA_MAGIC_NUMBER) {
            throw new ClassFormatException("Invalid Java .class file");
        }
    }
}
