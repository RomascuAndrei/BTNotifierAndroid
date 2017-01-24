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

import android.util.JsonReader;
import java.io.IOException;
import java.io.StringReader;

public class IncomingMessage {
    private EventType eventType;
    private JsonReader jsonReader;

    public IncomingMessage(String message) throws IOException {
        JsonReader reader = new JsonReader(new StringReader(message));
        reader.beginObject();
        if (reader.hasNext()) {
            String name = reader.nextName();
            if (name.equals("event")) {
                String typeString = reader.nextString();
                if (typeString.equals("action")) {
                    eventType = EventType.ACTION;
                } else if (typeString.equals("dismiss")) {
                    eventType = EventType.DISMISS;
                } else if (typeString.equals("blacklist")) {
                    eventType = EventType.BLACKLIST;
                } else if (typeString.equals("sms")) {
                    eventType = EventType.SMS;
                }
            }
        }
        jsonReader = reader;
    }

    public EventType getEventType() {
        return eventType;
    }

    public JsonReader getMsg() {
        return jsonReader;
    }

}
