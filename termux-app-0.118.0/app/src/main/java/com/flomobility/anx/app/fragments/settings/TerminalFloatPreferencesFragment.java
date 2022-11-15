package com.flomobility.anx.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.flomobility.anx.R;
import com.flomobility.anx.shared.settings.preferences.TerminalFloatAppSharedPreferences;

@Keep
public class TerminalFloatPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalFloatPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.terminal_float_preferences, rootKey);
    }

}

class TerminalFloatPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TerminalFloatAppSharedPreferences mPreferences;

    private static TerminalFloatPreferencesDataStore mInstance;

    private TerminalFloatPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TerminalFloatAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalFloatPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalFloatPreferencesDataStore(context);
        }
        return mInstance;
    }

}
