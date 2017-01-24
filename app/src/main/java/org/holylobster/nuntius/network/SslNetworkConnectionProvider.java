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

import org.holylobster.nuntius.connection.ConnectionManager;
import org.holylobster.nuntius.utils.SslUtils;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

public class SslNetworkConnectionProvider extends NetworkConnectionProvider {

    private static final String TAG = SslNetworkConnectionProvider.class.getSimpleName();

    private final File trustStore;

    public SslNetworkConnectionProvider(ConnectionManager connectionManager, File trustStore) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        super(connectionManager);
        this.trustStore = trustStore;
    }

    @Override
    public ServerSocket getServerSocket() throws Exception {
        SSLContext sslContext = SslUtils.getSSLContext(trustStore);
        SSLServerSocketFactory serverSocketFactory = sslContext.getServerSocketFactory();
        SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port);
        serverSocket.setNeedClientAuth(true);
        return serverSocket;
    }

}
