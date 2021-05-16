package jd.core.model.classfile;



public class LocalVariable 
	implements Comparable<LocalVariable>
{
	public int start_pc;
	public int length;
	public int name_index;
	public int signature_index;
	final public int index;
	public boolean exceptionOrReturnAddress;
	// Champ de bits utilisé pour determiner le type de la variable (byte, char, 
	// short, int).
	public int typesBitField;
	// Champs utilisé lors de la generation des declarations de variables 
	// locales (FastDeclarationAnalyzer.Analyze).
	public boolean declarationFlag = false;

	public boolean finalFlag = false;

	public LocalVariable(
			int start_pc, int length, int name_index, int signature_index, 
			int index) 
	{
		this(start_pc, length, name_index, signature_index, index, false, 0);
	}
	
	public LocalVariable(
			int start_pc, int length, int name_index, int signature_index, 
			int index, int typesBitSet) 
	{
		this(start_pc, length, name_index, signature_index, index, false, 
			 typesBitSet);
	}

	public LocalVariable(
			int start_pc, int length, int name_index, int signature_index, 
			int index, boolean exception) 
	{
		this(start_pc, length, name_index, signature_index, index, exception, 0);
	}

	protected LocalVariable(
		int start_pc, int length, int name_index, int signature_index, 
		int index, boolean exceptionOrReturnAddress, int typesBitField) 
	{
		this.start_pc = start_pc;
		this.length = length;
		this.name_index = name_index;
		this.signature_index = signature_index;
		this.index = index;
		this.exceptionOrReturnAddress = exceptionOrReturnAddress;
		this.declarationFlag = exceptionOrReturnAddress;
		this.typesBitField = typesBitField;
	}

	public void updateRange(int offset)
	{
		if (offset < this.start_pc)
		{
			this.length += (this.start_pc - offset);
			this.start_pc = offset;
		}
		
		if (offset >= this.start_pc+this.length)
		{
			this.length = offset - this.start_pc + 1;
		}
	}

	public void updateSignatureIndex(int signatureIndex)
	{
		this.signature_index = signatureIndex;
	}
	
	public String toString()
	{
		return 
			"LocalVariable{start_pc=" + start_pc +
			", length=" + length +
			", name_index=" + name_index +
			", signature_index=" + signature_index +
			", index=" + index +
			"}";
	}

	public int compareTo(LocalVariable other) 
	{
		if (other == null)
			return -1;

		if (this.name_index != other.name_index)
			return this.name_index - other.name_index;

		if (this.length != other.length)
			return this.length - other.length;

		if (this.start_pc != other.start_pc)
			return this.start_pc - other.start_pc;

		return this.index - other.index;
	}
}
