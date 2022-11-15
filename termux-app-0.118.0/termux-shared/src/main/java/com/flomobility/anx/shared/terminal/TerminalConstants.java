package com.flomobility.anx.shared.terminal;

import android.annotation.SuppressLint;

import com.flomobility.anx.shared.models.ResultConfig;
import com.flomobility.anx.shared.models.errors.Errno;

import java.io.File;
import java.util.Arrays;
import java.util.Formatter;
import java.util.IllegalFormatException;
import java.util.List;

/*
 * Version: v0.32.0
 *
 * Changelog
 *
 * - 0.1.0 (2021-03-08)
 *      - Initial Release.
 *
 * - 0.2.0 (2021-03-11)
 *      - Added `_DIR` and `_FILE` substrings to paths.
 *      - Added `INTERNAL_PRIVATE_APP_DATA_DIR*`, `TERMUX_CACHE_DIR*`, `TERMUX_DATABASES_DIR*`,
 *          `TERMUX_SHARED_PREFERENCES_DIR*`, `TERMUX_BIN_PREFIX_DIR*`, `TERMUX_ETC_DIR*`,
 *          `TERMUX_INCLUDE_DIR*`, `TERMUX_LIB_DIR*`, `TERMUX_LIBEXEC_DIR*`, `TERMUX_SHARE_DIR*`,
 *          `TERMUX_TMP_DIR*`, `TERMUX_VAR_DIR*`, `TERMUX_STAGING_PREFIX_DIR*`,
 *          `TERMUX_STORAGE_HOME_DIR*`, `TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME*`,
 *          `TERMUX_DEFAULT_PREFERENCES_FILE`.
 *      - Renamed `DATA_HOME_PATH` to `TERMUX_DATA_HOME_DIR_PATH`.
 *      - Renamed `CONFIG_HOME_PATH` to `TERMUX_CONFIG_HOME_DIR_PATH`.
 *      - Updated javadocs and spacing.
 *
 * - 0.3.0 (2021-03-12)
 *      - Remove `TERMUX_CACHE_DIR_PATH*`, `TERMUX_DATABASES_DIR_PATH*`,
 *          `TERMUX_SHARED_PREFERENCES_DIR_PATH*` since they may not be consistent on all devices.
 *      - Renamed `TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME` to
 *          `TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`. This should be used for
 *           accessing shared preferences between Terminal app and its plugins if ever needed by first
 *           getting shared package context with {@link Context.createPackageContext(String,int}).
 *
 * - 0.4.0 (2021-03-16)
 *      - Added `BROADCAST_TERMUX_OPENED`,
 *          `TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`
 *          `TERMUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `TERMUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `TERMUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `TERMUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`,
 *          `TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION`.
 *
 * - 0.5.0 (2021-03-16)
 *      - Renamed "Termux Plugin app" labels to "Termux:Tasker app".
 *
 * - 0.6.0 (2021-03-16)
 *      - Added `TERMUX_FILE_SHARE_URI_AUTHORITY`.
 *
 * - 0.7.0 (2021-03-17)
 *      - Fixed javadocs.
 *
 * - 0.8.0 (2021-03-18)
 *      - Fixed Intent extra types javadocs.
 *      - Added following to `TERMUX_SERVICE`:
 *          `EXTRA_PENDING_INTENT`, `EXTRA_RESULT_BUNDLE`,
 *          `EXTRA_STDOUT`, `EXTRA_STDERR`, `EXTRA_EXIT_CODE`,
 *          `EXTRA_ERR`, `EXTRA_ERRMSG`.
 *
 * - 0.9.0 (2021-03-18)
 *      - Fixed javadocs.
 *
 * - 0.10.0 (2021-03-19)
 *      - Added following to `TERMUX_SERVICE`:
 *          `EXTRA_SESSION_ACTION`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY`,
 *          `VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY`
 *          `VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_SESSION_ACTION`.
 *
 * - 0.11.0 (2021-03-24)
 *      - Added following to `TERMUX_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`, `EXTRA_PLUGIN_API_HELP`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_COMMAND_LABEL`, `EXTRA_COMMAND_DESCRIPTION`, `EXTRA_COMMAND_HELP`.
 *      - Updated `RESULT_BUNDLE` related extras with `PLUGIN_RESULT_BUNDLE` prefixes.
 *
 * - 0.12.0 (2021-03-25)
 *      - Added following to `TERMUX_SERVICE`:
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH`,
 *          `EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH`.
 *
 * - 0.13.0 (2021-03-25)
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `EXTRA_PENDING_INTENT`.
 *
 * - 0.14.0 (2021-03-25)
 *      - Added `FDROID_PACKAGES_BASE_URL`,
 *          `TERMUX_GITHUB_ORGANIZATION_NAME`, `TERMUX_GITHUB_ORGANIZATION_URL`,
 *          `TERMUX_GITHUB_REPO_NAME`, `TERMUX_GITHUB_REPO_URL`, `TERMUX_FDROID_PACKAGE_URL`,
 *          `TERMUX_API_GITHUB_REPO_NAME`,`TERMUX_API_GITHUB_REPO_URL`, `TERMUX_API_FDROID_PACKAGE_URL`,
 *          `TERMUX_BOOT_GITHUB_REPO_NAME`, `TERMUX_BOOT_GITHUB_REPO_URL`, `TERMUX_BOOT_FDROID_PACKAGE_URL`,
 *          `TERMUX_FLOAT_GITHUB_REPO_NAME`, `TERMUX_FLOAT_GITHUB_REPO_URL`, `TERMUX_FLOAT_FDROID_PACKAGE_URL`,
 *          `TERMUX_STYLING_GITHUB_REPO_NAME`, `TERMUX_STYLING_GITHUB_REPO_URL`, `TERMUX_STYLING_FDROID_PACKAGE_URL`,
 *          `TERMUX_TASKER_GITHUB_REPO_NAME`, `TERMUX_TASKER_GITHUB_REPO_URL`, `TERMUX_TASKER_FDROID_PACKAGE_URL`,
 *          `TERMUX_WIDGET_GITHUB_REPO_NAME`, `TERMUX_WIDGET_GITHUB_REPO_URL` `TERMUX_WIDGET_FDROID_PACKAGE_URL`.
 *
 * - 0.15.0 (2021-04-06)
 *      - Fixed some variables that had `PREFIX_` substring missing in their name.
 *      - Added `TERMUX_CRASH_LOG_FILE_PATH`, `TERMUX_CRASH_LOG_BACKUP_FILE_PATH`,
 *          `TERMUX_GITHUB_ISSUES_REPO_URL`, `TERMUX_API_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_BOOT_GITHUB_ISSUES_REPO_URL`, `TERMUX_FLOAT_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_STYLING_GITHUB_ISSUES_REPO_URL`, `TERMUX_TASKER_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_WIDGET_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_GITHUB_WIKI_REPO_URL`, `TERMUX_PACKAGES_GITHUB_WIKI_REPO_URL`,
 *          `TERMUX_PACKAGES_GITHUB_REPO_NAME`, `TERMUX_PACKAGES_GITHUB_REPO_URL`, `TERMUX_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_GAME_PACKAGES_GITHUB_REPO_NAME`, `TERMUX_GAME_PACKAGES_GITHUB_REPO_URL`, `TERMUX_GAME_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_SCIENCE_PACKAGES_GITHUB_REPO_NAME`, `TERMUX_SCIENCE_PACKAGES_GITHUB_REPO_URL`, `TERMUX_SCIENCE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_ROOT_PACKAGES_GITHUB_REPO_NAME`, `TERMUX_ROOT_PACKAGES_GITHUB_REPO_URL`, `TERMUX_ROOT_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_UNSTABLE_PACKAGES_GITHUB_REPO_NAME`, `TERMUX_UNSTABLE_PACKAGES_GITHUB_REPO_URL`, `TERMUX_UNSTABLE_PACKAGES_GITHUB_ISSUES_REPO_URL`,
 *          `TERMUX_X11_PACKAGES_GITHUB_REPO_NAME`, `TERMUX_X11_PACKAGES_GITHUB_REPO_URL`, `TERMUX_X11_PACKAGES_GITHUB_ISSUES_REPO_URL`.
 *      - Added following to `RUN_COMMAND_SERVICE`:
 *          `RUN_COMMAND_API_HELP_URL`.
 *
 * - 0.16.0 (2021-04-06)
 *      - Added `TERMUX_SUPPORT_EMAIL`, `TERMUX_SUPPORT_EMAIL_URL`, `TERMUX_SUPPORT_EMAIL_MAILTO_URL`,
 *          `TERMUX_REDDIT_SUBREDDIT`, `TERMUX_REDDIT_SUBREDDIT_URL`.
 *      - The `TERMUX_SUPPORT_EMAIL_URL` value must be fixed later when email has been set up.
 *
 * - 0.17.0 (2021-04-07)
 *      - Added `TERMUX_APP_NOTIFICATION_CHANNEL_ID`, `TERMUX_APP_NOTIFICATION_CHANNEL_NAME`, `TERMUX_APP_NOTIFICATION_ID`,
 *          `TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_ID`, `TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME`, `TERMUX_RUN_COMMAND_NOTIFICATION_ID`,
 *          `TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID`, `TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME`,
 *          `TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID`, `TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME`.
 *      - Updated javadocs.
 *
 * - 0.18.0 (2021-04-11)
 *      - Updated `TERMUX_SUPPORT_EMAIL_URL` to a valid email.
 *      - Removed `TERMUX_SUPPORT_EMAIL`.
 *
 * - 0.19.0 (2021-04-12)
 *      - Added `TERMUX_ACTIVITY.ACTION_REQUEST_PERMISSIONS`.
 *      - Added `TERMUX_SERVICE.EXTRA_STDIN`.
 *      - Added `RUN_COMMAND_SERVICE.EXTRA_STDIN`.
 *      - Deprecated `TERMUX_ACTIVITY.EXTRA_RELOAD_STYLE`.
 *
 * - 0.20.0 (2021-05-13)
 *      - Added `TERMUX_WIKI`, `TERMUX_WIKI_URL`, `TERMUX_PLUGIN_APP_NAMES_LIST`, `TERMUX_PLUGIN_APP_PACKAGE_NAMES_LIST`.
 *      - Added `TERMUX_SETTINGS_ACTIVITY_NAME`.
 *
 * - 0.21.0 (2021-05-13)
 *      - Added `APK_RELEASE_FDROID`, `APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD`, `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST`,
 *          `APK_RELEASE_GOOGLE_PLAYSTORE`, `APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.22.0 (2021-05-13)
 *      - Added `TERMUX_DONATE_URL`.
 *
 * - 0.23.0 (2021-06-12)
 *      - Rename `INTERNAL_PRIVATE_APP_DATA_DIR_PATH` to `TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH`.
 *
 * - 0.24.0 (2021-06-27)
 *      - Add `COMMA_NORMAL`, `COMMA_ALTERNATIVE`.
 *      - Added following to `TERMUX_APP.TERMUX_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`.
 *      - Added following to `TERMUX_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_RESULT_DIRECTORY`, `EXTRA_RESULT_SINGLE_FILE`, `EXTRA_RESULT_FILE_BASENAME`,
 *          `EXTRA_RESULT_FILE_OUTPUT_FORMAT`, `EXTRA_RESULT_FILE_ERROR_FORMAT`, `EXTRA_RESULT_FILES_SUFFIX`,
 *          `EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`, `EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS`.
 *      - Added following to `RESULT_SENDER`:
 *           `FORMAT_SUCCESS_STDOUT`, `FORMAT_SUCCESS_STDOUT__EXIT_CODE`, `FORMAT_SUCCESS_STDOUT__STDERR__EXIT_CODE`
 *           `FORMAT_FAILED_ERR__ERRMSG__STDOUT__STDERR__EXIT_CODE`,
 *           `RESULT_FILE_ERR_PREFIX`, `RESULT_FILE_ERRMSG_PREFIX` `RESULT_FILE_STDOUT_PREFIX`,
 *           `RESULT_FILE_STDERR_PREFIX`, `RESULT_FILE_EXIT_CODE_PREFIX`.
 *
 * - 0.25.0 (2021-08-19)
 *      - Added following to `TERMUX_APP.TERMUX_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *      - Added following to `TERMUX_APP.RUN_COMMAND_SERVICE`:
 *          `EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL`.
 *
 * - 0.26.0 (2021-08-25)
 *      - Changed `TERMUX_ACTIVITY.ACTION_FAILSAFE_SESSION` to `TERMUX_ACTIVITY.EXTRA_FAILSAFE_SESSION`.
 *
 * - 0.27.0 (2021-09-02)
 *      - Added `TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID`, `TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME`,
 *          `TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE_NAME`.
 *      - Added following to `TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE`:
 *          `ACTION_STOP_SERVICE`, `ACTION_SHOW`, `ACTION_HIDE`.
 *
 * - 0.28.0 (2021-09-02)
 *      - Added `TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE*` and `TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE*`.
 *
 * - 0.29.0 (2021-09-04)
 *      - Added `TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME`, `TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME`,
 *          `TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH`, `TERMUX_SHORTCUT_SCRIPT_ICONS_DIR`.
 *      - Added following to `TERMUX_WIDGET.TERMUX_WIDGET_PROVIDER`:
 *          `ACTION_WIDGET_ITEM_CLICKED`, `ACTION_REFRESH_WIDGET`, `EXTRA_FILE_CLICKED`.
 *      - Changed naming convention of `TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE.ACTION_*`.
 *      - Fixed wrong path set for `TERMUX_SHORTCUT_SCRIPTS_DIR_PATH`.
 *
 * - 0.30.0 (2021-09-08)
 *      - Changed `APK_RELEASE_GITHUB_DEBUG_BUILD`to `APK_RELEASE_GITHUB` and
 *          `APK_RELEASE_GITHUB_DEBUG_BUILD_SIGNING_CERTIFICATE_SHA256_DIGEST` to
 *          `APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST`.
 *
 * - 0.31.0 (2021-09-09)
 *      - Added following to `TERMUX_APP.TERMUX_SERVICE`:
 *          `MIN_VALUE_EXTRA_SESSION_ACTION` and `MAX_VALUE_EXTRA_SESSION_ACTION`.
 *
 * - 0.32.0 (2021-09-23)
 *      - Added `TERMUX_API.TERMUX_API_ACTIVITY_NAME`, `TERMUX_TASKER.TERMUX_TASKER_ACTIVITY_NAME`
 *          and `TERMUX_WIDGET.TERMUX_WIDGET_ACTIVITY_NAME`.
 */

