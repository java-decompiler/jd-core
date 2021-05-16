package jd.core.model.classfile.attribute;


public class AttributeInnerClasses extends Attribute 
{
	public final InnerClass[] classes;
	  
	public AttributeInnerClasses(byte tag, 
			                     int attribute_name_index, 
			                     InnerClass[] classes) 
	{
		super(tag, attribute_name_index);
		this.classes = classes;
	}
}
