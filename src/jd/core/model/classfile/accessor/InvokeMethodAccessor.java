package jd.core.model.classfile.accessor;

import java.util.List;


public class InvokeMethodAccessor extends Accessor 
{
	final public String className;
	final public int methodOpcode;
	final public String methodName;
	final public String methodDescriptor;
	final public List<String> listOfParameterSignatures;
	final public String returnedSignature;

	public InvokeMethodAccessor(
		byte tag, String className, int methodOpcode, 
		String methodName, String methodDescriptor,
		List<String> listOfParameterSignatures, String returnedSignature)
	{
		super(tag);
		this.className = className;
		this.methodOpcode = methodOpcode;
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
		this.listOfParameterSignatures = listOfParameterSignatures;
		this.returnedSignature = returnedSignature;
	}
}
