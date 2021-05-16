package jd.core.process.deserializer;

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import jd.core.CoreConstants;
import jd.core.loader.Loader;
import jd.core.loader.LoaderException;
import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.Field;
import jd.core.model.classfile.Method;
import jd.core.model.classfile.attribute.Attribute;
import jd.core.model.classfile.attribute.AttributeInnerClasses;
import jd.core.model.classfile.attribute.InnerClass;
import jd.core.model.classfile.constant.Constant;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantDouble;
import jd.core.model.classfile.constant.ConstantFieldref;
import jd.core.model.classfile.constant.ConstantFloat;
import jd.core.model.classfile.constant.ConstantInteger;
import jd.core.model.classfile.constant.ConstantInterfaceMethodref;
import jd.core.model.classfile.constant.ConstantLong;
import jd.core.model.classfile.constant.ConstantMethodref;
import jd.core.model.classfile.constant.ConstantNameAndType;
import jd.core.model.classfile.constant.ConstantString;
import jd.core.model.classfile.constant.ConstantUtf8;
import jd.core.util.StringConstants;



public class ClassFileDeserializer 
{
	public static ClassFile Deserialize(Loader loader, String internalClassPath) 
		throws LoaderException
	{
		ClassFile classFile = LoadSingleClass(loader, internalClassPath);
		if (classFile == null)
			return null;
		
		AttributeInnerClasses aics = classFile.getAttributeInnerClasses();
		if (aics == null)
			return classFile;
		
		String internalClassPathPrefix = 
			internalClassPath.substring(
				0, internalClassPath.length() - StringConstants.CLASS_FILE_SUFFIX.length());
		String innerInternalClassNamePrefix = 
			internalClassPathPrefix + StringConstants.INTERNAL_INNER_SEPARATOR;	
		ConstantPool constants = classFile.getConstantPool();

		InnerClass[] cs = aics.classes;
		int length = cs.length;
		ArrayList<ClassFile> innerClassFiles = new ArrayList<ClassFile>(length);
		
		for (int i=0; i<length; i++)
		{
	    	String innerInternalClassPath = 
	    		constants.getConstantClassName(cs[i].inner_class_index);
	    	
			if (! innerInternalClassPath.startsWith(innerInternalClassNamePrefix))
				continue;			
			int offsetInternalInnerSeparator = innerInternalClassPath.indexOf(
				StringConstants.INTERNAL_INNER_SEPARATOR, 
				innerInternalClassNamePrefix.length());
			if (offsetInternalInnerSeparator != -1)
			{
				String tmpInnerInternalClassPath = 
					innerInternalClassPath.substring(0, offsetInternalInnerSeparator) + 
					StringConstants.CLASS_FILE_SUFFIX;
				if (loader.canLoad(tmpInnerInternalClassPath))
					// 'innerInternalClassName' is not a direct inner classe.
					continue;	
			}
			
			try
			{
				ClassFile innerClassFile = 
					Deserialize(loader, innerInternalClassPath + 
					StringConstants.CLASS_FILE_SUFFIX);
				
				if (innerClassFile != null)
				{
					// Alter inner class access flag
					innerClassFile.setAccessFlags(cs[i].inner_access_flags);
					// Setup outer class reference
					innerClassFile.setOuterClass(classFile);
					// Add inner classes
					innerClassFiles.add(innerClassFile);
				}
			}
			catch (LoaderException e)
			{}
		}
		
		// Add inner classes
		if (innerClassFiles != null)
		{
			classFile.setInnerClassFiles(innerClassFiles);
		}
		
		return classFile;
	}

	private static ClassFile LoadSingleClass(
			Loader loader, String internalClassPath) 
		throws LoaderException
	{
		DataInputStream dis = null;
		ClassFile classFile = null;

		try 
		{
			dis = loader.load(internalClassPath);
			if (dis != null)
				classFile = Deserialize(dis);
		}
		catch (IOException e) 
		{
			classFile = null;
			// DEBUG e.printStackTrace();
		}
		finally
		{
			if (dis != null)
				try { dis.close(); } catch (IOException e) { }
		}
		
		return classFile;
	}
	
