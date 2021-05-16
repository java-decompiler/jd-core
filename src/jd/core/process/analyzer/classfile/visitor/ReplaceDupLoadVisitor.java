package jd.core.process.analyzer.classfile.visitor;

import java.util.List;

import jd.core.model.instruction.bytecode.ByteCodeConstants;
import jd.core.model.instruction.bytecode.instruction.ANewArray;
import jd.core.model.instruction.bytecode.instruction.AThrow;
import jd.core.model.instruction.bytecode.instruction.ArrayLength;
import jd.core.model.instruction.bytecode.instruction.ArrayLoadInstruction;
import jd.core.model.instruction.bytecode.instruction.ArrayStoreInstruction;
import jd.core.model.instruction.bytecode.instruction.AssignmentInstruction;
import jd.core.model.instruction.bytecode.instruction.BinaryOperatorInstruction;
import jd.core.model.instruction.bytecode.instruction.CheckCast;
import jd.core.model.instruction.bytecode.instruction.ComplexConditionalBranchInstruction;
import jd.core.model.instruction.bytecode.instruction.ConvertInstruction;
import jd.core.model.instruction.bytecode.instruction.DupLoad;
import jd.core.model.instruction.bytecode.instruction.DupStore;
import jd.core.model.instruction.bytecode.instruction.GetField;
import jd.core.model.instruction.bytecode.instruction.IfCmp;
import jd.core.model.instruction.bytecode.instruction.IfInstruction;
import jd.core.model.instruction.bytecode.instruction.IncInstruction;
import jd.core.model.instruction.bytecode.instruction.InitArrayInstruction;
import jd.core.model.instruction.bytecode.instruction.InstanceOf;
import jd.core.model.instruction.bytecode.instruction.Instruction;
import jd.core.model.instruction.bytecode.instruction.InvokeInstruction;
import jd.core.model.instruction.bytecode.instruction.InvokeNoStaticInstruction;
import jd.core.model.instruction.bytecode.instruction.LookupSwitch;
import jd.core.model.instruction.bytecode.instruction.MonitorEnter;
import jd.core.model.instruction.bytecode.instruction.MonitorExit;
import jd.core.model.instruction.bytecode.instruction.MultiANewArray;
import jd.core.model.instruction.bytecode.instruction.NewArray;
import jd.core.model.instruction.bytecode.instruction.Pop;
import jd.core.model.instruction.bytecode.instruction.PutField;
import jd.core.model.instruction.bytecode.instruction.PutStatic;
import jd.core.model.instruction.bytecode.instruction.ReturnInstruction;
import jd.core.model.instruction.bytecode.instruction.StoreInstruction;
import jd.core.model.instruction.bytecode.instruction.TableSwitch;
import jd.core.model.instruction.bytecode.instruction.TernaryOpStore;
import jd.core.model.instruction.bytecode.instruction.TernaryOperator;
import jd.core.model.instruction.bytecode.instruction.UnaryOperatorInstruction;
import jd.core.model.instruction.fast.FastConstants;
import jd.core.model.instruction.fast.instruction.FastDeclaration;
import jd.core.model.instruction.fast.instruction.FastFor;
import jd.core.model.instruction.fast.instruction.FastForEach;
import jd.core.model.instruction.fast.instruction.FastInstruction;
import jd.core.model.instruction.fast.instruction.FastLabel;
import jd.core.model.instruction.fast.instruction.FastList;
import jd.core.model.instruction.fast.instruction.FastSwitch;
import jd.core.model.instruction.fast.instruction.FastSynchronized;
import jd.core.model.instruction.fast.instruction.FastTest2Lists;
import jd.core.model.instruction.fast.instruction.FastTestList;
import jd.core.model.instruction.fast.instruction.FastTry;
import jd.core.model.instruction.fast.instruction.FastTry.FastCatch;


public class ReplaceDupLoadVisitor 
{
	private DupStore dupStore;
	private Instruction newInstruction;
	private Instruction parentFound;

	public ReplaceDupLoadVisitor()
	{
		this.dupStore = null;
		this.newInstruction = null;
		this.parentFound = null;
	}
	
	public ReplaceDupLoadVisitor(DupStore dupStore, Instruction newInstruction)
	{
		init(dupStore, newInstruction);
	}
	
	public void init(DupStore dupStore, Instruction newInstruction)
	{
		this.dupStore = dupStore;
		this.newInstruction = newInstruction;
		this.parentFound = null;
	}
	