/**
 * A class that defines shared constants of the Termux app and its plugins.
 * This class will be hosted by terminal-shared lib and should be imported by other terminal plugin
 * apps as is instead of copying constants to random classes. The 3rd party apps can also import
 * it for interacting with terminal apps. If changes are made to this file, increment the version number
 * and add an entry in the Changelog section above.
 *
 * Termux app default package name is "com.terminal" and is used in {@link #TERMINAL_PREFIX_DIR_PATH}.
 * The binaries compiled for terminal have {@link #TERMINAL_PREFIX_DIR_PATH} hardcoded in them but it
 * can be changed during compilation.
 *
 * The {@link #TERMINAL_PACKAGE_NAME} must be the same as the applicationId of terminal-app build.gradle
 * since its also used by {@link #TERMUX_FILES_DIR_PATH}.
 * If {@link #TERMINAL_PACKAGE_NAME} is changed, then binaries, specially used in bootstrap need to be
 * compiled appropriately. Check https://github.com/terminal/terminal-packages/wiki/Building-packages
 * for more info.
 *
 * Ideally the only places where changes should be required if changing package name are the following:
 * - The {@link #TERMINAL_PACKAGE_NAME} in {@link TerminalConstants}.
 * - The "applicationId" in "build.gradle" of terminal-app. This is package name that android and app
 *      stores will use and is also the final package name stored in "AndroidManifest.xml".
 * - The "manifestPlaceholders" values for {@link #TERMINAL_PACKAGE_NAME} and *_APP_NAME in
 *      "build.gradle" of terminal-app.
 * - The "ENTITY" values for {@link #TERMINAL_PACKAGE_NAME} and *_APP_NAME in "strings.xml" of
 *      terminal-app and of terminal-shared.
 * - The "shortcut.xml" and "*_preferences.xml" files of terminal-app since dynamic variables don't
 *      work in it.
 * - Optionally the "package" in "AndroidManifest.xml" if modifying project structure of terminal-app.
 *      This is package name for java classes project structure and is prefixed if activity and service
 *      names use dot (.) notation. This is currently not advisable since this will break lot of
 *      stuff, including terminal-* packages.
 * - Optionally the *_PATH variables in {@link TerminalConstants} containing the string "terminal".
 *
 * Check https://developer.android.com/studio/build/application-id for info on "package" in
 * "AndroidManifest.xml" and "applicationId" in "build.gradle".
 *
 * The {@link #TERMINAL_PACKAGE_NAME} must be used in source code of Termux app and its plugins instead
 * of hardcoded "com.terminal" paths.
 */
public final class TerminalConstants {


    /*
     * Termux organization variables.
     */

    /** Termux Github organization name */
    public static final String TERMINAL_GITHUB_ORGANIZATION_NAME = "terminal"; // Default: "terminal"
    /** Termux Github organization url */
    public static final String TERMINAL_GITHUB_ORGANIZATION_URL = "https://github.com" + "/" + TERMINAL_GITHUB_ORGANIZATION_NAME; // Default: "https://github.com/flo"

    /** F-Droid packages base url */
    public static final String FDROID_PACKAGES_BASE_URL = "https://f-droid.org/en/packages"; // Default: "https://f-droid.org/en/packages"





    /*
     * Termux and its plugin app and package names and urls.
     */

