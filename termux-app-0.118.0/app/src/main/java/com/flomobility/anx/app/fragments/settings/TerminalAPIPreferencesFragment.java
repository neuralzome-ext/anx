package com.flomobility.anx.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.flomobility.anx.R;
import com.flomobility.anx.shared.settings.preferences.TerminalAPIAppSharedPreferences;

@Keep
public class TerminalAPIPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalAPIPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.terminal_api_preferences, rootKey);
    }

}

class TerminalAPIPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TerminalAPIAppSharedPreferences mPreferences;

    private static TerminalAPIPreferencesDataStore mInstance;

    private TerminalAPIPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TerminalAPIAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalAPIPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalAPIPreferencesDataStore(context);
        }
        return mInstance;
    }

}
