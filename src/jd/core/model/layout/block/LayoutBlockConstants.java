package jd.core.model.layout.block;

public class LayoutBlockConstants 
{
	public static final byte UNDEFINED                                    = 0;

	public static final byte PACKAGE                                      = 1;
	public static final byte SEPARATOR                                    = 2;
	public static final byte SEPARATOR_AT_BEGINING                        = 3;
	public static final byte SEPARATOR_AFTER_IMPORTS                      = 4;
	public static final byte SEPARATOR_OF_STATEMENTS                      = 5;
	
	public static final byte IMPORTS                                      = 6;
	
	public static final byte TYPE_MARKER_START                            = 7;
	public static final byte TYPE_MARKER_END                              = 8;
	
	public static final byte FIELD_MARKER_START                           = 9;
	public static final byte FIELD_MARKER_END                             = 10;
	
	public static final byte METHOD_MARKER_START                          = 11;
	public static final byte METHOD_MARKER_END                            = 12;

	public static final byte TYPE_BODY_BLOCK_START                        = 13;
	public static final byte TYPE_BODY_BLOCK_END                          = 14;
	public static final byte TYPE_BODY_BLOCK_START_END                    = 15;
	
	public static final byte INNER_TYPE_BODY_BLOCK_START                  = 16;
	public static final byte INNER_TYPE_BODY_BLOCK_END                    = 17;
	public static final byte INNER_TYPE_BODY_BLOCK_START_END              = 18;
	
	public static final byte METHOD_BODY_BLOCK_START                      = 19;
	public static final byte METHOD_BODY_BLOCK_END                        = 20;
	public static final byte METHOD_BODY_BLOCK_START_END                  = 21;

	public static final byte METHOD_BODY_SINGLE_LINE_BLOCK_START          = 22;
	public static final byte METHOD_BODY_SINGLE_LINE_BLOCK_END            = 23;

	public static final byte STATEMENTS_BLOCK_START                       = 25;
	public static final byte STATEMENTS_BLOCK_END                         = 26;
	public static final byte STATEMENTS_BLOCK_START_END                   = 27;
	
	public static final byte SINGLE_STATEMENT_BLOCK_START                 = 28;
	public static final byte SINGLE_STATEMENT_BLOCK_END                   = 29;
	public static final byte SINGLE_STATEMENTS_BLOCK_START_END            = 30;

	public static final byte SWITCH_BLOCK_START                           = 31;
	public static final byte SWITCH_BLOCK_END                             = 32;

	public static final byte CASE_BLOCK_START                             = 33;
	public static final byte CASE_BLOCK_END                               = 34;

	public static final byte FOR_BLOCK_START                              = 37;
	public static final byte FOR_BLOCK_END                                = 38;

	public static final byte COMMENT_DEPRECATED                           = 39;
	public static final byte COMMENT_ERROR                                = 40;
	
	public static final byte ANNOTATIONS                                  = 41;

	public static final byte TYPE_NAME                                    = 42;
	public static final byte EXTENDS_SUPER_TYPE                           = 43;
	public static final byte EXTENDS_SUPER_INTERFACES                     = 44;
	public static final byte IMPLEMENTS_INTERFACES                        = 45;

	public static final byte GENERIC_TYPE_NAME                            = 46;
	public static final byte GENERIC_EXTENDS_SUPER_TYPE                   = 47;
	public static final byte GENERIC_EXTENDS_SUPER_INTERFACES             = 48;
	public static final byte GENERIC_IMPLEMENTS_INTERFACES                = 49;
	
	public static final byte FIELD_NAME                                   = 50;
	
	public static final byte METHOD_STATIC                                = 51;
	
	public static final byte METHOD_NAME                                  = 52;
	public static final byte THROWS                                       = 53;

	public static final byte INSTRUCTION                                  = 54;
	public static final byte INSTRUCTIONS                                 = 55;
	public static final byte BYTE_CODE                                    = 56;	
	public static final byte DECLARE                                      = 57;

	public static final byte SUBLIST_FIELD                                = 58;
	public static final byte SUBLIST_METHOD                               = 59;
	public static final byte SUBLIST_INNER_CLASS                          = 60;

	public static final byte FRAGMENT_WHILE                               = 61;
	public static final byte FRAGMENT_FOR                                 = 62;
	public static final byte FRAGMENT_IF                                  = 63;
	public static final byte FRAGMENT_SWITCH                              = 64;
	public static final byte FRAGMENT_CASE                                = 65;
	public static final byte FRAGMENT_CASE_ENUM                           = 66;
	public static final byte FRAGMENT_CASE_STRING                         = 67;
	public static final byte FRAGMENT_CATCH                               = 68;
	public static final byte FRAGMENT_SYNCHRONIZED                        = 69;
	public static final byte STATEMENT_LABEL                              = 70;
	
	public static final byte FRAGMENT_ELSE                                = 71;
	public static final byte FRAGMENT_ELSE_SPACE                          = 72;
	public static final byte FRAGMENT_DO                                  = 73;
	public static final byte FRAGMENT_INFINITE_LOOP                       = 74;
	public static final byte FRAGMENT_TRY                                 = 75;
	public static final byte FRAGMENT_FINALLY                             = 76;
	public static final byte FRAGMENT_CONTINUE                            = 77;
	public static final byte FRAGMENT_BREAK                               = 78;
	public static final byte FRAGMENT_LABELED_BREAK                       = 79;

	public static final byte FRAGMENT_RIGHT_ROUND_BRACKET                 = 80;
	public static final byte FRAGMENT_RIGHT_ROUND_BRACKET_SEMICOLON       = 81;
	public static final byte FRAGMENT_SEMICOLON                           = 82;
	public static final byte FRAGMENT_SEMICOLON_SPACE                     = 83;
	public static final byte FRAGMENT_SPACE_COLON_SPACE                   = 84;
	public static final byte FRAGMENT_COMA_SPACE                          = 85;

	public static final int UNLIMITED_LINE_COUNT = Integer.MAX_VALUE;
}
