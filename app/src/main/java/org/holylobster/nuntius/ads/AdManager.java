package org.holylobster.nuntius.ads;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;



public class AdManager {
    // Static fields are shared between all instances.
    static InterstitialAd mAd1;
    static InterstitialAd mAd2;



    public AdManager() {
    }

    public void createAds(Context context) {


            // Create an ad.
            mAd1 = new InterstitialAd(context);
            mAd1.setAdUnitId("");

            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .addTestDevice("5C195A4AE6121C0D42702AAAE118DC01")
                    .addTestDevice("E9DEB34031182776A4E765DCEF19F10D").build();


            mAd1.loadAd(adRequest);

        /*
        mAd2 = new InterstitialAd(context);
        mAd2.setAdUnitId("");


        AdRequest adRequest2 = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("E9DEB34031182776A4E765DCEF19F10D").build();

        mAd2.loadAd(adRequest2);
        */


        //listners
        mAd1.setAdListener(new AdListener() {


            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.d("ADS_REACH","Ad failed to load with code: "+errorCode);
            }

            @Override
            public void onAdLoaded() {
                Log.d("ADS_REACH","Interstitial 1 loaded");
            }

        });

        /*
        mAd2.setAdListener(new AdListener() {
            @Override
            public void onAdClosed() {

            }

            @Override
            public void onAdClosed() {
                Log.d("ADS_REACH","Ad2 was displayed");

            }


            @Override
            public void onAdFailedToLoad(int errorCode) {
                Log.d("ADS_REACH","Ad failed to load with code: "+errorCode);
            }

            @Override
            public void onAdLoaded() {
                Log.d("ADS_REACH","Interstitial 2 loaded");
            }

        });

        */
    }

    public static InterstitialAd getAd1() {
        return mAd1;
    }

    /*
    public static InterstitialAd getAd2() {
        return mAd2;
    }
    */
}
