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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.core.model.classfile.accessor.Accessor;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeConstants;
import jd.core.model.classfile.attribute.AttributeInnerClasses;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;



public class ClassFile extends Base
{
	private int minor_version;
	private int major_version;
	private int this_class;
	private int super_class;
	
	private int interfaces[];
	private Field fields[];
	private Method methods[];
	
	private ConstantPool constants;
	private String thisClassName;
	private String superClassName;
	private String internalClassName;
	private String internalPackageName;
	
	private ClassFile outerClass = null;
	private Field outerThisField = null;
	private ArrayList<ClassFile> innerClassFiles = null;

	private Method staticMethod = null;
	private List<Instruction> enumValues = null;
	private String internalAnonymousClassName;
	private Map<String, Map<String, Accessor>> accessors;
	
	// Attention :
	// - Dans le cas des instructions Switch+Enum d'Eclipse, la cl� de la map 
	//   est l'indexe du nom de la m�thode 
	//   "static int[] $SWITCH_TABLE$basic$data$TestEnum$enum1()".
	// - Dans le cas des instructions Switch+Enum des autres compilateurs, la 
	//   cl� de la map est l'indexe du nom de la classe interne "static class 1" 
	//   contenant le tableau de correspondance 
	//   "$SwitchMap$basic$data$TestEnum$enum1".
	private Map<Integer, List<Integer>> switchMaps;
	
	public ClassFile(int minor_version, int major_version, 
					 ConstantPool constants, int access_flags, int this_class, 
			         int super_class, int[] interfaces, Field[] fields, 
			         Method[] methods, Attribute[] attributes) 
	{
		super(access_flags, attributes);
		
		this.minor_version = minor_version;
		this.major_version = major_version;
		this.this_class = this_class;
		this.super_class = super_class;
		this.interfaces = interfaces;
		this.fields = fields;
		this.methods = methods;
		
		this.constants = constants;
		
		// internalClassName
		this.thisClassName = 
			this.constants.getConstantClassName(this.this_class);
		// internalSuperClassName
		this.superClassName = (this.super_class == 0) ? null :
			this.constants.getConstantClassName(this.super_class);
		//
		this.internalClassName = SignatureUtil.CreateTypeName(this.thisClassName);
		// internalPackageName
		int index = this.thisClassName.lastIndexOf(
				StringConstants.INTERNAL_PACKAGE_SEPARATOR);
		this.internalPackageName = 
			(index == -1) ? "" : this.thisClassName.substring(0, index);
		
		// staticMethod
		if (this.methods != null)
		{
			for (int i=this.methods.length-1; i>=0; --i)
			{
				Method method = this.methods[i];		
				
				if (((method.access_flags & ClassFileConstants.ACC_STATIC) != 0) &&
					(method.name_index == this.constants.classConstructorIndex))
		   		{
		   			this.staticMethod = method;
		   			break;
		   		}
			}
		}
		
		// internalAnonymousClassName
		this.internalAnonymousClassName = null;
		// accessors
		this.accessors = new HashMap<String, Map<String, Accessor>>(10);
		// SwitchMap for Switch+Enum instructions
		this.switchMaps = new HashMap<Integer, List<Integer>>();
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
		return major_version;
	}

	public int getMinorVersion()
	{
		return minor_version;
	}

	public int getSuperClassIndex() 
	{
		return super_class;
	}

	public int getThisClassIndex() 
	{
		return this_class;
	}

