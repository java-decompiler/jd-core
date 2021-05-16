package jd.core.process.layouter;

import java.util.List;

import jd.core.model.classfile.ClassFile;
import jd.core.model.classfile.ClassFileConstants;
import jd.core.model.layout.block.GenericExtendsSuperInterfacesLayoutBlock;
import jd.core.model.layout.block.GenericExtendsSuperTypeLayoutBlock;
import jd.core.model.layout.block.GenericImplementsInterfacesLayoutBlock;
import jd.core.model.layout.block.GenericTypeNameLayoutBlock;
import jd.core.model.layout.block.LayoutBlock;
import jd.core.util.CharArrayUtil;
import jd.core.util.SignatureUtil;
import jd.core.util.StringConstants;

public class SignatureLayouter 
{
	public static boolean CreateLayoutBlocksForClassSignature(
		ClassFile classFile, 
		String signature, 
		List<LayoutBlock> layoutBlockList)
	{
		boolean displayExtendsOrImplementsFlag = false;
		
		char[] caSignature = signature.toCharArray();
		int length = caSignature.length;
		int index = 0;
		int newIndex;
		
		layoutBlockList.add(new GenericTypeNameLayoutBlock(classFile, signature));
		
		// Affichage des generics
		index = SkipGenerics(caSignature, length, index);
		
		// Affichage de la classe mere
		newIndex = SignatureUtil.SkipSignature(caSignature, length, index);

		if (((classFile.access_flags & 
				(ClassFileConstants.ACC_INTERFACE|ClassFileConstants.ACC_ENUM)) == 0) &&
			!IsObjectClass(caSignature, index, newIndex))
		{
			displayExtendsOrImplementsFlag = true;
			layoutBlockList.add(
				new GenericExtendsSuperTypeLayoutBlock(
					classFile, caSignature, index));
		}

		// Affichage des interfaces ou des super interfaces
		if (newIndex < length)
		{	
			displayExtendsOrImplementsFlag = true;
			
			if ((classFile.access_flags & ClassFileConstants.ACC_INTERFACE) != 0)
			{
				layoutBlockList.add(
					new GenericExtendsSuperInterfacesLayoutBlock(
						classFile, caSignature, newIndex));
			}
			else
			{
				layoutBlockList.add(
					new GenericImplementsInterfacesLayoutBlock(
						classFile, caSignature, newIndex));
			}
		}
		
		return displayExtendsOrImplementsFlag;
	}
	
	private static int SkipGenerics(char[] caSignature, int length, int index)
	{
		if (caSignature[index] == '<')
		{
			int depth = 1;
			
			while (index < length)
			{
				char c = caSignature[++index];
				
				if (c == '<')
				{
					depth++;
				}
				else if (c == '>')
				{
					if (depth > 1)
					{
						depth--;
					}
					else
					{
						index++;
						break;						
					}
				}
			}	
		}

		return index;
	}

	private static boolean IsObjectClass(
		char[] caSignature, int beginIndex, int endIndex)
	{
		return CharArrayUtil.Substring(caSignature, beginIndex, endIndex)
					.equals(StringConstants.INTERNAL_OBJECT_SIGNATURE);
	}
}
