package com.flomobility.anx.shared.notification;

import android.content.Context;

import com.flomobility.anx.shared.settings.preferences.FloAppSharedPreferences;
import com.flomobility.anx.shared.settings.preferences.TerminalPreferenceConstants;
import com.flomobility.anx.shared.terminal.TerminalConstants;

public class TerminalNotificationUtils {
    /**
     * Try to get the next unique notification id that isn't already being used by the app.
     *
     * Termux app and its plugin must use unique notification ids from the same pool due to usage of android:sharedUserId.
     * https://commonsware.com/blog/2017/06/07/jobscheduler-job-ids-libraries.html
     *
     * @param context The {@link Context} for operations.
     * @return Returns the notification id that should be safe to use.
     */
    public synchronized static int getNextNotificationId(final Context context) {
        if (context == null) return TerminalPreferenceConstants.TERMINAL_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID;

        FloAppSharedPreferences preferences = FloAppSharedPreferences.build(context);
        if (preferences == null) return TerminalPreferenceConstants.TERMINAL_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID;

        int lastNotificationId = preferences.getLastNotificationId();

        int nextNotificationId = lastNotificationId + 1;
        while(nextNotificationId == TerminalConstants.TERMUX_APP_NOTIFICATION_ID || nextNotificationId == TerminalConstants.TERMUX_RUN_COMMAND_NOTIFICATION_ID) {
            nextNotificationId++;
        }

        if (nextNotificationId == Integer.MAX_VALUE || nextNotificationId < 0)
            nextNotificationId = TerminalPreferenceConstants.TERMINAL_APP.DEFAULT_VALUE_KEY_LAST_NOTIFICATION_ID;

        preferences.setLastNotificationId(nextNotificationId);
        return nextNotificationId;
    }
}
