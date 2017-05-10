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

package org.holylobster.nuntius.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import org.holylobster.nuntius.R;
import org.holylobster.nuntius.ads.AdManager;


public class SplashScreenActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        final boolean gotIt = settings.getBoolean("gotIt", false);


            Log.d("ADS_REACH","Ads started loading");

            Context context = getApplicationContext();
            AdManager adManager = new AdManager();
            adManager.createAds(context);


        ((GradientDrawable) findViewById(R.id.splash).getBackground().getCurrent())
                .setGradientRadius(getResources().getDimension(
                        R.dimen.gradient_radius));

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                //Class activityClass = gotIt ? SettingsActivity.class : WelcomeActivity.class;

                Class activityClass = WelcomeActivity.class;

                /* interventie 1 - IDEA (n-am schimbat inca) interstitial ADS to show always
                original:
                               Intent i = new Intent(SplashScreenActivity.this, activityClass);
                               */
                Intent i = new Intent(SplashScreenActivity.this, activityClass);
                startActivity(i);
                finish();
            }
        }, 2000);
    }

}
