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

package org.holylobster.nuntius.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

/**
 * Created by fly on 24/03/15.
 */
public class BlacklistedApp {
    private static final String TAG = BlacklistedApp.class.getSimpleName();

    private List<ApplicationInfo> blacklistedAppList;
    private Context context;
    private PackageManager pm;

    public BlacklistedApp(Context c) {
        context = c;
        pm = context.getPackageManager();
        getFromPref();
    }

    public void getFromPref() {
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        blacklistedAppList = new ArrayList<>();
        for (String packageName : defaultSharedPreferences.getStringSet("BlackList", new HashSet<String>())) {
            try {
                blacklistedAppList.add(pm.getApplicationInfo(packageName, 0));
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Error retrieving application info\n", e);
            }
        }
        Collections.sort(blacklistedAppList, new ApplicationInfo.DisplayNameComparator(pm));
    }

    public List<ApplicationInfo> getBlacklistedAppList() {
        return blacklistedAppList;
    }

    public void add(ApplicationInfo app) {
        blacklistedAppList.add(app);
        sortAndPush();
    }

    public void add(String s){
        try {
            this.add(pm.getApplicationInfo(s, 0));
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Incorrect android app\n", e);
        }
    }

    public void remove(int i) {
        blacklistedAppList.remove(i);
        sortAndPush();
    }

    private void sortAndPush() {
        Collections.sort(blacklistedAppList, new ApplicationInfo.DisplayNameComparator(pm));
        pushToPref();
    }

    public void pushToPref() {
        ArrayList<String> bl = new ArrayList<>();
        for (ApplicationInfo applicationInfo : blacklistedAppList) {
            bl.add(applicationInfo.packageName);
        }
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = defaultSharedPreferences.edit();
        editor.putStringSet("BlackList", new HashSet<>(bl));
        editor.commit();
    }
}