	public String getClassName()
	{
		if (this.outerClass == null)
		{
			// int index = this.thisClassName.lastIndexOf(
			//   AnalyzerConstants.INTERNAL_INNER_SEPARATOR);
			//if (index != -1)
			//	return this.thisClassName.substring(index+1);
			
			int index = this.thisClassName.lastIndexOf(
					StringConstants.INTERNAL_PACKAGE_SEPARATOR);
			return (index == -1) ? 
				this.thisClassName : 
				this.thisClassName.substring(index + 1);
		}
		else
		{
			String outerClassName = this.outerClass.getThisClassName();

			return this.thisClassName.substring(
				outerClassName.length() + 1);
		}
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

	public void setAccessFlags(int access_flags) 
	{
		this.access_flags = access_flags;
	}

	public Field[] getFields()
	{
		return this.fields;
	}
	
	public Method[] getMethods()
	{
		return this.methods;
	}
	
	public Attribute[] getAttributes()
	{
		return this.attributes;
	}
	
	public AttributeInnerClasses getAttributeInnerClasses()
	{
		if (this.attributes != null)
		{
			for (int i=0; i<this.attributes.length; i++)
				if (this.attributes[i].tag == AttributeConstants.ATTR_INNER_CLASSES)
					return (AttributeInnerClasses)this.attributes[i];		
		}
		
		return null;
	}
	
	private boolean isAnonymousClass()
	{
		int index = this.thisClassName.lastIndexOf(
				StringConstants.INTERNAL_INNER_SEPARATOR);
		
		if ((index == -1) || (index+1 >= this.thisClassName.length()))
			return false;
		
		return Character.isDigit(this.thisClassName.charAt(index+1));
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
			ConstantClass cc = this.constants.getConstantClass(this.super_class);
			
			if (cc.name_index != this.constants.objectClassNameIndex)
			{
				// Super class
				this.internalAnonymousClassName = this.superClassName;
			}
			else if ((this.interfaces != null) && (this.interfaces.length > 0))
			{
				// Interface
				int interfaceIndex = this.interfaces[0];
				this.internalAnonymousClassName =  
					this.constants.getConstantClassName(interfaceIndex);
			}
			else
			{
				this.internalAnonymousClassName = 
						StringConstants.INTERNAL_OBJECT_CLASS_NAME;
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

	public ArrayList<ClassFile> getInnerClassFiles() 
	{
		return innerClassFiles;
	}
	public void setInnerClassFiles(ArrayList<ClassFile> innerClassFiles) 
	{
		this.innerClassFiles = innerClassFiles;
	}
	public ClassFile getInnerClassFile(String internalClassName) 
	{
		if ((this.innerClassFiles != null) &&
			(internalClassName.length() > this.thisClassName.length()+1) &&
			(internalClassName.charAt(this.thisClassName.length()) == StringConstants.INTERNAL_INNER_SEPARATOR))
		{
			for (int i=this.innerClassFiles.size()-1; i>=0; --i)
				if (innerClassFiles.get(i).thisClassName.equals(internalClassName))
					return innerClassFiles.get(i);
		}
		
		return null;
	}
	
	public Field getField(int fieldNameIndex, int fieldDescriptorIndex)
	{
		if (this.fields != null)
		{
			for (int i=this.fields.length-1; i>=0; --i)
			{
				Field field = this.fields[i];		
				
				if ((fieldNameIndex == field.name_index) && 
					(fieldDescriptorIndex == field.descriptor_index))
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
			for (int i=this.fields.length-1; i>=0; --i)
			{
				Field field = this.fields[i];		
				
				String name = 
		   			this.constants.getConstantUtf8(field.name_index);		
				
				if (fieldName.equals(name))
				{
					String descriptor = 
			   			this.constants.getConstantUtf8(field.descriptor_index);		
					
					if (fieldDescriptor.equals(descriptor))
						return field;
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
			for (int i=this.methods.length-1; i>=0; --i)
			{
				Method method = this.methods[i];		
				
				if ((methodNameIndex == method.name_index) && 
					(methodDescriptorIndex == method.descriptor_index))
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
			for (int i=this.methods.length-1; i>=0; --i)
			{
				Method method = this.methods[i];		
				
				String name = 
		   			this.constants.getConstantUtf8(method.name_index);		
				
				if (methodName.equals(name))
				{
					String descriptor = 
			   			this.constants.getConstantUtf8(method.descriptor_index);		
					
					if (methodDescriptor.equals(descriptor))
						return method;
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
		Map<String, Accessor> map = this.accessors.get(name);
		
		if (map == null)
		{
			map = new HashMap<String, Accessor>(1);
			this.accessors.put(name, map);
		}
		
		map.put(descriptor, accessor);
	}
	
	public Accessor getAccessor(String name, String descriptor)
	{
		Map<String, Accessor> map = this.accessors.get(name);
		return (map == null) ? null : map.get(descriptor);
	}
	
	public Map<Integer, List<Integer>> getSwitchMaps()
	{
		return this.switchMaps;
	}	
}
