
package com.android.settings.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.provider.Settings;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import com.android.settings.R;

import com.android.settings.performance.Voltage;
import com.android.settings.performance.VoltageControlSettings;
import com.android.settings.util.CMDProcessor;

public class BootService extends Service {

    public static boolean servicesStarted = false;
    public static SharedPreferences preferences;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new BootWorker().execute();
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    class BootWorker extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... args) {
            Context c = getApplicationContext();
            preferences = PreferenceManager.getDefaultSharedPreferences(c);
            final CMDProcessor cmd = new CMDProcessor();

            if (HeadphoneService.getUserHeadphoneAudioMode(c) != -1
                || HeadphoneService.getUserBTAudioMode(c) != -1) {
                c.startService(new Intent(c, HeadphoneService.class));
            }

            if (FlipService.getUserFlipAudioMode(c) != -1
                || FlipService.getUserCallSilent(c) != 0)
                c.startService(new Intent(c, FlipService.class));

            if (preferences.getBoolean(VoltageControlSettings
                    .KEY_APPLY_BOOT, false)) {
                final List<Voltage> volts = VoltageControlSettings
                    .getVolts(preferences);
                final StringBuilder sb = new StringBuilder();
                for (final Voltage volt : volts) {
                    sb.append(volt.getSavedMV() + " ");
                }
                cmd.su.runWaitFor("busybox echo " + sb.toString() +
                        " > " + VoltageControlSettings.MV_TABLE0);
                if (new File(VoltageControlSettings.MV_TABLE1).exists()) {
                    cmd.su.runWaitFor("busybox echo " +
                    sb.toString() + " > " +
                    VoltageControlSettings.MV_TABLE1);
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            servicesStarted = true;
            stopSelf();
        }

    }
}
