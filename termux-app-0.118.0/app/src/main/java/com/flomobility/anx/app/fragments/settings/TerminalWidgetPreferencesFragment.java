package com.flomobility.anx.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.flomobility.anx.R;
import com.flomobility.anx.shared.settings.preferences.TerminalWidgetAppSharedPreferences;

@Keep
public class TerminalWidgetPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalWidgetPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.terminal_widget_preferences, rootKey);
    }

}

class TerminalWidgetPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TerminalWidgetAppSharedPreferences mPreferences;

    private static TerminalWidgetPreferencesDataStore mInstance;

    private TerminalWidgetPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TerminalWidgetAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalWidgetPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalWidgetPreferencesDataStore(context);
        }
        return mInstance;
    }

}
