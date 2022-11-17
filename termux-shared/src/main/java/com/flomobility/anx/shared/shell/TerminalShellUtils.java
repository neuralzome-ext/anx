package com.flomobility.anx.shared.shell;

import android.content.Context;

import androidx.annotation.NonNull;

import com.flomobility.anx.shared.models.errors.Error;
import com.flomobility.anx.shared.terminal.TerminalConstants;
import com.flomobility.anx.shared.file.FileUtils;
import com.flomobility.anx.shared.logger.Logger;
import com.flomobility.anx.shared.packages.PackageUtils;
import com.flomobility.anx.shared.terminal.TerminalUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TerminalShellUtils {

    public static String TERMINAL_VERSION_NAME;
    public static String TERMINAL_IS_DEBUGGABLE_BUILD;
    public static String TERMINAL_APP_PID;
    public static String TERMINAL_APK_RELEASE;

    public static String TERMINAL_API_VERSION_NAME;

    public static String getDefaultWorkingDirectoryPath() {
        return TerminalConstants.TERMINAL_HOME_DIR_PATH;
    }

    public static String getDefaultBinPath() {
        return TerminalConstants.TERMINAL_BIN_PREFIX_DIR_PATH;
    }

    public static String[] buildEnvironment(Context currentPackageContext, boolean isFailSafe, String workingDirectory) {
        TerminalConstants.TERMINAL_HOME_DIR.mkdirs();

        if (workingDirectory == null || workingDirectory.isEmpty())
            workingDirectory = getDefaultWorkingDirectoryPath();

        List<String> environment = new ArrayList<>();

        loadTerminalEnvVariables(currentPackageContext);

        if (TERMINAL_VERSION_NAME != null)
            environment.add("TERMINAL_VERSION=" + TERMINAL_VERSION_NAME);
        if (TERMINAL_IS_DEBUGGABLE_BUILD != null)
            environment.add("TERMINAL_IS_DEBUGGABLE_BUILD=" + TERMINAL_IS_DEBUGGABLE_BUILD);
        if (TERMINAL_APP_PID != null)
            environment.add("TERMINAL_APP_PID=" + TERMINAL_APP_PID);
        if (TERMINAL_APK_RELEASE != null)
            environment.add("TERMINAL_APK_RELEASE=" + TERMINAL_APK_RELEASE);

        if (TERMINAL_API_VERSION_NAME != null)
            environment.add("TERMINAL_API_VERSION=" + TERMINAL_API_VERSION_NAME);

        environment.add("TERM=xterm-256color");
        environment.add("COLORTERM=truecolor");
        environment.add("HOME=" + TerminalConstants.TERMINAL_HOME_DIR_PATH);
        environment.add("PREFIX=" + TerminalConstants.TERMINAL_PREFIX_DIR_PATH);
        environment.add("BOOTCLASSPATH=" + System.getenv("BOOTCLASSPATH"));
        environment.add("ANDROID_ROOT=" + System.getenv("ANDROID_ROOT"));
        environment.add("ANDROID_DATA=" + System.getenv("ANDROID_DATA"));
        // EXTERNAL_STORAGE is needed for /system/bin/am to work on at least
        // Samsung S7 - see https://plus.google.com/110070148244138185604/posts/gp8Lk3aCGp3.
        environment.add("EXTERNAL_STORAGE=" + System.getenv("EXTERNAL_STORAGE"));

        // These variables are needed if running on Android 10 and higher.
        addToEnvIfPresent(environment, "ANDROID_ART_ROOT");
        addToEnvIfPresent(environment, "DEX2OATBOOTCLASSPATH");
        addToEnvIfPresent(environment, "ANDROID_I18N_ROOT");
        addToEnvIfPresent(environment, "ANDROID_RUNTIME_ROOT");
        addToEnvIfPresent(environment, "ANDROID_TZDATA_ROOT");

        if (isFailSafe) {
            // Keep the default path so that system binaries can be used in the failsafe session.
            environment.add("PATH= " + System.getenv("PATH"));
        } else {
            environment.add("LANG=en_US.UTF-8");
            environment.add("PATH=" + TerminalConstants.TERMINAL_BIN_PREFIX_DIR_PATH);
            environment.add("PWD=" + workingDirectory);
            environment.add("TMPDIR=" + TerminalConstants.TERMINAL_TMP_PREFIX_DIR_PATH);
        }

        return environment.toArray(new String[0]);
    }

    public static void addToEnvIfPresent(List<String> environment, String name) {
        String value = System.getenv(name);
        if (value != null) {
            environment.add(name + "=" + value);
        }
    }

    public static String[] setupProcessArgs(@NonNull String fileToExecute, String[] arguments) {
        // The file to execute may either be:
        // - An elf file, in which we execute it directly.
        // - A script file without shebang, which we execute with our standard shell $PREFIX/bin/sh instead of the
        //   system /system/bin/sh. The system shell may vary and may not work at all due to LD_LIBRARY_PATH.
        // - A file with shebang, which we try to handle with e.g. /bin/foo -> $PREFIX/bin/foo.
        String interpreter = null;
        try {
            File file = new File(fileToExecute);
            try (FileInputStream in = new FileInputStream(file)) {
                byte[] buffer = new byte[256];
                int bytesRead = in.read(buffer);
                if (bytesRead > 4) {
                    if (buffer[0] == 0x7F && buffer[1] == 'E' && buffer[2] == 'L' && buffer[3] == 'F') {
                        // Elf file, do nothing.
                    } else if (buffer[0] == '#' && buffer[1] == '!') {
                        // Try to parse shebang.
                        StringBuilder builder = new StringBuilder();
                        for (int i = 2; i < bytesRead; i++) {
                            char c = (char) buffer[i];
                            if (c == ' ' || c == '\n') {
                                if (builder.length() == 0) {
                                    // Skip whitespace after shebang.
                                } else {
                                    // End of shebang.
                                    String executable = builder.toString();
                                    if (executable.startsWith("/usr") || executable.startsWith("/bin")) {
                                        String[] parts = executable.split("/");
                                        String binary = parts[parts.length - 1];
                                        interpreter = TerminalConstants.TERMINAL_BIN_PREFIX_DIR_PATH + "/" + binary;
                                    }
                                    break;
                                }
                            } else {
                                builder.append(c);
                            }
                        }
                    } else {
                        // No shebang and no ELF, use standard shell.
                        interpreter = TerminalConstants.TERMINAL_BIN_PREFIX_DIR_PATH + "/sh";
                    }
                }
            }
        } catch (IOException e) {
            // Ignore.
        }

        List<String> result = new ArrayList<>();
        if (interpreter != null) result.add(interpreter);
        result.add(fileToExecute);
        if (arguments != null) Collections.addAll(result, arguments);
        return result.toArray(new String[0]);
    }

    public static void clearTermuxTMPDIR(boolean onlyIfExists) {
        if(onlyIfExists && !FileUtils.directoryFileExists(TerminalConstants.TERMINAL_TMP_PREFIX_DIR_PATH, false))
            return;

        Error error;
        error = FileUtils.clearDirectory("$TMPDIR", FileUtils.getCanonicalPath(TerminalConstants.TERMINAL_TMP_PREFIX_DIR_PATH, null));
        if (error != null) {
            Logger.logErrorExtended(error.toString());
        }
    }

    public static void loadTerminalEnvVariables(Context currentPackageContext) {
        String terminalAPKReleaseOld = TERMINAL_APK_RELEASE;
        TERMINAL_VERSION_NAME = TERMINAL_IS_DEBUGGABLE_BUILD = TERMINAL_APP_PID = TERMINAL_APK_RELEASE = null;

        // Check if Termux app is installed and not disabled
        if (TerminalUtils.isTerminalAppInstalled(currentPackageContext) == null) {
            // This function may be called by a different package like a plugin, so we get version for Termux package via its context
            Context termuxPackageContext = TerminalUtils.getTermuxPackageContext(currentPackageContext);
            if (termuxPackageContext != null) {
                TERMINAL_VERSION_NAME = PackageUtils.getVersionNameForPackage(termuxPackageContext);
                TERMINAL_IS_DEBUGGABLE_BUILD = PackageUtils.isAppForPackageADebuggableBuild(termuxPackageContext) ? "1" : "0";

                TERMINAL_APP_PID = TerminalUtils.getTermuxAppPID(currentPackageContext);

                // Getting APK signature is a slightly expensive operation, so do it only when needed
                if (terminalAPKReleaseOld == null) {
                    String signingCertificateSHA256Digest = PackageUtils.getSigningCertificateSHA256DigestForPackage(termuxPackageContext);
                    if (signingCertificateSHA256Digest != null)
                        TERMINAL_APK_RELEASE = TerminalUtils.getAPKRelease(signingCertificateSHA256Digest).replaceAll("[^a-zA-Z]", "_").toUpperCase();
                } else {
                    TERMINAL_APK_RELEASE = terminalAPKReleaseOld;
                }
            }
        }


        TERMINAL_API_VERSION_NAME = null;

        // Check if Termux:API app is installed and not disabled
        if (TerminalUtils.isTermuxAPIAppInstalled(currentPackageContext) == null) {
            // This function may be called by a different package like a plugin, so we get version for Termux:API package via its context
            Context termuxAPIPackageContext = TerminalUtils.getTermuxAPIPackageContext(currentPackageContext);
            if (termuxAPIPackageContext != null)
                TERMINAL_API_VERSION_NAME = PackageUtils.getVersionNameForPackage(termuxAPIPackageContext);
        }
    }

}