	private static ClassFile Deserialize(DataInput di)
		throws IOException
	{
		CheckMagic(di);
	
		int minor_version = di.readUnsignedShort();
		int major_version = di.readUnsignedShort();
		
		Constant[] constants = DeserializeConstants(di);
		ConstantPool constantPool = new ConstantPool(constants);
		
		int access_flags = di.readUnsignedShort();
		int this_class = di.readUnsignedShort();
		int super_class = di.readUnsignedShort();
		
		int[] interfaces = DeserializeInterfaces(di);
		Field[] fieldInfos = DeserializeFields(di, constantPool);
		Method[] methodInfos = DeserializeMethods(di, constantPool);
	
		Attribute[] attributeInfos = 
			AttributeDeserializer.Deserialize(di, constantPool);
					
		return new ClassFile(
				minor_version, major_version,
				constantPool,
				access_flags, this_class, super_class,
				interfaces,
				fieldInfos,
				methodInfos,
				attributeInfos
		);
	}
	
	private static Constant[] DeserializeConstants(DataInput di)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;
		
		Constant[] constants = new Constant[count];
		
		for (int i=1; i<count; i++)
		{
			byte tag = di.readByte();
			
			switch (tag)
			{
			case ConstantConstant.CONSTANT_Class:
				constants[i] = new ConstantClass(tag, di.readUnsignedShort());
				break;
			case ConstantConstant.CONSTANT_Fieldref:
				constants[i] = new ConstantFieldref(tag, 
						                            di.readUnsignedShort(),
	                                                di.readUnsignedShort());
				break;
			case ConstantConstant.CONSTANT_Methodref:
				constants[i] = new ConstantMethodref(tag, 
						                             di.readUnsignedShort(),
	                                                 di.readUnsignedShort());
				break;
			case ConstantConstant.CONSTANT_InterfaceMethodref:
				constants[i] = new ConstantInterfaceMethodref(
													   tag, 
						                               di.readUnsignedShort(),
	                                                   di.readUnsignedShort());
				break;
			case ConstantConstant.CONSTANT_String:
				constants[i] = new ConstantString(tag, di.readUnsignedShort());
				break;
			case ConstantConstant.CONSTANT_Integer:
				constants[i] = new ConstantInteger(tag, di.readInt());
				break;
			case ConstantConstant.CONSTANT_Float:
				constants[i] = new ConstantFloat(tag, di.readFloat());
				break;
			case ConstantConstant.CONSTANT_Long:
				constants[i++] = new ConstantLong(tag, di.readLong());
				break;
			case ConstantConstant.CONSTANT_Double:
				constants[i++] = new ConstantDouble(tag, di.readDouble());
				break;
			case ConstantConstant.CONSTANT_NameAndType:
				constants[i] = new ConstantNameAndType(tag, 
						                               di.readUnsignedShort(), 
	                                                   di.readUnsignedShort());
				break;
			case ConstantConstant.CONSTANT_Utf8:			
				constants[i] = new ConstantUtf8(tag, di.readUTF());
				break;
			default:
				throw new ClassFormatException("Invalid constant pool entry");
			}
		}
		
		return constants;
	}
	
	private static int[] DeserializeInterfaces(DataInput di)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;
		
		int[] interfaces = new int[count];
		
		for (int i=0; i<count; i++)
			interfaces[i] = di.readUnsignedShort();
	
		return interfaces;
	}
	
	private static Field[] DeserializeFields(
			DataInput di, ConstantPool constantPool)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;
		
		Field[] fieldInfos = new Field[count];
		
		for (int i=0; i<count; i++)
			fieldInfos[i] = new Field(
						di.readUnsignedShort(), 
						di.readUnsignedShort(), 
						di.readUnsignedShort(), 
						AttributeDeserializer.Deserialize(di, constantPool));
		
		return fieldInfos;
	}
	
	private static Method[] DeserializeMethods(DataInput di, 
			ConstantPool constants)
		throws IOException
	{
		int count = di.readUnsignedShort();
		if (count == 0)
			return null;
		
		Method[] methodInfos = new Method[count];
		
		for (int i=0; i<count; i++)
			methodInfos[i] = new Method(
						di.readUnsignedShort(), 
						di.readUnsignedShort(), 
						di.readUnsignedShort(), 
						AttributeDeserializer.Deserialize(di, constants));
		
		return methodInfos;
	}
	
	private static void CheckMagic(DataInput di)
		throws IOException
	{
	    int magic = di.readInt();
	
	    if(magic != CoreConstants.JAVA_MAGIC_NUMBER)
	      throw new ClassFormatException("Invalid Java .class file");
	}
}
