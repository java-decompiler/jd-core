package jd.instruction.bytecode.reconstructor;

import java.util.List;

import jd.instruction.bytecode.ByteCodeConstants;
import jd.instruction.bytecode.instruction.ALoad;
import jd.instruction.bytecode.instruction.AStore;
import jd.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.instruction.bytecode.instruction.AssignmentInstruction;
import jd.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.instruction.bytecode.instruction.DupLoad;
import jd.instruction.bytecode.instruction.DupStore;
import jd.instruction.bytecode.instruction.GetField;
import jd.instruction.bytecode.instruction.GetStatic;
import jd.instruction.bytecode.instruction.ILoad;
import jd.instruction.bytecode.instruction.IStore;
import jd.instruction.bytecode.instruction.Instruction;
import jd.instruction.bytecode.instruction.LoadInstruction;
import jd.instruction.bytecode.instruction.PutField;
import jd.instruction.bytecode.instruction.PutStatic;
import jd.instruction.bytecode.instruction.StoreInstruction;
import jd.instruction.bytecode.instruction.attribute.ValuerefAttribute;
import jd.instruction.bytecode.visitor.CompareInstructionVisitor;
import jd.instruction.bytecode.visitor.SearchDupLoadInstructionVisitor;
import jd.instruction.fast.visitor.ReplaceDupLoadVisitor;


/*
 * Recontruction des affectations multiples depuis le motif :
 * DupStore( ??? )
 * ...
 * {?Store | PutField | PutStatic}( DupLoad )
 * ...
 * ???( DupLoad )
 * Deux types de reconstruction :
 *  - a = b = c;
 *  - b = c; ...; a = b;
 */
public class AssignmentInstructionReconstructor 
{
	public static void Reconstruct(List<Instruction> list)
	{
		for (int dupStoreIndex=0; dupStoreIndex<list.size(); dupStoreIndex++)
		{
			if (list.get(dupStoreIndex).opcode != ByteCodeConstants.DUPSTORE)
				continue;

			// DupStore trouvé
			DupStore dupStore = (DupStore)list.get(dupStoreIndex);

			int length = list.size();

			// Ne pas prendre en compte les instructions DupStore suivie par une 
			// instruction AASTORE ou ARRAYSTORE dont l'attribut arrayref pointe 
			// vers l'instruction DupStore : ce cas est traité par
			// 'InitArrayInstructionReconstructor'.
			if (dupStoreIndex+1 < length)
			{
				Instruction i = list.get(dupStoreIndex+1);				

				// Recherche de ?AStore ( DupLoad, index, value )
				if ((i.opcode == ByteCodeConstants.AASTORE) ||
					(i.opcode == ByteCodeConstants.ARRAYSTORE))
				{
					i = ((ArrayStoreInstruction)i).arrayref;					
					if ((i.opcode == ByteCodeConstants.DUPLOAD) &&
						(((DupLoad)i).dupStore == dupStore))
						continue;
				}
			}
			
			int xstorePutfieldPutstaticIndex = dupStoreIndex;
			
			while (++xstorePutfieldPutstaticIndex < length)
			{
				Instruction xstorePutfieldPutstatic = 
					list.get(xstorePutfieldPutstaticIndex);
				Instruction dupload1 = null;
				
				switch (xstorePutfieldPutstatic.opcode)
				{
				case ByteCodeConstants.ASTORE:
				case ByteCodeConstants.ISTORE:
				case ByteCodeConstants.STORE:
				case ByteCodeConstants.PUTFIELD:
				case ByteCodeConstants.PUTSTATIC:
				case ByteCodeConstants.AASTORE:
				case ByteCodeConstants.ARRAYSTORE:
					{
						Instruction i = 
							((ValuerefAttribute)xstorePutfieldPutstatic).getValueref();						
						if ((i.opcode == ByteCodeConstants.DUPLOAD) && 
							(((DupLoad)i).dupStore == dupStore))
						{
							// 1er DupLoad trouvé
							dupload1 = (DupLoad)i;
						}
					}
					break;
				case ByteCodeConstants.DSTORE:
				case ByteCodeConstants.FSTORE:
				case ByteCodeConstants.LSTORE:
					new RuntimeException("Instruction inattendue")
								.printStackTrace();
				}
					
				if (dupload1 == null)
					continue;
				
				// Recherche du 2eme DupLoad
				Instruction dupload2 = null;
				int dupload2Index = xstorePutfieldPutstaticIndex;
				
				while (++dupload2Index < length)
				{
					dupload2 = SearchDupLoadInstructionVisitor.visit(
						list.get(dupload2Index), dupStore);
					if (dupload2 != null)
						break;
				}
				
				if (dupload2 == null)
					continue;

				if (dupload1.lineNumber == dupload2.lineNumber)
				{
					// Assignation multiple sur une seule ligne : a = b = c;
					AssignmentInstruction ai = CreateAssignmentInstruction(
						xstorePutfieldPutstatic, dupStore);	
					
					// Remplacement du 2eme DupLoad
					ReplaceDupLoadVisitor visitor = 
						new ReplaceDupLoadVisitor(dupStore, ai);
					visitor.visit(list.get(dupload2Index));
					
					((ValuerefAttribute)xstorePutfieldPutstatic)
						.getValueref().opcode = ByteCodeConstants.NOP;		
					
					list.remove(xstorePutfieldPutstaticIndex);
					list.remove(dupStoreIndex);
					dupStoreIndex--;
					length -= 2;
				}
				else
				{
					// Assignation multiple sur deux lignes : b = c; a = b;
					
					// Create new instruction 
					// {?Load | GetField | GetStatic | AALoad | ARRAYLoad }
					Instruction newInstruction = 
						CreateInstruction(xstorePutfieldPutstatic);
					
					if (newInstruction != null)
					{
						// Remplacement du 1er DupLoad
						ReplaceDupLoadVisitor visitor = 
							new ReplaceDupLoadVisitor(dupStore, dupStore.objectref);
						visitor.visit(xstorePutfieldPutstatic);
						
						// Remplacement du 2eme DupLoad
						visitor.init(dupStore, newInstruction);
						visitor.visit(list.get(dupload2Index));
						
						list.remove(dupStoreIndex);
						dupStoreIndex--;
						length--;
					}
				}
			}
		}
	}
	
