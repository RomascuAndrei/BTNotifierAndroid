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

package org.holylobster.nuntius.connection;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.holylobster.nuntius.bluetooth.BluetoothConnectionProvider;
import org.holylobster.nuntius.network.NetworkConnectionProvider;
import org.holylobster.nuntius.network.SslNetworkConnectionProvider;

import org.holylobster.nuntius.notifications.NotiHandler;
import org.holylobster.nuntius.notifications.IntentRequestCodes;
import org.holylobster.nuntius.notifications.NotificationListenerService;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.holylobster.nuntius.sms.SMessage;
import org.holylobster.nuntius.sms.SmsObservable;

import java.util.concurrent.CopyOnWriteArrayList;

public final class Server extends BroadcastReceiver implements SharedPreferences.OnSharedPreferenceChangeListener, ConnectionManager, Observer {

    private static final String TAG = Server.class.getSimpleName();

    public static final boolean BLUETOOTH_ENABLED = false;

    private boolean ssl = true;

    private final List<Connection> connections = new CopyOnWriteArrayList<>();

    private BluetoothConnectionProvider bluetoothConnectionProvider;
    private NetworkConnectionProvider networkConnectionProvider;

    private Set<String> blacklistedApp;

    private int minNotificationPriority = Notification.PRIORITY_DEFAULT;

    private final NotificationListenerService context;


    public Server(NotificationListenerService context) {
        this.context = context;
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        blacklistedApp = defaultSharedPreferences.getStringSet("BlackList", new HashSet<String>());
        Log.d(TAG, "server created");
        SmsObservable.getInstance().addObserver(this);
    }

    public static Boolean bluetoothAvailable = null;

