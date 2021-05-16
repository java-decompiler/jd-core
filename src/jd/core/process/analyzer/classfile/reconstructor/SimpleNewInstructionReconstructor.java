package jd.core.process.analyzer.classfile.reconstructor;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.Method;
import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNew;
import jd.core.model.instruction.bytecode.instruction.Invokespecial;
import jd.core.model.instruction.bytecode.instruction.New;


/*
 * Recontruction de l'instruction 'new' depuis le motif :
 * Invokespecial(New, <init>, [ IConst_1 ])
 */
public class SimpleNewInstructionReconstructor 
	extends NewInstructionReconstructorBase
{
	public static void Reconstruct(
		ClassFile classFile, Method method, List<Instruction> list)
	{
		for (int invokespecialIndex=0; 
			 invokespecialIndex<list.size(); 
			 invokespecialIndex++)
		{
			if (list.get(invokespecialIndex).opcode != ByteCodeConstants.INVOKESPECIAL)
				continue;
			
			Invokespecial is = (Invokespecial)list.get(invokespecialIndex);
			
			if (is.objectref.opcode != ByteCodeConstants.NEW)
				continue;
			
			New nw = (New)is.objectref;		
			InvokeNew invokeNew = new InvokeNew(
				ByteCodeConstants.INVOKENEW, is.offset, 
				nw.lineNumber, is.index, is.args);
			
			list.set(invokespecialIndex, invokeNew);
			
			InitAnonymousClassConstructorParameterName(
				classFile, method, invokeNew);
		}
	}
}
