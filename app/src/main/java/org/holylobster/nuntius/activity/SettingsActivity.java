/*
 * Copyright (C) 2015 - Holy Lobster
 *
 * Nuntius is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Nuntius is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Nuntius. If not, see <http://www.gnu.org/licenses/>.
 */

package org.holylobster.nuntius.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import org.apache.http.conn.util.InetAddressUtils;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.holylobster.nuntius.R;
import org.holylobster.nuntius.connection.Server;
import org.holylobster.nuntius.notifications.IntentRequestCodes;
import org.holylobster.nuntius.notifications.NotificationListenerService;
import org.holylobster.nuntius.utils.PairingData;
import org.holylobster.nuntius.utils.SslUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;


public class SettingsActivity extends ActionBarActivity {

    private static final String TAG = SettingsActivity.class.getSimpleName();

    private static Context context;

    private static PairingData currentPairingData;

    AdView mAdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)        // All emulators
                .addTestDevice("E9DEB34031182776A4E765DCEF19F10D")  // My phone
                .build();
        mAdView.loadAd(adRequest);


        try {
            SslUtils.generateSelfSignedCertificate();
        } catch (Exception e) {
            Log.e(TAG, "Unable to secure network connection. Certificate creation failed", e) ;
            Toast.makeText(context, "Unable to secure network connection. Certificate creation failed", Toast.LENGTH_SHORT).show();
            throw new RuntimeException();
        }

        android.support.v7.widget.Toolbar toolbar = (android.support.v7.widget.Toolbar) findViewById(R.id.settingstoolbar);
        setSupportActionBar(toolbar);

        // Display the fragment as the main content.
        getFragmentManager()
                .beginTransaction()
                .replace(R.id.content_frame, new SettingsFragment())
                .commit();



    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            String contents = scanResult.getContents();
            if (contents != null) {
                Log.i(TAG, "" + contents);
                currentPairingData = new PairingData(contents);
            }
        }
    }

    public static PairingData getCurrentPairingData() {
        PairingData p = currentPairingData;
        currentPairingData = null;
        return p;
    }

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
        private BroadcastReceiver br;
        private InterstitialAd mInterstitialAd;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_general);

            mInterstitialAd = new InterstitialAd(getActivity());
            mInterstitialAd.setAdUnitId("ca-app-pub-6887589184636373/9378703641");
            AdRequest adRequestInterstial = new AdRequest.Builder()
                    .addTestDevice("E9DEB34031182776A4E765DCEF19F10D")
                    .build();
            mInterstitialAd.loadAd(adRequestInterstial);

