package jd.classfile.attribute;


public class AttributeRuntimeVisibleAnnotations extends Attribute
{
	public final Annotation[] annotations;

	public AttributeRuntimeVisibleAnnotations(byte tag, 
			                                  int attribute_name_index, 
			                                  Annotation[] annotations)
	{
		super(tag, attribute_name_index);
		this.annotations = annotations;
	}
}
