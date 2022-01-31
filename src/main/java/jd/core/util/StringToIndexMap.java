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

public class StringToIndexMap
{
    private static final int INITIAL_CAPACITY = 128*2;

    private final HashEntry[] entries;

    public StringToIndexMap()
    {
        this.entries = new HashEntry[INITIAL_CAPACITY];
    }

    public void put(String key, int value)
    {
        int hashCode = key.hashCode();
        int index = hashCodeToIndex(hashCode, this.entries.length);
        HashEntry entry = this.entries[index];

        while (entry != null)
        {
            if (entry.hash == hashCode && key.equals(entry.key))
            {
                entry.value = value;
                return;
            }
            entry = entry.next;
        }

        this.entries[index] =
            new HashEntry(key, hashCode, value, this.entries[index]);
    }

    public int get(String key)
    {
        int hashCode = key.hashCode();
        int index = hashCodeToIndex(hashCode, this.entries.length);
        HashEntry entry = this.entries[index];

         while (entry != null)
         {
             if (entry.hash == hashCode && key.equals(entry.key)) {
                return entry.value;
            }
            entry = entry.next;
         }

         return -1;
    }

    private static int hashCodeToIndex(int hash, int size)
    {
        return hash & size - 1;
    }

    private static class HashEntry
    {
        private final String key;
        private final int hash;
        private int value;
        private final HashEntry next;

        public HashEntry(String key, int hash, int value, HashEntry next)
        {
            this.key = key;
            this.hash = hash;
            this.value = value;
            this.next = next;
        }
    }
}
