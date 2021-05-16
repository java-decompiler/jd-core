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
package jd.core.model.instruction.bytecode;




public class ByteCodeConstants 
{
	public static final short  NO_OF_OPERANDS_UNDEFINED      = -1;
	public static final short  NO_OF_OPERANDS_UNPREDICTABLE  = -2;
	public static final short  NO_OF_OPERANDS_RESERVED       = -3;

	public static final String ILLEGAL_OPCODE = "<illegal opcode>";
	public static final String ILLEGAL_TYPE   = "<illegal type>";

	
	/** Java VM opcodes.
	 */
	public static final int NOP              = 0;
	public static final int ACONST_NULL      = 1;
	public static final int ICONST_M1        = 2;
	public static final int ICONST_0         = 3;
	public static final int ICONST_1         = 4;
	public static final int ICONST_2         = 5;
	public static final int ICONST_3         = 6;
	public static final int ICONST_4         = 7;
	public static final int ICONST_5         = 8;
	public static final int LCONST_0         = 9;
	public static final int LCONST_1         = 10;
	public static final int FCONST_0         = 11;
	public static final int FCONST_1         = 12;
	public static final int FCONST_2         = 13;
	public static final int DCONST_0         = 14;
	public static final int DCONST_1         = 15;
	public static final int BIPUSH           = 16;
	public static final int SIPUSH           = 17;
	public static final int LDC              = 18;
	public static final int LDC_W            = 19;
	public static final int LDC2_W           = 20;
	public static final int ILOAD            = 21;
	public static final int LLOAD            = 22;
	public static final int FLOAD            = 23;
	public static final int DLOAD            = 24;
	public static final int ALOAD            = 25;
	public static final int ILOAD_0          = 26;
	public static final int ILOAD_1          = 27;
	public static final int ILOAD_2          = 28;
	public static final int ILOAD_3          = 29;
	public static final int LLOAD_0          = 30;
	public static final int LLOAD_1          = 31;
	public static final int LLOAD_2          = 32;
	public static final int LLOAD_3          = 33;
	public static final int FLOAD_0          = 34;
	public static final int FLOAD_1          = 35;
	public static final int FLOAD_2          = 36;
	public static final int FLOAD_3          = 37;
	public static final int DLOAD_0          = 38;
	public static final int DLOAD_1          = 39;
	public static final int DLOAD_2          = 40;
	public static final int DLOAD_3          = 41;
	public static final int ALOAD_0          = 42;
	public static final int ALOAD_1          = 43;
	public static final int ALOAD_2          = 44;
	public static final int ALOAD_3          = 45;
	public static final int IALOAD           = 46;
	public static final int LALOAD           = 47;
	public static final int FALOAD           = 48;
	public static final int DALOAD           = 49;
	public static final int AALOAD           = 50;
	public static final int BALOAD           = 51;
	public static final int CALOAD           = 52;
	public static final int SALOAD           = 53;
	public static final int ISTORE           = 54;
	public static final int LSTORE           = 55;
	public static final int FSTORE           = 56;
	public static final int DSTORE           = 57;
	public static final int ASTORE           = 58;
	public static final int ISTORE_0         = 59;
	public static final int ISTORE_1         = 60;
	public static final int ISTORE_2         = 61;
	public static final int ISTORE_3         = 62;
	public static final int LSTORE_0         = 63;
	public static final int LSTORE_1         = 64;
	public static final int LSTORE_2         = 65;
	public static final int LSTORE_3         = 66;
	public static final int FSTORE_0         = 67;
	public static final int FSTORE_1         = 68;
	public static final int FSTORE_2         = 69;
	public static final int FSTORE_3         = 70;
	public static final int DSTORE_0         = 71;
	public static final int DSTORE_1         = 72;
	public static final int DSTORE_2         = 73;
	public static final int DSTORE_3         = 74;
	public static final int ASTORE_0         = 75;
	public static final int ASTORE_1         = 76;
	public static final int ASTORE_2         = 77;
	public static final int ASTORE_3         = 78;
	public static final int IASTORE          = 79;
	public static final int LASTORE          = 80;
	public static final int FASTORE          = 81;
	public static final int DASTORE          = 82;
	public static final int AASTORE          = 83;
	public static final int BASTORE          = 84;
	public static final int CASTORE          = 85;
	public static final int SASTORE          = 86;
	public static final int POP              = 87;
	public static final int POP2             = 88;
	public static final int DUP              = 89;
	public static final int DUP_X1           = 90;
	public static final int DUP_X2           = 91;
	public static final int DUP2             = 92;
	public static final int DUP2_X1          = 93;
	public static final int DUP2_X2          = 94;
	public static final int SWAP             = 95;
	public static final int IADD             = 96;
	public static final int LADD             = 97;
	public static final int FADD             = 98;
	public static final int DADD             = 99;
	public static final int ISUB             = 100;
	public static final int LSUB             = 101;
	public static final int FSUB             = 102;
	public static final int DSUB             = 103;
	public static final int IMUL             = 104;
	public static final int LMUL             = 105;
	public static final int FMUL             = 106;
	public static final int DMUL             = 107;
	public static final int IDIV             = 108;
	public static final int LDIV             = 109;
	public static final int FDIV             = 110;
	public static final int DDIV             = 111;
	public static final int IREM             = 112;
	public static final int LREM             = 113;
	public static final int FREM             = 114;
	public static final int DREM             = 115;
	public static final int INEG             = 116;
	public static final int LNEG             = 117;
	public static final int FNEG             = 118;
	public static final int DNEG             = 119;
	public static final int ISHL             = 120;
	public static final int LSHL             = 121;
	public static final int ISHR             = 122;
	public static final int LSHR             = 123;
	public static final int IUSHR            = 124;
	public static final int LUSHR            = 125;
	public static final int IAND             = 126;
	public static final int LAND             = 127;
	public static final int IOR              = 128;
	public static final int LOR              = 129;
	public static final int IXOR             = 130;
	public static final int LXOR             = 131;
	public static final int IINC             = 132;
	public static final int I2L              = 133;
	public static final int I2F              = 134;
	public static final int I2D              = 135;
	public static final int L2I              = 136;
	public static final int L2F              = 137;
	public static final int L2D              = 138;
	public static final int F2I              = 139;
	public static final int F2L              = 140;
	public static final int F2D              = 141;
	public static final int D2I              = 142;
	public static final int D2L              = 143;
	public static final int D2F              = 144;
	public static final int I2B              = 145;
	public static final int INT2BYTE         = 145; // Old notion
	public static final int I2C              = 146;
	public static final int INT2CHAR         = 146; // Old notion
	public static final int I2S              = 147;
	public static final int INT2SHORT        = 147; // Old notion
	public static final int LCMP             = 148;
	public static final int FCMPL            = 149;
	public static final int FCMPG            = 150;
	public static final int DCMPL            = 151;
	public static final int DCMPG            = 152;
	public static final int IFEQ             = 153;
	public static final int IFNE             = 154;
	public static final int IFLT             = 155;
	public static final int IFGE             = 156;
	public static final int IFGT             = 157;
	public static final int IFLE             = 158;
	public static final int IF_ICMPEQ        = 159;
	public static final int IF_ICMPNE        = 160;
	public static final int IF_ICMPLT        = 161;
	public static final int IF_ICMPGE        = 162;
	public static final int IF_ICMPGT        = 163;
	public static final int IF_ICMPLE        = 164;
	public static final int IF_ACMPEQ        = 165;
	public static final int IF_ACMPNE        = 166;
	public static final int GOTO             = 167;
	public static final int JSR              = 168;
	public static final int RET              = 169;
	public static final int TABLESWITCH      = 170;
	public static final int LOOKUPSWITCH     = 171;
	public static final int IRETURN          = 172;
	public static final int LRETURN          = 173;
	public static final int FRETURN          = 174;
	public static final int DRETURN          = 175;
	public static final int ARETURN          = 176;
	public static final int RETURN           = 177;
	public static final int GETSTATIC        = 178;
	public static final int PUTSTATIC        = 179;
	public static final int GETFIELD         = 180;
	public static final int PUTFIELD         = 181;
	public static final int INVOKEVIRTUAL    = 182;
	public static final int INVOKESPECIAL    = 183;
	public static final int INVOKENONVIRTUAL = 183; // Old name in JDK 1.0
	public static final int INVOKESTATIC     = 184;
	public static final int INVOKEINTERFACE  = 185;
	public static final int NEW              = 187;
	public static final int NEWARRAY         = 188;
	public static final int ANEWARRAY        = 189;
	public static final int ARRAYLENGTH      = 190;
	public static final int ATHROW           = 191;
	public static final int CHECKCAST        = 192;
	public static final int INSTANCEOF       = 193;
	public static final int MONITORENTER     = 194;
	public static final int MONITOREXIT      = 195;
	public static final int WIDE             = 196;
	public static final int MULTIANEWARRAY   = 197;
	public static final int IFNULL           = 198;
	public static final int IFNONNULL        = 199;
	public static final int GOTO_W           = 200;
	public static final int JSR_W            = 201;
	
