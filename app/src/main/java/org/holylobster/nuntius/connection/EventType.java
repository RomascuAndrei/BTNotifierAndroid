package org.holylobster.nuntius.connection;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.util.JsonReader;
import android.util.Log;

import org.holylobster.nuntius.data.BlacklistedApp;
import org.holylobster.nuntius.notifications.NotificationListenerService;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by fly on 18/07/15.
 */

public enum EventType {

    ACTION {
        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        @Override
        void manageEvent(NotificationListenerService context, JsonReader reader) throws IOException {
            String[] values = parse(reader);
            String key = values[0];
            String actionTitle = values[1];
            if (!key.equals("")) {
                StatusBarNotification[] activeNotifications = context.getActiveNotifications(new String[]{key});
                if (activeNotifications.length > 0) {
                    StatusBarNotification activeNotification = activeNotifications[0];
                    for (Notification.Action action : activeNotification.getNotification().actions) {
                        if (actionTitle.equals(action.title)) {
                            try {
                                action.actionIntent.send();
                            } catch (PendingIntent.CanceledException e) {
                            }
                        }
                    }
                }
            }
        }

        private String[] parse(JsonReader reader) throws IOException {
            String key = "";
            String actionTitle = "";
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "action":
                        reader.beginObject();
                        break;
                    case "key":
                        key = reader.nextString();
                        break;
                    case "actionName":
                        actionTitle = reader.nextString();
                        break;
                }
            }
            return new String[] { key, actionTitle };
        }
    },
    DISMISS {
        @Override
        void manageEvent(NotificationListenerService context, JsonReader reader) throws IOException {
            String[] values = parse(reader);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String key = values[0];
                context.cancelNotification(key);
            } else {
                String packageName = values[0];
                String flag = values[1];
                int id = Integer.parseInt(values[2]);
                context.cancelNotification(packageName, flag, id);
            }
        }

        private String[] parse(JsonReader reader) throws IOException {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                String key = "";
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (name.equals("notification")) {
                        reader.beginObject();
                    } else if (name.equals("key")) {
                        key = reader.nextString();
                    }
                }
                return new String[] {key};
            } else {
                String packageName = "";
                String flag = "";
                String id = "";
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "notification":
                            reader.beginObject();
                            break;
                        case "packageName":
                            packageName = reader.nextString();
                            break;
                        case "flag":
                            flag = reader.nextString();
                            break;
                        case "id":
                            id = reader.nextString();
                            break;
                    }
                }
                return new String[] {packageName, flag, id};
            }
        }
    },
    BLACKLIST {
        @Override
        void manageEvent(NotificationListenerService context, JsonReader reader) throws IOException {
            String pckgNm = parse(reader)[0];
            if (pckgNm != null) {
                BlacklistedApp blacklist = new BlacklistedApp(context);
                blacklist.add(pckgNm);
            }
        }

        private String[] parse(JsonReader reader) throws IOException {
            String pckgNm = null;
            while (reader.hasNext()) {
                String name = reader.nextName();
                if (name.equals("app")) {
                    reader.beginObject();
                } else if (name.equals("packageName")) {
                    pckgNm = reader.nextString();
                }
            }
            return new String[] {pckgNm};
        }
    },
    SMS {
        @Override
        void manageEvent(NotificationListenerService context, JsonReader reader) throws IOException {
            String[] values = parse(reader);
            String number = values[0];
            String message = values[1];
            SmsManager smsManager = SmsManager.getDefault();
            ArrayList<String> messages = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(number, null, messages, null, null);
        }

        private String[] parse(JsonReader reader) throws IOException {
            String number = "";
            String message = "";
            while (reader.hasNext()) {
                String name = reader.nextName();
                switch (name) {
                    case "sms":
                        reader.beginObject();
                        break;
                    case "senderNum":
                        number = reader.nextString();
                        break;
                    case "msg":
                        message = reader.nextString();
                        break;
                }
            }
            return new String[] {number, message};
        }
    };

    abstract void manageEvent(NotificationListenerService context, JsonReader reader) throws IOException;

}