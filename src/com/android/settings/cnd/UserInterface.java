
package com.android.settings.cnd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemProperties;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Spannable;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.util.CMDProcessor;
import com.android.settings.util.Helpers;

public class UserInterface extends SettingsPreferenceFragment implements Preference.OnPreferenceChangeListener {

    public static final String TAG = "UserInterface";

    private static final String PREF_CUSTOM_CARRIER_LABEL = "custom_carrier_label";
    private static final String KEY_IME_SWITCHER = "status_bar_ime_switcher";
    private static final String PREF_RECENT_KILL_ALL = "recent_kill_all";
    private static final String VOLUME_KEY_CURSOR_CONTROL = "volume_key_cursor_control";
    private static final String PREF_KILL_APP_LONGPRESS_BACK = "kill_app_longpress_back";
    private static final String PREF_ALARM_ENABLE = "alarm";
    private static final String PREF_MODE_TABLET_UI = "mode_tabletui";

    Preference mCustomLabel;
    CheckBoxPreference mStatusBarImeSwitcher;
    CheckBoxPreference mRecentKillAll;
    ListPreference mVolumeKeyCursorControl;
    CheckBoxPreference mKillAppLongpressBack;
    CheckBoxPreference mAlarm;
    CheckBoxPreference mTabletui;
    Preference mLcdDensity;

    String mCustomLabelText = null;

    int newDensityValue;

    DensityChanger densityFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.user_interface_settings);

        PreferenceScreen prefs = getPreferenceScreen();

        mLcdDensity = findPreference("lcd_density_setup");
        String currentProperty = SystemProperties.get("ro.sf.lcd_density");
        try {
            newDensityValue = Integer.parseInt(currentProperty);
        } catch (Exception e) {
            getPreferenceScreen().removePreference(mLcdDensity);
        }

        mLcdDensity.setSummary(getResources().getString(R.string.current_lcd_density) + currentProperty);

        mCustomLabel = findPreference(PREF_CUSTOM_CARRIER_LABEL);
        updateCustomLabelTextSummary();

        // Enable or disable mStatusBarImeSwitcher based on boolean value: config_show_cmIMESwitcher
        if (!getResources().getBoolean(com.android.internal.R.bool.config_show_cmIMESwitcher)) {
            getPreferenceScreen().removePreference(findPreference(KEY_IME_SWITCHER));
        } else {
            mStatusBarImeSwitcher = (CheckBoxPreference) findPreference(KEY_IME_SWITCHER);
            if (mStatusBarImeSwitcher != null) {
                mStatusBarImeSwitcher.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                        Settings.System.STATUS_BAR_IME_SWITCHER, 1) != 0);
            }
        }

        mRecentKillAll = (CheckBoxPreference) findPreference(PREF_RECENT_KILL_ALL);
        mRecentKillAll.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RECENT_KILL_ALL_BUTTON, 0) == 1);

        mVolumeKeyCursorControl = (ListPreference) findPreference(VOLUME_KEY_CURSOR_CONTROL);
        mVolumeKeyCursorControl.setOnPreferenceChangeListener(this);
        mVolumeKeyCursorControl.setValue(Integer.toString(Settings.System.getInt(getActivity()
                .getContentResolver(), Settings.System.VOLUME_KEY_CURSOR_CONTROL, 0)));
        mVolumeKeyCursorControl.setSummary(mVolumeKeyCursorControl.getEntry());

        mKillAppLongpressBack = (CheckBoxPreference) findPreference(PREF_KILL_APP_LONGPRESS_BACK);
                updateKillAppLongpressBackOptions();
        
        mAlarm = (CheckBoxPreference) findPreference(PREF_ALARM_ENABLE);
        mAlarm.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_SHOW_ALARM, 1) == 1);

        mTabletui = (CheckBoxPreference) findPreference(PREF_MODE_TABLET_UI);
        mTabletui.setChecked(Settings.System.getBoolean(mContext.getContentResolver(),
                    Settings.System.MODE_TABLET_UI, false));

        boolean hasNavBarByDefault = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_showNavigationBar);

        if (hasNavBarByDefault || mTablet) {
            ((PreferenceGroup) findPreference("misc")).removePreference(mKillAppLongpressBack);
        }

        if (mTablet) {
        } else {
            prefs.removePreference(mTabletui);
        }
    }

    private void writeKillAppLongpressBackOptions() {
        Settings.System.putInt(getActivity().getContentResolver(),
                Settings.System.KILL_APP_LONGPRESS_BACK, mKillAppLongpressBack.isChecked() ? 1 : 0);
    }
    
    private void updateKillAppLongpressBackOptions() {
        mKillAppLongpressBack.setChecked(Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.KILL_APP_LONGPRESS_BACK, 0) != 0);
    }

    private void updateCustomLabelTextSummary() {
        mCustomLabelText = Settings.System.getString(getActivity().getContentResolver(),
                Settings.System.CUSTOM_CARRIER_LABEL);
        if (mCustomLabelText == null || mCustomLabelText.length() == 0) {
            mCustomLabel.setSummary(R.string.custom_carrier_label_notset);
        } else {
            mCustomLabel.setSummary(mCustomLabelText);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            Preference preference) {
        if (preference == mStatusBarImeSwitcher) {
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUS_BAR_IME_SWITCHER, mStatusBarImeSwitcher.isChecked() ? 1 : 0);
            return true;
        } else if (preference == mRecentKillAll) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.RECENT_KILL_ALL_BUTTON, checked ? 1 : 0);
            Helpers.restartSystemUI();
            return true;
        } else if (preference == mKillAppLongpressBack) {
            writeKillAppLongpressBackOptions();
        } else if (preference == mAlarm) {
            boolean checked = ((CheckBoxPreference) preference).isChecked();
            Settings.System.putInt(getActivity().getContentResolver(),
                    Settings.System.STATUSBAR_SHOW_ALARM, checked ? 1 : 0);
        } else if (preference == mTabletui) {
            Settings.System.putBoolean(mContext.getContentResolver(),
                    Settings.System.MODE_TABLET_UI,
                    ((CheckBoxPreference) preference).isChecked());
            return true;
        } else if (preference == mCustomLabel) {
            AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());

            alert.setTitle(R.string.custom_carrier_label_title);
            alert.setMessage(R.string.custom_carrier_label_explain);

            // Set an EditText view to get user input
            final EditText input = new EditText(getActivity());
            input.setText(mCustomLabelText != null ? mCustomLabelText : "");
            alert.setView(input);

            alert.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = ((Spannable) input.getText()).toString();
                    Settings.System.putString(getActivity().getContentResolver(),
                            Settings.System.CUSTOM_CARRIER_LABEL, value);
                    updateCustomLabelTextSummary();
                    Intent i = new Intent();
                    i.setAction("com.android.settings.LABEL_CHANGED");
                    mContext.sendBroadcast(i);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    // Canceled.
                }
            });

            alert.show();
        } else if (preference == mLcdDensity) {
            ((PreferenceActivity) getActivity())
            .startPreferenceFragment(new DensityChanger(), true);
            return true;
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference == mVolumeKeyCursorControl) {
            String volumeKeyCursorControl = (String) value;
            int val = Integer.parseInt(volumeKeyCursorControl);
            Settings.System.putInt(getActivity().getContentResolver(),
                                   Settings.System.VOLUME_KEY_CURSOR_CONTROL, val);
            int index = mVolumeKeyCursorControl.findIndexOfValue(volumeKeyCursorControl);
            mVolumeKeyCursorControl.setSummary(mVolumeKeyCursorControl.getEntries()[index]);
            return true;
        }
        return false;
    }
}
