/*
 * Copyright (C) 2016 The CyanogenMod Project
 * Copyright (C) 2017 The LineageOS Project
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

package org.lineageos.oclickhandler;

import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.provider.SearchIndexableResource;
import android.provider.SearchIndexablesProvider;

import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_CLASS_NAME;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_ICON_RESID;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_ACTION;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RANK;
import static android.provider.SearchIndexablesContract.COLUMN_INDEX_XML_RES_RESID;
import static android.provider.SearchIndexablesContract.INDEXABLES_RAW_COLUMNS;
import static android.provider.SearchIndexablesContract.INDEXABLES_XML_RES_COLUMNS;
import static android.provider.SearchIndexablesContract.NON_INDEXABLES_KEYS_COLUMNS;

public class OclickSearchIndexablesProvider extends SearchIndexablesProvider {
    private static final String TAG = "OclickSearchIndexablesProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public Cursor queryXmlResources(String[] projection) {
        Object[] ref = new Object[7];
        ref[COLUMN_INDEX_XML_RES_RANK] = 1;
        ref[COLUMN_INDEX_XML_RES_RESID] = R.xml.oclick_panel;
        ref[COLUMN_INDEX_XML_RES_CLASS_NAME] = null;
        ref[COLUMN_INDEX_XML_RES_ICON_RESID] = R.drawable.ic_oclick_notification;
        ref[COLUMN_INDEX_XML_RES_INTENT_ACTION] = Intent.ACTION_MAIN;
        ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_PACKAGE] = getContext().getPackageName();
        ref[COLUMN_INDEX_XML_RES_INTENT_TARGET_CLASS] = BluetoothInputSettings.class.getName();

        MatrixCursor cursor = new MatrixCursor(INDEXABLES_XML_RES_COLUMNS);
        cursor.addRow(ref);
        return cursor;
    }

    @Override
    public Cursor queryRawData(String[] projection) {
        return new MatrixCursor(INDEXABLES_RAW_COLUMNS);
    }

    @Override
    public Cursor queryNonIndexableKeys(String[] projection) {
        return new MatrixCursor(NON_INDEXABLES_KEYS_COLUMNS);
    }
}