	/**
	 * Non-legal opcodes, may be used by JVM internally.
	 */
	public static final int BREAKPOINT                = 202;
	public static final int LDC_QUICK                 = 203;
	public static final int LDC_W_QUICK               = 204;
	public static final int LDC2_W_QUICK              = 205;
	public static final int GETFIELD_QUICK            = 206;
	public static final int PUTFIELD_QUICK            = 207;
	public static final int GETFIELD2_QUICK           = 208;
	public static final int PUTFIELD2_QUICK           = 209;
	public static final int GETSTATIC_QUICK           = 210;
	public static final int PUTSTATIC_QUICK           = 211;
	public static final int GETSTATIC2_QUICK          = 212;
	public static final int PUTSTATIC2_QUICK          = 213;
	public static final int INVOKEVIRTUAL_QUICK       = 214;
	public static final int INVOKENONVIRTUAL_QUICK    = 215;
	public static final int INVOKESUPER_QUICK         = 216;
	public static final int INVOKESTATIC_QUICK        = 217;
	public static final int INVOKEINTERFACE_QUICK     = 218;
	public static final int INVOKEVIRTUALOBJECT_QUICK = 219;
	public static final int NEW_QUICK                 = 221;
	public static final int ANEWARRAY_QUICK           = 222;
	public static final int MULTIANEWARRAY_QUICK      = 223;
	public static final int CHECKCAST_QUICK           = 224;
	public static final int INSTANCEOF_QUICK          = 225;
	public static final int INVOKEVIRTUAL_QUICK_W     = 226;
	public static final int GETFIELD_QUICK_W          = 227;
	public static final int PUTFIELD_QUICK_W          = 228;
	public static final int IMPDEP1                   = 254;
	public static final int IMPDEP2                   = 255;

	
	// Extension for decompiler
	public static final int ICONST                    = 256;
	public static final int LCONST                    = 257;
	public static final int FCONST                    = 258;
	public static final int DCONST                    = 259;
	public static final int IF                        = 260;
	public static final int IFCMP                     = 261;
	public static final int IFXNULL                   = 262;
	public static final int DUPLOAD                   = 263;
	public static final int DUPSTORE                  = 264;
	public static final int ASSIGNMENT                = 265;
	public static final int UNARYOP                   = 266;
	public static final int BINARYOP                  = 267;
	public static final int LOAD                      = 268;
	public static final int STORE                     = 269;
	public static final int EXCEPTIONLOAD             = 270;
	public static final int ARRAYLOAD                 = 271;
	public static final int ARRAYSTORE                = 272;
	public static final int XRETURN                   = 273;
	public static final int INVOKENEW                 = 274;
	public static final int CONVERT                   = 275;
	public static final int IMPLICITCONVERT           = 276;	
	public static final int PREINC                    = 277;
	public static final int POSTINC                   = 278;	
	public static final int RETURNADDRESSLOAD         = 279;	
	public static final int TERNARYOPSTORE            = 280;
	public static final int TERNARYOP                 = 281;
	public static final int INITARRAY                 = 282;
	public static final int NEWANDINITARRAY           = 283;
	public static final int COMPLEXIF                 = 284;
	public static final int OUTERTHIS                 = 285;
	public static final int ASSERT                    = 286;
	
	
	public static final String[] OPCODE_NAMES = {
	    "nop", "aconst_null", "iconst_m1", "iconst_0", "iconst_1",
	    "iconst_2", "iconst_3", "iconst_4", "iconst_5", "lconst_0",
	    "lconst_1", "fconst_0", "fconst_1", "fconst_2", "dconst_0",
	    "dconst_1", "bipush", "sipush", "ldc", "ldc_w", "ldc2_w", "iload",
	    "lload", "fload", "dload", "aload", "iload_0", "iload_1", "iload_2",
	    "iload_3", "lload_0", "lload_1", "lload_2", "lload_3", "fload_0",
	    "fload_1", "fload_2", "fload_3", "dload_0", "dload_1", "dload_2",
	    "dload_3", "aload_0", "aload_1", "aload_2", "aload_3", "iaload",
	    "laload", "faload", "daload", "aaload", "baload", "caload", "saload",
	    "istore", "lstore", "fstore", "dstore", "astore", "istore_0",
	    "istore_1", "istore_2", "istore_3", "lstore_0", "lstore_1",
	    "lstore_2", "lstore_3", "fstore_0", "fstore_1", "fstore_2",
	    "fstore_3", "dstore_0", "dstore_1", "dstore_2", "dstore_3",
	    "astore_0", "astore_1", "astore_2", "astore_3", "iastore", "lastore",
	    "fastore", "dastore", "aastore", "bastore", "castore", "sastore",
	    "pop", "pop2", "dup", "dup_x1", "dup_x2", "dup2", "dup2_x1",
	    "dup2_x2", "swap", "iadd", "ladd", "fadd", "dadd", "isub", "lsub",
	    "fsub", "dsub", "imul", "lmul", "fmul", "dmul", "idiv", "ldiv",
	    "fdiv", "ddiv", "irem", "lrem", "frem", "drem", "ineg", "lneg",
	    "fneg", "dneg", "ishl", "lshl", "ishr", "lshr", "iushr", "lushr",
	    "iand", "land", "ior", "lor", "ixor", "lxor", "iinc", "i2l", "i2f",
	    "i2d", "l2i", "l2f", "l2d", "f2i", "f2l", "f2d", "d2i", "d2l", "d2f",
	    "i2b", "i2c", "i2s", "lcmp", "fcmpl", "fcmpg",
	    "dcmpl", "dcmpg", "ifeq", "ifne", "iflt", "ifge", "ifgt", "ifle",
	    "if_icmpeq", "if_icmpne", "if_icmplt", "if_icmpge", "if_icmpgt",
	    "if_icmple", "if_acmpeq", "if_acmpne", "goto", "jsr", "ret",
	    "tableswitch", "lookupswitch", "ireturn", "lreturn", "freturn",
	    "dreturn", "areturn", "return", "getstatic", "putstatic", "getfield",
	    "putfield", "invokevirtual", "invokespecial", "invokestatic",
	    "invokeinterface", ILLEGAL_OPCODE, "new", "newarray", "anewarray",
	    "arraylength", "athrow", "checkcast", "instanceof", "monitorenter",
	    "monitorexit", "wide", "multianewarray", "ifnull", "ifnonnull",
	    "goto_w", "jsr_w", "breakpoint", ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE, ILLEGAL_OPCODE,
	    ILLEGAL_OPCODE, "impdep1", "impdep2"
	  };

