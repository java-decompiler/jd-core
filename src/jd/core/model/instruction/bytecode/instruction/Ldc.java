package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.constant.Constant;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.util.StringConstants;

public class Ldc extends LdcInstruction 
{
	public Ldc(int opcode, int offset, int lineNumber, int index)
	{
		super(opcode, offset, lineNumber, index);
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
		
		Constant c = constants.get(this.index);
		
		if (c == null)
			return null;
		
		switch (c.tag)
		{
		case ConstantConstant.CONSTANT_Float:
			return "F";
		case ConstantConstant.CONSTANT_Integer:
			return "I";
		case ConstantConstant.CONSTANT_String:
			return StringConstants.INTERNAL_STRING_SIGNATURE;
		case ConstantConstant.CONSTANT_Class:
			return StringConstants.INTERNAL_CLASS_SIGNATURE;
			/*{
				int index = ((ConstantClass)c).name_index;			
				return SignatureUtil.CreateTypeName(
					constants.getConstantUtf8(index));
			}*/
		default:
			return null;
		}
	}	
}
