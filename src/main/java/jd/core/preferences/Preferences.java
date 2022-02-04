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
package jd.core.preferences;

import java.util.Map;

public final class Preferences {

    public static final String JD_CORE_VERSION             = "JdGuiPreferences.jdCoreVersion";
    public static final String DISPLAY_DEFAULT_CONSTRUCTOR = "ClassFileViewerPreferences.displayDefaultConstructor";
    public static final String WRITE_LINE_NUMBERS          = "ClassFileSaverPreferences.writeLineNumbers";
    public static final String WRITE_METADATA              = "ClassFileSaverPreferences.writeMetadata";
    public static final String ESCAPE_UNICODE_CHARACTERS   = "ClassFileSaverPreferences.escapeUnicodeCharacters";
    public static final String OMIT_THIS_PREFIX            = "ClassFileSaverPreferences.omitThisPrefix";
    public static final String WRITE_DEFAULT_CONSTRUCTOR   = "ClassFileSaverPreferences.writeDefaultConstructor";
    public static final String REALIGN_LINE_NUMBERS        = "ClassFileSaverPreferences.realignLineNumbers";

    private boolean showDefaultConstructor;
    private boolean realignmentLineNumber;
    private boolean showPrefixThis;
    private boolean unicodeEscape;
    private boolean showLineNumbers;
    private boolean writeMetaData;

    public Preferences() {
        this.showDefaultConstructor = false;
        this.realignmentLineNumber = true;
        this.showPrefixThis = true;
        this.unicodeEscape = false;
        this.showLineNumbers = true;
        this.writeMetaData = true;
    }

    public Preferences(Map<String, String> preferences) {
        setUnicodeEscape(Boolean.parseBoolean(preferences.getOrDefault(ESCAPE_UNICODE_CHARACTERS, Boolean.FALSE.toString())));
        setShowPrefixThis(!Boolean.parseBoolean(preferences.getOrDefault(OMIT_THIS_PREFIX, Boolean.FALSE.toString())));
        setShowDefaultConstructor(Boolean.parseBoolean(preferences.getOrDefault(DISPLAY_DEFAULT_CONSTRUCTOR, Boolean.FALSE.toString())));
        setRealignmentLineNumber(Boolean.parseBoolean(preferences.getOrDefault(REALIGN_LINE_NUMBERS, Boolean.FALSE.toString())));
        setShowLineNumbers(Boolean.parseBoolean(preferences.getOrDefault(WRITE_LINE_NUMBERS, Boolean.FALSE.toString())));
        setWriteMetaData(Boolean.parseBoolean(preferences.getOrDefault(WRITE_METADATA, Boolean.FALSE.toString())));
    }

    public boolean getShowDefaultConstructor() {
        return this.showDefaultConstructor;
    }

    public boolean getRealignmentLineNumber() {
        return this.realignmentLineNumber;
    }

    public void setShowDefaultConstructor(boolean b) {
        showDefaultConstructor = b;
    }

    public void setRealignmentLineNumber(boolean b) {
        realignmentLineNumber = b;
    }

    public void setShowPrefixThis(boolean b) {
        showPrefixThis = b;
    }

    public void setUnicodeEscape(boolean b) {
        unicodeEscape = b;
    }

    public void setShowLineNumbers(boolean b) {
        showLineNumbers = b;
    }

    public boolean isShowPrefixThis() {
        return showPrefixThis;
    }

    public boolean isUnicodeEscape() {
        return unicodeEscape;
    }

    public boolean isShowLineNumbers() {
        return showLineNumbers;
    }

    public boolean isWriteMetaData() {
        return writeMetaData;
    }

    public void setWriteMetaData(boolean writeMetaData) {
        this.writeMetaData = writeMetaData;
    }
}