	private static Instruction CreateInstruction(
		Instruction xstorePutfieldPutstatic)
	{
		switch (xstorePutfieldPutstatic.opcode)
		{
		case ByteCodeConstants.ASTORE:
			return new ALoad(
				ByteCodeConstants.ALOAD, 
				xstorePutfieldPutstatic.offset, 
				xstorePutfieldPutstatic.lineNumber,
				((AStore)xstorePutfieldPutstatic).index);
		case ByteCodeConstants.ISTORE:
			return new ILoad(
				ByteCodeConstants.ILOAD, 
				xstorePutfieldPutstatic.offset, 
				xstorePutfieldPutstatic.lineNumber,
				((IStore)xstorePutfieldPutstatic).index);
		case ByteCodeConstants.STORE:
			return new LoadInstruction(
				ByteCodeConstants.LOAD, 
				xstorePutfieldPutstatic.offset, 
				xstorePutfieldPutstatic.lineNumber,
				((StoreInstruction)xstorePutfieldPutstatic).index, 
				xstorePutfieldPutstatic.getReturnedSignature(null, null));
		case ByteCodeConstants.PUTFIELD:
			return new GetField(
				ByteCodeConstants.GETFIELD, 
				xstorePutfieldPutstatic.offset, 
				xstorePutfieldPutstatic.lineNumber,
				((PutField)xstorePutfieldPutstatic).index, 
				((PutField)xstorePutfieldPutstatic).objectref);
		case ByteCodeConstants.PUTSTATIC:
			return new GetStatic(
				ByteCodeConstants.GETSTATIC, 
				xstorePutfieldPutstatic.offset,
				xstorePutfieldPutstatic.lineNumber,
				((PutStatic)xstorePutfieldPutstatic).index);
		/* case ByteCodeConstants.AASTORE:
			return new AALoad(
				ByteCodeConstants.ARRAYLOAD, 
				xstorePutfieldPutstatic.offset, 
				xstorePutfieldPutstatic.lineNumber,
				((AAStore)xstorePutfieldPutstatic).arrayref,
				((AAStore)xstorePutfieldPutstatic).indexref);
		case ByteCodeConstants.ARRAYSTORE:
			return new ArrayLoadInstruction(
				ByteCodeConstants.ARRAYLOAD, 
				xstorePutfieldPutstatic.offset, 
				xstorePutfieldPutstatic.lineNumber,
				((ArrayStoreInstruction)xstorePutfieldPutstatic).arrayref,
				((ArrayStoreInstruction)xstorePutfieldPutstatic).indexref,
				((ArrayStoreInstruction)xstorePutfieldPutstatic).signature); */
		default:
			return null;
		}
	}
	
