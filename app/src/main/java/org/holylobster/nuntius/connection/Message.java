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
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.service.notification.StatusBarNotification;
import android.util.Base64;
import android.util.JsonWriter;
import android.util.Log;

import org.holylobster.nuntius.sms.SMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;

public class Message {
    // Used for logging
    private String TAG = this.getClass().getSimpleName();

    private String event;
    private SendType type;
    private StatusBarNotification[] notifications;
    private SMessage sMessage;

    public Message(String event, StatusBarNotification... notifications) {
        this.notifications = notifications;
        this.event = event;
        this.type = SendType.NOTI;

    }

    public Message(SMessage sMessage){
        this.sMessage = sMessage;
        this.event = "sms";
        this.type = SendType.SMS;
    }

    public String toJSON(Context context) {
        StringWriter out = new StringWriter();
        JsonWriter writer = new JsonWriter(out);

        try {
            writer.beginObject();
            writer.name("event").value(event);

            writer.name("eventItems");
            writer.beginArray();

            if (type == SendType.SMS) {
                toJsonSMessage(context, writer, sMessage);
            } else if (type == SendType.NOTI) {
                for (StatusBarNotification sbn : notifications) {
                    toJsonNotification(context, writer, sbn);
                }
            }
            writer.endArray();
            writer.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toString();
    }

    private void toJsonSMessage(Context context, JsonWriter writer, SMessage sMessage) throws IOException {
        writer.beginObject();
        writer.name("id").value("0"); // test value
        writer.name("sender").value(sMessage.getSender());
        writer.name("sender_num").value(sMessage.getSenderNum());
        writer.name("message").value(sMessage.getMessage());

        if (context != null){
            final PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo ai = pm.getApplicationInfo(Telephony.Sms.getDefaultSmsPackage(context), 0); // require api 19 min is actually 19.

                Bitmap bitmap = toBitmap(pm.getApplicationIcon(ai));
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                writer.name("icon").value(new String(Base64.encode(stream.toByteArray(), Base64.DEFAULT), "UTF-8"));
            } catch (final PackageManager.NameNotFoundException e) {
                Log.d(TAG, "Could not get the icon and label for the default sms app");
            }
        }
        writer.endObject();
    }

    private void toJsonNotification(Context context, JsonWriter writer, StatusBarNotification sbn) throws IOException {
        writer.beginObject();

        writer.name("id").value(sbn.getId());
        writer.name("packageName").value(sbn.getPackageName());
        writer.name("clearable").value(sbn.isClearable());
        writer.name("ongoing").value(sbn.isOngoing());
        writer.name("postTime").value(sbn.getPostTime());
        String tag = sbn.getTag();
        if (tag != null) {
            writer.name("tag").value(tag);
        }

        final PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(sbn.getPackageName(), 0);
            writer.name("appName").value(pm.getApplicationLabel(ai).toString());

            Bitmap bitmap = toBitmap(pm.getApplicationIcon(ai));
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            writer.name("icon").value(new String(Base64.encode(stream.toByteArray(), Base64.DEFAULT), "UTF-8"));
        } catch (final PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Could not get the icon and label for package: " + sbn.getPackageName());
        }

        writePropertiesLollipop(writer, sbn);

        writer.name("notification");
        Notification notification = sbn.getNotification();

        writer.beginObject();
        writer.name("priority").value(notification.priority);
        writer.name("when").value(notification.when);
        writer.name("defaults").value(notification.defaults);
        writer.name("flags").value(notification.flags);
        writer.name("number").value(notification.number);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Bundle extras = notification.extras;

            CharSequence notificationTitle = extras.getCharSequence(Notification.EXTRA_TITLE);
            if (notificationTitle != null) {
                writer.name("title").value(notificationTitle.toString());
            }

            CharSequence notificationText = extras.getCharSequence(Notification.EXTRA_TEXT);
            if (notificationText != null) {
                writer.name("text").value(notificationText.toString());
            }

            CharSequence notificationSubText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
            if (notificationSubText != null) {
                writer.name("subText").value(notificationSubText.toString());
            }
        }

        CharSequence tickerText = notification.tickerText;
        if (tickerText != null) {
            writer.name("tickerText").value(tickerText.toString());
        }

        PendingIntent contentIntent = notification.contentIntent;
        PendingIntent deleteIntent = notification.deleteIntent;
        PendingIntent fullScreenIntent = notification.fullScreenIntent;
        if (contentIntent != null) {
            writer.name("contentIntent").value(contentIntent.toString());
        }

        if (deleteIntent != null) {
            writer.name("deleteIntent").value(deleteIntent.toString());
        }

        if (fullScreenIntent != null) {
            writer.name("fullScreenIntent").value(fullScreenIntent.toString());
        }

        writeNotificationLollipop(writer, notification);

        writeActions(writer, notification);

        writer.endObject();
        writer.endObject();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void writePropertiesLollipop(JsonWriter writer, StatusBarNotification sbn) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        writer.name("key").value(sbn.getKey());
        writer.name("groupKey").value(sbn.getGroupKey());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void writeNotificationLollipop(JsonWriter writer, Notification notification) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
        }

        String category = notification.category;
        if (category != null) {
            writer.name("category").value(category);
        }
        writer.name("color").value(notification.color);
        writer.name("visibility").value(notification.visibility);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void writeActions(JsonWriter writer, Notification notification) throws IOException {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            return;
        }

        if (notification.actions != null) {
            Log.d(TAG, "writing action");
            writer.name("actions");
            writer.beginArray();
            for (Notification.Action a : notification.actions) {
                Log.d(TAG, "writing action : " + a.title.toString());
                writer.beginObject();
                writer.name("title").value(a.title.toString());
                writer.endObject();
            }
            writer.endArray();
        }
    }

    public static Bitmap toBitmap(Drawable drawable) throws IOException {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        } else {
            final Bitmap bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );
            final Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }
    }

    @Override
    public String toString() {
        return "Message{" +
                "TAG='" + TAG + '\'' +
                ", event='" + event + '\'' +
                ", notifications=" + Arrays.toString(notifications) +
                ", sMessage=" + sMessage +
                '}';
    }

    public enum SendType {
        NOTI, SMS
    }
}
