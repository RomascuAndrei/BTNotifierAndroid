package org.holylobster.nuntius.sms;

import java.util.Observable;

/**
 * Created by fly on 22/06/15.
 */
public class SmsObservable extends Observable {
    private static SmsObservable instance = new SmsObservable();

    public static SmsObservable getInstance() {
        return instance;
    }

    private SmsObservable() {
    }

    public void updateValue(Object data) {
        synchronized (this) {
            setChanged();
            notifyObservers(data);
        }
    }
}