	public void visit(Instruction instruction)
	{
		switch (instruction.opcode)
		{
		case ByteCodeConstants.ARRAYLENGTH:
			{
				ArrayLength al = (ArrayLength)instruction;
				if (match(al, al.arrayref))
					al.arrayref = this.newInstruction;
				else
					visit(al.arrayref);
			}
			break;
		case ByteCodeConstants.AASTORE:
		case ByteCodeConstants.ARRAYSTORE:
			{
				ArrayStoreInstruction asi = (ArrayStoreInstruction)instruction;
				if (match(asi, asi.arrayref))
				{
					asi.arrayref = this.newInstruction;
				}
				else
				{
					visit(asi.arrayref);
					
					if (this.parentFound == null)
					{
						if (match(asi, asi.indexref))
						{
							asi.indexref = this.newInstruction;
						}
						else
						{
							visit(asi.indexref);
							
							if (this.parentFound == null)
							{
								if (match(asi, asi.valueref))
									asi.valueref = this.newInstruction;
								else
									visit(asi.valueref);
							}
						}
					}
				}
			}
			break;
		case ByteCodeConstants.ATHROW:
			{
				AThrow aThrow = (AThrow)instruction;
				if (match(aThrow, aThrow.value))
					aThrow.value = this.newInstruction;
				else
					visit(aThrow.value);
			}
			break;
		case ByteCodeConstants.UNARYOP:
			{
				UnaryOperatorInstruction uoi = (UnaryOperatorInstruction)instruction;
				if (match(uoi, uoi.value))
					uoi.value = this.newInstruction;
				else
					visit(uoi.value);
			}
			break;
		case ByteCodeConstants.BINARYOP:
			{
				BinaryOperatorInstruction boi = (BinaryOperatorInstruction)instruction;
				if (match(boi, boi.value1))
				{
					boi.value1 = this.newInstruction;
				}
				else
				{
					visit(boi.value1);
				
					if (this.parentFound == null)
					{
						if (match(boi, boi.value2))
							boi.value2 = this.newInstruction;
						else
							visit(boi.value2);
					}
				}
			}
			break;
		case ByteCodeConstants.CHECKCAST:
			{
				CheckCast checkCast = (CheckCast)instruction;
				if (match(checkCast, checkCast.objectref))
					checkCast.objectref = this.newInstruction;
				else
					visit(checkCast.objectref);
			}
			break;
		case ByteCodeConstants.STORE:
		case ByteCodeConstants.ASTORE:
		case ByteCodeConstants.ISTORE:
			{
				StoreInstruction storeInstruction = (StoreInstruction)instruction;
				if (match(storeInstruction, storeInstruction.valueref))
					storeInstruction.valueref = this.newInstruction;
				else
					visit(storeInstruction.valueref);
			}
			break;
		case ByteCodeConstants.DUPSTORE:
			{
				DupStore dupStore = (DupStore)instruction;
				if (match(dupStore, dupStore.objectref))
					dupStore.objectref = this.newInstruction;
				else
					visit(dupStore.objectref);
			}
			break;
		case ByteCodeConstants.CONVERT:
		case ByteCodeConstants.IMPLICITCONVERT:
			{
				ConvertInstruction ci = (ConvertInstruction)instruction;
				if (match(ci, ci.value))
					ci.value = this.newInstruction;
				else
					visit(ci.value);
			}
			break;
		case ByteCodeConstants.IFCMP:
			{
				IfCmp ifCmp = (IfCmp)instruction;
				if (match(ifCmp, ifCmp.value1))
				{
					ifCmp.value1 = this.newInstruction;
				}
				else
				{
					visit(ifCmp.value1);
					
					if (this.parentFound == null)
					{
						if (match(ifCmp, ifCmp.value2))
							ifCmp.value2 = this.newInstruction;
						else
							visit(ifCmp.value2);
					}
				}
			}
			break;
		case ByteCodeConstants.IF:
		case ByteCodeConstants.IFXNULL:
			{
				IfInstruction iff = (IfInstruction)instruction;
				if (match(iff, iff.value))
					iff.value = this.newInstruction;
				else
					visit(iff.value);
			}
			break;			
		case ByteCodeConstants.COMPLEXIF:
			{
				List<Instruction> branchList = 
					((ComplexConditionalBranchInstruction)instruction).instructions;
				for (int i=branchList.size()-1; (i>=0) && (this.parentFound == null); --i)
				{
					visit(branchList.get(i));
				}
			}
			break;
		case ByteCodeConstants.INSTANCEOF:
			{
				InstanceOf instanceOf = (InstanceOf)instruction;
				if (match(instanceOf, instanceOf.objectref))
					instanceOf.objectref = this.newInstruction;
				else
					visit(instanceOf.objectref);
			}
			break;
		case ByteCodeConstants.INVOKEINTERFACE:
		case ByteCodeConstants.INVOKESPECIAL:
		case ByteCodeConstants.INVOKEVIRTUAL:
			{
				InvokeNoStaticInstruction insi = 
					(InvokeNoStaticInstruction)instruction;
				if (match(insi, insi.objectref))
					insi.objectref = this.newInstruction;
				else
					visit(insi.objectref);
			}
		case ByteCodeConstants.INVOKESTATIC:
		case ByteCodeConstants.INVOKENEW:
			{
				List<Instruction> list = ((InvokeInstruction)instruction).args;
				for (int i=list.size()-1; (i>=0) && (this.parentFound == null); --i)
				{
					if (match(instruction, list.get(i)))
						list.set(i, this.newInstruction);
					else
						visit(list.get(i));
				}
			}
			break;
		case ByteCodeConstants.LOOKUPSWITCH:
			{
				LookupSwitch ls = (LookupSwitch)instruction;
				if (match(ls, ls.key))
					ls.key = this.newInstruction;
				else
					visit(ls.key);
			}
			break;			
		case ByteCodeConstants.MONITORENTER:
			{
				MonitorEnter monitorEnter = (MonitorEnter)instruction;
				if (match(monitorEnter, monitorEnter.objectref))
					monitorEnter.objectref = this.newInstruction;
				else
					visit(monitorEnter.objectref);
			}
			break;
		case ByteCodeConstants.MONITOREXIT:
			{
				MonitorExit monitorExit = (MonitorExit)instruction;
				if (match(monitorExit, monitorExit.objectref))
					monitorExit.objectref = this.newInstruction;
				else
					visit(monitorExit.objectref);
			}
			break;
		case ByteCodeConstants.MULTIANEWARRAY:
			{
				Instruction[] dimensions = ((MultiANewArray)instruction).dimensions;
				for (int i=dimensions.length-1; (i>=0) && (this.parentFound == null); --i)
				{
					if (match(instruction, dimensions[i]))
						dimensions[i] = this.newInstruction;
					else
						visit(dimensions[i]);
				}
			}
			break;
		case ByteCodeConstants.NEWARRAY:
			{
				NewArray newArray = (NewArray)instruction;
				if (match(newArray, newArray.dimension))
					newArray.dimension = this.newInstruction;
				else
					visit(newArray.dimension);
			}
			break;
		case ByteCodeConstants.ANEWARRAY:
			{
				ANewArray aNewArray = (ANewArray)instruction;
				if (match(aNewArray, aNewArray.dimension))
					aNewArray.dimension = this.newInstruction;
				else
					visit(aNewArray.dimension);
			}
			break;
		case ByteCodeConstants.POP:
			{
				Pop pop = (Pop)instruction;
				if (match(pop, pop.objectref))
					pop.objectref = this.newInstruction;
				else
					visit(pop.objectref);
			}
			break;
		case ByteCodeConstants.PUTFIELD:
			{
				PutField putField = (PutField)instruction;
				if (match(putField, putField.objectref))
				{
					putField.objectref = this.newInstruction;
				}
				else
				{
					visit(putField.objectref);
					
					if (this.parentFound == null)
					{
						if (match(putField, putField.valueref))
							putField.valueref = this.newInstruction;
						else
							visit(putField.valueref);
					}
				}
			}
			break;
		case ByteCodeConstants.PUTSTATIC:
			{
				PutStatic putStatic = (PutStatic)instruction;
				if (match(putStatic, putStatic.valueref))
					putStatic.valueref = this.newInstruction;
				else
					visit(putStatic.valueref);
			}
			break;
		case ByteCodeConstants.XRETURN:
			{
				ReturnInstruction ri = (ReturnInstruction)instruction;
				if (match(ri, ri.valueref))
					ri.valueref = this.newInstruction;
				else
					visit(ri.valueref);
			}
			break;			
		case ByteCodeConstants.TABLESWITCH:
			{
				TableSwitch ts = (TableSwitch)instruction;
				if (match(ts, ts.key))
					ts.key = this.newInstruction;
				else
					visit(ts.key);
			}
			break;			
		case ByteCodeConstants.TERNARYOPSTORE:
			{
				TernaryOpStore tos = (TernaryOpStore)instruction;
				if (match(tos, tos.objectref))
					tos.objectref = this.newInstruction;
				else
					visit(tos.objectref);	
			}
			break;		
		case ByteCodeConstants.TERNARYOP:	
			{
				TernaryOperator to = (TernaryOperator)instruction;
				if (match(to, to.value1))
				{
					to.value1 = this.newInstruction;
				}
				else
				{
					visit(to.value1);
	
					if (this.parentFound == null)
					{
						if (match(to, to.value2))
							to.value2 = this.newInstruction;
						else
							visit(to.value2);
					}
				}
			}
			break;	
		case ByteCodeConstants.ASSIGNMENT:
			{
				AssignmentInstruction ai = (AssignmentInstruction)instruction;
				if (match(ai, ai.value1))
				{
					ai.value1 = this.newInstruction;
				}
				else
				{
					visit(ai.value1);
	
					if (this.parentFound == null)
					{
						if (match(ai, ai.value2))
							ai.value2 = this.newInstruction;
						else
							visit(ai.value2);
					}
				}
			}
			break;
		case ByteCodeConstants.ARRAYLOAD:
			{
				ArrayLoadInstruction ali = (ArrayLoadInstruction)instruction;
				if (match(ali, ali.arrayref))
				{
					ali.arrayref = this.newInstruction;
				}
				else
				{
					visit(ali.arrayref);
	
					if (this.parentFound == null)
					{
						if (match(ali, ali.indexref))
							ali.indexref = this.newInstruction;
						else
							visit(ali.indexref);
					}
				}
			}
			break;
		case ByteCodeConstants.PREINC:			
		case ByteCodeConstants.POSTINC:		
			{
				IncInstruction ii = (IncInstruction)instruction;
				if (match(ii, ii.value))
					ii.value = this.newInstruction;
				else
					visit(ii.value);
			}
			break;
		case ByteCodeConstants.GETFIELD:
			{
				GetField gf = (GetField)instruction;
				if (match(gf, gf.objectref))
					gf.objectref = this.newInstruction;
				else
					visit(gf.objectref);
			}
			break;
		case ByteCodeConstants.INITARRAY:
		case ByteCodeConstants.NEWANDINITARRAY:
			{
				InitArrayInstruction iai = (InitArrayInstruction)instruction;
				if (match(iai, iai.newArray))
				{
					iai.newArray = this.newInstruction;
				}
				else
				{
					visit(iai.newArray);
					
					if ((this.parentFound == null) && (iai.values != null))
						visit(iai.values);
				}
			}
			break;
		case FastConstants.FOR:
			{
				FastFor ff = (FastFor)instruction;
				
				if (ff.init != null)
				{
					if (match(ff, ff.init))
						ff.init = this.newInstruction;
					else
						visit(ff.init);
				}
				
				if ((this.parentFound == null) && (ff.inc != null))
				{
					if (match(ff, ff.inc))
						ff.inc = this.newInstruction;
					else
						visit(ff.inc);
				}
			}
		case FastConstants.WHILE:
		case FastConstants.DO_WHILE:
		case FastConstants.IF_:
			{
				FastTestList ftl = (FastTestList)instruction;
				if (ftl.test != null)
				{
					if (match(ftl, ftl.test))
						ftl.test = this.newInstruction;
					else
						visit(ftl.test);
				}
			}
		case FastConstants.INFINITE_LOOP:
			{
				List<Instruction> instructions = 
						((FastList)instruction).instructions;
				if (instructions != null)
					visit(instructions);
			}
			break;
		case FastConstants.FOREACH:
			{
				FastForEach ffe = (FastForEach)instruction;
				if (match(ffe, ffe.variable))
				{
					ffe.variable = this.newInstruction;
				}
				else
				{
					visit(ffe.variable);
					
					if (this.parentFound == null)
					{
						if (match(ffe, ffe.values))
						{
							ffe.values = this.newInstruction;
						}
						else
						{
							visit(ffe.values);
		
							if (this.parentFound == null)
								visit(ffe.instructions);
						}
					}
				}
			}
			break;
		case FastConstants.IF_ELSE:
			{
				FastTest2Lists ft2l = (FastTest2Lists)instruction;
				if (match(ft2l, ft2l.test))
				{
					ft2l.test = this.newInstruction;
				}
				else
				{
					visit(ft2l.test);
					
					if (this.parentFound == null)
					{
						visit(ft2l.instructions);
						
						if (this.parentFound == null)
							visit(ft2l.instructions2);
					}
				}
			}
			break;
		case FastConstants.IF_CONTINUE:
		case FastConstants.IF_BREAK:
		case FastConstants.IF_LABELED_BREAK:
		case FastConstants.GOTO_CONTINUE:
		case FastConstants.GOTO_BREAK:
		case FastConstants.GOTO_LABELED_BREAK:
			{
				FastInstruction fi = (FastInstruction)instruction;
				if (fi.instruction != null)
				{
					if (match(fi, fi.instruction))
						fi.instruction = this.newInstruction;
					else
						visit(fi.instruction);
				}
			}
			break;
		case FastConstants.SWITCH:
		case FastConstants.SWITCH_ENUM:
		case FastConstants.SWITCH_STRING:
			{
				FastSwitch fs = (FastSwitch)instruction;
				if (match(fs, fs.test))
				{
					fs.test = this.newInstruction;
				}
				else
				{
					visit(fs.test);
						
					FastSwitch.Pair[] pairs = fs.pairs;
					for (int i=pairs.length-1; (i>=0) && (this.parentFound == null); --i)
						visit(pairs[i].getInstructions());
				}
			}
			break;
		case FastConstants.TRY:
			{
				FastTry ft = (FastTry)instruction;
				visit(ft.instructions);	
				
				if (this.parentFound == null)
				{
					if (ft.finallyInstructions != null)
						visit(ft.finallyInstructions);	
				
					List<FastCatch> catchs = ft.catches;
					for (int i=catchs.size()-1; (i>=0) && (this.parentFound == null); --i)
						visit(catchs.get(i).instructions);
				}
			}
			break;
		case FastConstants.SYNCHRONIZED:
			{
				FastSynchronized fsd = (FastSynchronized)instruction;
				if (match(fsd, fsd.monitor))
				{
					fsd.monitor = this.newInstruction;
				}
				else
				{
					visit(fsd.monitor);
					
					if (this.parentFound == null)
						visit(fsd.instructions);
				}
			}
			break;
		case FastConstants.LABEL:
			{
				FastLabel fl = (FastLabel)instruction;
				if (fl.instruction != null)
				{
					if (match(fl, fl.instruction))
						fl.instruction = this.newInstruction;
					else
						visit(fl.instruction);
				}
			}
			break;
		case FastConstants.DECLARE:
			{
				FastDeclaration fd = (FastDeclaration)instruction;
				if (fd.instruction != null)
				{
					if (match(fd, fd.instruction))
						fd.instruction = this.newInstruction;
					else
						visit(fd.instruction);
				}
			}
			break;
		case ByteCodeConstants.ACONST_NULL:
		case ByteCodeConstants.LOAD:
		case ByteCodeConstants.ALOAD:
		case ByteCodeConstants.ILOAD:
		case ByteCodeConstants.BIPUSH:
		case ByteCodeConstants.ICONST:
		case ByteCodeConstants.LCONST:
		case ByteCodeConstants.FCONST:
		case ByteCodeConstants.DCONST:
		case ByteCodeConstants.DUPLOAD:
		case ByteCodeConstants.GETSTATIC:
		case ByteCodeConstants.OUTERTHIS:
		case ByteCodeConstants.GOTO:
		case ByteCodeConstants.IINC:			
		case ByteCodeConstants.JSR:			
		case ByteCodeConstants.LDC:
		case ByteCodeConstants.LDC2_W:
		case ByteCodeConstants.NEW:
		case ByteCodeConstants.NOP:
		case ByteCodeConstants.SIPUSH:
		case ByteCodeConstants.RET:
		case ByteCodeConstants.RETURN:
		case ByteCodeConstants.EXCEPTIONLOAD:
		case ByteCodeConstants.RETURNADDRESSLOAD:
			break;
		default:
			System.err.println(
					"Can not replace DupLoad in " + 
					instruction.getClass().getName() + 
					", opcode=" + instruction.opcode);
		}
	}
	
	private void visit(List<Instruction> instructions)
	{
		for (int i=instructions.size()-1; i>=0; --i)
			visit(instructions.get(i));
	}
	
	/**
	 * @return le dernier parent sur lequel une substitution a été faite
	 */
	public Instruction getParentFound() 
	{
		return this.parentFound;
	}

	private boolean match(Instruction parent, Instruction i)
	{
		if (i.opcode != ByteCodeConstants.DUPLOAD)
			return false;
		
		DupLoad dupload = (DupLoad)i;
		
		if (dupload.dupStore == this.dupStore)
		{
			this.parentFound = parent;
			return true;
		}
		
		return false;
	}
}
