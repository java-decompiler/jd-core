/**
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
 */
package jd.core.model.classfile;

import org.apache.bcel.Const;
import org.apache.bcel.classfile.ConstantClass;
import org.jd.core.v1.api.loader.Loader;
import org.jd.core.v1.util.StringConstants;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.core.model.classfile.accessor.Accessor;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeBootstrapMethods;
import jd.core.model.classfile.attribute.AttributeInnerClasses;
import jd.core.model.classfile.attribute.AttributeMethodParameters;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.util.SignatureUtil;

public class ClassFile extends Base
{
    private final int minorVersion;
    private final int majorVersion;
    private final int thisClass;
    private final int superClass;

    private final int[] interfaces;
    private final Field[] fields;
    private final Method[] methods;

    private final ConstantPool constants;
    private final String thisClassName;
    private final String superClassName;
    private final String internalClassName;
    private final String internalPackageName;

    private ClassFile outerClass;
    private Field outerThisField;
    private List<ClassFile> innerClassFiles;

    private final Method staticMethod;
    private List<Instruction> enumValues;
    private String internalAnonymousClassName;
    private final Map<String, Map<String, Accessor>> accessors;

    /**
     * Attention :
     * - Dans le cas des instructions Switch+Enum d'Eclipse, la clé de la map
     *   est l'indexe du nom de la méthode
     *   "static int[] $SWITCH_TABLE$basic$data$TestEnum$enum1()".
     * - Dans le cas des instructions Switch+Enum des autres compilateurs, la
     *   clé de la map est l'indexe du nom de la classe interne "static class 1"
     *   contenant le tableau de correspondance
     *   "$SwitchMap$basic$data$TestEnum$enum1".
     */
    private final Map<Integer, List<Integer>> switchMaps;
    
    private final Loader loader;

    public ClassFile(int minorVersion, int majorVersion,
                     ConstantPool constants, int accessFlags, int thisClass,
                     int superClass, int[] interfaces, Field[] fields,
                     Method[] methods, Attribute[] attributes, Loader loader)
    {
        super(accessFlags, attributes);

        this.loader = loader;
        this.minorVersion = minorVersion;
        this.majorVersion = majorVersion;
        this.thisClass = thisClass;
        this.superClass = superClass;
        this.interfaces = interfaces;
        this.fields = fields;
        this.methods = methods;

        this.constants = constants;

        // internalClassName
        this.thisClassName =
            this.constants.getConstantClassName(this.thisClass);
        // internalSuperClassName
        this.superClassName = this.superClass == 0 ? null :
            this.constants.getConstantClassName(this.superClass);
        this.internalClassName = SignatureUtil.createTypeName(this.thisClassName);
        // internalPackageName
        int index = this.thisClassName.lastIndexOf(
                StringConstants.INTERNAL_PACKAGE_SEPARATOR);
        this.internalPackageName =
            index == -1 ? "" : this.thisClassName.substring(0, index);

        // staticMethod
        this.staticMethod = findStaticMethod();

        // internalAnonymousClassName
        this.internalAnonymousClassName = null;
        // accessors
        this.accessors = new HashMap<>(10);
        // SwitchMap for Switch+Enum instructions
        this.switchMaps = new HashMap<>();
    }

    private Method findStaticMethod() {
        if (this.methods != null)
        {
            for (Method method : methods)
            {
                if ((method.getAccessFlags() & Const.ACC_STATIC) != 0 &&
                    method.getNameIndex() == this.constants.getClassConstructorIndex())
                {
                    return method;
                }
            }
        }
        return null;
    }
    
    public ConstantPool getConstantPool()
    {
        return this.constants;
    }

    public int[] getInterfaces()
    {
        return interfaces;
    }

    public int getMajorVersion()
    {
        return majorVersion;
    }

    public int getMinorVersion()
    {
        return minorVersion;
    }

    public int getSuperClassIndex()
    {
        return superClass;
    }

    public int getThisClassIndex()
    {
        return thisClass;
    }

    public String getClassName()
    {
        if (this.outerClass == null)
        {
            // int index = this.thisClassName.lastIndexOf(
            //   AnalyzerConstants.INTERNAL_INNER_SEPARATOR);
            //if (index != -1)
            //    return this.thisClassName.substring(index+1);

            int index = this.thisClassName.lastIndexOf(
                    StringConstants.INTERNAL_PACKAGE_SEPARATOR);
            return index == -1 ?
                this.thisClassName :
                this.thisClassName.substring(index + 1);
        }
        String outerClassName = this.outerClass.getThisClassName();
        return this.thisClassName.substring(
            outerClassName.length() + 1);
    }

    public String getThisClassName()
    {
        return this.thisClassName;
    }

    public String getSuperClassName()
    {
        return this.superClassName;
    }

    public String getInternalClassName()
    {
        return this.internalClassName;
    }

    public String getInternalPackageName()
    {
        return this.internalPackageName;
    }

    public Field[] getFields()
    {
        return this.fields;
    }

    public Method[] getMethods()
    {
        return this.methods;
    }

    public Method getMethod(int i)
    {
        return this.methods[i];
    }

    public AttributeInnerClasses getAttributeInnerClasses()
    {
        if (this.getAttributes() != null)
        {
            for (Attribute attribute : this.getAttributes()) {
                if (attribute.getTag() == Const.ATTR_INNER_CLASSES) {
                    return (AttributeInnerClasses)attribute;
                }
            }
        }

        return null;
    }

    public AttributeBootstrapMethods getAttributeBootstrapMethods()
    {
        if (this.getAttributes() != null)
        {
            for (Attribute attribute : this.getAttributes()) {
                if (attribute.getTag() == Const.ATTR_BOOTSTRAP_METHODS) {
                    return (AttributeBootstrapMethods)attribute;
                }
            }
        }
        
        return null;
    }
    
