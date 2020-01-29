/*
 * Copyright (C) 2015 The CyanogenMod Project
 *               2017 The LineageOS Project
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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.core.content.ContextCompat;

public class ServiceUpdateReceiver extends BroadcastReceiver {
    private static final String TAG = "O-Click";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (lineageos.content.Intent.ACTION_INITIALIZE_LINEAGE_HARDWARE.equals(action)) {
            updateOClickServiceState(context);
        } else if (intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            updateOClickServiceState(context);
        }
    }

    private void updateOClickServiceState(Context context) {
        BluetoothManager btManager = (BluetoothManager)
                context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = btManager.getAdapter();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean shouldStartService = adapter != null
                && adapter.getState() == BluetoothAdapter.STATE_ON
                && prefs.contains(PreferenceKeys.DEVICE_ADDRESS);
        Intent serviceIntent = new Intent(context, OclickService.class);

        if (shouldStartService) {
            ContextCompat.startForegroundService(context, serviceIntent);
        } else {
            context.stopService(serviceIntent);
        }
    }
}