	private static AssignmentInstruction CreateAssignmentInstruction(
		Instruction xstorePutfieldPutstatic, DupStore dupStore)
	{
		if (dupStore.objectref.opcode == ByteCodeConstants.BINARYOP)
		{
			Instruction value1 = 
				((BinaryOperatorInstruction)dupStore.objectref).value1;

			if (xstorePutfieldPutstatic.lineNumber == value1.lineNumber) 
			{
				switch (xstorePutfieldPutstatic.opcode)
				{
				case ByteCodeConstants.ASTORE:
					if ((value1.opcode == ByteCodeConstants.ALOAD) &&
						(((StoreInstruction)xstorePutfieldPutstatic).index == 
							((LoadInstruction)value1).index))
						return CreateBinaryOperatorAssignmentInstruction(
							xstorePutfieldPutstatic, dupStore);
					break;
				case ByteCodeConstants.ISTORE:
					if ((value1.opcode == ByteCodeConstants.ILOAD) &&
						(((StoreInstruction)xstorePutfieldPutstatic).index == 
							((LoadInstruction)value1).index))
						return CreateBinaryOperatorAssignmentInstruction(
							xstorePutfieldPutstatic, dupStore);
					break;
				case ByteCodeConstants.STORE:
					if ((value1.opcode == ByteCodeConstants.LOAD) &&
						(((StoreInstruction)xstorePutfieldPutstatic).index == 
							((LoadInstruction)value1).index))
						return CreateBinaryOperatorAssignmentInstruction(
							xstorePutfieldPutstatic, dupStore);
					break;
				case ByteCodeConstants.PUTFIELD:
					if ((value1.opcode == ByteCodeConstants.GETFIELD) &&
						(((PutField)xstorePutfieldPutstatic).index == 
							((GetField)value1).index))
					{
						CompareInstructionVisitor visitor = 
							new CompareInstructionVisitor();
						
						if (visitor.visit(
								((PutField)xstorePutfieldPutstatic).objectref,
								((GetField)value1).objectref))
							return CreateBinaryOperatorAssignmentInstruction(
								xstorePutfieldPutstatic, dupStore);
					}
					break;
				case ByteCodeConstants.PUTSTATIC:
					if ((value1.opcode == ByteCodeConstants.GETFIELD) &&
						(((PutStatic)xstorePutfieldPutstatic).index == 
							((GetStatic)value1).index))
						return CreateBinaryOperatorAssignmentInstruction(
							xstorePutfieldPutstatic, dupStore);
					break;
				case ByteCodeConstants.AASTORE:
					if (value1.opcode == ByteCodeConstants.AALOAD)
					{
						ArrayStoreInstruction aas = 
							(ArrayStoreInstruction)xstorePutfieldPutstatic;
						ArrayLoadInstruction aal = 
							(ArrayLoadInstruction)value1;
						CompareInstructionVisitor visitor = 
							new CompareInstructionVisitor();
						
						if (visitor.visit(
								aas.arrayref, aal.arrayref) && 
							visitor.visit(
								aas.indexref, aal.indexref))
							return CreateBinaryOperatorAssignmentInstruction(
									xstorePutfieldPutstatic, dupStore);
					}
					break;
				case ByteCodeConstants.ARRAYSTORE:
					if (value1.opcode == ByteCodeConstants.ARRAYLOAD)
					{
						ArrayStoreInstruction aas = 
							(ArrayStoreInstruction)xstorePutfieldPutstatic;
						ArrayLoadInstruction aal = 
							(ArrayLoadInstruction)value1;
						CompareInstructionVisitor visitor = 
							new CompareInstructionVisitor();
						
						if (visitor.visit(
								aas.arrayref, aal.arrayref) && 
							visitor.visit(
								aas.indexref, aal.indexref))
							return CreateBinaryOperatorAssignmentInstruction(
									xstorePutfieldPutstatic, dupStore);
					}
					break;
				case ByteCodeConstants.DSTORE:
				case ByteCodeConstants.FSTORE:
				case ByteCodeConstants.LSTORE:
					new RuntimeException("Unexpected instruction")
								.printStackTrace();			
				}
			}
		}
		
		return new AssignmentInstruction(
				ByteCodeConstants.ASSIGNMENT, xstorePutfieldPutstatic.offset,
				dupStore.lineNumber, 14, "=", 
				xstorePutfieldPutstatic, dupStore.objectref);
	}

	private static AssignmentInstruction CreateBinaryOperatorAssignmentInstruction(
			Instruction xstorePutfieldPutstatic, DupStore dupstore)
	{
		BinaryOperatorInstruction boi = 
			(BinaryOperatorInstruction)dupstore.objectref;
		
		String newOperator = boi.operator + "=";
		
		return new AssignmentInstruction(
				ByteCodeConstants.ASSIGNMENT, xstorePutfieldPutstatic.offset,
				dupstore.lineNumber, boi.getPriority(), newOperator,
				xstorePutfieldPutstatic, boi.value2);
	}
}
