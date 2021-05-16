package jd.core.model.classfile;


public class ClassFileConstants 
{
	// Access flag for Class, Field, Method, Nested class
	public final static short ACC_PUBLIC       = 0x0001; // C F M N
	public final static short ACC_PRIVATE      = 0x0002; //   F M N
	public final static short ACC_PROTECTED    = 0x0004; //   F M N
	public final static short ACC_STATIC       = 0x0008; //   F M N
	public final static short ACC_FINAL        = 0x0010; // C F M N
	public final static short ACC_SYNCHRONIZED = 0x0020; //     M
	public final static short ACC_SUPER        = 0x0020; // C
	public final static short ACC_VOLATILE     = 0x0040; //   F
	public final static short ACC_BRIDGE       = 0x0040; //     M
	public final static short ACC_TRANSIENT    = 0x0080; //   F
	public final static short ACC_VARARGS      = 0x0080; //     M
	public final static short ACC_NATIVE       = 0x0100; //     M
	public final static short ACC_INTERFACE    = 0x0200; // C     N
	public final static short ACC_ABSTRACT     = 0x0400; // C   M N
	public final static short ACC_STRICT       = 0x0800; //     M
	public final static short ACC_SYNTHETIC    = 0x1000; // C F M N
	public final static short ACC_ANNOTATION   = 0x2000; // C     N
	public final static short ACC_ENUM         = 0x4000; // C F   N
}
