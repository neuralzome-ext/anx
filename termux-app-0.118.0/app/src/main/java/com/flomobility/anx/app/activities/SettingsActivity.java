package com.flomobility.anx.app.activities;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.flomobility.anx.R;
import com.flomobility.anx.shared.activities.ReportActivity;
import com.flomobility.anx.shared.file.FileUtils;
import com.flomobility.anx.shared.models.ReportInfo;
import com.flomobility.anx.app.models.UserAction;
import com.flomobility.anx.shared.interact.ShareUtils;
import com.flomobility.anx.shared.packages.PackageUtils;
import com.flomobility.anx.shared.settings.preferences.TerminalAPIAppSharedPreferences;
import com.flomobility.anx.shared.settings.preferences.TerminalFloatAppSharedPreferences;
import com.flomobility.anx.shared.settings.preferences.TerminalTaskerAppSharedPreferences;
import com.flomobility.anx.shared.settings.preferences.TerminalWidgetAppSharedPreferences;
import com.flomobility.anx.shared.terminal.AndroidUtils;
import com.flomobility.anx.shared.terminal.TerminalConstants;
import com.flomobility.anx.shared.terminal.TerminalUtils;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new RootPreferencesFragment())
                .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public static class RootPreferencesFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            Context context = getContext();
            if (context == null) return;

            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            configureTerminalAPIPreference(context);
            configureTerminalFloatPreference(context);
            configureTerminalTaskerPreference(context);
            configureTerminalWidgetPreference(context);
            configureAboutPreference(context);
            configureDonatePreference(context);
        }

        private void configureTerminalAPIPreference(@NonNull Context context) {
            Preference termuxAPIPreference = findPreference("terminal_api");
            if (termuxAPIPreference != null) {
                TerminalAPIAppSharedPreferences preferences = TerminalAPIAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxAPIPreference.setVisible(preferences != null);
            }
        }

        private void configureTerminalFloatPreference(@NonNull Context context) {
            Preference termuxFloatPreference = findPreference("terminal_float");
            if (termuxFloatPreference != null) {
                TerminalFloatAppSharedPreferences preferences = TerminalFloatAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxFloatPreference.setVisible(preferences != null);
            }
        }

        private void configureTerminalTaskerPreference(@NonNull Context context) {
            Preference termuxTaskerPreference = findPreference("terminal_tasker");
            if (termuxTaskerPreference != null) {
                TerminalTaskerAppSharedPreferences preferences = TerminalTaskerAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxTaskerPreference.setVisible(preferences != null);
            }
        }

        private void configureTerminalWidgetPreference(@NonNull Context context) {
            Preference termuxWidgetPreference = findPreference("terminal_widget");
            if (termuxWidgetPreference != null) {
                TerminalWidgetAppSharedPreferences preferences = TerminalWidgetAppSharedPreferences.build(context, false);
                // If failed to get app preferences, then likely app is not installed, so do not show its preference
                termuxWidgetPreference.setVisible(preferences != null);
            }
        }

        private void configureAboutPreference(@NonNull Context context) {
            Preference aboutPreference = findPreference("about");
            if (aboutPreference != null) {
                aboutPreference.setOnPreferenceClickListener(preference -> {
                    new Thread() {
                        @Override
                        public void run() {
                            String title = "About";

                            StringBuilder aboutString = new StringBuilder();
                            aboutString.append(TerminalUtils.getAppInfoMarkdownString(context, false));

                            String termuxPluginAppsInfo =  TerminalUtils.getTermuxPluginAppsInfoMarkdownString(context);
                            if (termuxPluginAppsInfo != null)
                                aboutString.append("\n\n").append(termuxPluginAppsInfo);

                            aboutString.append("\n\n").append(AndroidUtils.getDeviceInfoMarkdownString(context));
                            aboutString.append("\n\n").append(TerminalUtils.getImportantLinksMarkdownString(context));

                            String userActionName = UserAction.ABOUT.getName();
                            ReportActivity.startReportActivity(context, new ReportInfo(userActionName,
                                TerminalConstants.TERMUX_APP.TERMUX_SETTINGS_ACTIVITY_NAME, title, null,
                                aboutString.toString(), null, false,
                                userActionName,
                                Environment.getExternalStorageDirectory() + "/" +
                                    FileUtils.sanitizeFileName(TerminalConstants.TERMINAL_APP_NAME + "-" + userActionName + ".log", true, true)));
                        }
                    }.start();

                    return true;
                });
            }
        }

        private void configureDonatePreference(@NonNull Context context) {
            Preference donatePreference = findPreference("donate");
            if (donatePreference != null) {
                String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(context);
                if (signingCertificateSHA256Digest != null) {
                    // If APK is a Google Playstore release, then do not show the donation link
                    // since Termux isn't exempted from the playstore policy donation links restriction
                    // Check Fund solicitations: https://pay.google.com/intl/en_in/about/policy/
                    String apkRelease = TerminalUtils.getAPKRelease(signingCertificateSHA256Digest);
                    if (apkRelease == null || apkRelease.equals(TerminalConstants.APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST)) {
                        donatePreference.setVisible(false);
                        return;
                    } else {
                        donatePreference.setVisible(true);
                    }
                }

                donatePreference.setOnPreferenceClickListener(preference -> {
                    ShareUtils.openURL(context, TerminalConstants.TERMUX_DONATE_URL);
                    return true;
                });
            }
        }
    }

}
