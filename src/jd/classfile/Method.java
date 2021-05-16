package jd.classfile;

import java.util.List;

import jd.Constants;
import jd.classfile.attribute.Attribute;
import jd.classfile.attribute.AttributeCode;
import jd.classfile.attribute.AttributeExceptions;
import jd.classfile.attribute.AttributeLocalVariableTable;
import jd.classfile.attribute.AttributeNumberTable;
import jd.classfile.attribute.AttributeRuntimeInvisibleParameterAnnotations;
import jd.classfile.attribute.AttributeRuntimeVisibleParameterAnnotations;
import jd.classfile.attribute.CodeException;
import jd.classfile.attribute.LineNumber;
import jd.classfile.attribute.ParameterAnnotations;
import jd.instruction.bytecode.instruction.Instruction;



public class Method extends FieldOrMethod
{
	private boolean containsError;
	private int[] exceptionIndexes;
	private byte[] code;
	private LineNumber[] lineNumbers;
	private CodeException[] codeExceptions;
	private ParameterAnnotations[] invisibleParameterAnnotations;  
	private ParameterAnnotations[] visibleParameterAnnotations;  
	private List<Instruction> instructions;
	private List<Instruction> fastNodes;
	
	private LocalVariables localVariables;
	
	public Method(int access_flags, int name_index, int descriptor_index, 
			      Attribute[] attributes)
	{
		super(access_flags, name_index, descriptor_index, attributes);

		this.containsError = false;
		this.exceptionIndexes = null;
		this.code = null;
		this.localVariables = null;
		this.lineNumbers = null;
		this.codeExceptions = null;
		this.invisibleParameterAnnotations = null;
		this.visibleParameterAnnotations = null;
		
		if (attributes != null)
		{
			// exceptionIndexes
			AttributeExceptions ae = getAttributeExceptions();
			if (ae != null)
				this.exceptionIndexes = ae.exception_index_table;
				
			// code, localVariables, lineNumbers & exceptionIndexes
			AttributeCode ac = getAttributeCode();
			if (ac != null)
			{
				this.code = ac.code;
				
				// localVariables
				AttributeLocalVariableTable alvt = ac.getAttributeLocalVariableTable();
				if ((alvt != null) && (alvt.local_variable_table != null))
					this.localVariables = 
						new LocalVariables(alvt.local_variable_table);
	
				// lineNumbers
				AttributeNumberTable ant = ac.getAttributeLineNumberTable();
				this.lineNumbers = (ant == null) ? null : ant.line_number_table;	
				
				// codeExceptions
				this.codeExceptions = ac.exception_table;	
			}
			
			// invisibleParameterAnnotations
			AttributeRuntimeInvisibleParameterAnnotations aripa =
				getAttributeRuntimeInvisibleParameterAnnotations();
			if (aripa != null)
				this.invisibleParameterAnnotations = aripa.parameter_annotations;
			
			// visibleParameterAnnotations
			AttributeRuntimeVisibleParameterAnnotations arvpa =
				getAttributeRuntimeVisibleParameterAnnotations();
			if (arvpa != null)
				this.visibleParameterAnnotations = arvpa.parameter_annotations;
		}
	}
	
	public boolean containsError() 
	{
		return containsError;
	}

	public void setContainsError(boolean containsError) 
	{
		this.containsError = containsError;
	}

	public int[] getExceptionIndexes() 
	{
		return this.exceptionIndexes;
	}

	public LocalVariables getLocalVariables() 
	{
		return this.localVariables;
	}
	
	public void setLocalVariables(LocalVariables llv) 
	{
		this.localVariables = llv;
	}
	
	public List<Instruction> getInstructions() 
	{
		return instructions;
	}
	public void setInstructions(List<Instruction> instructions) 
	{
		this.instructions = instructions;
	}	

	public List<Instruction> getFastNodes() 
	{
		return fastNodes;
	}
	public void setFastNodes(List<Instruction> fastNodes) 
	{
		this.fastNodes = fastNodes;
	}

	public byte[] getCode() 
	{
		return this.code;
	}

	public LineNumber[] getLineNumbers()
	{
		return lineNumbers;
	}
	
	public CodeException[] getCodeExceptions()
	{
		return this.codeExceptions;
	}
	
	public ParameterAnnotations[] getInvisibleParameterAnnotations()
	{
		return this.invisibleParameterAnnotations;
	}
	
	public ParameterAnnotations[] getVisibleParameterAnnotations()
	{
		return this.visibleParameterAnnotations;
	}  

	// --------------------------------------------------------------------- //
	
	private AttributeExceptions getAttributeExceptions() 
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == Constants.ATTR_EXCEPTIONS)
					return (AttributeExceptions)attributes[i];

		return null;
	}
	
	private AttributeCode getAttributeCode() 
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == Constants.ATTR_CODE)
					return (AttributeCode)this.attributes[i];

		return null;
	}
	
	private AttributeRuntimeInvisibleParameterAnnotations 
		getAttributeRuntimeInvisibleParameterAnnotations() 
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == 
					Constants.ATTR_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)
					return (AttributeRuntimeInvisibleParameterAnnotations)attributes[i];

		return null;
	}
	
	private AttributeRuntimeVisibleParameterAnnotations 
		getAttributeRuntimeVisibleParameterAnnotations() 
	{
		if (this.attributes != null)
			for (int i=this.attributes.length-1; i>=0; --i)
				if (this.attributes[i].tag == 
					Constants.ATTR_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)
					return (AttributeRuntimeVisibleParameterAnnotations)attributes[i];

		return null;
	}
}
