package jd.core.model.classfile.attribute;


public class ElementValueAnnotationValue extends ElementValue
{
	final public Annotation annotation_value;
	
	public ElementValueAnnotationValue(byte tag, Annotation annotation_value) 
	{ 
		super(tag);
		this.annotation_value = annotation_value;
	}
}
