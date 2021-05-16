package jd.core.model.layout.block;

import jd.core.model.instruction.bytecode.instruction.Instruction;

public class CommentErrorLayoutBlock extends LayoutBlock 
{
	public CommentErrorLayoutBlock() 
	{
		super(
			LayoutBlockConstants.COMMENT_ERROR,
			Instruction.UNKNOWN_LINE_NUMBER, Instruction.UNKNOWN_LINE_NUMBER, 
			0, 1, 1);
	}
}
