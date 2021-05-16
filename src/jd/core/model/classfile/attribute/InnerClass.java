package jd.core.model.classfile.attribute;


public class InnerClass 
{
	public final int inner_class_index;
	public final int outer_class_index;
	public final int inner_name_index;
	public final int inner_access_flags;
	  
	public InnerClass(int inner_class_index, int outer_class_index, 
			            int inner_name_index, int inner_access_flags) 
	{
		this.inner_class_index = inner_class_index;
		this.outer_class_index = outer_class_index;
		this.inner_name_index = inner_name_index;
		this.inner_access_flags = inner_access_flags;
	}
}
