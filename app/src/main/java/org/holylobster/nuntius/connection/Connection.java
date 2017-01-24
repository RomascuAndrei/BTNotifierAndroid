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

import android.content.Context;
import android.util.Log;

import org.holylobster.nuntius.notifications.NotiHandler;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class Connection extends Thread {
    // Used for logging
    private final String TAG = this.getClass().getSimpleName();

    private final BlockingQueue<Message> queue = new LinkedBlockingDeque<>();

    private final Thread senderThread;
    private final Thread receiverThread;
    private final Socket socket;

    private final NotiHandler handler;
    private final String destination;

    boolean gracefulClose = false;

    public Connection(final Context context, final Socket socket, final NotiHandler handler) {
        this.socket = socket;
        this.handler = handler;
        this.destination = socket.getDestination();

        senderThread = new Thread() {
            public void run() {
                try {
                    OutputStream outputStream = new BufferedOutputStream(socket.getOutputStream());
                    while (checkConnected(socket) && !gracefulClose) {
                        Message message = queue.take();
                        String json = message.toJSON(context);
                        Log.i(TAG, "Sending message (size " + json.length() + ")");
                        outputStream.write(json.getBytes());
                        outputStream.write('\r');
                        outputStream.write('\n');
                        outputStream.flush();
                    }
                } catch (InterruptedException e) {
                    Log.i(TAG, "Sender thread interrupted while waiting for a message");
                } catch (IOException e) {
                    Log.e(TAG, "Error in sender thread", e);
                }
                Log.i(TAG, "Sender thread is closing...");
                cleanup(socket);
            }
            private boolean checkConnected(Socket socket) {
                return socket != null && socket.isConnected();
            }
        };
        receiverThread = new Thread() {
            public void run() {
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                try {
                    InputStream inputStream = new BufferedInputStream(socket.getInputStream());
                    while (checkConnected(socket) && !gracefulClose) {
                        int c;
                        if ((c = inputStream.read()) == -1) {
                            throw new IOException("End of input stream reached");
                        }
                        baos.write((byte) c);
                        if (c == '\n') {
                            String data = new String(baos.toByteArray(), Charset.forName("UTF-8"));
                            Log.i(TAG, "Read " + data.length() + " chars");
                            Log.d(TAG, "Read : " + data);
                            try {
                                IncomingMessage message = new IncomingMessage(data);
                                handler.onMessageReceived(message);
                            } catch (IOException e) {
                                Log.e(TAG, "Unable to parse: " + data);
                            }
                            baos.reset();
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error in receiver thread", e);
                }
                Log.i(TAG, "Receiver thread is closing...");
                cleanup(socket);
            }
            private boolean checkConnected(Socket socket) {
                return socket != null && socket.isConnected();
            }
        };
        receiverThread.start();
        senderThread.start();
    }

    private void cleanup(Socket socket) {
        Log.i(TAG, "Cleanup of connection resources...");
        if (socket != null) {
            try {
                InputStream inputStream = socket.getInputStream();
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
            } catch (IOException e) {
            }
            try {
                OutputStream outputStream = socket.getOutputStream();
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                }
            } catch (IOException e) {
            }

            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "Error closing socket", e);
            }
        }
        queue.clear();
        Log.i(TAG, "Cleanup completed");
        handler.onConnectionClosed(this);
    }

    public boolean enqueue(Message m) {
        return queue.offer(m);
    }

    public void close() {
        gracefulClose = true;
        // Wait for max 250 * 10 = 2.5s for threads to gracefully close
        for (int i = 0; i < 10 && (senderThread.isAlive() || receiverThread.isAlive()); i++)  {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
            }
        }
        // Kill threads if the did not close in time
        if (senderThread.isAlive()) {
            senderThread.interrupt();
        }
        if (receiverThread.isAlive()) {
            receiverThread.interrupt();
        }
        cleanup(socket);
    }

    public String getDestination() {
        return destination;
    }
}
