package jd.classfile.attribute;

public class AttributeRuntimeInvisibleParameterAnnotations  extends Attribute
{
	public ParameterAnnotations[] parameter_annotations;
	
	public AttributeRuntimeInvisibleParameterAnnotations(
			byte tag, int attribute_name_index, 
			ParameterAnnotations[] parameter_annotations)
	{
		super(tag, attribute_name_index);
		this.parameter_annotations = parameter_annotations;
	}
}