    public AttributeMethodParameters getAttributeMethodParameters()
    {
        if (this.getAttributes() != null)
        {
            for (Attribute attribute : this.getAttributes()) {
                if (attribute.getTag() == Const.ATTR_METHOD_PARAMETERS) {
                    return (AttributeMethodParameters)attribute;
                }
            }
        }
        
        return null;
    }
    
    private boolean isAnonymousClass()
    {
        int index = this.thisClassName.lastIndexOf(
                StringConstants.INTERNAL_INNER_SEPARATOR);

        return index != -1 && index+1 < this.thisClassName.length() && Character.isDigit(this.thisClassName.charAt(index+1));
    }

    public boolean isAInnerClass()
    {
        return this.outerClass != null;
    }
    public ClassFile getOuterClass()
    {
        return outerClass;
    }
    public void setOuterClass(ClassFile outerClass)
    {
        this.outerClass = outerClass;

        // internalAnonymousClassName
        if (isAnonymousClass())
        {
            ConstantClass cc = this.constants.getConstantClass(this.superClass);

            if (cc.getNameIndex() != this.constants.getObjectClassNameIndex())
            {
                // Super class
                this.internalAnonymousClassName = this.superClassName;
            }
            else if (this.interfaces != null && this.interfaces.length > 0)
            {
                // Interface
                int interfaceIndex = this.interfaces[0];
                this.internalAnonymousClassName =
                    this.constants.getConstantClassName(interfaceIndex);
            }
            else
            {
                this.internalAnonymousClassName = StringConstants.JAVA_LANG_OBJECT;
            }
        }
        else
        {
            this.internalAnonymousClassName = null;
        }
    }

    public Field getOuterThisField()
    {
        return outerThisField;
    }
    public void setOuterThisField(Field outerThisField)
    {
        this.outerThisField = outerThisField;
    }

    public List<ClassFile> getInnerClassFiles()
    {
        return innerClassFiles;
    }
    public void setInnerClassFiles(List<ClassFile> innerClassFiles)
    {
        this.innerClassFiles = innerClassFiles;
    }
    public ClassFile getInnerClassFile(String internalClassName)
    {
        if (this.innerClassFiles != null &&
            internalClassName.length() > this.thisClassName.length()+1 &&
            internalClassName.charAt(this.thisClassName.length()) == StringConstants.INTERNAL_INNER_SEPARATOR)
        {
            for (int i=this.innerClassFiles.size()-1; i>=0; --i) {
                if (innerClassFiles.get(i).thisClassName.equals(internalClassName)) {
                    return innerClassFiles.get(i);
                }
            }
        }

        return null;
    }

    public Field getField(int fieldNameIndex, int fieldDescriptorIndex)
    {
        if (this.fields != null)
        {
            Field field;
            for (int i=this.fields.length-1; i>=0; --i)
            {
                field = this.fields[i];

                if (fieldNameIndex == field.getNameIndex() &&
                    fieldDescriptorIndex == field.getDescriptorIndex())
                {
                    return field;
                }
            }
        }

        return null;
    }
    public Field getField(String fieldName, String fieldDescriptor)
    {
        if (this.fields != null)
        {
            Field field;
            String name;
            for (int i=this.fields.length-1; i>=0; --i)
            {
                field = this.fields[i];

                name = this.constants.getConstantUtf8(field.getNameIndex());

                if (fieldName.equals(name))
                {
                    String descriptor =
                        this.constants.getConstantUtf8(field.getDescriptorIndex());

                    if (fieldDescriptor.equals(descriptor)) {
                        return field;
                    }
                }
            }
        }

        return null;
    }

    public Method getStaticMethod()
    {
        return staticMethod;
    }
    public Method getMethod(int methodNameIndex, int methodDescriptorIndex)
    {
        if (this.methods != null)
        {
            Method method;
            for (int i=this.methods.length-1; i>=0; --i)
            {
                method = this.methods[i];

                if (methodNameIndex == method.getNameIndex() &&
                    methodDescriptorIndex == method.getDescriptorIndex())
                {
                    return method;
                }
            }
        }

        return null;
    }
    public Method getMethod(String methodName, String methodDescriptor)
    {
        if (this.methods != null)
        {
            Method method;
            String name;
            for (int i=this.methods.length-1; i>=0; --i)
            {
                method = this.methods[i];

                name = this.constants.getConstantUtf8(method.getNameIndex());

                if (methodName.equals(name))
                {
                    String descriptor =
                        this.constants.getConstantUtf8(method.getDescriptorIndex());

                    if (methodDescriptor.equals(descriptor)) {
                        return method;
                    }
                }
            }
        }

        return null;
    }

    public List<Instruction> getEnumValues()
    {
        return enumValues;
    }

    public void setEnumValues(List<Instruction> enumValues)
    {
        this.enumValues = enumValues;
    }

    public String getInternalAnonymousClassName()
    {
        return internalAnonymousClassName;
    }

    public void addAccessor(String name, String descriptor, Accessor accessor)
    {
        this.accessors.computeIfAbsent(name, k -> new HashMap<>(1)).put(descriptor, accessor);
    }

    public Accessor getAccessor(String name, String descriptor)
    {
        Map<String, Accessor> map = this.accessors.get(name);
        return map == null ? null : map.get(descriptor);
    }

    public Map<Integer, List<Integer>> getSwitchMaps()
    {
        return this.switchMaps;
    }

    public Loader getLoader() {
        return loader;
    }

    @Override
    public String toString() {
        return internalClassName;
    }
}
