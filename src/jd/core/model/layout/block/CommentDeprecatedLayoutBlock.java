package jd.core.model.layout.block;

import jd.core.model.instruction.bytecode.instruction.Instruction;

public class CommentDeprecatedLayoutBlock extends LayoutBlock 
{
	public CommentDeprecatedLayoutBlock() 
	{
		super(
			LayoutBlockConstants.COMMENT_DEPRECATED,
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 3, 3);
	}
}
