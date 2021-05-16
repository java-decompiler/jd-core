package jd.classfile.attribute;


public class CodeException 
{
	public int start_pc;
	public int end_pc;
	public final int handler_pc; 
	public final int catch_type;
	
	public CodeException(int start_pc, int end_pc, int handler_pc, 
			             int catch_type) 
	{
		this.start_pc = start_pc;
		this.end_pc = end_pc;
		this.handler_pc = handler_pc;
		this.catch_type = catch_type;
	}
}
