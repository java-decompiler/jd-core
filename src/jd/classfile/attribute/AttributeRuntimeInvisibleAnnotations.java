package jd.classfile.attribute;


public class AttributeRuntimeInvisibleAnnotations extends Attribute
{
	public final Annotation[] annotations;
	
	public AttributeRuntimeInvisibleAnnotations(byte tag, 
			                                    int attribute_name_index, 
                                                Annotation[] annotations)
	{
		super(tag, attribute_name_index);
		this.annotations = annotations;
	}
}
