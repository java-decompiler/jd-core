package jd.core.process.analyzer.variable;


public interface VariableNameGenerator 
{
	public void clearLocalNames();
	
	public String generateParameterNameFromSignature(
			String signature, boolean appearsOnceFlag, 
			boolean varargsFlag, int anonymousClassDepth);
	
	public String generateLocalVariableNameFromSignature(
			String signature, boolean appearsOnce);
}
