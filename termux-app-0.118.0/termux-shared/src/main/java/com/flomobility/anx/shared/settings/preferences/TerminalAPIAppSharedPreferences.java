package com.flomobility.anx.shared.settings.preferences;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.flomobility.anx.shared.logger.Logger;
import com.flomobility.anx.shared.packages.PackageUtils;
import com.flomobility.anx.shared.settings.preferences.TerminalPreferenceConstants.TERMINAL_API_APP;
import com.flomobility.anx.shared.terminal.TerminalConstants;

public class TerminalAPIAppSharedPreferences {

    private final Context mContext;
    private final SharedPreferences mSharedPreferences;
    private final SharedPreferences mMultiProcessSharedPreferences;


    private static final String LOG_TAG = "TermuxAPIAppSharedPreferences";

    private TerminalAPIAppSharedPreferences(@NonNull Context context) {
        mContext = context;
        mSharedPreferences = getPrivateSharedPreferences(mContext);
        mMultiProcessSharedPreferences = getPrivateAndMultiProcessSharedPreferences(mContext);
    }

    /**
     * Get the {@link Context} for a package name.
     *
     * @param context The {@link Context} to use to get the {@link Context} of the
     *                {@link TerminalConstants#TERMUX_API_PACKAGE_NAME}.
     * @return Returns the {@link TerminalAPIAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    @Nullable
    public static TerminalAPIAppSharedPreferences build(@NonNull final Context context) {
        Context termuxTaskerPackageContext = PackageUtils.getContextForPackage(context, TerminalConstants.TERMUX_API_PACKAGE_NAME);
        if (termuxTaskerPackageContext == null)
            return null;
        else
            return new TerminalAPIAppSharedPreferences(termuxTaskerPackageContext);
    }

    /**
     * Get the {@link Context} for a package name.
     *
     * @param context The {@link Activity} to use to get the {@link Context} of the
     *                {@link TerminalConstants#TERMUX_API_PACKAGE_NAME}.
     * @param exitAppOnError If {@code true} and failed to get package context, then a dialog will
     *                       be shown which when dismissed will exit the app.
     * @return Returns the {@link TerminalAPIAppSharedPreferences}. This will {@code null} if an exception is raised.
     */
    public static TerminalAPIAppSharedPreferences build(@NonNull final Context context, final boolean exitAppOnError) {
        Context termuxTaskerPackageContext = PackageUtils.getContextForPackageOrExitApp(context, TerminalConstants.TERMUX_API_PACKAGE_NAME, exitAppOnError);
        if (termuxTaskerPackageContext == null)
            return null;
        else
            return new TerminalAPIAppSharedPreferences(termuxTaskerPackageContext);
    }

    private static SharedPreferences getPrivateSharedPreferences(Context context) {
        if (context == null) return null;
        return SharedPreferenceUtils.getPrivateSharedPreferences(context, TerminalConstants.TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION);
    }

    private static SharedPreferences getPrivateAndMultiProcessSharedPreferences(Context context) {
        if (context == null) return null;
        return SharedPreferenceUtils.getPrivateAndMultiProcessSharedPreferences(context, TerminalConstants.TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION);
    }



    public int getLogLevel(boolean readFromFile) {
        if (readFromFile)
            return SharedPreferenceUtils.getInt(mMultiProcessSharedPreferences, TERMINAL_API_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
        else
            return SharedPreferenceUtils.getInt(mSharedPreferences, TERMINAL_API_APP.KEY_LOG_LEVEL, Logger.DEFAULT_LOG_LEVEL);
    }

    public void setLogLevel(Context context, int logLevel, boolean commitToFile) {
        logLevel = Logger.setLogLevel(context, logLevel);
        SharedPreferenceUtils.setInt(mSharedPreferences, TERMINAL_API_APP.KEY_LOG_LEVEL, logLevel, commitToFile);
    }

}