    /** Termux app name */
    public static final String TERMINAL_APP_NAME = "ANX"; // Default: "Termux"
    /** Termux package name */
    public static final String TERMINAL_PACKAGE_NAME = "com.flomobility.anx"; // Default: "com.terminal"
    /** Termux Github repo name */
    public static final String TERMUX_GITHUB_REPO_NAME = "terminal-app"; // Default: "terminal-app"
    /** Termux Github repo url */
    public static final String TERMUX_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_GITHUB_REPO_NAME; // Default: "https://github.com/flomobility/terminal-app"
    /** Termux Github issues repo url */
    public static final String TERMUX_GITHUB_ISSUES_REPO_URL = TERMUX_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/flomobility/terminal-app/issues"
    /** Termux F-Droid package url */
    public static final String TERMUX_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMINAL_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.terminal"


    /** Termux:API app name */
    public static final String TERMUX_API_APP_NAME = "Terminal:API"; // Default: "Termux:API"
    /** Termux:API app package name */
    public static final String TERMUX_API_PACKAGE_NAME = TERMINAL_PACKAGE_NAME + ".api"; // Default: "com.terminal.api"
    /** Termux:API Github repo name */
    public static final String TERMUX_API_GITHUB_REPO_NAME = "terminal-api"; // Default: "terminal-api"
    /** Termux:API Github repo url */
    public static final String TERMUX_API_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_API_GITHUB_REPO_NAME;
    /** Termux:API Github issues repo url */
    public static final String TERMUX_API_GITHUB_ISSUES_REPO_URL = TERMUX_API_GITHUB_REPO_URL + "/issues";
    /** Termux:API F-Droid package url */
    public static final String TERMUX_API_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_API_PACKAGE_NAME;


    /** Termux:Boot app name */
    public static final String TERMUX_BOOT_APP_NAME = "Terminal:Boot"; // Default: "Termux:Boot"
    /** Termux:Boot app package name */
    public static final String TERMUX_BOOT_PACKAGE_NAME = TERMINAL_PACKAGE_NAME + ".boot"; // Default: "com.terminal.boot"
    /** Termux:Boot Github repo name */
    public static final String TERMUX_BOOT_GITHUB_REPO_NAME = "terminal-boot"; // Default: "terminal-boot"
    /** Termux:Boot Github repo url */
    public static final String TERMUX_BOOT_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_BOOT_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/terminal-boot"
    /** Termux:Boot Github issues repo url */
    public static final String TERMUX_BOOT_GITHUB_ISSUES_REPO_URL = TERMUX_BOOT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/terminal-boot/issues"
    /** Termux:Boot F-Droid package url */
    public static final String TERMUX_BOOT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_BOOT_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.terminal.boot"


    /** Termux:Float app name */
    public static final String TERMUX_FLOAT_APP_NAME = "Termux:Float"; // Default: "Termux:Float"
    /** Termux:Float app package name */
    public static final String TERMUX_FLOAT_PACKAGE_NAME = TERMINAL_PACKAGE_NAME + ".window"; // Default: "com.terminal.window"
    /** Termux:Float Github repo name */
    public static final String TERMUX_FLOAT_GITHUB_REPO_NAME = "terminal-float"; // Default: "terminal-float"
    /** Termux:Float Github repo url */
    public static final String TERMUX_FLOAT_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_FLOAT_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/terminal-float"
    /** Termux:Float Github issues repo url */
    public static final String TERMUX_FLOAT_GITHUB_ISSUES_REPO_URL = TERMUX_FLOAT_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/terminal-float/issues"
    /** Termux:Float F-Droid package url */
    public static final String TERMUX_FLOAT_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_FLOAT_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.terminal.window"


    /** Termux:Styling app name */
    public static final String TERMUX_STYLING_APP_NAME = "Termux:Styling"; // Default: "Termux:Styling"
    /** Termux:Styling app package name */
    public static final String TERMUX_STYLING_PACKAGE_NAME = TERMINAL_PACKAGE_NAME + ".styling"; // Default: "com.terminal.styling"
    /** Termux:Styling Github repo name */
    public static final String TERMUX_STYLING_GITHUB_REPO_NAME = "terminal-styling"; // Default: "terminal-styling"
    /** Termux:Styling Github repo url */
    public static final String TERMUX_STYLING_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_STYLING_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/terminal-styling"
    /** Termux:Styling Github issues repo url */
    public static final String TERMUX_STYLING_GITHUB_ISSUES_REPO_URL = TERMUX_STYLING_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/terminal-styling/issues"
    /** Termux:Styling F-Droid package url */
    public static final String TERMUX_STYLING_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_STYLING_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.terminal.styling"


    /** Termux:Tasker app name */
    public static final String TERMUX_TASKER_APP_NAME = "Termux:Tasker"; // Default: "Termux:Tasker"
    /** Termux:Tasker app package name */
    public static final String TERMUX_TASKER_PACKAGE_NAME = TERMINAL_PACKAGE_NAME + ".tasker"; // Default: "com.terminal.tasker"
    /** Termux:Tasker Github repo name */
    public static final String TERMUX_TASKER_GITHUB_REPO_NAME = "terminal-tasker"; // Default: "terminal-tasker"
    /** Termux:Tasker Github repo url */
    public static final String TERMUX_TASKER_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_TASKER_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/terminal-tasker"
    /** Termux:Tasker Github issues repo url */
    public static final String TERMUX_TASKER_GITHUB_ISSUES_REPO_URL = TERMUX_TASKER_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/terminal-tasker/issues"
    /** Termux:Tasker F-Droid package url */
    public static final String TERMUX_TASKER_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_TASKER_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.terminal.tasker"


    /** Termux:Widget app name */
    public static final String TERMUX_WIDGET_APP_NAME = "Termux:Widget"; // Default: "Termux:Widget"
    /** Termux:Widget app package name */
    public static final String TERMUX_WIDGET_PACKAGE_NAME = TERMINAL_PACKAGE_NAME + ".widget"; // Default: "com.terminal.widget"
    /** Termux:Widget Github repo name */
    public static final String TERMUX_WIDGET_GITHUB_REPO_NAME = "terminal-widget"; // Default: "terminal-widget"
    /** Termux:Widget Github repo url */
    public static final String TERMUX_WIDGET_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_WIDGET_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/terminal-widget"
    /** Termux:Widget Github issues repo url */
    public static final String TERMUX_WIDGET_GITHUB_ISSUES_REPO_URL = TERMUX_WIDGET_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/terminal-widget/issues"
    /** Termux:Widget F-Droid package url */
    public static final String TERMUX_WIDGET_FDROID_PACKAGE_URL = FDROID_PACKAGES_BASE_URL + "/" + TERMUX_WIDGET_PACKAGE_NAME; // Default: "https://f-droid.org/en/packages/com.terminal.widget"





    /*
     * Termux plugin apps lists.
     */

    public static final List<String> TERMUX_PLUGIN_APP_NAMES_LIST = Arrays.asList(
        TERMUX_API_APP_NAME,
        TERMUX_BOOT_APP_NAME,
        TERMUX_FLOAT_APP_NAME,
        TERMUX_STYLING_APP_NAME,
        TERMUX_TASKER_APP_NAME,
        TERMUX_WIDGET_APP_NAME);

    public static final List<String> TERMUX_PLUGIN_APP_PACKAGE_NAMES_LIST = Arrays.asList(
        TERMUX_API_PACKAGE_NAME,
        TERMUX_BOOT_PACKAGE_NAME,
        TERMUX_FLOAT_PACKAGE_NAME,
        TERMUX_STYLING_PACKAGE_NAME,
        TERMUX_TASKER_PACKAGE_NAME,
        TERMUX_WIDGET_PACKAGE_NAME);





    /*
     * Termux APK releases.
     */

    /** F-Droid APK release */
    public static final String APK_RELEASE_FDROID = "F-Droid"; // Default: "F-Droid"

    /** F-Droid APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_FDROID_SIGNING_CERTIFICATE_SHA256_DIGEST = "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"; // Default: "228FB2CFE90831C1499EC3CCAF61E96E8E1CE70766B9474672CE427334D41C42"

    /** Github APK release */
    public static final String APK_RELEASE_GITHUB = "Github"; // Default: "Github"

    /** Github APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GITHUB_SIGNING_CERTIFICATE_SHA256_DIGEST = "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"; // Default: "B6DA01480EEFD5FBF2CD3771B8D1021EC791304BDD6C4BF41D3FAABAD48EE5E1"

    /** Google Play Store APK release */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE = "Google Play Store"; // Default: "Google Play Store"

    /** Google Play Store APK release signing certificate SHA-256 digest */
    public static final String APK_RELEASE_GOOGLE_PLAYSTORE_SIGNING_CERTIFICATE_SHA256_DIGEST = "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"; // Default: "738F0A30A04D3C8A1BE304AF18D0779BCF3EA88FB60808F657A3521861C2EBF9"





    /*
     * Termux packages urls.
     */

    /** Termux Packages Github repo name */
    public static final String TERMUX_PACKAGES_GITHUB_REPO_NAME = "terminal-packages"; // Default: "terminal-packages"
    /** Termux Packages Github repo url */
    public static final String TERMUX_PACKAGES_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/terminal-packages"
    /** Termux Packages Github issues repo url */
    public static final String TERMUX_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/terminal-packages/issues"


