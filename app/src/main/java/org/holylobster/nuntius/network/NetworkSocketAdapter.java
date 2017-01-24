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

package org.holylobster.nuntius.network;

import org.holylobster.nuntius.connection.Socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class NetworkSocketAdapter implements Socket {

    private final java.net.Socket networkSocket;

    public NetworkSocketAdapter(java.net.Socket networkSocket) {
        this.networkSocket = networkSocket;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return networkSocket.getOutputStream();
    }

    @Override
    public boolean isConnected() {
        return networkSocket.isConnected();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return networkSocket.getInputStream();
    }

    @Override
    public void close() throws IOException {
        networkSocket.close();
    }

    @Override
    public String getDestination() {
        return networkSocket.getInetAddress().getHostAddress();
    }
}