//listner for adClosed
            mInterstitialAd.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    AdRequest adRequest = new AdRequest.Builder()
                            .addTestDevice("E9DEB34031182776A4E765DCEF19F10D")
                            .build();
                    mInterstitialAd.loadAd(adRequest);
                }
            });



            Preference ip = findPreference("ip");
            PreferenceScreen screen = getPreferenceScreen();
            if (Server.BLUETOOTH_ENABLED) {
                screen.removePreference(ip);

            } else {
                ip.setSummary(getLocalIpAddress());
            }
            Preference myPref = (Preference) findPreference("qrcode");
            myPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                public boolean onPreferenceClick(Preference preference) {
                    IntentIntegrator integrator = new IntentIntegrator(getActivity());
                    integrator.initiateScan();
                    return true;
                }
            });



        }

        @Override
        public void onResume() {
            super.onResume();

            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

            IntentFilter filter = new IntentFilter();
            filter.addAction(IntentRequestCodes.INTENT_SERVER_STATUS_CHANGE);
            br = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mInterstitialAd.show();
                    String status = intent.getStringExtra("status");
                    Log.d(TAG, "Received server status change: " + status);
                    updatePreference(findPreference("main_enable_switch"));

                }
            };
            getActivity().registerReceiver(br, filter);

            for (int i = 0; i < getPreferenceScreen().getPreferenceCount(); ++i) {
                Preference preference = getPreferenceScreen().getPreference(i);
                if (preference instanceof PreferenceGroup) {
                    PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                    for (int j = 0; j < preferenceGroup.getPreferenceCount(); ++j) {
                        updatePreference(preferenceGroup.getPreference(j));
                    }
                } else {
                    updatePreference(preference);
                }
            }
        }



        public String getLocalIpAddress() {
            try {
                for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                    NetworkInterface intf = en.nextElement();
                    for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                        InetAddress inetAddress = enumIpAddr.nextElement();
                        if (!inetAddress.isLoopbackAddress() && InetAddressUtils.isIPv4Address(inetAddress.getHostAddress())) {
                            return inetAddress.getHostAddress();
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e("IP Address", ex.toString());
            }
            return null;
        }

        private void updatePreference(Preference preference) {
            if (preference.getKey().equals("main_enable_switch")) {
                if (preference.getSharedPreferences().getBoolean("main_enable_switch", true)) {
                    updateSummary(preference);
                }
            } else if (preference.getKey().equals("version")) {
                PackageInfo packageInfo = getPackageInfo();
                preference.setSummary(String.format("v%s (%d)", packageInfo.versionName, packageInfo.versionCode));
            }
        }

        public PackageInfo getPackageInfo() {
            try {
                return getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            } catch (PackageManager.NameNotFoundException e) {
                // "My first four books, from 'Fight Club' to 'Choke,' dealt with personal identity issues.
                // The crises the narrators found themselves in were generated by themselves."
                //   Chuck Palahniuk
                throw new RuntimeException(e);
            }
        }

        private void updateSummary(Preference preference) {
            String summary;
            if (NotificationListenerService.server != null) {
                String message = NotificationListenerService.server.getStatusMessage();
                switch (message){
                    case "connection":
                        int connections = NotificationListenerService.server.getNumberOfConnections();
                        summary = getResources().getQuantityString(R.plurals.running_with_x_connections, connections, connections);
                        break;
                    case "notification":
                        summary = getString(R.string.notification_not_enabled);
                        break;
                    case "bluetooth":
                        summary = getString(R.string.bluetooth_not_enabled);
                        break;
                    case "pair":
                        summary = getString(R.string.not_paired);
                        break;
                    case "relaunch":
                        summary = getString(R.string.relaunch_server);
                        break;
                    default:
                        summary = "...";
                        break;
                }
            } else if (!NotificationListenerService.isNotificationAccessEnabled()) {
                summary = getString(R.string.notification_not_enabled);
            } else {
                summary = "Something went wrong...";
            }
            preference.setSummary(summary);
        }

        @Override
        public void onPause() {
            super.onPause();
            getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            getActivity().unregisterReceiver(br);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Preference preference = findPreference(key);
            updatePreference(preference);
            if (preference.getKey().equals("main_enable_switch")) {
                if (preference.getSharedPreferences().getBoolean("main_enable_switch", true)) {
                    if (Server.BLUETOOTH_ENABLED) {
                        if (!Server.bluetoothAvailable()) {
                            Toast.makeText(getActivity(), getString(R.string.bluetooth_not_available), Toast.LENGTH_LONG).show();
                        }
                        else if (!Server.bluetoothEnabled()) {
                            if (!requestEnableBluetooth()) {
                                Toast.makeText(getActivity(), getString(R.string.bluetooth_not_enabled), Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }

                    if (!NotificationListenerService.isNotificationAccessEnabled()) {
                        new AskNotificationAccessDialogFragment().show(getFragmentManager(), "NoticeDialogFragment");
                    }
                    updatePreference(preference);
                }
            }
        }

        public boolean requestEnableBluetooth() {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, IntentRequestCodes.BT_REQUEST_ENABLE);
            return true;
        }

    }

    protected boolean isValidFragment(String fragmentName) {
        return SettingsFragment.class.getName().equals(fragmentName);
    }

    public static class AskNotificationAccessDialogFragment extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.dialog_ask_notification_access)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Toast.makeText(getActivity(), getString(R.string.enable_notification_toast_hint), Toast.LENGTH_LONG).show();
                            getActivity().startActivity((new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")));
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
            // Create the AlertDialog object and return it
            return builder.create();
        }
    }

    @Override
    protected void onPause() {
        mAdView.pause();
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(mAdView!=null){  // Check if Adview is not null in case of fist time load.
            mAdView.resume();}
    }

}
