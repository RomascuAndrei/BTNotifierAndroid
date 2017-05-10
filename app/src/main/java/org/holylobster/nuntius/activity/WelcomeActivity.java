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
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;

import org.holylobster.nuntius.R;
import org.holylobster.nuntius.ads.AdManager;


public class WelcomeActivity extends Activity {

    //private InterstitialAd mInterstitialAd;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        /*
        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId("ca-app-pub-6887589184636373/9378703641");
        AdRequest adRequestInterstial = new AdRequest.Builder()
                .addTestDevice("E9DEB34031182776A4E765DCEF19F10D")
                .addTestDevice("5C195A4AE6121C0D42702AAAE118DC01")
                .build();
        mInterstitialAd.loadAd(adRequestInterstial);

//listner for adClosed
        mInterstitialAd.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {
                AdRequest adRequest = new AdRequest.Builder()
                         .addTestDevice("5C195A4AE6121C0D42702AAAE118DC01")
                         .addTestDevice("E9DEB34031182776A4E765DCEF19F10D")
                        .build();
                mInterstitialAd.loadAd(adRequest);
            }

            //intervientie 3
            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.d("ADS_REACH","Ad failed to load with code: "+errorCode);
            }

            @Override
            public void onAdLoaded() {
                Log.d("ADS_REACH","Interstitial ad loaded");
            }

        });
        */

        Log.d("ADS_REACH","Normal interstitial would start to load");



    }

    public void onGotIt(View button) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putBoolean("gotIt", true);
        prefEditor.apply();

        Intent i = new Intent(this, SettingsActivity.class);
        startActivity(i);

        //mInterstitialAd.show();
        InterstitialAd ad1 = AdManager.getAd1();
        if (ad1.isLoaded()) {
            ad1.show();
        }
        //interventie 2
        Log.d("ADS_REACH","Reached ad code");

        finish();


        //aici era interstitial initial

    }





}