    /** Termux Game Packages Github repo name */
    public static final String TERMUX_GAME_PACKAGES_GITHUB_REPO_NAME = "game-packages"; // Default: "game-packages"
    /** Termux Game Packages Github repo url */
    public static final String TERMUX_GAME_PACKAGES_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_GAME_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/game-packages"
    /** Termux Game Packages Github issues repo url */
    public static final String TERMUX_GAME_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_GAME_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/game-packages/issues"


    /** Termux Science Packages Github repo name */
    public static final String TERMUX_SCIENCE_PACKAGES_GITHUB_REPO_NAME = "science-packages"; // Default: "science-packages"
    /** Termux Science Packages Github repo url */
    public static final String TERMUX_SCIENCE_PACKAGES_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_SCIENCE_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/science-packages"
    /** Termux Science Packages Github issues repo url */
    public static final String TERMUX_SCIENCE_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_SCIENCE_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/science-packages/issues"


    /** Termux Root Packages Github repo name */
    public static final String TERMUX_ROOT_PACKAGES_GITHUB_REPO_NAME = "terminal-root-packages"; // Default: "terminal-root-packages"
    /** Termux Root Packages Github repo url */
    public static final String TERMUX_ROOT_PACKAGES_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_ROOT_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/terminal-root-packages"
    /** Termux Root Packages Github issues repo url */
    public static final String TERMUX_ROOT_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_ROOT_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/terminal-root-packages/issues"


    /** Termux Unstable Packages Github repo name */
    public static final String TERMUX_UNSTABLE_PACKAGES_GITHUB_REPO_NAME = "unstable-packages"; // Default: "unstable-packages"
    /** Termux Unstable Packages Github repo url */
    public static final String TERMUX_UNSTABLE_PACKAGES_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_UNSTABLE_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/unstable-packages"
    /** Termux Unstable Packages Github issues repo url */
    public static final String TERMUX_UNSTABLE_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_UNSTABLE_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/unstable-packages/issues"


    /** Termux X11 Packages Github repo name */
    public static final String TERMUX_X11_PACKAGES_GITHUB_REPO_NAME = "x11-packages"; // Default: "x11-packages"
    /** Termux X11 Packages Github repo url */
    public static final String TERMUX_X11_PACKAGES_GITHUB_REPO_URL = TERMINAL_GITHUB_ORGANIZATION_URL + "/" + TERMUX_X11_PACKAGES_GITHUB_REPO_NAME; // Default: "https://github.com/terminal/x11-packages"
    /** Termux X11 Packages Github issues repo url */
    public static final String TERMUX_X11_PACKAGES_GITHUB_ISSUES_REPO_URL = TERMUX_X11_PACKAGES_GITHUB_REPO_URL + "/issues"; // Default: "https://github.com/terminal/x11-packages/issues"





    /*
     * Termux miscellaneous urls.
     */

    /** Termux Wiki */
    public static final String TERMUX_WIKI = TERMINAL_APP_NAME + " Wiki"; // Default: "Termux Wiki"

    /** Termux Wiki url */
    public static final String TERMUX_WIKI_URL = "https://wiki.terminal.com"; // Default: "https://wiki.terminal.com"

    /** Termux Github wiki repo url */
    public static final String TERMUX_GITHUB_WIKI_REPO_URL = TERMUX_GITHUB_REPO_URL + "/wiki"; // Default: "https://github.com/terminal/terminal-app/wiki"

    /** Termux Packages wiki repo url */
    public static final String TERMUX_PACKAGES_GITHUB_WIKI_REPO_URL = TERMUX_PACKAGES_GITHUB_REPO_URL + "/wiki"; // Default: "https://github.com/terminal/terminal-packages/wiki"


    /** Termux support email url */
    public static final String TERMUX_SUPPORT_EMAIL_URL = "terminalreports@groups.io"; // Default: "terminalreports@groups.io"

    /** Termux support email mailto url */
    public static final String TERMUX_SUPPORT_EMAIL_MAILTO_URL = "mailto:" + TERMUX_SUPPORT_EMAIL_URL; // Default: "mailto:terminalreports@groups.io"


    /** Termux Reddit subreddit */
    public static final String TERMUX_REDDIT_SUBREDDIT = "r/terminal"; // Default: "r/terminal"

    /** Termux Reddit subreddit url */
    public static final String TERMUX_REDDIT_SUBREDDIT_URL = "https://www.reddit.com/r/terminal"; // Default: "https://www.reddit.com/r/terminal"


    /** Termux donate url */
    public static final String TERMUX_DONATE_URL = TERMUX_PACKAGES_GITHUB_REPO_URL + "/wiki/Donate"; // Default: "https://github.com/terminal/terminal-packages/wiki/Donate"





    /*
     * Termux app core directory paths.
     */

    /** Termux app internal private app data directory path */
    @SuppressLint("SdCardPath")
    public static final String TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH = "/data/data/" + TERMINAL_PACKAGE_NAME; // Default: "/data/data/com.terminal"
    /** Termux app internal private app data directory */
    public static final File TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR = new File(TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH);



    /** Termux app Files directory path */
    public static final String TERMUX_FILES_DIR_PATH = TERMUX_INTERNAL_PRIVATE_APP_DATA_DIR_PATH + "/files"; // Default: "/data/data/com.terminal/files"
    /** Termux app Files directory */
    public static final File TERMUX_FILES_DIR = new File(TERMUX_FILES_DIR_PATH);



