/*******************************************************************************
 * Copyright (C) 2007-2019 Emmanuel Dupuy GPLv3
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package jd.core.process.analyzer.instruction.bytecode.factory;

import jd.core.model.instruction.bytecode.ByteCodeConstants;

public class InstructionFactoryConstants 
{
	public final static InstructionFactory[] FACTORIES;
	
	static
	{
		FACTORIES   = new InstructionFactory[256];
		
		FACTORIES[ByteCodeConstants.NOP]             = new DummyFactory();
		FACTORIES[ByteCodeConstants.ACONST_NULL]     = new AConstNullFactory();
		FACTORIES[ByteCodeConstants.ICONST_M1]       = new IConstFactory();
		FACTORIES[ByteCodeConstants.ICONST_0]        = FACTORIES[ByteCodeConstants.ICONST_M1];
		FACTORIES[ByteCodeConstants.ICONST_1]        = FACTORIES[ByteCodeConstants.ICONST_M1];
		FACTORIES[ByteCodeConstants.ICONST_2]        = FACTORIES[ByteCodeConstants.ICONST_M1];
		FACTORIES[ByteCodeConstants.ICONST_3]        = FACTORIES[ByteCodeConstants.ICONST_M1];
		FACTORIES[ByteCodeConstants.ICONST_4]        = FACTORIES[ByteCodeConstants.ICONST_M1];
		FACTORIES[ByteCodeConstants.ICONST_5]        = FACTORIES[ByteCodeConstants.ICONST_M1];
		FACTORIES[ByteCodeConstants.LCONST_0]        = new LConstFactory();
		FACTORIES[ByteCodeConstants.LCONST_1]        = FACTORIES[ByteCodeConstants.LCONST_0];
		FACTORIES[ByteCodeConstants.FCONST_0]        = new FConstFactory();
		FACTORIES[ByteCodeConstants.FCONST_1]        = FACTORIES[ByteCodeConstants.FCONST_0];
		FACTORIES[ByteCodeConstants.FCONST_2]        = FACTORIES[ByteCodeConstants.FCONST_0];
		FACTORIES[ByteCodeConstants.DCONST_0]        = new DConstFactory();
		FACTORIES[ByteCodeConstants.DCONST_1]        = FACTORIES[ByteCodeConstants.DCONST_0];
		FACTORIES[ByteCodeConstants.BIPUSH]          = new BIPushFactory();
		FACTORIES[ByteCodeConstants.SIPUSH]          = new SIPushFactory();
		FACTORIES[ByteCodeConstants.LDC]             = new LdcFactory();
		FACTORIES[ByteCodeConstants.LDC_W]           = new LdcWFactory();
		FACTORIES[ByteCodeConstants.LDC2_W]          = new Ldc2WFactory();
		FACTORIES[ByteCodeConstants.ILOAD]           = new ILoadFactory();
		FACTORIES[ByteCodeConstants.LLOAD]           = new LLoadFactory();
		FACTORIES[ByteCodeConstants.FLOAD]           = new FLoadFactory();
		FACTORIES[ByteCodeConstants.DLOAD]           = new DLoadFactory();
		FACTORIES[ByteCodeConstants.ALOAD]           = new ALoadFactory();
		FACTORIES[ByteCodeConstants.ILOAD_0]         = FACTORIES[ByteCodeConstants.ILOAD];
		FACTORIES[ByteCodeConstants.ILOAD_1]         = FACTORIES[ByteCodeConstants.ILOAD];
		FACTORIES[ByteCodeConstants.ILOAD_2]         = FACTORIES[ByteCodeConstants.ILOAD];
		FACTORIES[ByteCodeConstants.ILOAD_3]         = FACTORIES[ByteCodeConstants.ILOAD];
		FACTORIES[ByteCodeConstants.LLOAD_0]         = FACTORIES[ByteCodeConstants.LLOAD];
		FACTORIES[ByteCodeConstants.LLOAD_1]         = FACTORIES[ByteCodeConstants.LLOAD];
		FACTORIES[ByteCodeConstants.LLOAD_2]         = FACTORIES[ByteCodeConstants.LLOAD];
		FACTORIES[ByteCodeConstants.LLOAD_3]         = FACTORIES[ByteCodeConstants.LLOAD];
		FACTORIES[ByteCodeConstants.FLOAD_0]         = FACTORIES[ByteCodeConstants.FLOAD];
		FACTORIES[ByteCodeConstants.FLOAD_1]         = FACTORIES[ByteCodeConstants.FLOAD];
		FACTORIES[ByteCodeConstants.FLOAD_2]         = FACTORIES[ByteCodeConstants.FLOAD];
		FACTORIES[ByteCodeConstants.FLOAD_3]         = FACTORIES[ByteCodeConstants.FLOAD];
		FACTORIES[ByteCodeConstants.DLOAD_0]         = FACTORIES[ByteCodeConstants.DLOAD];
		FACTORIES[ByteCodeConstants.DLOAD_1]         = FACTORIES[ByteCodeConstants.DLOAD];
		FACTORIES[ByteCodeConstants.DLOAD_2]         = FACTORIES[ByteCodeConstants.DLOAD];
		FACTORIES[ByteCodeConstants.DLOAD_3]         = FACTORIES[ByteCodeConstants.DLOAD];
		FACTORIES[ByteCodeConstants.ALOAD_0]         = FACTORIES[ByteCodeConstants.ALOAD];
		FACTORIES[ByteCodeConstants.ALOAD_1]         = FACTORIES[ByteCodeConstants.ALOAD];
		FACTORIES[ByteCodeConstants.ALOAD_2]         = FACTORIES[ByteCodeConstants.ALOAD];
		FACTORIES[ByteCodeConstants.ALOAD_3]         = FACTORIES[ByteCodeConstants.ALOAD];
		FACTORIES[ByteCodeConstants.IALOAD]          = new ArrayLoadInstructionFactory("I");
		FACTORIES[ByteCodeConstants.LALOAD]          = new ArrayLoadInstructionFactory("J");
		FACTORIES[ByteCodeConstants.FALOAD]          = new ArrayLoadInstructionFactory("F");
		FACTORIES[ByteCodeConstants.DALOAD]          = new ArrayLoadInstructionFactory("D");
		FACTORIES[ByteCodeConstants.AALOAD]          = new AALoadFactory();
		FACTORIES[ByteCodeConstants.BALOAD]          = new ArrayLoadInstructionFactory("B");
		FACTORIES[ByteCodeConstants.CALOAD]          = new ArrayLoadInstructionFactory("C");
		FACTORIES[ByteCodeConstants.SALOAD]          = new ArrayLoadInstructionFactory("S");
		FACTORIES[ByteCodeConstants.ISTORE]          = new IStoreFactory();
		FACTORIES[ByteCodeConstants.LSTORE]          = new LStoreFactory();
		FACTORIES[ByteCodeConstants.FSTORE]          = new FStoreFactory();
		FACTORIES[ByteCodeConstants.DSTORE]          = new DStoreFactory();
		FACTORIES[ByteCodeConstants.ASTORE]          = new AStoreFactory();
		FACTORIES[ByteCodeConstants.ISTORE_0]        = FACTORIES[ByteCodeConstants.ISTORE];
		FACTORIES[ByteCodeConstants.ISTORE_1]        = FACTORIES[ByteCodeConstants.ISTORE];
		FACTORIES[ByteCodeConstants.ISTORE_2]        = FACTORIES[ByteCodeConstants.ISTORE];
		FACTORIES[ByteCodeConstants.ISTORE_3]        = FACTORIES[ByteCodeConstants.ISTORE];
		FACTORIES[ByteCodeConstants.LSTORE_0]        = FACTORIES[ByteCodeConstants.LSTORE];
		FACTORIES[ByteCodeConstants.LSTORE_1]        = FACTORIES[ByteCodeConstants.LSTORE];
		FACTORIES[ByteCodeConstants.LSTORE_2]        = FACTORIES[ByteCodeConstants.LSTORE];
		FACTORIES[ByteCodeConstants.LSTORE_3]        = FACTORIES[ByteCodeConstants.LSTORE];
		FACTORIES[ByteCodeConstants.FSTORE_0]        = FACTORIES[ByteCodeConstants.FSTORE];
		FACTORIES[ByteCodeConstants.FSTORE_1]        = FACTORIES[ByteCodeConstants.FSTORE];
		FACTORIES[ByteCodeConstants.FSTORE_2]        = FACTORIES[ByteCodeConstants.FSTORE];
		FACTORIES[ByteCodeConstants.FSTORE_3]        = FACTORIES[ByteCodeConstants.FSTORE];
		FACTORIES[ByteCodeConstants.DSTORE_0]        = FACTORIES[ByteCodeConstants.DSTORE];
		FACTORIES[ByteCodeConstants.DSTORE_1]        = FACTORIES[ByteCodeConstants.DSTORE];
		FACTORIES[ByteCodeConstants.DSTORE_2]        = FACTORIES[ByteCodeConstants.DSTORE];
		FACTORIES[ByteCodeConstants.DSTORE_3]        = FACTORIES[ByteCodeConstants.DSTORE];
		FACTORIES[ByteCodeConstants.ASTORE_0]        = FACTORIES[ByteCodeConstants.ASTORE];
		FACTORIES[ByteCodeConstants.ASTORE_1]        = FACTORIES[ByteCodeConstants.ASTORE];
		FACTORIES[ByteCodeConstants.ASTORE_2]        = FACTORIES[ByteCodeConstants.ASTORE];
		FACTORIES[ByteCodeConstants.ASTORE_3]        = FACTORIES[ByteCodeConstants.ASTORE];
		FACTORIES[ByteCodeConstants.IASTORE]         = new ArrayStoreInstructionFactory("I");
		FACTORIES[ByteCodeConstants.LASTORE]         = new ArrayStoreInstructionFactory("J");
		FACTORIES[ByteCodeConstants.FASTORE]         = new ArrayStoreInstructionFactory("F");
		FACTORIES[ByteCodeConstants.DASTORE]         = new ArrayStoreInstructionFactory("D");
		FACTORIES[ByteCodeConstants.AASTORE]         = new AAStoreFactory();
		FACTORIES[ByteCodeConstants.BASTORE]         = new ArrayStoreInstructionFactory("B");
		FACTORIES[ByteCodeConstants.CASTORE]         = new ArrayStoreInstructionFactory("C");
		FACTORIES[ByteCodeConstants.SASTORE]         = new ArrayStoreInstructionFactory("S");
		FACTORIES[ByteCodeConstants.POP]             = new PopFactory();
		FACTORIES[ByteCodeConstants.POP2]            = new Pop2Factory();
		FACTORIES[ByteCodeConstants.DUP]             = new DupFactory();
		FACTORIES[ByteCodeConstants.DUP_X1]          = new DupX1Factory();
		FACTORIES[ByteCodeConstants.DUP_X2]          = new DupX2Factory();
		FACTORIES[ByteCodeConstants.DUP2]            = new Dup2Factory();
		FACTORIES[ByteCodeConstants.DUP2_X1]         = new Dup2X1Factory();
		FACTORIES[ByteCodeConstants.DUP2_X2]         = new Dup2X2Factory();
		FACTORIES[ByteCodeConstants.SWAP]            = new SwapFactory();
		FACTORIES[ByteCodeConstants.IADD]            = new BinaryOperatorFactory(4, "I", "+");
		FACTORIES[ByteCodeConstants.LADD]            = new BinaryOperatorFactory(4, "J", "+");
		FACTORIES[ByteCodeConstants.FADD]            = new BinaryOperatorFactory(4, "F", "+");
		FACTORIES[ByteCodeConstants.DADD]            = new BinaryOperatorFactory(4, "D", "+");
		FACTORIES[ByteCodeConstants.ISUB]            = new BinaryOperatorFactory(4, "I", "-");
		FACTORIES[ByteCodeConstants.LSUB]            = new BinaryOperatorFactory(4, "J", "-");
		FACTORIES[ByteCodeConstants.FSUB]            = new BinaryOperatorFactory(4, "F", "-");
		FACTORIES[ByteCodeConstants.DSUB]            = new BinaryOperatorFactory(4, "D", "-");
		FACTORIES[ByteCodeConstants.IMUL]            = new BinaryOperatorFactory(3, "I", "*");
		FACTORIES[ByteCodeConstants.LMUL]            = new BinaryOperatorFactory(3, "J", "*");
		FACTORIES[ByteCodeConstants.FMUL]            = new BinaryOperatorFactory(3, "F", "*");
		FACTORIES[ByteCodeConstants.DMUL]            = new BinaryOperatorFactory(3, "D", "*");
		FACTORIES[ByteCodeConstants.IDIV]            = new BinaryOperatorFactory(3, "I", "/");
		FACTORIES[ByteCodeConstants.LDIV]            = new BinaryOperatorFactory(3, "J", "/");
		FACTORIES[ByteCodeConstants.FDIV]            = new BinaryOperatorFactory(3, "F", "/");
		FACTORIES[ByteCodeConstants.DDIV]            = new BinaryOperatorFactory(3, "D", "/");
		FACTORIES[ByteCodeConstants.IREM]            = new BinaryOperatorFactory(3, "I", "%");
		FACTORIES[ByteCodeConstants.LREM]            = new BinaryOperatorFactory(3, "J", "%");
		FACTORIES[ByteCodeConstants.FREM]            = new BinaryOperatorFactory(3, "F", "%");
		FACTORIES[ByteCodeConstants.DREM]            = new BinaryOperatorFactory(3, "D", "%");
		FACTORIES[ByteCodeConstants.INEG]            = new UnaryOperatorFactory(2, "I", "-");
		FACTORIES[ByteCodeConstants.LNEG]            = new UnaryOperatorFactory(2, "J", "-");
		FACTORIES[ByteCodeConstants.FNEG]            = new UnaryOperatorFactory(2, "F", "-");
		FACTORIES[ByteCodeConstants.DNEG]            = new UnaryOperatorFactory(2, "D", "-");
		FACTORIES[ByteCodeConstants.ISHL]            = new BinaryOperatorFactory(5, "I", "<<");
		FACTORIES[ByteCodeConstants.LSHL]            = new BinaryOperatorFactory(5, "J", "<<");
		FACTORIES[ByteCodeConstants.ISHR]            = new BinaryOperatorFactory(5, "I", ">>");
		FACTORIES[ByteCodeConstants.LSHR]            = new BinaryOperatorFactory(5, "J", ">>");
		FACTORIES[ByteCodeConstants.IUSHR]           = new BinaryOperatorFactory(5, "I", ">>>");
		FACTORIES[ByteCodeConstants.LUSHR]           = new BinaryOperatorFactory(5, "J", ">>>");
		FACTORIES[ByteCodeConstants.IAND]            = new IBinaryOperatorFactory(8, "&");
		FACTORIES[ByteCodeConstants.LAND]            = new BinaryOperatorFactory(8, "J", "&");
		FACTORIES[ByteCodeConstants.IOR]             = new IBinaryOperatorFactory(10, "|");
		FACTORIES[ByteCodeConstants.LOR]             = new BinaryOperatorFactory(10, "J", "|");
		FACTORIES[ByteCodeConstants.IXOR]            = new IBinaryOperatorFactory(9, "^");
		FACTORIES[ByteCodeConstants.LXOR]            = new BinaryOperatorFactory(9, "J", "^");
		FACTORIES[ByteCodeConstants.IINC]            = new IIncFactory();
		FACTORIES[ByteCodeConstants.I2L]             = new ImplicitConvertInstructionFactory("J");
		FACTORIES[ByteCodeConstants.I2F]             = new ImplicitConvertInstructionFactory("F");
		FACTORIES[ByteCodeConstants.I2D]             = new ImplicitConvertInstructionFactory("D");
		FACTORIES[ByteCodeConstants.L2I]             = new ConvertInstructionFactory("I");
		FACTORIES[ByteCodeConstants.L2F]             = new ConvertInstructionFactory("F");
		FACTORIES[ByteCodeConstants.L2D]             = FACTORIES[ByteCodeConstants.I2D];
		FACTORIES[ByteCodeConstants.F2I]             = FACTORIES[ByteCodeConstants.L2I];
		FACTORIES[ByteCodeConstants.F2L]             = new ConvertInstructionFactory("J");
		FACTORIES[ByteCodeConstants.F2D]             = FACTORIES[ByteCodeConstants.I2D];
		FACTORIES[ByteCodeConstants.D2I]             = FACTORIES[ByteCodeConstants.L2I];
		FACTORIES[ByteCodeConstants.D2L]             = FACTORIES[ByteCodeConstants.F2L];
		FACTORIES[ByteCodeConstants.D2F]             = FACTORIES[ByteCodeConstants.L2F];
		FACTORIES[ByteCodeConstants.I2B]             = new ConvertInstructionFactory("B");
		FACTORIES[ByteCodeConstants.I2C]             = new ConvertInstructionFactory("C");
		FACTORIES[ByteCodeConstants.I2S]             = new ConvertInstructionFactory("S");
		FACTORIES[ByteCodeConstants.LCMP]            = new CmpFactory(6, "Z", "<"); 
		FACTORIES[ByteCodeConstants.FCMPL]           = FACTORIES[ByteCodeConstants.LCMP]; 
		FACTORIES[ByteCodeConstants.FCMPG]           = FACTORIES[ByteCodeConstants.LCMP];
		FACTORIES[ByteCodeConstants.DCMPL]           = FACTORIES[ByteCodeConstants.LCMP]; 
		FACTORIES[ByteCodeConstants.DCMPG]           = FACTORIES[ByteCodeConstants.LCMP];
		FACTORIES[ByteCodeConstants.IFEQ]            = new IfFactory(ByteCodeConstants.CMP_EQ);
		FACTORIES[ByteCodeConstants.IFNE]            = new IfFactory(ByteCodeConstants.CMP_NE);
		FACTORIES[ByteCodeConstants.IFLT]            = new IfFactory(ByteCodeConstants.CMP_LT);
		FACTORIES[ByteCodeConstants.IFGE]            = new IfFactory(ByteCodeConstants.CMP_GE);
		FACTORIES[ByteCodeConstants.IFGT]            = new IfFactory(ByteCodeConstants.CMP_GT);
		FACTORIES[ByteCodeConstants.IFLE]            = new IfFactory(ByteCodeConstants.CMP_LE);
		FACTORIES[ByteCodeConstants.IF_ICMPEQ]       = new IfCmpFactory(ByteCodeConstants.CMP_EQ);
		FACTORIES[ByteCodeConstants.IF_ICMPNE]       = new IfCmpFactory(ByteCodeConstants.CMP_NE);
		FACTORIES[ByteCodeConstants.IF_ICMPLT]       = new IfCmpFactory(ByteCodeConstants.CMP_LT);
		FACTORIES[ByteCodeConstants.IF_ICMPGE]       = new IfCmpFactory(ByteCodeConstants.CMP_GE);
		FACTORIES[ByteCodeConstants.IF_ICMPGT]       = new IfCmpFactory(ByteCodeConstants.CMP_GT);
		FACTORIES[ByteCodeConstants.IF_ICMPLE]       = new IfCmpFactory(ByteCodeConstants.CMP_LE);
		FACTORIES[ByteCodeConstants.IF_ACMPEQ]       = FACTORIES[ByteCodeConstants.IF_ICMPEQ];
		FACTORIES[ByteCodeConstants.IF_ACMPNE]       = FACTORIES[ByteCodeConstants.IF_ICMPNE];
		FACTORIES[ByteCodeConstants.GOTO]            = new GotoFactory();
		FACTORIES[ByteCodeConstants.JSR]             = new JsrFactory();
		FACTORIES[ByteCodeConstants.RET]             = new RetFactory();
		FACTORIES[ByteCodeConstants.TABLESWITCH]     = new TableSwitchFactory();
		FACTORIES[ByteCodeConstants.LOOKUPSWITCH]    = new LookupSwitchFactory();
		FACTORIES[ByteCodeConstants.IRETURN]         = new ReturnInstructionFactory();
		FACTORIES[ByteCodeConstants.LRETURN]         = FACTORIES[ByteCodeConstants.IRETURN];
		FACTORIES[ByteCodeConstants.FRETURN]         = FACTORIES[ByteCodeConstants.IRETURN];
		FACTORIES[ByteCodeConstants.DRETURN]         = FACTORIES[ByteCodeConstants.IRETURN];
		FACTORIES[ByteCodeConstants.ARETURN]         = FACTORIES[ByteCodeConstants.IRETURN];
		FACTORIES[ByteCodeConstants.RETURN]          = new ReturnFactory();
		FACTORIES[ByteCodeConstants.GETSTATIC]       = new GetStaticFactory();
		FACTORIES[ByteCodeConstants.PUTSTATIC]       = new PutStaticFactory();
		FACTORIES[ByteCodeConstants.GETFIELD]        = new GetFieldFactory();
		FACTORIES[ByteCodeConstants.PUTFIELD]        = new PutFieldFactory();	
		FACTORIES[ByteCodeConstants.INVOKEVIRTUAL]   = new InvokevirtualFactory();
		FACTORIES[ByteCodeConstants.INVOKESPECIAL]   = new InvokespecialFactory();
		FACTORIES[ByteCodeConstants.INVOKESTATIC ]   = new InvokestaticFactory();
		FACTORIES[ByteCodeConstants.INVOKEINTERFACE] = new InvokeinterfaceFactory();
		FACTORIES[ByteCodeConstants.NEW]             = new NewFactory();
		FACTORIES[ByteCodeConstants.NEWARRAY]        = new NewArrayFactory();
		FACTORIES[ByteCodeConstants.ANEWARRAY]       = new ANewArrayFactory();
		FACTORIES[ByteCodeConstants.ARRAYLENGTH]     = new ArrayLengthFactory();
		FACTORIES[ByteCodeConstants.ATHROW]          = new AThrowFactory();
		FACTORIES[ByteCodeConstants.CHECKCAST]       = new CheckCastFactory();
		FACTORIES[ByteCodeConstants.INSTANCEOF]      = new InstanceOfFactory();
		FACTORIES[ByteCodeConstants.MONITORENTER]    = new MonitorEnterFactory();
		FACTORIES[ByteCodeConstants.MONITOREXIT]     = new MonitorExitFactory();
		FACTORIES[ByteCodeConstants.WIDE]            = new WideFactory();
		FACTORIES[ByteCodeConstants.MULTIANEWARRAY]  = new MultiANewArrayFactory();
		FACTORIES[ByteCodeConstants.IFNULL]          = new IfXNullFactory(ByteCodeConstants.CMP_EQ);
		FACTORIES[ByteCodeConstants.IFNONNULL]       = new IfXNullFactory(ByteCodeConstants.CMP_NE);
		FACTORIES[ByteCodeConstants.GOTO_W]          = new GotoWFactory();
		FACTORIES[ByteCodeConstants.JSR_W]           = new JsrWFactory();
		
		/**
		 * Non-legal opcodes, may be used by JVM internally.
		 */
