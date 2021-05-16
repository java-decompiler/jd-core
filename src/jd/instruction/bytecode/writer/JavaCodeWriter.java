package jd.instruction.bytecode.writer;

import java.util.HashSet;
import java.util.List;

import jd.Preferences;
import jd.classfile.ClassFile;
import jd.classfile.Method;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.visitor.ByteCodeSourceWriterVisitor;
import jd.printer.Printer;
import jd.util.ReferenceMap;



public class JavaCodeWriter
{
	public static void Write(
			HashSet<String> keywordSet, Preferences preferences, Printer spw, 
			ReferenceMap referenceMap, ClassFile classFile, Method method)
	{
		List<Instruction> list = method.getInstructions();
		
		if (list != null)
		{
			spw.startComment();
			
			spw.print(Printer.UNKNOWN_LINE_NUMBER, "// Byte code++:");
			spw.endOfLineComment();
			
			ByteCodeSourceWriterVisitor swv = 
				new ByteCodeSourceWriterVisitor(
						keywordSet, preferences, spw, referenceMap, classFile, 
						method.access_flags, method.getLocalVariables());
			int length = list.size();
	
			for (int i=0; i<length; i++)
			{
				Instruction instruction = list.get(i);
				
				spw.print(Printer.UNKNOWN_LINE_NUMBER, "//  ");
				spw.print(Printer.UNKNOWN_LINE_NUMBER, instruction.offset);
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ": ");
//				spw.print("label");
//				spw.print(list.get(i).offset);
//				spw.print(": ");
				swv.visit(instruction);
				spw.print(Printer.UNKNOWN_LINE_NUMBER, ';');
				spw.endOfLineComment();
			}
			
			spw.endComment();
		}
	}
}