    /** Termux app $PREFIX directory path */
    public static final String TERMINAL_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr"; // Default: "/data/data/com.terminal/files/usr"
    /** Termux app $PREFIX directory */
    public static final File TERMUX_PREFIX_DIR = new File(TERMINAL_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/bin directory path */
    public static final String TERMINAL_BIN_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/bin"; // Default: "/data/data/com.terminal/files/usr/bin"
    /** Termux app $PREFIX/bin directory */
    public static final File TERMUX_BIN_PREFIX_DIR = new File(TERMINAL_BIN_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/etc directory path */
    public static final String TERMUX_ETC_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/etc"; // Default: "/data/data/com.terminal/files/usr/etc"
    /** Termux app $PREFIX/etc directory */
    public static final File TERMUX_ETC_PREFIX_DIR = new File(TERMUX_ETC_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/include directory path */
    public static final String TERMUX_INCLUDE_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/include"; // Default: "/data/data/com.terminal/files/usr/include"
    /** Termux app $PREFIX/include directory */
    public static final File TERMUX_INCLUDE_PREFIX_DIR = new File(TERMUX_INCLUDE_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/lib directory path */
    public static final String TERMUX_LIB_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/lib"; // Default: "/data/data/com.terminal/files/usr/lib"
    /** Termux app $PREFIX/lib directory */
    public static final File TERMUX_LIB_PREFIX_DIR = new File(TERMUX_LIB_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/libexec directory path */
    public static final String TERMUX_LIBEXEC_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/libexec"; // Default: "/data/data/com.terminal/files/usr/libexec"
    /** Termux app $PREFIX/libexec directory */
    public static final File TERMUX_LIBEXEC_PREFIX_DIR = new File(TERMUX_LIBEXEC_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/share directory path */
    public static final String TERMUX_SHARE_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/share"; // Default: "/data/data/com.terminal/files/usr/share"
    /** Termux app $PREFIX/share directory */
    public static final File TERMUX_SHARE_PREFIX_DIR = new File(TERMUX_SHARE_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/tmp and $TMPDIR directory path */
    public static final String TERMINAL_TMP_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/tmp"; // Default: "/data/data/com.terminal/files/usr/tmp"
    /** Termux app $PREFIX/tmp and $TMPDIR directory */
    public static final File TERMUX_TMP_PREFIX_DIR = new File(TERMINAL_TMP_PREFIX_DIR_PATH);


    /** Termux app $PREFIX/var directory path */
    public static final String TERMUX_VAR_PREFIX_DIR_PATH = TERMINAL_PREFIX_DIR_PATH + "/var"; // Default: "/data/data/com.terminal/files/usr/var"
    /** Termux app $PREFIX/var directory */
    public static final File TERMUX_VAR_PREFIX_DIR = new File(TERMUX_VAR_PREFIX_DIR_PATH);



    /** Termux app usr-staging directory path */
    public static final String TERMUX_STAGING_PREFIX_DIR_PATH = TERMUX_FILES_DIR_PATH + "/usr-staging"; // Default: "/data/data/com.terminal/files/usr-staging"
    /** Termux app usr-staging directory */
    public static final File TERMUX_STAGING_PREFIX_DIR = new File(TERMUX_STAGING_PREFIX_DIR_PATH);



    /** Termux app $HOME directory path */
    public static final String TERMINAL_HOME_DIR_PATH = TERMUX_FILES_DIR_PATH + "/home"; // Default: "/data/data/com.terminal/files/home"
    /** Termux app $HOME directory */
    public static final File TERMINAL_HOME_DIR = new File(TERMINAL_HOME_DIR_PATH);


    /** Termux app config home directory path */
    public static final String TERMUX_CONFIG_HOME_DIR_PATH = TERMINAL_HOME_DIR_PATH + "/.config/terminal"; // Default: "/data/data/com.terminal/files/home/.config/terminal"
    /** Termux app config home directory */
    public static final File TERMUX_CONFIG_HOME_DIR = new File(TERMUX_CONFIG_HOME_DIR_PATH);


    /** Termux app data home directory path */
    public static final String TERMUX_DATA_HOME_DIR_PATH = TERMINAL_HOME_DIR_PATH + "/.terminal"; // Default: "/data/data/com.terminal/files/home/.terminal"
    /** Termux app data home directory */
    public static final File TERMUX_DATA_HOME_DIR = new File(TERMUX_DATA_HOME_DIR_PATH);


    /** Termux app storage home directory path */
    public static final String TERMUX_STORAGE_HOME_DIR_PATH = TERMINAL_HOME_DIR_PATH + "/storage"; // Default: "/data/data/com.terminal/files/home/storage"
    /** Termux app storage home directory */
    public static final File TERMUX_STORAGE_HOME_DIR = new File(TERMUX_STORAGE_HOME_DIR_PATH);





    /*
     * Termux app and plugin preferences and properties file paths.
     */

    /** Termux app default SharedPreferences file basename without extension */
    public static final String TERMUX_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMINAL_PACKAGE_NAME + "_preferences"; // Default: "com.terminal_preferences"

    /** Termux:API app default SharedPreferences file basename without extension */
    public static final String TERMUX_API_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_API_PACKAGE_NAME + "_preferences"; // Default: "com.terminal.api_preferences"

    /** Termux:Boot app default SharedPreferences file basename without extension */
    public static final String TERMUX_BOOT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_BOOT_PACKAGE_NAME + "_preferences"; // Default: "com.terminal.boot_preferences"

    /** Termux:Float app default SharedPreferences file basename without extension */
    public static final String TERMUX_FLOAT_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_FLOAT_PACKAGE_NAME + "_preferences"; // Default: "com.terminal.window_preferences"

    /** Termux:Styling app default SharedPreferences file basename without extension */
    public static final String TERMUX_STYLING_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_STYLING_PACKAGE_NAME + "_preferences"; // Default: "com.terminal.styling_preferences"

    /** Termux:Tasker app default SharedPreferences file basename without extension */
    public static final String TERMUX_TASKER_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_TASKER_PACKAGE_NAME + "_preferences"; // Default: "com.terminal.tasker_preferences"

    /** Termux:Widget app default SharedPreferences file basename without extension */
    public static final String TERMUX_WIDGET_DEFAULT_PREFERENCES_FILE_BASENAME_WITHOUT_EXTENSION = TERMUX_WIDGET_PACKAGE_NAME + "_preferences"; // Default: "com.terminal.widget_preferences"


    /** Termux app terminal.properties primary file path */
    public static final String TERMUX_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.properties"; // Default: "/data/data/com.terminal/files/home/.terminal/terminal.properties"
    /** Termux app terminal.properties primary file */
    public static final File TERMUX_PROPERTIES_PRIMARY_FILE = new File(TERMUX_PROPERTIES_PRIMARY_FILE_PATH);

    /** Termux app terminal.properties secondary file path */
    public static final String TERMUX_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.properties"; // Default: "/data/data/com.terminal/files/home/.config/terminal/terminal.properties"
    /** Termux app terminal.properties secondary file */
    public static final File TERMUX_PROPERTIES_SECONDARY_FILE = new File(TERMUX_PROPERTIES_SECONDARY_FILE_PATH);


    /** Termux:Float app terminal.properties primary file path */
    public static final String TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/termux.float.properties"; // Default: "/data/data/com.terminal/files/home/.terminal/terminal.float.properties"
    /** Termux:Float app terminal.properties primary file */
    public static final File TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE = new File(TERMUX_FLOAT_PROPERTIES_PRIMARY_FILE_PATH);

    /** Termux:Float app terminal.properties secondary file path */
    public static final String TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH = TERMUX_CONFIG_HOME_DIR_PATH + "/termux.float.properties"; // Default: "/data/data/com.terminal/files/home/.config/terminal/terminal.float.properties"
    /** Termux:Float app terminal.properties secondary file */
    public static final File TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE = new File(TERMUX_FLOAT_PROPERTIES_SECONDARY_FILE_PATH);


    /** Termux app and Termux:Styling colors.properties file path */
    public static final String TERMUX_COLOR_PROPERTIES_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/colors.properties"; // Default: "/data/data/com.terminal/files/home/.terminal/colors.properties"
    /** Termux app and Termux:Styling colors.properties file */
    public static final File TERMUX_COLOR_PROPERTIES_FILE = new File(TERMUX_COLOR_PROPERTIES_FILE_PATH);

    /** Termux app and Termux:Styling font.ttf file path */
    public static final String TERMUX_FONT_FILE_PATH = TERMUX_DATA_HOME_DIR_PATH + "/font.ttf"; // Default: "/data/data/com.terminal/files/home/.terminal/font.ttf"
    /** Termux app and Termux:Styling font.ttf file */
    public static final File TERMUX_FONT_FILE = new File(TERMUX_FONT_FILE_PATH);


    /** Termux app and plugins crash log file path */
    public static final String TERMUX_CRASH_LOG_FILE_PATH = TERMINAL_HOME_DIR_PATH + "/crash_log.md"; // Default: "/data/data/com.terminal/files/home/crash_log.md"

    /** Termux app and plugins crash log backup file path */
    public static final String TERMUX_CRASH_LOG_BACKUP_FILE_PATH = TERMINAL_HOME_DIR_PATH + "/crash_log_backup.md"; // Default: "/data/data/com.terminal/files/home/crash_log_backup.md"





    /*
     * Termux app plugin specific paths.
     */

    /** Termux app directory path to store scripts to be run at boot by Termux:Boot */
    public static final String TERMUX_BOOT_SCRIPTS_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/boot"; // Default: "/data/data/com.terminal/files/home/.terminal/boot"
    /** Termux app directory to store scripts to be run at boot by Termux:Boot */
    public static final File TERMUX_BOOT_SCRIPTS_DIR = new File(TERMUX_BOOT_SCRIPTS_DIR_PATH);


    /** Termux app directory path to store foreground scripts that can be run by the terminal launcher
     * widget provided by Termux:Widget */
    public static final String TERMUX_SHORTCUT_SCRIPTS_DIR_PATH = TERMINAL_HOME_DIR_PATH + "/.shortcuts"; // Default: "/data/data/com.terminal/files/home/.shortcuts"
    /** Termux app directory to store foreground scripts that can be run by the terminal launcher widget provided by Termux:Widget */
    public static final File TERMUX_SHORTCUT_SCRIPTS_DIR = new File(TERMUX_SHORTCUT_SCRIPTS_DIR_PATH);


    /** Termux app directory basename that stores background scripts that can be run by the terminal
     * launcher widget provided by Termux:Widget */
    public static final String TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME =  "tasks"; // Default: "tasks"
    /** Termux app directory path to store background scripts that can be run by the terminal launcher
     * widget provided by Termux:Widget */
    public static final String TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH = TERMUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_BASENAME; // Default: "/data/data/com.terminal/files/home/.shortcuts/tasks"
    /** Termux app directory to store background scripts that can be run by the terminal launcher widget provided by Termux:Widget */
    public static final File TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR = new File(TERMUX_SHORTCUT_TASKS_SCRIPTS_DIR_PATH);


    /** Termux app directory basename that stores icons for the foreground and background scripts
     * that can be run by the terminal launcher widget provided by Termux:Widget */
    public static final String TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME =  "icons"; // Default: "icons"
    /** Termux app directory path to store icons for the foreground and background scripts that can
     * be run by the terminal launcher widget provided by Termux:Widget */
    public static final String TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH = TERMUX_SHORTCUT_SCRIPTS_DIR_PATH + "/" + TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_BASENAME; // Default: "/data/data/com.terminal/files/home/.shortcuts/icons"
    /** Termux app directory to store icons for the foreground and background scripts that can be
     * run by the terminal launcher widget provided by Termux:Widget */
    public static final File TERMUX_SHORTCUT_SCRIPT_ICONS_DIR = new File(TERMUX_SHORTCUT_SCRIPT_ICONS_DIR_PATH);


    /** Termux app directory path to store scripts to be run by 3rd party twofortyfouram locale plugin
     * host apps like Tasker app via the Termux:Tasker plugin client */
    public static final String TERMUX_TASKER_SCRIPTS_DIR_PATH = TERMUX_DATA_HOME_DIR_PATH + "/tasker"; // Default: "/data/data/com.terminal/files/home/.terminal/tasker"
    /** Termux app directory to store scripts to be run by 3rd party twofortyfouram locale plugin host apps like Tasker app via the Termux:Tasker plugin client */
    public static final File TERMUX_TASKER_SCRIPTS_DIR = new File(TERMUX_TASKER_SCRIPTS_DIR_PATH);





    /*
     * Termux app and plugins notification variables.
     */

    /** Termux app notification channel id used by {@link TERMUX_APP.TERMUX_SERVICE} */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_ID = "hermes_service_channel";
    /** Termux app notification channel name used by {@link TERMUX_APP.TERMUX_SERVICE} */
    public static final String TERMUX_APP_NOTIFICATION_CHANNEL_NAME = "Hermes Comms";
    /** Termux app unique notification id used by {@link TERMUX_APP.TERMUX_SERVICE} */
    public static final int TERMUX_APP_NOTIFICATION_ID = 1;

    /** Termux app notification channel id used by {@link TERMUX_APP.RUN_COMMAND_SERVICE} */
    public static final String TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_ID = "termux_run_command_notification_channel";
    /** Termux app notification channel name used by {@link TERMUX_APP.RUN_COMMAND_SERVICE} */
    public static final String TERMUX_RUN_COMMAND_NOTIFICATION_CHANNEL_NAME = TerminalConstants.TERMINAL_APP_NAME + " RunCommandService";
    /** Termux app unique notification id used by {@link TERMUX_APP.RUN_COMMAND_SERVICE} */
    public static final int TERMUX_RUN_COMMAND_NOTIFICATION_ID = 1338;

    /** Termux app notification channel id used for plugin command errors */
    public static final String TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_ID = "termux_plugin_command_errors_notification_channel";
    /** Termux app notification channel name used for plugin command errors */
    public static final String TERMUX_PLUGIN_COMMAND_ERRORS_NOTIFICATION_CHANNEL_NAME = TerminalConstants.TERMINAL_APP_NAME + " Plugin Commands Errors";

    /** Termux app notification channel id used for crash reports */
    public static final String TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_ID = "termux_crash_reports_notification_channel";
    /** Termux app notification channel name used for crash reports */
    public static final String TERMUX_CRASH_REPORTS_NOTIFICATION_CHANNEL_NAME = TerminalConstants.TERMINAL_APP_NAME + " Crash Reports";


    /** Termux app notification channel id used by {@link TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE} */
    public static final String TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID = "termux_float_notification_channel";
    /** Termux app notification channel name used by {@link TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE} */
    public static final String TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME = TerminalConstants.TERMUX_FLOAT_APP_NAME + " App";
    /** Termux app unique notification id used by {@link TERMUX_APP.TERMUX_SERVICE} */
    public static final int TERMUX_FLOAT_APP_NOTIFICATION_ID = 1339;





    /*
     * Termux app and plugins miscellaneous variables.
     */

    /** Android OS permission declared by Termux app in AndroidManifest.xml which can be requested by
     * 3rd party apps to run various commands in Termux app context */
    public static final String PERMISSION_RUN_COMMAND = TERMINAL_PACKAGE_NAME + ".permission.RUN_COMMAND"; // Default: "com.terminal.permission.RUN_COMMAND"

    /** Termux property defined in terminal.properties file as a secondary check to PERMISSION_RUN_COMMAND
     * to allow 3rd party apps to run various commands in Termux app context */
    public static final String PROP_ALLOW_EXTERNAL_APPS = "allow-external-apps"; // Default: "allow-external-apps"
    /** Default value for {@link #PROP_ALLOW_EXTERNAL_APPS} */
    public static final String PROP_DEFAULT_VALUE_ALLOW_EXTERNAL_APPS = "false"; // Default: "false"

    /** The broadcast action sent when Termux App opens */
    public static final String BROADCAST_TERMUX_OPENED = TERMINAL_PACKAGE_NAME + ".app.OPENED";

    /** The Uri authority for Termux app file shares */
    public static final String TERMUX_FILE_SHARE_URI_AUTHORITY = TERMINAL_PACKAGE_NAME + ".files"; // Default: "com.terminal.files"

    /** The normal comma character (U+002C, &comma;, &#44;, comma) */
    public static final String COMMA_NORMAL = ","; // Default: ","

    /** The alternate comma character (U+201A, &sbquo;, &#8218;, single low-9 quotation mark) that
     * may be used instead of {@link #COMMA_NORMAL} */
    public static final String COMMA_ALTERNATIVE = "‚"; // Default: "‚"






    /**
     * Termux app constants.
     */
    public static final class TERMUX_APP {

        /** Termux app core activity name. */
        public static final String TERMUX_ACTIVITY_NAME = TERMINAL_PACKAGE_NAME + ".app.TermuxActivity"; // Default: "com.terminal.app.TermuxActivity"

        /**
         * Termux app core activity.
         */
        public static final class TERMUX_ACTIVITY {

            /** Intent extra for if terminal failsafe session needs to be started and is used by {@link TERMUX_ACTIVITY} and {@link TERMUX_SERVICE#ACTION_STOP_SERVICE} */
            public static final String EXTRA_FAILSAFE_SESSION = TerminalConstants.TERMINAL_PACKAGE_NAME + ".app.failsafe_session"; // Default: "com.terminal.app.failsafe_session"


            /** Intent action to make terminal request storage permissions */
            public static final String ACTION_REQUEST_PERMISSIONS = TerminalConstants.TERMINAL_PACKAGE_NAME + ".app.request_storage_permissions"; // Default: "com.terminal.app.request_storage_permissions"

            /** Intent action to make terminal reload its terminal session styling */
            public static final String ACTION_RELOAD_STYLE = TerminalConstants.TERMINAL_PACKAGE_NAME + ".app.reload_style"; // Default: "com.terminal.app.reload_style"
            /** Intent {@code String} extra for what to reload for the TERMUX_ACTIVITY.ACTION_RELOAD_STYLE intent. This has been deperecated. */
            @Deprecated
            public static final String EXTRA_RELOAD_STYLE = TerminalConstants.TERMINAL_PACKAGE_NAME + ".app.reload_style"; // Default: "com.terminal.app.reload_style"

        }





        /** Termux app settings activity name. */
        public static final String TERMUX_SETTINGS_ACTIVITY_NAME = TERMINAL_PACKAGE_NAME + ".app.activities.SettingsActivity"; // Default: "com.terminal.app.activities.SettingsActivity"





        /** Termux app core service name. */
        public static final String TERMUX_SERVICE_NAME = TERMINAL_PACKAGE_NAME + ".app.EndlessService"; // Default: "com.terminal.app.EndlessService"

        /**
         * Termux app core service.
         */
        public static final class TERMUX_SERVICE {

            /** Intent action to stop TERMUX_SERVICE */
            public static final String ACTION_STOP_SERVICE = TERMINAL_PACKAGE_NAME + ".service_stop"; // Default: "com.terminal.service_stop"


            /** Intent action to make TERMUX_SERVICE acquire a wakelock */
            public static final String ACTION_WAKE_LOCK = TERMINAL_PACKAGE_NAME + ".service_wake_lock"; // Default: "com.terminal.service_wake_lock"


            /** Intent action to make TERMUX_SERVICE release wakelock */
            public static final String ACTION_WAKE_UNLOCK = TERMINAL_PACKAGE_NAME + ".service_wake_unlock"; // Default: "com.terminal.service_wake_unlock"


            /** Intent action to execute command with TERMUX_SERVICE */
            public static final String ACTION_SERVICE_EXECUTE = TERMINAL_PACKAGE_NAME + ".service_execute"; // Default: "com.terminal.service_execute"

            /** Uri scheme for paths sent via intent to TERMUX_SERVICE */
            public static final String URI_SCHEME_SERVICE_EXECUTE = TERMINAL_PACKAGE_NAME + ".file"; // Default: "com.terminal.file"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_ARGUMENTS = TERMINAL_PACKAGE_NAME + ".execute.arguments"; // Default: "com.terminal.execute.arguments"
            /** Intent {@code String} extra for stdin of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_STDIN = TERMINAL_PACKAGE_NAME + ".execute.stdin"; // Default: "com.terminal.execute.stdin"
            /** Intent {@code String} extra for command current working directory for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_WORKDIR = TERMINAL_PACKAGE_NAME + ".execute.cwd"; // Default: "com.terminal.execute.cwd"
            /** Intent {@code boolean} extra for command background mode for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_BACKGROUND = TERMINAL_PACKAGE_NAME + ".execute.background"; // Default: "com.terminal.execute.background"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.flomobility.anx.shared.logger.Logger} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = TERMINAL_PACKAGE_NAME + ".execute.background_custom_log_level"; // Default: "com.terminal.execute.background_custom_log_level"
            /** Intent {@code String} extra for session action for foreground commands for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_SESSION_ACTION = TERMINAL_PACKAGE_NAME + ".execute.session_action"; // Default: "com.terminal.execute.session_action"
            /** Intent {@code String} extra for label of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_LABEL = TERMINAL_PACKAGE_NAME + ".execute.command_label"; // Default: "com.terminal.execute.command_label"
            /** Intent markdown {@code String} extra for description of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = TERMINAL_PACKAGE_NAME + ".execute.command_description"; // Default: "com.terminal.execute.command_description"
            /** Intent markdown {@code String} extra for help of the command for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_COMMAND_HELP = TERMINAL_PACKAGE_NAME + ".execute.command_help"; // Default: "com.terminal.execute.command_help"
            /** Intent markdown {@code String} extra for help of the plugin API for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent (Internal Use Only) */
            public static final String EXTRA_PLUGIN_API_HELP = TERMINAL_PACKAGE_NAME + ".execute.plugin_api_help"; // Default: "com.terminal.execute.plugin_help"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the
             * result of the execution command to the execute command caller for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_PENDING_INTENT = "pendingIntent"; // Default: "pendingIntent"
            /** Intent {@code String} extra for the directory path in which to write the result of the
             * execution command for the execute command caller for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_DIRECTORY = TERMINAL_PACKAGE_NAME + ".execute.result_directory"; // Default: "com.terminal.execute.result_directory"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = TERMINAL_PACKAGE_NAME + ".execute.result_single_file"; // Default: "com.terminal.execute.result_single_file"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = TERMINAL_PACKAGE_NAME + ".execute.result_file_basename"; // Default: "com.terminal.execute.result_file_basename"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = TERMINAL_PACKAGE_NAME + ".execute.result_file_output_format"; // Default: "com.terminal.execute.result_file_output_format"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = TERMINAL_PACKAGE_NAME + ".execute.result_file_error_format"; // Default: "com.terminal.execute.result_file_error_format"
            /** Intent {@code String} extra for the optional suffix of the result files that should
             * be created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = TERMINAL_PACKAGE_NAME + ".execute.result_files_suffix"; // Default: "com.terminal.execute.result_files_suffix"



            /** The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session and will start {@link TERMUX_ACTIVITY} if its not running to bring
             * the new session to foreground.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY = 0;

            /** The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session and will start {@link TERMUX_ACTIVITY} if its not running to
             * bring the existing session to foreground. The new session will be added to the left
             * sidebar in the sessions list.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY = 1;

            /** The value for {@link #EXTRA_SESSION_ACTION} extra that will set the new session as
             * the current session but will not start {@link TERMUX_ACTIVITY} if its not running
             * and session(s) will be seen in Termux notification and can be clicked to bring new
             * session to foreground. If the {@link TERMUX_ACTIVITY} is already running, then this
             * will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_DONT_OPEN_ACTIVITY = 2;

            /** The value for {@link #EXTRA_SESSION_ACTION} extra that will keep any existing session
             * as the current session but will not start {@link TERMUX_ACTIVITY} if its not running
             * and session(s) will be seen in Termux notification and can be clicked to bring
             * existing session to foreground. If the {@link TERMUX_ACTIVITY} is already running,
             * then this will behave like {@link #VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_OPEN_ACTIVITY}.
             */
            public static final int VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY = 3;

            /** The minimum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MIN_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_SWITCH_TO_NEW_SESSION_AND_OPEN_ACTIVITY;

            /** The maximum allowed value for {@link #EXTRA_SESSION_ACTION}. */
            public static final int MAX_VALUE_EXTRA_SESSION_ACTION = VALUE_EXTRA_SESSION_ACTION_KEEP_CURRENT_SESSION_AND_DONT_OPEN_ACTIVITY;


            /** Intent {@code Bundle} extra to store result of execute command that is sent back for the
             * TERMUX_SERVICE.ACTION_SERVICE_EXECUTE intent if the {@link #EXTRA_PENDING_INTENT} is not
             * {@code null} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE = "result"; // Default: "result"
            /** Intent {@code String} extra for stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"; // Default: "stdout"
            /** Intent {@code String} extra for original length of stdout value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT_ORIGINAL_LENGTH = "stdout_original_length"; // Default: "stdout_original_length"
            /** Intent {@code String} extra for stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"; // Default: "stderr"
            /** Intent {@code String} extra for original length of stderr value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_STDERR_ORIGINAL_LENGTH = "stderr_original_length"; // Default: "stderr_original_length"
            /** Intent {@code int} extra for exit code value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"; // Default: "exitCode"
            /** Intent {@code int} extra for err value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"; // Default: "err"
            /** Intent {@code String} extra for errmsg value of execute command of the {@link #EXTRA_PLUGIN_RESULT_BUNDLE} */
            public static final String EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"; // Default: "errmsg"

        }





        /** Termux app run command service name. */
        public static final String RUN_COMMAND_SERVICE_NAME = TERMINAL_PACKAGE_NAME + ".app.RunCommandService"; // Termux app service to receive commands from 3rd party apps "com.terminal.app.RunCommandService"

        /**
         * Termux app run command service to receive commands sent by 3rd party apps.
         */
        public static final class RUN_COMMAND_SERVICE {

            /** Termux RUN_COMMAND Intent help url */
            public static final String RUN_COMMAND_API_HELP_URL = TERMUX_GITHUB_WIKI_REPO_URL + "/RUN_COMMAND-Intent"; // Default: "https://github.com/terminal/terminal-app/wiki/RUN_COMMAND-Intent"


            /** Intent action to execute command with RUN_COMMAND_SERVICE */
            public static final String ACTION_RUN_COMMAND = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND"; // Default: "com.terminal.RUN_COMMAND"

            /** Intent {@code String} extra for absolute path of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_PATH = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_PATH"; // Default: "com.terminal.RUN_COMMAND_PATH"
            /** Intent {@code String[]} extra for arguments to the executable of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_ARGUMENTS = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_ARGUMENTS"; // Default: "com.terminal.RUN_COMMAND_ARGUMENTS"
            /** Intent {@code boolean} extra for whether to replace comma alternative characters in arguments with comma characters for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.terminal.RUN_COMMAND_REPLACE_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"
            /** Intent {@code String} extra for the comma alternative characters in arguments that should be replaced instead of the default {@link #COMMA_ALTERNATIVE} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"; // Default: "com.terminal.RUN_COMMAND_COMMA_ALTERNATIVE_CHARS_IN_ARGUMENTS"

            /** Intent {@code String} extra for stdin of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_STDIN = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_STDIN"; // Default: "com.terminal.RUN_COMMAND_STDIN"
            /** Intent {@code String} extra for current working directory of command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_WORKDIR = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_WORKDIR"; // Default: "com.terminal.RUN_COMMAND_WORKDIR"
            /** Intent {@code boolean} extra for whether to run command in background or foreground terminal session for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_BACKGROUND = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND"; // Default: "com.terminal.RUN_COMMAND_BACKGROUND"
            /** Intent {@code String} extra for custom log level for background commands defined by {@link com.flomobility.anx.shared.logger.Logger} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_BACKGROUND_CUSTOM_LOG_LEVEL = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"; // Default: "com.terminal.RUN_COMMAND_BACKGROUND_CUSTOM_LOG_LEVEL"
            /** Intent {@code String} extra for session action of foreground commands for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_SESSION_ACTION = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_SESSION_ACTION"; // Default: "com.terminal.RUN_COMMAND_SESSION_ACTION"
            /** Intent {@code String} extra for label of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_LABEL = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_LABEL"; // Default: "com.terminal.RUN_COMMAND_COMMAND_LABEL"
            /** Intent markdown {@code String} extra for description of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_DESCRIPTION = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_DESCRIPTION"; // Default: "com.terminal.RUN_COMMAND_COMMAND_DESCRIPTION"
            /** Intent markdown {@code String} extra for help of the command for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_COMMAND_HELP = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_COMMAND_HELP"; // Default: "com.terminal.RUN_COMMAND_COMMAND_HELP"
            /** Intent {@code Parcelable} extra for the pending intent that should be sent with the result of the execution command to the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_PENDING_INTENT = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_PENDING_INTENT"; // Default: "com.terminal.RUN_COMMAND_PENDING_INTENT"
            /** Intent {@code String} extra for the directory path in which to write the result of
             * the execution command for the execute command caller for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_DIRECTORY = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_RESULT_DIRECTORY"; // Default: "com.terminal.RUN_COMMAND_RESULT_DIRECTORY"
            /** Intent {@code boolean} extra for whether the result should be written to a single file
             * or multiple files (err, errmsg, stdout, stderr, exit_code) in
             * {@link #EXTRA_RESULT_DIRECTORY} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_SINGLE_FILE = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_RESULT_SINGLE_FILE"; // Default: "com.terminal.RUN_COMMAND_RESULT_SINGLE_FILE"
            /** Intent {@code String} extra for the basename of the result file that should be created
             * in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is {@code true}
             * for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_BASENAME = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_BASENAME"; // Default: "com.terminal.RUN_COMMAND_RESULT_FILE_BASENAME"
            /** Intent {@code String} extra for the output {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_OUTPUT_FORMAT = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"; // Default: "com.terminal.RUN_COMMAND_RESULT_FILE_OUTPUT_FORMAT"
            /** Intent {@code String} extra for the error {@link Formatter} format of the
             * {@link #EXTRA_RESULT_FILE_BASENAME} result file for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILE_ERROR_FORMAT = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"; // Default: "com.terminal.RUN_COMMAND_RESULT_FILE_ERROR_FORMAT"
            /** Intent {@code String} extra for the optional suffix of the result files that should be
             * created in {@link #EXTRA_RESULT_DIRECTORY} if {@link #EXTRA_RESULT_SINGLE_FILE} is
             * {@code false} for the RUN_COMMAND_SERVICE.ACTION_RUN_COMMAND intent */
            public static final String EXTRA_RESULT_FILES_SUFFIX = TERMINAL_PACKAGE_NAME + ".RUN_COMMAND_RESULT_FILES_SUFFIX"; // Default: "com.terminal.RUN_COMMAND_RESULT_FILES_SUFFIX"

        }
    }





    /**
     * Termux class to send back results of commands to their callers like plugin or 3rd party apps.
     */
    public static final class RESULT_SENDER {

        /*
         * The default `Formatter` format strings to use for `ResultConfig#resultFileBasename`
         * if `ResultConfig#resultSingleFile` is `true`.
         */

        /** The {@link Formatter} format string for success if only `stdout` needs to be written to
         * {@link ResultConfig#resultFileBasename} where `stdout` maps to `%1$s`.
         * This is used when `err` equals {@link Errno#ERRNO_SUCCESS} (-1) and `stderr` is empty
         * and `exit_code` equals `0` and {@link ResultConfig#resultFileOutputFormat} is not passed. */
        public static final String FORMAT_SUCCESS_STDOUT = "%1$s%n";
        /** The {@link Formatter} format string for success if `stdout` and `exit_code` need to be written to
         * {@link ResultConfig#resultFileBasename} where `stdout` maps to `%1$s` and `exit_code` to `%2$s`.
         * This is used when `err` equals {@link Errno#ERRNO_SUCCESS} (-1) and `stderr` is empty
         * and `exit_code` does not equal `0` and {@link ResultConfig#resultFileOutputFormat} is not passed.
         * The exit code will be placed in a markdown inline code. */
        public static final String FORMAT_SUCCESS_STDOUT__EXIT_CODE = "%1$s%n%n%n%nexit_code=%2$s%n";
        /** The {@link Formatter} format string for success if `stdout`, `stderr` and `exit_code` need to be
         * written to {@link ResultConfig#resultFileBasename} where `stdout` maps to `%1$s`, `stderr`
         * maps to `%2$s` and `exit_code` to `%3$s`.
         * This is used when `err` equals {@link Errno#ERRNO_SUCCESS} (-1) and `stderr` is not empty
         * and {@link ResultConfig#resultFileOutputFormat} is not passed.
         * The stdout and stderr will be placed in a markdown code block. The exit code will be placed
         * in a markdown inline code. The surrounding backticks will be 3 more than the consecutive
         * backticks in any parameter itself for code blocks. */
        public static final String FORMAT_SUCCESS_STDOUT__STDERR__EXIT_CODE = "stdout=%n%1$s%n%n%n%nstderr=%n%2$s%n%n%n%nexit_code=%3$s%n";
        /** The {@link Formatter} format string for failure if `err`, `errmsg`(`error`), `stdout`,
         * `stderr` and `exit_code` need to be written to {@link ResultConfig#resultFileBasename} where
         * `err` maps to `%1$s`, `errmsg` maps to `%2$s`, `stdout` maps
         * to `%3$s`, `stderr` to `%4$s` and `exit_code` maps to `%5$s`.
         * Do not define an argument greater than `5`, like `%6$s` if you change this value since it will
         * raise {@link IllegalFormatException}.
         * This is used when `err` does not equal {@link Errno#ERRNO_SUCCESS} (-1) and
         * {@link ResultConfig#resultFileErrorFormat} is not passed.
         * The errmsg, stdout and stderr will be placed in a markdown code block. The err and exit code
         * will be placed in a markdown inline code. The surrounding backticks will be 3 more than
         * the consecutive backticks in any parameter itself for code blocks. The stdout, stderr
         * and exit code may be empty without any surrounding backticks if not set. */
        public static final String FORMAT_FAILED_ERR__ERRMSG__STDOUT__STDERR__EXIT_CODE = "err=%1$s%n%n%n%nerrmsg=%n%2$s%n%n%n%nstdout=%n%3$s%n%n%n%nstderr=%n%4$s%n%n%n%nexit_code=%5$s%n";



        /*
         * The default prefixes to use for result files under `ResultConfig#resultDirectoryPath`
         * if `ResultConfig#resultSingleFile` is `false`.
         */

        /** The prefix for the err result file. */
        public static final String RESULT_FILE_ERR_PREFIX = "err";
        /** The prefix for the errmsg result file. */
        public static final String RESULT_FILE_ERRMSG_PREFIX = "errmsg";
        /** The prefix for the stdout result file. */
        public static final String RESULT_FILE_STDOUT_PREFIX = "stdout";
        /** The prefix for the stderr result file. */
        public static final String RESULT_FILE_STDERR_PREFIX = "stderr";
        /** The prefix for the exitCode result file. */
        public static final String RESULT_FILE_EXIT_CODE_PREFIX = "exit_code";

    }





    /**
     * Termux:API app constants.
     */
    public static final class TERMUX_API {

        /** Termux:API app core activity name. */
        public static final String TERMUX_API_ACTIVITY_NAME = TERMUX_API_PACKAGE_NAME + ".activities.TermuxAPIActivity"; // Default: "com.terminal.tasker.activities.TermuxAPIActivity"

    }





    /**
     * Termux:Float app constants.
     */
    public static final class TERMUX_FLOAT_APP {

        /** Termux:Float app core service name. */
        public static final String TERMUX_FLOAT_SERVICE_NAME = TERMUX_FLOAT_PACKAGE_NAME + ".TermuxFloatService"; // Default: "com.terminal.window.TermuxFloatService"

        /**
         * Termux:Float app core service.
         */
        public static final class TERMUX_FLOAT_SERVICE {

            /** Intent action to stop TERMUX_FLOAT_SERVICE. */
            public static final String ACTION_STOP_SERVICE = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_STOP_SERVICE"; // Default: "com.terminal.float.ACTION_STOP_SERVICE"

            /** Intent action to show float window. */
            public static final String ACTION_SHOW = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_SHOW"; // Default: "com.terminal.float.ACTION_SHOW"

            /** Intent action to hide float window. */
            public static final String ACTION_HIDE = TERMUX_FLOAT_PACKAGE_NAME + ".ACTION_HIDE"; // Default: "com.terminal.float.ACTION_HIDE"

        }

    }





    /**
     * Termux:Styling app constants.
     */
    public static final class TERMUX_STYLING {

        /** Termux:Styling app core activity name. */
        public static final String TERMUX_STYLING_ACTIVITY_NAME = TERMUX_STYLING_PACKAGE_NAME + ".TermuxStyleActivity"; // Default: "com.terminal.styling.TermuxStyleActivity"

    }





    /**
     * Termux:Tasker app constants.
     */
    public static final class TERMUX_TASKER {

        /** Termux:Tasker app core activity name. */
        public static final String TERMUX_TASKER_ACTIVITY_NAME = TERMUX_TASKER_PACKAGE_NAME + ".activities.TermuxTaskerActivity"; // Default: "com.terminal.tasker.activities.TermuxTaskerActivity"

    }





    /**
     * Termux:Widget app constants.
     */
    public static final class TERMUX_WIDGET {

        /** Termux:Widget app core activity name. */
        public static final String TERMUX_WIDGET_ACTIVITY_NAME = TERMUX_WIDGET_PACKAGE_NAME + ".activities.TermuxWidgetActivity"; // Default: "com.terminal.widget.activities.TermuxWidgetActivity"


        /**  Intent {@code String} extra for the token of the Termux:Widget app shortcuts. */
        public static final String EXTRA_TOKEN_NAME = TERMINAL_PACKAGE_NAME + ".shortcut.token"; // Default: "com.terminal.shortcut.token"

        /**
         * Termux:Widget app {@link android.appwidget.AppWidgetProvider} class.
         */
        public static final class TERMUX_WIDGET_PROVIDER {

            /** Intent action for if an item is clicked in the widget. */
            public static final String ACTION_WIDGET_ITEM_CLICKED = TERMUX_WIDGET_PACKAGE_NAME + ".ACTION_WIDGET_ITEM_CLICKED"; // Default: "com.terminal.widget.ACTION_WIDGET_ITEM_CLICKED"


            /** Intent action to refresh files in the widget. */
            public static final String ACTION_REFRESH_WIDGET = TERMUX_WIDGET_PACKAGE_NAME + ".ACTION_REFRESH_WIDGET"; // Default: "com.terminal.widget.ACTION_REFRESH_WIDGET"


            /**  Intent {@code String} extra for the file clicked for the TERMUX_WIDGET_PROVIDER.ACTION_WIDGET_ITEM_CLICKED intent. */
            public static final String EXTRA_FILE_CLICKED = TERMUX_WIDGET_PACKAGE_NAME + ".EXTRA_FILE_CLICKED"; // Default: "com.terminal.widget.EXTRA_FILE_CLICKED"

        }

    }

}