	/**
	 * Number of byte code operands, i.e., number of bytes after the tag byte
	 * itself.
	 */
	public static final short[] NO_OF_OPERANDS = {
	    0/*nop*/, 0/*aconst_null*/, 0/*iconst_m1*/, 0/*iconst_0*/,
	    0/*iconst_1*/, 0/*iconst_2*/, 0/*iconst_3*/, 0/*iconst_4*/,
	    0/*iconst_5*/, 0/*lconst_0*/, 0/*lconst_1*/, 0/*fconst_0*/,
	    0/*fconst_1*/, 0/*fconst_2*/, 0/*dconst_0*/, 0/*dconst_1*/,
	    1/*bipush*/, 2/*sipush*/, 1/*ldc*/, 2/*ldc_w*/, 2/*ldc2_w*/,
	    1/*iload*/, 1/*lload*/, 1/*fload*/, 1/*dload*/, 1/*aload*/,
	    0/*iload_0*/, 0/*iload_1*/, 0/*iload_2*/, 0/*iload_3*/,
	    0/*lload_0*/, 0/*lload_1*/, 0/*lload_2*/, 0/*lload_3*/,
	    0/*fload_0*/, 0/*fload_1*/, 0/*fload_2*/, 0/*fload_3*/,
	    0/*dload_0*/, 0/*dload_1*/, 0/*dload_2*/, 0/*dload_3*/,
	    0/*aload_0*/, 0/*aload_1*/, 0/*aload_2*/, 0/*aload_3*/,
	    0/*iaload*/, 0/*laload*/, 0/*faload*/, 0/*daload*/,
	    0/*aaload*/, 0/*baload*/, 0/*caload*/, 0/*saload*/,
	    1/*istore*/, 1/*lstore*/, 1/*fstore*/, 1/*dstore*/,
	    1/*astore*/, 0/*istore_0*/, 0/*istore_1*/, 0/*istore_2*/,
	    0/*istore_3*/, 0/*lstore_0*/, 0/*lstore_1*/, 0/*lstore_2*/,
	    0/*lstore_3*/, 0/*fstore_0*/, 0/*fstore_1*/, 0/*fstore_2*/,
	    0/*fstore_3*/, 0/*dstore_0*/, 0/*dstore_1*/, 0/*dstore_2*/,
	    0/*dstore_3*/, 0/*astore_0*/, 0/*astore_1*/, 0/*astore_2*/,
	    0/*astore_3*/, 0/*iastore*/, 0/*lastore*/, 0/*fastore*/,
	    0/*dastore*/, 0/*aastore*/, 0/*bastore*/, 0/*castore*/,
	    0/*sastore*/, 0/*pop*/, 0/*pop2*/, 0/*dup*/, 0/*dup_x1*/,
	    0/*dup_x2*/, 0/*dup2*/, 0/*dup2_x1*/, 0/*dup2_x2*/, 0/*swap*/,
	    0/*iadd*/, 0/*ladd*/, 0/*fadd*/, 0/*dadd*/, 0/*isub*/,
	    0/*lsub*/, 0/*fsub*/, 0/*dsub*/, 0/*imul*/, 0/*lmul*/,
	    0/*fmul*/, 0/*dmul*/, 0/*idiv*/, 0/*ldiv*/, 0/*fdiv*/,
	    0/*ddiv*/, 0/*irem*/, 0/*lrem*/, 0/*frem*/, 0/*drem*/,
	    0/*ineg*/, 0/*lneg*/, 0/*fneg*/, 0/*dneg*/, 0/*ishl*/,
	    0/*lshl*/, 0/*ishr*/, 0/*lshr*/, 0/*iushr*/, 0/*lushr*/,
	    0/*iand*/, 0/*land*/, 0/*ior*/, 0/*lor*/, 0/*ixor*/, 0/*lxor*/,
	    2/*iinc*/, 0/*i2l*/, 0/*i2f*/, 0/*i2d*/, 0/*l2i*/, 0/*l2f*/,
	    0/*l2d*/, 0/*f2i*/, 0/*f2l*/, 0/*f2d*/, 0/*d2i*/, 0/*d2l*/,
	    0/*d2f*/, 0/*i2b*/, 0/*i2c*/, 0/*i2s*/, 0/*lcmp*/, 0/*fcmpl*/,
	    0/*fcmpg*/, 0/*dcmpl*/, 0/*dcmpg*/, 2/*ifeq*/, 2/*ifne*/,
	    2/*iflt*/, 2/*ifge*/, 2/*ifgt*/, 2/*ifle*/, 2/*if_icmpeq*/,
	    2/*if_icmpne*/, 2/*if_icmplt*/, 2/*if_icmpge*/, 2/*if_icmpgt*/,
	    2/*if_icmple*/, 2/*if_acmpeq*/, 2/*if_acmpne*/, 2/*goto*/,
	    2/*jsr*/, 1/*ret*/, NO_OF_OPERANDS_UNPREDICTABLE/*tableswitch*/, NO_OF_OPERANDS_UNPREDICTABLE/*lookupswitch*/,
	    0/*ireturn*/, 0/*lreturn*/, 0/*freturn*/,
	    0/*dreturn*/, 0/*areturn*/, 0/*return*/,
	    2/*getstatic*/, 2/*putstatic*/, 2/*getfield*/,
	    2/*putfield*/, 2/*invokevirtual*/, 2/*invokespecial*/, 2/*invokestatic*/,
	    4/*invokeinterface*/, NO_OF_OPERANDS_UNDEFINED, 2/*new*/,
	    1/*newarray*/, 2/*anewarray*/,
	    0/*arraylength*/, 0/*athrow*/, 2/*checkcast*/,
	    2/*instanceof*/, 0/*monitorenter*/,
	    0/*monitorexit*/, NO_OF_OPERANDS_UNPREDICTABLE/*wide*/, 3/*multianewarray*/,
	    2/*ifnull*/, 2/*ifnonnull*/, 4/*goto_w*/,
	    4/*jsr_w*/, 0/*breakpoint*/, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED,
	    NO_OF_OPERANDS_UNDEFINED, NO_OF_OPERANDS_UNDEFINED, 
	    NO_OF_OPERANDS_RESERVED/*impdep1*/, NO_OF_OPERANDS_RESERVED/*impdep2*/
	  };
	  

