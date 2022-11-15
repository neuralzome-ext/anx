package com.flomobility.anx.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.flomobility.anx.R;
import com.flomobility.anx.shared.settings.preferences.FloAppSharedPreferences;

@Keep
public class TerminalPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.terminal_preferences, rootKey);
    }

}

class TerminalPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final FloAppSharedPreferences mPreferences;

    private static TerminalPreferencesDataStore mInstance;

    private TerminalPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = FloAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalPreferencesDataStore(context);
        }
        return mInstance;
    }

}
