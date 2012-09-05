/*
 * Copyright (C) 2012 CyanogenMod
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.cnd;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.view.IWindowManager;
import android.view.VolumePanel;

import com.android.settings.R;
import com.android.settings.service.FlipService;
import com.android.settings.service.HeadphoneService;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

public class SoundSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "SoundSettings";

    private static final String KEY_VOLUME_OVERLAY = "volume_overlay";
    private static final String KEY_SAFE_HEADSET_RESTORE = "safe_headset_restore";
    private static final String KEY_VOLBTN_MUSIC_CTRL = "volbtn_music_controls";
    private static final String KEY_VOLUME_ADJUST_SOUNDS = "volume_adjust_sounds";
    private static final String PREF_HEADPHONES_PLUGGED_ACTION = "headphone_audio_mode";
    private static final String PREF_BT_CONNECTED_ACTION = "bt_audio_mode";
    private static final String PREF_FLIP_ACTION = "flip_mode";
    private static final String PREF_USER_TIMEOUT = "user_timeout";
    private static final String PREF_USER_DOWN_MS = "user_down_ms";
    private static final String PREF_PHONE_RING_SILENCE = "phone_ring_silence";

    private final Configuration mCurConfig = new Configuration();

    private SharedPreferences prefs;
    private ListPreference mVolumeOverlay;
    private CheckBoxPreference mSafeHeadsetRestore;
    private CheckBoxPreference mVolBtnMusicCtrl;
    private CheckBoxPreference mVolumeAdjustSounds;
    private ListPreference mHeadphonesPluggedAction;
    private ListPreference mBTPluggedAction;
    private ListPreference mFlipAction;
    private ListPreference mUserDownMS;
    private ListPreference mFlipScreenOff;
    private ListPreference mPhoneSilent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getContentResolver();

        addPreferencesFromResource(R.xml.sound_settings_rom);
        PreferenceManager.setDefaultValues(mContext, R.xml.sound_settings_rom, true);
        prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mVolumeOverlay = (ListPreference) findPreference(KEY_VOLUME_OVERLAY);
        mVolumeOverlay.setOnPreferenceChangeListener(this);
        int volumeOverlay = Settings.System.getInt(getContentResolver(),
                Settings.System.MODE_VOLUME_OVERLAY,
                VolumePanel.VOLUME_OVERLAY_SINGLE);
        mVolumeOverlay.setValue(Integer.toString(volumeOverlay));
        mVolumeOverlay.setSummary(mVolumeOverlay.getEntry());

        mSafeHeadsetRestore = (CheckBoxPreference) findPreference(KEY_SAFE_HEADSET_RESTORE);
        mSafeHeadsetRestore.setPersistent(false);
        mSafeHeadsetRestore.setChecked(Settings.System.getInt(resolver,
                Settings.System.SAFE_HEADSET_VOLUME_RESTORE, 1) != 0);

        mVolBtnMusicCtrl = (CheckBoxPreference) findPreference(KEY_VOLBTN_MUSIC_CTRL);
        mVolBtnMusicCtrl.setChecked(Settings.System.getInt(resolver,
                Settings.System.VOLBTN_MUSIC_CONTROLS, 1) != 0);

        mVolumeAdjustSounds = (CheckBoxPreference) findPreference(KEY_VOLUME_ADJUST_SOUNDS);
        mVolumeAdjustSounds.setPersistent(false);
        mVolumeAdjustSounds.setChecked(Settings.System.getInt(resolver,
                Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED, 1) != 0);

        mFlipAction = (ListPreference) findPreference(PREF_FLIP_ACTION);
        mFlipAction.setOnPreferenceChangeListener(this);
        mFlipAction.setValue((prefs.getString(PREF_FLIP_ACTION, "-1")));
        
        mUserDownMS = (ListPreference) findPreference(PREF_USER_DOWN_MS);
        mUserDownMS.setEnabled(Integer.parseInt(prefs.getString(PREF_FLIP_ACTION, "-1")) != -1);
        
        mFlipScreenOff = (ListPreference) findPreference(PREF_USER_TIMEOUT);
        mFlipScreenOff.setEnabled(Integer.parseInt(prefs.getString(PREF_FLIP_ACTION, "-1")) != -1);
        
        mPhoneSilent = (ListPreference) findPreference(PREF_PHONE_RING_SILENCE);
        mPhoneSilent.setValue((prefs.getString(PREF_PHONE_RING_SILENCE, "0")));
        mPhoneSilent.setOnPreferenceChangeListener(this);
        
        if (HeadphoneService.DEBUG)
            mContext.startService(new Intent(mContext, HeadphoneService.class));
        
        if (FlipService.DEBUG)
            mContext.startService(new Intent(mContext, FlipService.class));
    }
    
    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    // updateState in fact updates the UI to reflect the system state
    private void updateState(boolean force) {
        if (getActivity() == null) return;
        ContentResolver resolver = getContentResolver();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {

        if (preference == mSafeHeadsetRestore) {
            Settings.System.putInt(getContentResolver(),
                    Settings.System.SAFE_HEADSET_VOLUME_RESTORE,
                    mSafeHeadsetRestore.isChecked() ? 1 : 0);

		} else if (preference == mVolBtnMusicCtrl) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLBTN_MUSIC_CONTROLS,
                    mVolBtnMusicCtrl.isChecked() ? 1 : 0);

        } else if (preference == mVolumeAdjustSounds) {
            Settings.System.putInt(getContentResolver(), Settings.System.VOLUME_ADJUST_SOUNDS_ENABLED ,
                    mVolumeAdjustSounds.isChecked() ? 1 : 0);

        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }

        return true;
    }

    private void toggleFlipService() {
        if (FlipService.isStarted()) {
            mContext.stopService(new Intent(mContext, FlipService.class));
        }
        mContext.startService(new Intent(mContext, FlipService.class));
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        
        if (preference == mVolumeOverlay) {
            final int value = Integer.valueOf((String) objValue);
            final int index = mVolumeOverlay.findIndexOfValue((String) objValue);
            Settings.System.putInt(getContentResolver(),
                    Settings.System.MODE_VOLUME_OVERLAY, value);
            mVolumeOverlay.setSummary(mVolumeOverlay.getEntries()[index]);
            return true;
        } else if (preference == mFlipAction) {
            int val = Integer.parseInt((String) objValue);
            if (val != -1) {
                mUserDownMS.setEnabled(true);
                mFlipScreenOff.setEnabled(true);
                AlertDialog.Builder ad = new AlertDialog.Builder(getActivity());
                ad.setTitle(getResources().getString(R.string.flip_dialog_title));
                ad.setMessage(getResources().getString(R.string.flip_dialog_msg));
                ad.setPositiveButton(
                        getResources().getString(R.string.flip_action_positive),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                ad.show();
                toggleFlipService();
            } else {
                mUserDownMS.setEnabled(false);
                mFlipScreenOff.setEnabled(false);
            }
            return true;
                
        } else if (preference == mPhoneSilent) {
            int val = Integer.parseInt((String) objValue);
            if (val != 0) {
                toggleFlipService();
            }
            return true;
        }
        return false;
    }
}
