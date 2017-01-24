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

package org.holylobster.nuntius.utils;

import android.util.Log;

import org.holylobster.nuntius.activity.SettingsActivity;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.cert.X509CertificateHolder;
import org.spongycastle.cert.X509v3CertificateBuilder;
import org.spongycastle.cert.jcajce.JcaX509CertificateConverter;
import org.spongycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by andreac on 25/03/15.
 */
public class SslUtils {

    private static final Provider PROVIDER = new BouncyCastleProvider();

    /** Current time minus 1 year, just in case software clock goes back due to time synchronization */
    static final Date NOT_BEFORE = new Date(System.currentTimeMillis() - 86400000L * 365);
    /** The maximum possible value in X.509 specification: 9999-12-31 23:59:59 */
    static final Date NOT_AFTER = new Date(253402300799000L);

    private static final char[] pwd = null;

    private static final String TAG = SslUtils.class.getSimpleName();

    public static SSLContext getSSLContext(File myTrustStore) throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(
                new KeyManager[] { new MyX509KeyManager() },
                new TrustManager[] { new MyX509TrustManager(myTrustStore) },
                null
        );
        return sslContext;
    }

    private static class MyX509TrustManager implements X509TrustManager {

        private final File trustStorePath;
        private X509TrustManager trustManager;

        public MyX509TrustManager(File trustStorePath) throws Exception {
            this.trustStorePath = trustStorePath;
            reloadTrustManager();
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            Log.i(TAG, "Checking client certificate chain " + chain);

            try {
                trustManager.checkClientTrusted(chain, authType);
            } catch (CertificateException e) {
                if (pairAndTrust(chain[0])) {
                    trustManager.checkClientTrusted(chain, authType);
                } else {
                    throw e;
                }
            }
        }

        private boolean pairAndTrust(X509Certificate cert) {
            PairingData pairingData = SettingsActivity.getCurrentPairingData();
            if (pairingData == null) {
                return false;
            }

            try {
                byte[] fingerprint = fingerprint(cert);
                byte[] bytes = pairingData.getFingerprint();

                if (Arrays.equals(fingerprint, bytes)) {
                    Log.i(TAG, "The fingerprint matches!");
                    trustCertificate(cert, pairingData.getDeviceLabel());
                    reloadTrustManager();
                    return true;
                } else {
                    Log.e(TAG, "The fingerprint does NOT match!");
                }

            } catch (Exception e) {
                Log.e(TAG, "Error trying to pair a new certificate", e);
            }
            return false;
        }

        private void trustCertificate(Certificate cert, String deviceLabel) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
            KeyStore ts = getKeyStore();

            Log.i(TAG, "Adding certificate ID " + deviceLabel + " to Trust store (" + trustStorePath + "): " + cert);
            ts.setCertificateEntry(deviceLabel, cert);

            ts.store(new FileOutputStream(trustStorePath), null);
        }

        private byte[] fingerprint(X509Certificate cert) throws CertificateEncodingException, NoSuchAlgorithmException {
            MessageDigest md = MessageDigest.getInstance("SHA1");
            md.reset();
            return md.digest(cert.getEncoded());
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            trustManager.checkServerTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return trustManager.getAcceptedIssuers();
        }

        private void reloadTrustManager() throws Exception {
            KeyStore ts = getKeyStore();

            Enumeration<String> aliases = ts.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Log.i(TAG, "Trusted certificate " + alias + ": " + ts.getCertificate(alias));
            }

            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ts);

            TrustManager tms[] = tmf.getTrustManagers();
            for (int i = 0; i < tms.length; i++) {
                if (tms[i] instanceof X509TrustManager) {
                    trustManager = (X509TrustManager) tms[i];
                    return;
                }
            }

            throw new NoSuchAlgorithmException("No X509TrustManager in TrustManagerFactory");
        }

        private KeyStore getKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
            KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());

            if (trustStorePath.exists()) {
                Log.i(TAG, "Loading certificates from Trust store (" + trustStorePath + ")");
                InputStream in = null;
                try {
                    in = new FileInputStream(trustStorePath);
                    ts.load(in, null);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                        }
                    }
                }
            } else {
                Log.i(TAG, "Creating custom Trust Manager " + trustStorePath.getAbsolutePath());
                ts.load(null, null);
                ts.store(new FileOutputStream(trustStorePath), null);
            }
            return ts;
        }

    }

    /**
     * Always returns the only key associated to "nuntius" alias
     */
    private static class MyX509KeyManager implements X509KeyManager {
        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return "nuntius";
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            Log.i(TAG, "getCertificateChain for " + alias);

            try {
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                return new X509Certificate[] {
                        (X509Certificate) ks.getCertificate(alias)
                };
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
                Log.e(TAG, "Error during getCertificateChain(" + alias + ")", e);
            }
            return null;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[0];
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return new String[] { "nuntius" };
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            Log.i(TAG, "getPrivateKey for " + alias);
            try {
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                return (PrivateKey) ks.getKey(alias, null);
            } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException | UnrecoverableKeyException e) {
                Log.e(TAG, "Error during getPrivateKey(" + alias + ")", e);
            }
            return null;
        }
    }

    public static void generateSelfSignedCertificate() throws Exception {
        String alias = "nuntius";
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        Enumeration<String> aliases = ks.aliases();
        boolean found = false;
        while (aliases.hasMoreElements()) {
            String currentAlias = aliases.nextElement();
            if (alias.equals(currentAlias)) {
                found = true;
                Log.i(TAG, "Self Signed Certificate found in keystore");
                Key key = ks.getKey(alias, pwd);
                Log.i(TAG, "Key: " + key);
                Certificate certificate = ks.getCertificate(alias);
                Log.i(TAG, "Certificate: " + certificate);
            }
        }

        if (found) {
            return;
        }

        Log.i(TAG, "Self Signed Certificate not found in keystore. Generating a new one...");

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name("CN=nuntius");
        X500Name issuer = subject ;

        X509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(
                issuer,
                new BigInteger(64, new SecureRandom()),
                NOT_BEFORE,
                NOT_AFTER,
                subject,
                keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSAEncryption").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = builder.build(signer);
        X509Certificate cert = new JcaX509CertificateConverter().setProvider(PROVIDER).getCertificate(certHolder);
        cert.verify(keyPair.getPublic());

        Log.i(TAG, "Certificate generated: " + cert);

        ks.setKeyEntry(alias, keyPair.getPrivate(), pwd, new Certificate[] { cert });
    }


}
