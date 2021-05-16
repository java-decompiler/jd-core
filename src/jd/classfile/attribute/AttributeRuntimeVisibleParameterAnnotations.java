package jd.classfile.attribute;


public class AttributeRuntimeVisibleParameterAnnotations  extends Attribute
{
	public ParameterAnnotations[] parameter_annotations;
	
	public AttributeRuntimeVisibleParameterAnnotations(
			byte tag, int attribute_name_index, 
			ParameterAnnotations[] parameter_annotations)
	{
		super(tag, attribute_name_index);
		this.parameter_annotations = parameter_annotations;
	}
}
