package com.flomobility.anx.app.terminal.io;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.drawerlayout.widget.DrawerLayout;

import com.flomobility.anx.app.terminal.FloTerminalSessionClient;
import com.flomobility.anx.app.terminal.FloTerminalViewClient;
import com.flomobility.anx.shared.terminal.io.TerminalExtraKeys;
import com.flomobility.anx.view.TerminalView;

public class TermuxTerminalExtraKeys extends TerminalExtraKeys {


    FloTerminalViewClient mFloTerminalViewClient;
    FloTerminalSessionClient mFloTerminalSessionClient;

    public TermuxTerminalExtraKeys(@NonNull TerminalView terminalView,
                                   FloTerminalViewClient floTerminalViewClient,
                                   FloTerminalSessionClient floTerminalSessionClient) {
        super(terminalView);
        mFloTerminalViewClient = floTerminalViewClient;
        mFloTerminalSessionClient = floTerminalSessionClient;
    }

    @SuppressLint("RtlHardcoded")
    @Override
    public void onTerminalExtraKeyButtonClick(View view, String key, boolean ctrlDown, boolean altDown, boolean shiftDown, boolean fnDown) {
        if ("KEYBOARD".equals(key)) {
            if(mFloTerminalViewClient != null)
                mFloTerminalViewClient.onToggleSoftKeyboardRequest();
        } else if ("DRAWER".equals(key)) {
            DrawerLayout drawerLayout = mFloTerminalViewClient.getActivity().getDrawer();
            if (drawerLayout.isDrawerOpen(Gravity.LEFT))
                drawerLayout.closeDrawer(Gravity.LEFT);
            else
                drawerLayout.openDrawer(Gravity.LEFT);
        } else if ("PASTE".equals(key)) {
            if(mFloTerminalSessionClient != null)
                mFloTerminalSessionClient.onPasteTextFromClipboard(null);
        } else {
            super.onTerminalExtraKeyButtonClick(view, key, ctrlDown, altDown, shiftDown, fnDown);
        }
    }

}
