package jd.core.model.instruction.bytecode.instruction;

import jd.core.model.classfile.ConstantPool;
import jd.core.model.classfile.LocalVariables;
import jd.core.model.classfile.constant.Constant;
import jd.core.model.classfile.constant.ConstantClass;
import jd.core.model.classfile.constant.ConstantConstant;
import jd.core.model.classfile.constant.ConstantUtf8;
import jd.core.util.SignatureUtil;


public class CheckCast extends IndexInstruction
{
	public Instruction objectref;

	public CheckCast(
		int opcode, int offset, int lineNumber, 
		int index, Instruction objectref)
	{
		super(opcode, offset, lineNumber, index);
		this.objectref = objectref;
	}

	public String getReturnedSignature(
			ConstantPool constants, LocalVariables localVariables) 
	{
		if (constants == null)
			return null;
		
		Constant c = constants.get(this.index);
		
		if (c.tag == ConstantConstant.CONSTANT_Utf8)
		{
			ConstantUtf8 cutf8 = (ConstantUtf8)c;
			return cutf8.bytes;
		}
		else
		{
			ConstantClass cc = (ConstantClass)c;
			String signature = constants.getConstantUtf8(cc.name_index);
			if (signature.charAt(0) != '[')
				signature = SignatureUtil.CreateTypeName(signature);
			return signature;
		}			
	}
	
	public int getPriority()
	{
		return 2;
	}
}