//		public static final int BREAKPOINT                    = 202;
//		public static final int LDC_QUICK                     = 203;
//		public static final int LDC_W_QUICK                   = 204;
//		public static final int LDC2_W_QUICK                  = 205;
//		public static final int GETFIELD_QUICK                = 206;
//		public static final int PUTFIELD_QUICK                = 207;
//		public static final int GETFIELD2_QUICK               = 208;
//		public static final int PUTFIELD2_QUICK               = 209;
//		public static final int GETSTATIC_QUICK               = 210;
//		public static final int PUTSTATIC_QUICK               = 211;
//		public static final int GETSTATIC2_QUICK              = 212;
//		public static final int PUTSTATIC2_QUICK              = 213;
//		public static final int INVOKEVIRTUAL_QUICK           = 214;
//		public static final int INVOKENONVIRTUAL_QUICK        = 215;
//		public static final int INVOKESUPER_QUICK             = 216;
//		public static final int INVOKESTATIC_QUICK            = 217;
//		public static final int INVOKEINTERFACE_QUICK         = 218;
//		public static final int INVOKEVIRTUALOBJECT_QUICK     = 219;
//		public static final int NEW_QUICK                     = 221;
//		public static final int ANEWARRAY_QUICK               = 222;
//		public static final int MULTIANEWARRAY_QUICK          = 223;
//		public static final int CHECKCAST_QUICK               = 224;
//		public static final int INSTANCEOF_QUICK              = 225;
//		public static final int INVOKEVIRTUAL_QUICK_W         = 226;
//		public static final int GETFIELD_QUICK_W              = 227;
//		public static final int PUTFIELD_QUICK_W              = 228;
//		public static final int IMPDEP1                       = 254;
//		public static final int IMPDEP2                       = 255;
	}
}
