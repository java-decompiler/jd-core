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
    protected ByteCodeConstants() {
        super();
    }

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


    public static final int CMP_MAX_INDEX = 7;

    public static final int CMP_EQ  = 0;
    public static final int CMP_LT  = 1;
    public static final int CMP_GT  = 2;
    public static final int CMP_UEQ = 3; // NO_UCD (unused code)
    public static final int CMP_UNE = 4; // NO_UCD (unused code)
    public static final int CMP_LE  = 5;
    public static final int CMP_GE  = 6;
    public static final int CMP_NE  = 7;

}
