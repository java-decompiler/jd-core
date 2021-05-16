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
package jd.core.model.classfile.accessor;

import java.util.List;


public class InvokeMethodAccessor extends Accessor 
{
	final public String className;
	final public int methodOpcode;
	final public String methodName;
	final public String methodDescriptor;
	final public List<String> listOfParameterSignatures;
	final public String returnedSignature;

	public InvokeMethodAccessor(
		byte tag, String className, int methodOpcode, 
		String methodName, String methodDescriptor,
		List<String> listOfParameterSignatures, String returnedSignature)
	{
		super(tag);
		this.className = className;
		this.methodOpcode = methodOpcode;
		this.methodName = methodName;
		this.methodDescriptor = methodDescriptor;
		this.listOfParameterSignatures = listOfParameterSignatures;
		this.returnedSignature = returnedSignature;
	}
}
