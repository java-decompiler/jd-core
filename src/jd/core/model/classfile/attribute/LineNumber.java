package jd.core.model.classfile.attribute;


public class LineNumber 
{
	public final int start_pc;
	public final int line_number;
	
	public LineNumber(int start_pc, int line_number) 
	{
		this.start_pc = start_pc;
		this.line_number = line_number;
	}
}
