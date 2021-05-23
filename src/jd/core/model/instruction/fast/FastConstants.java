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
package jd.core.model.instruction.fast;

import jd.core.model.instruction.bytecode.ByteCodeConstants;

public class FastConstants extends ByteCodeConstants
{
	// Fast opcode
	public static final int WHILE              = 301;
	public static final int DO_WHILE           = 302;
	public static final int INFINITE_LOOP      = 303;
	public static final int FOR                = 304;
	public static final int FOREACH            = 305;
	
	public static final int IF_                = 306;
	public static final int IF_ELSE            = 307;
	
	public static final int IF_CONTINUE        = 308;
	public static final int IF_BREAK           = 309;
	public static final int IF_LABELED_BREAK   = 310;
	public static final int GOTO_CONTINUE      = 311;
	public static final int GOTO_BREAK         = 312;
	public static final int GOTO_LABELED_BREAK = 313;
	
	public static final int SWITCH             = 314;
	public static final int SWITCH_ENUM        = 315;
	public static final int SWITCH_STRING      = 316;
	public static final int DECLARE            = 317;
	public static final int TRY                = 318;
	public static final int SYNCHRONIZED       = 319;
	
	public static final int LABEL              = 320;
	
	public static final int ENUMVALUE          = 321;
	
	// Type of try blocks 
	public static final int TYPE_UNDEFINED                 = 0;
	public static final int TYPE_CATCH                     = 1;
	public static final int TYPE_118_FINALLY               = 2;
	public static final int TYPE_118_FINALLY_2             = 3;
	public static final int TYPE_118_FINALLY_THROW         = 4;
	public static final int TYPE_118_SYNCHRONIZED          = 5;
	public static final int TYPE_118_SYNCHRONIZED_DOUBLE   = 6;
	public static final int TYPE_118_CATCH_FINALLY         = 7;
	public static final int TYPE_118_CATCH_FINALLY_2       = 8;
	public static final int TYPE_131_CATCH_FINALLY         = 9;
	public static final int TYPE_142                       = 10;
	public static final int TYPE_142_FINALLY_THROW         = 11;
	// Jikes & Hors Eclipse
	public static final int TYPE_JIKES_122                 = 12;
	// Dans Eclipse
	public static final int TYPE_ECLIPSE_677_FINALLY       = 13; 
	public static final int TYPE_ECLIPSE_677_CATCH_FINALLY = 14;
	

	public static final String LABEL_PREFIX = "label";
}
