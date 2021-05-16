package jd.core.model.classfile.attribute;


public class AttributeRuntimeAnnotations extends Attribute
{
	public final Annotation[] annotations;
	
	public AttributeRuntimeAnnotations(byte tag, 
			                                    int attribute_name_index, 
                                                Annotation[] annotations)
	{
		super(tag, attribute_name_index);
		this.annotations = annotations;
	}
}
