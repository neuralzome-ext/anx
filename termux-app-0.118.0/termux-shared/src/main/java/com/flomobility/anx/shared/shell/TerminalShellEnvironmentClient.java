package com.flomobility.anx.shared.shell;

import android.content.Context;

import androidx.annotation.NonNull;

public class TerminalShellEnvironmentClient implements ShellEnvironmentClient {

    @NonNull
    @Override
    public String getDefaultWorkingDirectoryPath() {
        return TerminalShellUtils.getDefaultWorkingDirectoryPath();
    }

    @NonNull
    @Override
    public String getDefaultBinPath() {
        return TerminalShellUtils.getDefaultBinPath();
    }

    @NonNull
    @Override
    public String[] buildEnvironment(Context currentPackageContext, boolean isFailSafe, String workingDirectory) {
        return TerminalShellUtils.buildEnvironment(currentPackageContext, isFailSafe, workingDirectory);
    }

    @NonNull
    @Override
    public String[] setupProcessArgs(@NonNull String fileToExecute, String[] arguments) {
        return TerminalShellUtils.setupProcessArgs(fileToExecute, arguments);
    }

}
