package jd.classfile.analyzer.variable;


public interface VariableNameGenerator 
{
	public void clearLocalNames();
	
	public String generateParameterNameFromSignature(
			String signature, boolean appearsOnce);
	
	public String generateLocalVariableNameFromSignature(
			String signature, boolean appearsOnce);
}
