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

import org.apache.bcel.Const;

import jd.core.model.instruction.bytecode.ByteCodeConstants;

public final class InstructionFactoryConstants
{
    private InstructionFactoryConstants() {
        super();
    }

    private static final InstructionFactory[] FACTORIES;

    public static InstructionFactory getInstructionFactory(int opcode) {
        return FACTORIES[opcode];
    }

    static
    {
        FACTORIES   = new InstructionFactory[256];

        FACTORIES[Const.NOP]             = new DummyFactory();
        FACTORIES[Const.ACONST_NULL]     = new AConstNullFactory();
        FACTORIES[Const.ICONST_M1]       = new IConstFactory();
        FACTORIES[Const.ICONST_0]        = FACTORIES[Const.ICONST_M1];
        FACTORIES[Const.ICONST_1]        = FACTORIES[Const.ICONST_M1];
        FACTORIES[Const.ICONST_2]        = FACTORIES[Const.ICONST_M1];
        FACTORIES[Const.ICONST_3]        = FACTORIES[Const.ICONST_M1];
        FACTORIES[Const.ICONST_4]        = FACTORIES[Const.ICONST_M1];
        FACTORIES[Const.ICONST_5]        = FACTORIES[Const.ICONST_M1];
        FACTORIES[Const.LCONST_0]        = new LConstFactory();
        FACTORIES[Const.LCONST_1]        = FACTORIES[Const.LCONST_0];
        FACTORIES[Const.FCONST_0]        = new FConstFactory();
        FACTORIES[Const.FCONST_1]        = FACTORIES[Const.FCONST_0];
        FACTORIES[Const.FCONST_2]        = FACTORIES[Const.FCONST_0];
        FACTORIES[Const.DCONST_0]        = new DConstFactory();
        FACTORIES[Const.DCONST_1]        = FACTORIES[Const.DCONST_0];
        FACTORIES[Const.BIPUSH]          = new BIPushFactory();
        FACTORIES[Const.SIPUSH]          = new SIPushFactory();
        FACTORIES[Const.LDC]             = new LdcFactory();
        FACTORIES[Const.LDC_W]           = new LdcWFactory();
        FACTORIES[Const.LDC2_W]          = new Ldc2WFactory();
        FACTORIES[Const.ILOAD]           = new ILoadFactory();
        FACTORIES[Const.LLOAD]           = new LLoadFactory();
        FACTORIES[Const.FLOAD]           = new FLoadFactory();
        FACTORIES[Const.DLOAD]           = new DLoadFactory();
        FACTORIES[Const.ALOAD]           = new ALoadFactory();
        FACTORIES[Const.ILOAD_0]         = FACTORIES[Const.ILOAD];
        FACTORIES[Const.ILOAD_1]         = FACTORIES[Const.ILOAD];
        FACTORIES[Const.ILOAD_2]         = FACTORIES[Const.ILOAD];
        FACTORIES[Const.ILOAD_3]         = FACTORIES[Const.ILOAD];
        FACTORIES[Const.LLOAD_0]         = FACTORIES[Const.LLOAD];
        FACTORIES[Const.LLOAD_1]         = FACTORIES[Const.LLOAD];
        FACTORIES[Const.LLOAD_2]         = FACTORIES[Const.LLOAD];
        FACTORIES[Const.LLOAD_3]         = FACTORIES[Const.LLOAD];
        FACTORIES[Const.FLOAD_0]         = FACTORIES[Const.FLOAD];
        FACTORIES[Const.FLOAD_1]         = FACTORIES[Const.FLOAD];
        FACTORIES[Const.FLOAD_2]         = FACTORIES[Const.FLOAD];
        FACTORIES[Const.FLOAD_3]         = FACTORIES[Const.FLOAD];
        FACTORIES[Const.DLOAD_0]         = FACTORIES[Const.DLOAD];
        FACTORIES[Const.DLOAD_1]         = FACTORIES[Const.DLOAD];
        FACTORIES[Const.DLOAD_2]         = FACTORIES[Const.DLOAD];
        FACTORIES[Const.DLOAD_3]         = FACTORIES[Const.DLOAD];
        FACTORIES[Const.ALOAD_0]         = FACTORIES[Const.ALOAD];
        FACTORIES[Const.ALOAD_1]         = FACTORIES[Const.ALOAD];
        FACTORIES[Const.ALOAD_2]         = FACTORIES[Const.ALOAD];
        FACTORIES[Const.ALOAD_3]         = FACTORIES[Const.ALOAD];
        FACTORIES[Const.IALOAD]          = new ArrayLoadInstructionFactory("I");
        FACTORIES[Const.LALOAD]          = new ArrayLoadInstructionFactory("J");
        FACTORIES[Const.FALOAD]          = new ArrayLoadInstructionFactory("F");
        FACTORIES[Const.DALOAD]          = new ArrayLoadInstructionFactory("D");
        FACTORIES[Const.AALOAD]          = new AALoadFactory();
        FACTORIES[Const.BALOAD]          = new ArrayLoadInstructionFactory("B");
        FACTORIES[Const.CALOAD]          = new ArrayLoadInstructionFactory("C");
        FACTORIES[Const.SALOAD]          = new ArrayLoadInstructionFactory("S");
        FACTORIES[Const.ISTORE]          = new IStoreFactory();
        FACTORIES[Const.LSTORE]          = new LStoreFactory();
        FACTORIES[Const.FSTORE]          = new FStoreFactory();
        FACTORIES[Const.DSTORE]          = new DStoreFactory();
        FACTORIES[Const.ASTORE]          = new AStoreFactory();
        FACTORIES[Const.ISTORE_0]        = FACTORIES[Const.ISTORE];
        FACTORIES[Const.ISTORE_1]        = FACTORIES[Const.ISTORE];
        FACTORIES[Const.ISTORE_2]        = FACTORIES[Const.ISTORE];
        FACTORIES[Const.ISTORE_3]        = FACTORIES[Const.ISTORE];
        FACTORIES[Const.LSTORE_0]        = FACTORIES[Const.LSTORE];
        FACTORIES[Const.LSTORE_1]        = FACTORIES[Const.LSTORE];
        FACTORIES[Const.LSTORE_2]        = FACTORIES[Const.LSTORE];
        FACTORIES[Const.LSTORE_3]        = FACTORIES[Const.LSTORE];
        FACTORIES[Const.FSTORE_0]        = FACTORIES[Const.FSTORE];
        FACTORIES[Const.FSTORE_1]        = FACTORIES[Const.FSTORE];
        FACTORIES[Const.FSTORE_2]        = FACTORIES[Const.FSTORE];
        FACTORIES[Const.FSTORE_3]        = FACTORIES[Const.FSTORE];
        FACTORIES[Const.DSTORE_0]        = FACTORIES[Const.DSTORE];
        FACTORIES[Const.DSTORE_1]        = FACTORIES[Const.DSTORE];
        FACTORIES[Const.DSTORE_2]        = FACTORIES[Const.DSTORE];
        FACTORIES[Const.DSTORE_3]        = FACTORIES[Const.DSTORE];
        FACTORIES[Const.ASTORE_0]        = FACTORIES[Const.ASTORE];
        FACTORIES[Const.ASTORE_1]        = FACTORIES[Const.ASTORE];
        FACTORIES[Const.ASTORE_2]        = FACTORIES[Const.ASTORE];
        FACTORIES[Const.ASTORE_3]        = FACTORIES[Const.ASTORE];
        FACTORIES[Const.IASTORE]         = new ArrayStoreInstructionFactory("I");
        FACTORIES[Const.LASTORE]         = new ArrayStoreInstructionFactory("J");
        FACTORIES[Const.FASTORE]         = new ArrayStoreInstructionFactory("F");
        FACTORIES[Const.DASTORE]         = new ArrayStoreInstructionFactory("D");
        FACTORIES[Const.AASTORE]         = new AAStoreFactory();
        FACTORIES[Const.BASTORE]         = new ArrayStoreInstructionFactory("B");
        FACTORIES[Const.CASTORE]         = new ArrayStoreInstructionFactory("C");
        FACTORIES[Const.SASTORE]         = new ArrayStoreInstructionFactory("S");
        FACTORIES[Const.POP]             = new PopFactory();
        FACTORIES[Const.POP2]            = new Pop2Factory();
        FACTORIES[Const.DUP]             = new DupFactory();
        FACTORIES[Const.DUP_X1]          = new DupX1Factory();
        FACTORIES[Const.DUP_X2]          = new DupX2Factory();
        FACTORIES[Const.DUP2]            = new Dup2Factory();
        FACTORIES[Const.DUP2_X1]         = new Dup2X1Factory();
        FACTORIES[Const.DUP2_X2]         = new Dup2X2Factory();
        FACTORIES[Const.SWAP]            = new SwapFactory();
        FACTORIES[Const.IADD]            = new BinaryOperatorFactory(4, "I", "+");
        FACTORIES[Const.LADD]            = new BinaryOperatorFactory(4, "J", "+");
        FACTORIES[Const.FADD]            = new BinaryOperatorFactory(4, "F", "+");
        FACTORIES[Const.DADD]            = new BinaryOperatorFactory(4, "D", "+");
        FACTORIES[Const.ISUB]            = new BinaryOperatorFactory(4, "I", "-");
        FACTORIES[Const.LSUB]            = new BinaryOperatorFactory(4, "J", "-");
        FACTORIES[Const.FSUB]            = new BinaryOperatorFactory(4, "F", "-");
        FACTORIES[Const.DSUB]            = new BinaryOperatorFactory(4, "D", "-");
        FACTORIES[Const.IMUL]            = new BinaryOperatorFactory(3, "I", "*");
        FACTORIES[Const.LMUL]            = new BinaryOperatorFactory(3, "J", "*");
        FACTORIES[Const.FMUL]            = new BinaryOperatorFactory(3, "F", "*");
        FACTORIES[Const.DMUL]            = new BinaryOperatorFactory(3, "D", "*");
        FACTORIES[Const.IDIV]            = new BinaryOperatorFactory(3, "I", "/");
        FACTORIES[Const.LDIV]            = new BinaryOperatorFactory(3, "J", "/");
        FACTORIES[Const.FDIV]            = new BinaryOperatorFactory(3, "F", "/");
        FACTORIES[Const.DDIV]            = new BinaryOperatorFactory(3, "D", "/");
        FACTORIES[Const.IREM]            = new BinaryOperatorFactory(3, "I", "%");
        FACTORIES[Const.LREM]            = new BinaryOperatorFactory(3, "J", "%");
        FACTORIES[Const.FREM]            = new BinaryOperatorFactory(3, "F", "%");
        FACTORIES[Const.DREM]            = new BinaryOperatorFactory(3, "D", "%");
        FACTORIES[Const.INEG]            = new UnaryOperatorFactory(2, "I", "-");
        FACTORIES[Const.LNEG]            = new UnaryOperatorFactory(2, "J", "-");
        FACTORIES[Const.FNEG]            = new UnaryOperatorFactory(2, "F", "-");
        FACTORIES[Const.DNEG]            = new UnaryOperatorFactory(2, "D", "-");
        FACTORIES[Const.ISHL]            = new BinaryOperatorFactory(5, "I", "<<");
        FACTORIES[Const.LSHL]            = new BinaryOperatorFactory(5, "J", "<<");
        FACTORIES[Const.ISHR]            = new BinaryOperatorFactory(5, "I", ">>");
        FACTORIES[Const.LSHR]            = new BinaryOperatorFactory(5, "J", ">>");
        FACTORIES[Const.IUSHR]           = new BinaryOperatorFactory(5, "I", ">>>");
        FACTORIES[Const.LUSHR]           = new BinaryOperatorFactory(5, "J", ">>>");
        FACTORIES[Const.IAND]            = new IBinaryOperatorFactory(8, "&");
        FACTORIES[Const.LAND]            = new BinaryOperatorFactory(8, "J", "&");
        FACTORIES[Const.IOR]             = new IBinaryOperatorFactory(10, "|");
        FACTORIES[Const.LOR]             = new BinaryOperatorFactory(10, "J", "|");
        FACTORIES[Const.IXOR]            = new IBinaryOperatorFactory(9, "^");
        FACTORIES[Const.LXOR]            = new BinaryOperatorFactory(9, "J", "^");
        FACTORIES[Const.IINC]            = new IIncFactory();
        FACTORIES[Const.I2L]             = new ImplicitConvertInstructionFactory("J");
        FACTORIES[Const.I2F]             = new ImplicitConvertInstructionFactory("F");
        FACTORIES[Const.I2D]             = new ImplicitConvertInstructionFactory("D");
        FACTORIES[Const.L2I]             = new ConvertInstructionFactory("I");
        FACTORIES[Const.L2F]             = new ConvertInstructionFactory("F");
        FACTORIES[Const.L2D]             = FACTORIES[Const.I2D];
        FACTORIES[Const.F2I]             = FACTORIES[Const.L2I];
        FACTORIES[Const.F2L]             = new ConvertInstructionFactory("J");
        FACTORIES[Const.F2D]             = FACTORIES[Const.I2D];
        FACTORIES[Const.D2I]             = FACTORIES[Const.L2I];
        FACTORIES[Const.D2L]             = FACTORIES[Const.F2L];
        FACTORIES[Const.D2F]             = FACTORIES[Const.L2F];
        FACTORIES[Const.I2B]             = new ConvertInstructionFactory("B");
        FACTORIES[Const.I2C]             = new ConvertInstructionFactory("C");
        FACTORIES[Const.I2S]             = new ConvertInstructionFactory("S");
        FACTORIES[Const.LCMP]            = new CmpFactory(6, "Z", "<");
        FACTORIES[Const.FCMPL]           = FACTORIES[Const.LCMP];
        FACTORIES[Const.FCMPG]           = FACTORIES[Const.LCMP];
        FACTORIES[Const.DCMPL]           = FACTORIES[Const.LCMP];
        FACTORIES[Const.DCMPG]           = FACTORIES[Const.LCMP];
        FACTORIES[Const.IFEQ]            = new IfFactory(ByteCodeConstants.CMP_EQ);
        FACTORIES[Const.IFNE]            = new IfFactory(ByteCodeConstants.CMP_NE);
        FACTORIES[Const.IFLT]            = new IfFactory(ByteCodeConstants.CMP_LT);
        FACTORIES[Const.IFGE]            = new IfFactory(ByteCodeConstants.CMP_GE);
        FACTORIES[Const.IFGT]            = new IfFactory(ByteCodeConstants.CMP_GT);
        FACTORIES[Const.IFLE]            = new IfFactory(ByteCodeConstants.CMP_LE);
        FACTORIES[Const.IF_ICMPEQ]       = new IfCmpFactory(ByteCodeConstants.CMP_EQ);
        FACTORIES[Const.IF_ICMPNE]       = new IfCmpFactory(ByteCodeConstants.CMP_NE);
        FACTORIES[Const.IF_ICMPLT]       = new IfCmpFactory(ByteCodeConstants.CMP_LT);
        FACTORIES[Const.IF_ICMPGE]       = new IfCmpFactory(ByteCodeConstants.CMP_GE);
        FACTORIES[Const.IF_ICMPGT]       = new IfCmpFactory(ByteCodeConstants.CMP_GT);
        FACTORIES[Const.IF_ICMPLE]       = new IfCmpFactory(ByteCodeConstants.CMP_LE);
        FACTORIES[Const.IF_ACMPEQ]       = FACTORIES[Const.IF_ICMPEQ];
        FACTORIES[Const.IF_ACMPNE]       = FACTORIES[Const.IF_ICMPNE];
        FACTORIES[Const.GOTO]            = new GotoFactory();
        FACTORIES[Const.JSR]             = new JsrFactory();
        FACTORIES[Const.RET]             = new RetFactory();
        FACTORIES[Const.TABLESWITCH]     = new TableSwitchFactory();
        FACTORIES[Const.LOOKUPSWITCH]    = new LookupSwitchFactory();
        FACTORIES[Const.IRETURN]         = new ReturnInstructionFactory();
        FACTORIES[Const.LRETURN]         = FACTORIES[Const.IRETURN];
        FACTORIES[Const.FRETURN]         = FACTORIES[Const.IRETURN];
        FACTORIES[Const.DRETURN]         = FACTORIES[Const.IRETURN];
        FACTORIES[Const.ARETURN]         = FACTORIES[Const.IRETURN];
        FACTORIES[Const.RETURN]          = new ReturnFactory();
        FACTORIES[Const.GETSTATIC]       = new GetStaticFactory();
        FACTORIES[Const.PUTSTATIC]       = new PutStaticFactory();
        FACTORIES[Const.GETFIELD]        = new GetFieldFactory();
        FACTORIES[Const.PUTFIELD]        = new PutFieldFactory();
        FACTORIES[Const.INVOKEVIRTUAL]   = new InvokevirtualFactory();
        FACTORIES[Const.INVOKESPECIAL]   = new InvokespecialFactory();
        FACTORIES[Const.INVOKESTATIC ]   = new InvokestaticFactory();
        FACTORIES[Const.INVOKEINTERFACE] = new InvokeinterfaceFactory();
        FACTORIES[Const.INVOKEDYNAMIC]   = new InvokeDynamicFactory();
        FACTORIES[Const.NEW]             = new NewFactory();
        FACTORIES[Const.NEWARRAY]        = new NewArrayFactory();
        FACTORIES[Const.ANEWARRAY]       = new ANewArrayFactory();
        FACTORIES[Const.ARRAYLENGTH]     = new ArrayLengthFactory();
        FACTORIES[Const.ATHROW]          = new AThrowFactory();
        FACTORIES[Const.CHECKCAST]       = new CheckCastFactory();
        FACTORIES[Const.INSTANCEOF]      = new InstanceOfFactory();
        FACTORIES[Const.MONITORENTER]    = new MonitorEnterFactory();
        FACTORIES[Const.MONITOREXIT]     = new MonitorExitFactory();
        FACTORIES[Const.WIDE]            = new WideFactory();
        FACTORIES[Const.MULTIANEWARRAY]  = new MultiANewArrayFactory();
        FACTORIES[Const.IFNULL]          = new IfXNullFactory(ByteCodeConstants.CMP_EQ);
        FACTORIES[Const.IFNONNULL]       = new IfXNullFactory(ByteCodeConstants.CMP_NE);
        FACTORIES[Const.GOTO_W]          = new GotoWFactory();
        FACTORIES[Const.JSR_W]           = new JsrWFactory();

        /**
         * Non-legal opcodes, may be used by JVM internally.
         */
//        public static final int BREAKPOINT                    = 202;
//        public static final int LDC_QUICK                     = 203;
//        public static final int LDC_W_QUICK                   = 204;
//        public static final int LDC2_W_QUICK                  = 205;
//        public static final int GETFIELD_QUICK                = 206;
//        public static final int PUTFIELD_QUICK                = 207;
//        public static final int GETFIELD2_QUICK               = 208;
//        public static final int PUTFIELD2_QUICK               = 209;
//        public static final int GETSTATIC_QUICK               = 210;
//        public static final int PUTSTATIC_QUICK               = 211;
//        public static final int GETSTATIC2_QUICK              = 212;
//        public static final int PUTSTATIC2_QUICK              = 213;
//        public static final int INVOKEVIRTUAL_QUICK           = 214;
//        public static final int INVOKENONVIRTUAL_QUICK        = 215;
//        public static final int INVOKESUPER_QUICK             = 216;
//        public static final int INVOKESTATIC_QUICK            = 217;
//        public static final int INVOKEINTERFACE_QUICK         = 218;
//        public static final int INVOKEVIRTUALOBJECT_QUICK     = 219;
//        public static final int NEW_QUICK                     = 221;
//        public static final int ANEWARRAY_QUICK               = 222;
//        public static final int MULTIANEWARRAY_QUICK          = 223;
//        public static final int CHECKCAST_QUICK               = 224;
//        public static final int INSTANCEOF_QUICK              = 225;
//        public static final int INVOKEVIRTUAL_QUICK_W         = 226;
//        public static final int GETFIELD_QUICK_W              = 227;
//        public static final int PUTFIELD_QUICK_W              = 228;
//        public static final int IMPDEP1                       = 254;
//        public static final int IMPDEP2                       = 255;
    }
}
