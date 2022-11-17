package com.flomobility.anx.shared.crash;

import android.content.Context;

import androidx.annotation.NonNull;

import com.flomobility.anx.shared.terminal.TerminalConstants;
import com.flomobility.anx.shared.terminal.TerminalUtils;

public class TerminalCrashUtils implements CrashHandler.CrashHandlerClient {

    /**
     * Set default uncaught crash handler of current thread to {@link CrashHandler} for Termux app
     * and its plugin to log crashes at {@link TerminalConstants#TERMUX_CRASH_LOG_FILE_PATH}.
     */
    public static void setCrashHandler(@NonNull final Context context) {
        CrashHandler.setCrashHandler(context, new TerminalCrashUtils());
    }

    @NonNull
    @Override
    public String getCrashLogFilePath(Context context) {
        return TerminalConstants.TERMUX_CRASH_LOG_FILE_PATH;
    }

    @Override
    public String getAppInfoMarkdownString(Context context) {
        return TerminalUtils.getAppInfoMarkdownString(context, true);
    }

}
