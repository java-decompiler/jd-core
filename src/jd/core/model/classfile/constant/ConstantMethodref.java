package jd.core.model.classfile.constant;

import java.util.List;


public class ConstantMethodref extends Constant 
{
	final public int class_index;
	final public int name_and_type_index;

	private List<String> listOfParameterSignatures;
	private String returnedSignature;
	
	public ConstantMethodref(
		byte tag, int class_index, int name_and_type_index)
	{
		super(tag);
		this.class_index = class_index;
		this.name_and_type_index = name_and_type_index;
		
		this.listOfParameterSignatures = null;
		this.returnedSignature = null;
	}

	public ConstantMethodref(
		byte tag, int class_index, int name_and_type_index,
		List<String> listOfParameterSignatures, String returnedSignature)
	{
		super(tag);
		this.class_index = class_index;
		this.name_and_type_index = name_and_type_index;
		
		this.listOfParameterSignatures = listOfParameterSignatures;
		this.returnedSignature = returnedSignature;
	}

	public List<String> getListOfParameterSignatures() 
	{
		return listOfParameterSignatures;
	}
	public void setParameterSignatures(List<String> listOfParameterSignatures) 
	{
		this.listOfParameterSignatures = listOfParameterSignatures;
	}
	public int getNbrOfParameters()
	{
		return (this.listOfParameterSignatures == null) ? 
				0 : this.listOfParameterSignatures.size();
	}
	
	public String getReturnedSignature() 
	{
		return returnedSignature;
	}
	public void setReturnedSignature(String returnedSignature) 
	{
		this.returnedSignature = returnedSignature;
	}
	public boolean returnAResult()
	{
		return (this.returnedSignature == null) ? 
				false : !"V".equals(this.returnedSignature);
	}
}
