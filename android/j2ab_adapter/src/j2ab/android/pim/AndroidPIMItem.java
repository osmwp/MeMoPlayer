/*
 * Copyright (C) 2009 The J2AB Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package j2ab.android.pim;

import java.util.Map;

import javax.microedition.pim.PIMItem;

import android.database.Cursor;

public class AndroidPIMItem implements PIMItem {
    private Cursor cursor;
    private Map<Integer, String> columnMappings;

    public AndroidPIMItem(Cursor cursor, Map<Integer, String> columnMappings) {
        this.cursor = cursor;
        this.columnMappings = columnMappings;

    }

    // @Override
    public int countValues (int field) {
        return 1;
    }

    // @Override
    public int getAttributes (int field, int index) {
        return 0;
    }

    public long getDate (String columnName) {
        return this.cursor.getLong (this.cursor.getColumnIndex (columnName));
    }

    // @Override
    public long getDate (int field, int index) {
        return getDate (getColumnName (field));
    }

    public String getString (String columnName) {
        return this.cursor.getString (this.cursor.getColumnIndex (columnName));
    }

    // @Override
    public String getString (int field, int index) {
        return this.getString (getColumnName (field));
    }

    public String[] getStringArray (String columnName) {
        return new String[] { getString (columnName) };
    }

    // @Override
    public String[] getStringArray (int field, int index) {
        return getStringArray (this.getColumnName (field));
    }

    protected String getColumnName (int field) {
        return this.columnMappings.get (field);
    }

    protected int getColumnIndex (int field) {
        String columnName = getColumnName (field);
        if (columnName != null) {
            return this.cursor.getColumnIndex (columnName);
        } else {
            throw new NullPointerException ("no column name for field " + field);
        }
    }
}