    public static boolean bluetoothEnabled() {
        return bluetoothAvailable() && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    public static boolean bluetoothAvailable() {
        if (bluetoothAvailable == null) {
            bluetoothAvailable = BluetoothAdapter.getDefaultAdapter() != null;
        }
        return bluetoothAvailable;
    }

    public void onNotificationPosted(StatusBarNotification sbn) {
        if (filter(sbn)) {
            Message message = new Message("notificationPosted", sbn);
            sendMessage(message);
        }
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (filter(sbn)) {
            Message message = new Message("notificationRemoved", sbn);
            sendMessage(message);
        }
    }

    private boolean filter(StatusBarNotification sbn) {
        Notification notification = sbn.getNotification();
        Log.d("blacklist", " " + blacklistedApp);
        return
                notification != null
                        // Filter low priority notifications
                        && notification.priority >= minNotificationPriority
                        // Notification flags
                        && !isOngoing(notification)
                        && !isLocalOnly(notification)
                        && !isBlacklisted(sbn);
    }

    private boolean isBlacklisted(StatusBarNotification sbn) {
        return blacklistedApp.contains(sbn.getPackageName());
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    private static boolean isLocalOnly(Notification notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT_WATCH) {
            return false;
        }
        boolean local = (notification.flags & Notification.FLAG_LOCAL_ONLY) != 0;
        Log.d(TAG, String.format("Notification is local: %1s", local));
        return local;

    }

    private static boolean isOngoing(Notification notification) {
        boolean ongoing = (notification.flags & Notification.FLAG_ONGOING_EVENT) != 0;
        Log.d(TAG, String.format("Notification is ongoing: %1s", ongoing));
        return ongoing;
    }

    private void sendMessage(Message message) {
        //Log.d(TAG, message.toJSON(context));
        for (Connection connection : connections) {
            boolean queued = connection.enqueue(message);
            if (!queued) {
                Log.w(TAG, "Unable to enqueue message on connection " + connection);
            }
        }
    }

    public void start() {
        Log.i(TAG, "Server starting...");
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(this, filter);

        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        boolean mustRun = defaultSharedPreferences.getBoolean("main_enable_switch", true);

        if (mustRun) {
            startAll();
        }
    }

    public void stop() {
        Log.d(TAG, "Server stopping...");

        context.unregisterReceiver(this);

        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(this);

        stopAll();
    }

    void stopAll() {
        stopBluetooth();
        stopNetwork();
        for (Connection connection : connections) {
            connection.close();
        }
        connections.clear();
    }

    public String getStatusMessage() {
        if (BLUETOOTH_ENABLED) {
            if (bluetoothEnabled() && getNumberOfConnections() == 0 ) {
                return "pair";
            } else if (bluetoothConnectionProvider != null && bluetoothConnectionProvider.isAlive()) {
                return  "connection";
            } else if (!NotificationListenerService.isNotificationAccessEnabled()) {
                return "notification";
            } else if (!bluetoothEnabled()) {
                return "bluetooth";
            } else {
                return "...";
            }
        } else {
            if (networkConnectionProvider != null && networkConnectionProvider.isAlive() && getNumberOfConnections() == 0) {
                return "pair";
            } else if (networkConnectionProvider != null && networkConnectionProvider.isAlive()) {
                return "connection";
            } else {
                return "relaunch";
            }
        }

    }

    public int getNumberOfConnections() {
        return connections.size();
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
            final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_TURNING_ON:
                    stopBluetooth();
                    break;
                case BluetoothAdapter.STATE_ON:
                    startBluetooth();
                    break;
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.i(TAG, "Changes to preference " + key);
        switch (key) {
            case "main_enable_switch":
                if (sharedPreferences.getBoolean("main_enable_switch", true)) {
                    startAll();
                } else {
                    stopAll();
                }
                break;
            case "pref_min_notification_priority":
                minNotificationPriority = Integer.parseInt(sharedPreferences.getString("pref_min_notification_priority", String.valueOf(Notification.PRIORITY_DEFAULT)));
                break;
            case "BlackList":
                blacklistedApp = sharedPreferences.getStringSet("BlackList", new HashSet<String>());
                break;
            default:
        }
    }

    void startAll() {
        if (BLUETOOTH_ENABLED) {
            startBluetooth();
        } else {
            startNetwork();
        }
    }

    private void startBluetooth() {
        if (bluetoothEnabled()) {
            bluetoothConnectionProvider = new BluetoothConnectionProvider(this);
            bluetoothConnectionProvider.start();
        } else {
            Log.i(TAG, "Bluetooth not available or enabled. Cannot start Bluetooth server");
        }
        notifyListener(getStatusMessage());
    }

    private void startNetwork() {
        if (networkAvailable()) {
            try {
                if (ssl) {
                    networkConnectionProvider = new SslNetworkConnectionProvider(this, new File(context.getFilesDir(), "custom.bks"));
                } else {
                    networkConnectionProvider = new NetworkConnectionProvider(this);
                }
                networkConnectionProvider.start();
            } catch (CertificateException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
                Log.e(TAG, "Error creating SSL server", e);
            }
        }
        notifyListener(getStatusMessage());
    }

    private boolean networkAvailable() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        //NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        //return mWifi.isConnected();

        if (connManager.getActiveNetworkInfo() != null && connManager.getActiveNetworkInfo().isAvailable() && connManager.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    private void stopBluetooth() {
        Log.i(TAG, "Stopping server thread.");
        if (bluetoothConnectionProvider != null) {
            bluetoothConnectionProvider.close();
            Log.i(TAG, "Bluetooth Server thread stopped.");
        } else {
            Log.i(TAG, "Bluetooth Server thread already stopped.");
        }

        notifyListener(getStatusMessage());
    }

    private void stopNetwork() {
        if (networkConnectionProvider != null) {
            networkConnectionProvider.close();
            Log.i(TAG, "Network Server thread stopped.");
        } else {
            Log.i(TAG, "Network Server thread already stopped.");
        }

        notifyListener(getStatusMessage());
    }

    private void notifyListener(String status) {
        Intent intent = new Intent(IntentRequestCodes.INTENT_SERVER_STATUS_CHANGE);
        intent.putExtra("status", status);
        Log.d(TAG, "Sending server status change: " + status);
        context.sendBroadcast(intent);
    }

    public void newConnection(Socket socket) {
        NotiHandler notiHandler = new NotiHandler() {
            @Override
            public void onMessageReceived(IncomingMessage message) {
                Log.d(TAG, "Message received: " + message);
                try {
                    message.getEventType().manageEvent(context, message.getMsg());
                } catch (IOException e) {
                    Log.e(TAG,"Error when parsing the message\n" + e.getMessage());
                }

            }

            @Override
            public void onConnectionClosed(Connection connection) {
                connections.remove(connection);
                Log.i(TAG, ">>Connection closed (" + connection.getDestination() + ")");
                notifyListener(getStatusMessage());
            }
        };
        connections.add(new Connection(context, socket, notiHandler));
        notifyListener(getStatusMessage());
    }

    public String getContactName(String phoneNumber) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if(cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }

        if(!cursor.isClosed()) {
            cursor.close();
        }
        return contactName;
    }

    @Override
    public void update(Observable observable, Object data) {
        if (data instanceof SMessage) {
            SMessage sMessage = (SMessage) data;

            sMessage.setSender(getContactName(sMessage.getSenderNum()));
            sendMessage(new Message(sMessage));
        }
    }
}