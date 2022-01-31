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
package jd.core.util;

public final class StringUtil
{
    private StringUtil() {
        super();
    }

    private static void escapeChar(StringBuilder sb, char c)
    {
        switch (c)
        {
        case '\\':
            sb.append("\\\\");
            break;
        case '\b':
            sb.append("\\b");
            break;
        case '\f':
            sb.append("\\f");
            break;
        case '\n':
            sb.append("\\n");
            break;
        case '\r':
            sb.append("\\r");
            break;
        case '\t':
            sb.append("\\t");
            break;
        default:
            if (c < ' ')
            {
                sb.append("\\0");
                sb.append((char)('0' + (c >> 3)));
                sb.append((char)('0' + (c & 7)));
            }
            else
            {
                sb.append(c);
            }
        }
    }

    public static String escapeStringAndAppendQuotationMark(String s)
    {
        int length = s.length();
        StringBuilder sb = new StringBuilder(length * 2 + 2);

        sb.append('"');

        if (length > 0)
        {
            for (int i=0; i<length; i++)
            {
                if (s.charAt(i) == '"') {
                    sb.append("\\\"");
                } else {
                    escapeChar(sb, s.charAt(i));
                }
            }
        }

        sb.append('"');

        return sb.toString();
    }

    public static String escapeCharAndAppendApostrophe(char c)
    {
        StringBuilder sb = new StringBuilder(10);

        sb.append('\'');

        if (c == '\'') {
            sb.append("\\'");
        } else {
            escapeChar(sb,  c);
        }

        sb.append('\'');

        return sb.toString();
    }
}
