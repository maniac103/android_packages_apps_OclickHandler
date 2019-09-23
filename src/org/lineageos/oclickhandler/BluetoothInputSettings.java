/*
 * Copyright (C) 2015 The CyanogenMod Project
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

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation")
public class BluetoothInputSettings extends FragmentActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bt_settings);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_frame, new SettingsFragment())
                    .commit();
        }

        final Toolbar toolbar = findViewById(R.id.action_bar);
        setActionBar(toolbar);
        getActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        private static final int BLUETOOTH_REQUEST_CODE = 1;
        private static final int LOCATION_PERM_REQUEST_CODE = 2;
        private static final String CATEGORY_ACTIONS = "oclick_action_category";
        private static final String CATEGORY_ALERT = "oclick_alert_category";

        private ProgressDialog mProgressDialog;
        private boolean mConnected;
        private Handler mHandler = new Handler();
        private BluetoothManager mBtManager;
        private BluetoothAdapter mAdapter;
        private SharedPreferences mPrefs;

        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String address = mPrefs.getString(PreferenceKeys.DEVICE_ADDRESS, null);
                if (device != null && TextUtils.equals(address, device.getAddress())) {
                    updateConnectedState();
                }
            }
        };

        private ScanCallback mScanCallback = new ScanCallback() {
            @Override
            public void onScanFailed(int errorCode) {
                stopScanning();
            }

            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                handleScanResult(result);
            }
        };

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mBtManager = (BluetoothManager) getContext().getSystemService(BLUETOOTH_SERVICE);
            mAdapter = mBtManager.getAdapter();
            mPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.oclick_panel);
        }

        @Override
        public void onResume() {
            super.onResume();

            updateConnectedState();

            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
            filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
            getContext().registerReceiver(mReceiver, filter);
        }

        @Override
        public void onPause() {
            super.onPause();
            getContext().unregisterReceiver(mReceiver);
        }

        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            if (!preference.getKey().equals(PreferenceKeys.CONNECT)) {
                return super.onPreferenceTreeClick(preference);
            }
            if (mConnected) {
                mPrefs.edit().remove(PreferenceKeys.DEVICE_ADDRESS).apply();
                getContext().stopService(new Intent(getContext(), OclickService.class));
                updateConnectedState();
            } else if (!mAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, BLUETOOTH_REQUEST_CODE);
            } else {
                askForPermissionOrScan(false);
            }
            return true;
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                String[] permissions, int[] grantResults) {
            if (requestCode == LOCATION_PERM_REQUEST_CODE
                    && grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == BLUETOOTH_REQUEST_CODE && resultCode == RESULT_OK) {
                startScanning();
            }
        }

        public void askForPermissionOrScan(boolean skipRationale) {
            if (ContextCompat.checkSelfPermission(getActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startScanning();
                return;
            }

            boolean shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
                    getActivity(), Manifest.permission.ACCESS_FINE_LOCATION);
            if (shouldShowRationale && !skipRationale) {
                    DialogFragment f = new LocationPermissionRationaleFragment();
                    f.setTargetFragment(this, 0);
                    f.show(getFragmentManager(), "location_rationale");
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                        LOCATION_PERM_REQUEST_CODE);
            }
        }

        private void startScanning() {
            BluetoothLeScanner scanner = mAdapter.getBluetoothLeScanner();
            ScanSettings settings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();

            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            // O-Click 1
            filters.add(new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(OclickService.TRIGGER_SERVICE_UUID))
                    .build());
            // O-Click 2
            filters.add(new ScanFilter.Builder()
                    .setServiceUuid(new ParcelUuid(OclickService.OCLICK2_SERVICE_UUID))
                    .build());

            scanner.startScan(filters, settings, mScanCallback);

            String dialogTitle = this.getString(R.string.oclick_dialog_title);
            String dialogMessage = this.getString(R.string.oclick_dialog_connecting_message);
            mProgressDialog = ProgressDialog.show(getContext(), dialogTitle, dialogMessage, true);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopScanning();
                }
            }, 10000);
        }

        private void stopScanning() {
            BluetoothLeScanner scanner = mAdapter.getBluetoothLeScanner();
            scanner.stopScan(mScanCallback);
            if (mProgressDialog != null) {
                mProgressDialog.dismiss();
                mProgressDialog = null;
            }
        }

        private void handleScanResult(ScanResult result) {
            stopScanning();
            String address = result.getDevice().getAddress();
            mPrefs.edit().putString(PreferenceKeys.DEVICE_ADDRESS, address).apply();
            getContext().startService(new Intent(getContext(), OclickService.class));
            updateConnectedState();
        }

        private boolean isBluetoothDeviceConnected(String address) {
            BluetoothDevice device = mAdapter.getRemoteDevice(address);
            int state = mBtManager.getConnectionState(device, BluetoothProfile.GATT);
            return state == BluetoothProfile.STATE_CONNECTED;
        }

        private void updateConnectedState() {
            String address = mPrefs.getString(PreferenceKeys.DEVICE_ADDRESS, null);
            mConnected = !TextUtils.isEmpty(address);

            findPreference(CATEGORY_ACTIONS).setEnabled(mConnected);
            findPreference(CATEGORY_ALERT).setEnabled(mConnected);

            Preference connectPref = findPreference(PreferenceKeys.CONNECT);
            connectPref.setTitle(mConnected ?
                    R.string.oclick_disconnect_string : R.string.oclick_connect_string);
            if (mConnected && isBluetoothDeviceConnected(address)) {
                connectPref.setSummary(R.string.oclick_summary_connected);
            } else if (mConnected) {
                connectPref.setSummary(R.string.oclick_summary_paired);
            } else {
                connectPref.setSummary(null);
            }
        }
    }

    public static class LocationPermissionRationaleFragment extends DialogFragment
            implements DialogInterface.OnClickListener {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Rationale dialog should not be cancelable
            setCancelable(false);

            return new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.retry, this)
                .setMessage(R.string.location_permission_rationale)
                .create();
        }

        @Override
        public void onClick(DialogInterface dialog, int which) {
            ((SettingsFragment) getTargetFragment()).askForPermissionOrScan(true);
        }
    }
}