	public static final int CMP_MAX_INDEX = 7;
	
	public static final int CMP_EQ  = 0;
	public static final int CMP_LT  = 1;
	public static final int CMP_GT  = 2;	
	public static final int CMP_UEQ = 3;	
	public static final int CMP_UNE = 4;	
	public static final int CMP_LE  = 5;
	public static final int CMP_GE  = 6;
	public static final int CMP_NE  = 7;
	
	public static final String[] CMP_NAMES = { 
		"==", "<", ">", "", "!", "<=", ">=", "!=" };
	

	public static final byte T_BOOLEAN = 4;
	public static final byte T_CHAR    = 5;
	public static final byte T_FLOAT   = 6;
	public static final byte T_DOUBLE  = 7;
	public static final byte T_BYTE    = 8;
	public static final byte T_SHORT   = 9;
	public static final byte T_INT     = 10;
	public static final byte T_LONG    = 11;

	public static final byte T_VOID      = 12; // Non-standard
	public static final byte T_ARRAY     = 13;
	public static final byte T_OBJECT    = 14;
	public static final byte T_REFERENCE = 14; // Deprecated
	public static final byte T_UNKNOWN   = 15;
	public static final byte T_ADDRESS   = 16;

	/** The primitive type names corresponding to the T_XX constants,
	 * e.g., TYPE_NAMES[T_INT] = "int"
	 */
	public static final String[] TYPE_NAMES = {
	    ILLEGAL_TYPE, ILLEGAL_TYPE,  ILLEGAL_TYPE, ILLEGAL_TYPE,
	    "boolean", "char", "float", "double", "byte", "short", "int", "long",
	    "void", "array", "object", "unknown" // Non-standard
	};
	  
	/**
	 * Types Bit Fields
	 */
	public static final byte TBF_INT_CHAR    = 1;
	public static final byte TBF_INT_BYTE    = 2;
	public static final byte TBF_INT_SHORT   = 4;
	public static final byte TBF_INT_INT     = 8;
	public static final byte TBF_INT_BOOLEAN = 16;
	
	/**
	 * Binary operator constants
	 */
	public static final int CMP_AND          = 0;
	public static final int CMP_NONE         = 1;
	public static final int CMP_OR           = 2;
}
