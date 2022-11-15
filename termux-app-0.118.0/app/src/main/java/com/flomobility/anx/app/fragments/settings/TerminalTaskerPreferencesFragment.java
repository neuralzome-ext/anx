package com.flomobility.anx.app.fragments.settings;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.Keep;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.flomobility.anx.R;
import com.flomobility.anx.shared.settings.preferences.TerminalTaskerAppSharedPreferences;

@Keep
public class TerminalTaskerPreferencesFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        Context context = getContext();
        if (context == null) return;

        PreferenceManager preferenceManager = getPreferenceManager();
        preferenceManager.setPreferenceDataStore(TerminalTaskerPreferencesDataStore.getInstance(context));

        setPreferencesFromResource(R.xml.terminal_tasker_preferences, rootKey);
    }

}

class TerminalTaskerPreferencesDataStore extends PreferenceDataStore {

    private final Context mContext;
    private final TerminalTaskerAppSharedPreferences mPreferences;

    private static TerminalTaskerPreferencesDataStore mInstance;

    private TerminalTaskerPreferencesDataStore(Context context) {
        mContext = context;
        mPreferences = TerminalTaskerAppSharedPreferences.build(context, true);
    }

    public static synchronized TerminalTaskerPreferencesDataStore getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new TerminalTaskerPreferencesDataStore(context);
        }
        return mInstance;
    }

}
