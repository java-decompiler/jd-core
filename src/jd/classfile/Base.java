package jd.classfile;

import jd.Constants;
import jd.classfile.attribute.Annotation;
import jd.classfile.attribute.Attribute;
import jd.classfile.attribute.AttributeRuntimeInvisibleAnnotations;
import jd.classfile.attribute.AttributeRuntimeVisibleAnnotations;
import jd.classfile.attribute.AttributeSignature;


public class Base 
{
	public int access_flags;
	final public Attribute[] attributes;

	public Base(int access_flags, Attribute[] attributes)
	{
		this.access_flags = access_flags;
		this.attributes = attributes;
	}
	
	public AttributeSignature getAttributeSignature()
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == Constants.ATTR_SIGNATURE)
					return (AttributeSignature)this.attributes[i];
		
		return null;
	}
	
	public boolean containsAttributeDeprecated()
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == Constants.ATTR_DEPRECATED)
					return true;
		
		return false;
	}
	
	public boolean containsAnnotationDeprecated(ClassFile classFile)
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
			{
				switch (this.attributes[i].tag)
				{
				case Constants.ATTR_RUNTIME_INVISIBLE_ANNOTATIONS:
					if (containsAnnotationDeprecated(
							classFile, ((AttributeRuntimeInvisibleAnnotations)attributes[i]).annotations))
						return true;
					break;

				case Constants.ATTR_RUNTIME_VISIBLE_ANNOTATIONS:
					if (containsAnnotationDeprecated(
							classFile, ((AttributeRuntimeVisibleAnnotations)attributes[i]).annotations))
						return true;
					break;
				}
			}
		
		return false;
	}
	
	private boolean containsAnnotationDeprecated(
			ClassFile classFile, Annotation[] annotations)
	{
		if (annotations != null)
		{
			int internalDeprecatedSignature = 
				classFile.getConstantPool().internalDeprecatedSignatureIndex;
			
			for (int i=annotations.length-1; i>=0; --i)
				if (internalDeprecatedSignature == annotations[i].type_index)
					return true;
		}
		
		return false;
	}
}
