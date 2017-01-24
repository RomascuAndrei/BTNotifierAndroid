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

import android.bluetooth.BluetoothSocket;

import org.holylobster.nuntius.connection.Socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BluetoothSocketAdapter implements Socket {

    private final BluetoothSocket bluetoothSocket;

    public BluetoothSocketAdapter(BluetoothSocket bluetoothSocket) {
        this.bluetoothSocket = bluetoothSocket;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return bluetoothSocket.getOutputStream();
    }

    @Override
    public boolean isConnected() {
        return bluetoothSocket.isConnected();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return bluetoothSocket.getInputStream();
    }

    @Override
    public void close() throws IOException {
        bluetoothSocket.close();
    }

    @Override
    public String getDestination() {
        return bluetoothSocket.getRemoteDevice().getAddress();
    }
}
