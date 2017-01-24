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

package org.holylobster.nuntius.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import org.holylobster.nuntius.connection.ConnectionManager;
import org.holylobster.nuntius.connection.ConnectionProvider;
import org.holylobster.nuntius.connection.Socket;

import java.io.IOException;
import java.util.UUID;

public class BluetoothConnectionProvider implements ConnectionProvider {

    private static final String TAG = BluetoothConnectionProvider.class.getSimpleName();

    public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";

    private static final UUID uuidSpp = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private final ConnectionManager connectionManager;

    private BluetoothServerSocket serverSocket;

    private final Thread thread;

    public BluetoothConnectionProvider(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.thread = new Thread() {
            public void run() {
                Log.i(TAG, "Listen server started");

                BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();

                try {
                    serverSocket = btAdapter.listenUsingRfcommWithServiceRecord(PROTOCOL_SCHEME_RFCOMM, uuidSpp);
                    Log.d(TAG, "Server socket created");

                    while (serverSocket != null && btAdapter.isEnabled()) {
                        try {
                            BluetoothSocket bluetoothSocket = serverSocket.accept();
                            Socket socket = new BluetoothSocketAdapter(bluetoothSocket);
                            Log.i(TAG, ">>Connection opened (" + socket.getDestination() + ")");
                            BluetoothConnectionProvider.this.connectionManager.newConnection(socket);
                        } catch (IOException e) {
                            Log.e(TAG, "Error during accept", e);
                            Log.i(TAG, "Waiting 5 seconds before accepting again...");
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e1) {
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error in listenUsingRfcommWithServiceRecord", e);
                }
                Log.i(TAG, "Listen server stopped");
            }
        };
    }

    public void close() {
        thread.interrupt();
        if (serverSocket != null) {
            Log.i(TAG, "Closing server listening socket...");
            try {
                serverSocket.close();
                Log.i(TAG, "Server listening socket closed.");
            } catch (IOException e) {
                Log.e(TAG, "Unable to close server socket", e);
            } finally {
                serverSocket = null;
            }
        }
    }

    @Override
    public void start() {
        thread.start();
    }

    @Override
    public boolean isAlive() {
        return thread.isAlive();
    }
}
